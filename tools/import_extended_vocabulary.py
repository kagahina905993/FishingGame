#!/usr/bin/env python3
"""Add openly licensed CEFR-tagged vocabulary to words.json.

The importer is deterministic and rebuilds only OPEN_VOCAB_EXTENDED_V1 rows.
It combines WortUniversum frequency/POS metadata, the CEFR-J and Octanove
profiles, and CC0 Japanese definitions from EJDict-hand.  It deliberately
skips entries whose short Japanese prompt cannot be made unambiguous.
"""

import argparse
import csv
import gzip
import json
import re
import sqlite3
import xml.etree.ElementTree as ET
from collections import Counter, defaultdict
from pathlib import Path


SOURCE_CODE = "OPEN_VOCAB_EXTENDED_V1"
STRICT_REVIEWED_TOTAL = 6722
ENGLISH_WORD = re.compile(r"^[a-z]+$")
JAPANESE_TEXT = re.compile(r"[ぁ-んァ-ヶ一-龯]")
BRACKET_LABEL = re.compile(r"《[^》]*》|〈[^〉]*〉|\([^)]*\)|（[^）]*）")
EMPHASIZED = re.compile(r"『([^』]+)』")

POS_NAMES = {
    "noun": "名詞",
    "verb": "動詞",
    "adjective": "形容詞",
    "adverb": "副詞",
    "preposition": "前置詞",
    "pronoun": "代名詞",
    "conjunction": "接続詞",
}

WORDNET_POS = {
    "noun": {"n"},
    "verb": {"v"},
    "adjective": {"a", "s"},
    "adverb": {"r"},
}

# The source database is general-purpose. These entries are inappropriate for
# an all-ages study game even when they have valid linguistic metadata.
BLOCKED_WORDS = {
    "arse", "ass", "asshole", "bastard", "bitch", "bullshit", "cocksucker",
    "cunt", "damn", "dick", "fuck", "fucker", "fucking", "motherfucker",
    "nigger", "piss", "porn", "pornography", "shit", "slut", "whore",
    "hooker", "john", "sexy",
}

# These otherwise well-sourced headwords are excluded because their generated
# prompt would be unfair in a Japanese-to-English quiz. ``draught`` overlaps
# heavily with ``draft`` in meaning/spelling, ``ling`` selected a rare plant
# sense instead of the dictionary's first fish sense, and ``offence``/``vigor``
# duplicate American/British spelling variants already in the dataset.
QUIZ_QUALITY_EXCLUSIONS = {"draught", "ling", "offence", "vigor"}

# WordNet lemmas can be terse or duplicated. Keep the meaning within the
# concepts confirmed by both Japanese WordNet and EJDict, but present it as
# natural Japanese for the actual quiz prompt.
QUIZ_MEANING_OVERRIDES = {
    "distinguished": "著名な；優秀な",
    "fighting": "戦闘",
    "gang": "集団；一味",
    "hay": "干し草",
    "hunting": "狩猟",
    "landing": "着陸；上陸",
    "learning": "学習；学問",
    "marketing": "マーケティング；販売",
    "mogul": "大立者；有力者",
    "outskirts": "郊外；周辺",
    "related": "関係のある",
    "remains": "残り；遺体",
    "rite": "宗教的な儀式",
    "scooter": "スクーター",
    "shipping": "発送；運送",
    "spectacle": "光景；見せ物",
    "suffering": "苦痛；苦悩",
    "vigour": "精",
}

# CEFR-J A1 headwords that were absent from the strict WordNet intersection.
# Each row was checked manually against the pinned CEFR-J and EJDict-hand
# inputs. Keep this list small: inflected auxiliaries, proper calendar names,
# number words, spelling variants, and prompts that would be ambiguous in a
# Japanese-to-English quiz are intentionally excluded.
CURATED_A1_FOUNDATION_ADDITIONS = (
    {"english": "baseball", "quizMeaning": "野球", "quizPartOfSpeech": "名詞", "eiken": "5"},
    {"english": "bee", "quizMeaning": "はち", "quizPartOfSpeech": "名詞", "eiken": "5"},
    {"english": "grandpa", "quizMeaning": "おじいちゃん", "quizPartOfSpeech": "名詞", "eiken": "5"},
    {"english": "grape", "quizMeaning": "ぶどう", "quizPartOfSpeech": "名詞", "eiken": "5"},
    {"english": "thanks", "quizMeaning": "感謝の言葉", "quizPartOfSpeech": "名詞", "eiken": "5"},
    {"english": "vase", "quizMeaning": "花びん", "quizPartOfSpeech": "名詞", "eiken": "5"},
    {"english": "cartoon", "quizMeaning": "漫画；アニメ", "quizPartOfSpeech": "名詞", "eiken": "4"},
    {"english": "haircut", "quizMeaning": "散髪；髪型", "quizPartOfSpeech": "名詞", "eiken": "4"},
    {"english": "hometown", "quizMeaning": "故郷の町", "quizPartOfSpeech": "名詞", "eiken": "4"},
    {"english": "rainy", "quizMeaning": "雨の；雨模様の", "quizPartOfSpeech": "形容詞", "eiken": "4"},
    {"english": "surf", "quizMeaning": "岸に寄せる波", "quizPartOfSpeech": "名詞", "eiken": "4"},
    {"english": "trousers", "quizMeaning": "ズボン", "quizPartOfSpeech": "名詞", "eiken": "4"},
    {"english": "awake", "quizMeaning": "目が覚めている", "quizPartOfSpeech": "形容詞", "eiken": "3"},
    {"english": "broken", "quizMeaning": "壊れた；折れた", "quizPartOfSpeech": "形容詞", "eiken": "3"},
    {"english": "excited", "quizMeaning": "興奮した；わくわくした", "quizPartOfSpeech": "形容詞", "eiken": "3"},
    {"english": "exciting", "quizMeaning": "興奮させる；わくわくする", "quizPartOfSpeech": "形容詞", "eiken": "3"},
    {"english": "foggy", "quizMeaning": "霧のかかった", "quizPartOfSpeech": "形容詞", "eiken": "3"},
    {"english": "interested", "quizMeaning": "興味のある", "quizPartOfSpeech": "形容詞", "eiken": "3"},
)

EIKEN_FROM_CEFR = {
    "A1": ("5", "4", "3"),
    "A2": ("pre2", "pre2plus"),
    "B1": ("2",),
    "B2": ("pre1",),
    "C1": ("1",),
    "C2": ("1",),
}

COURSE_METADATA = {
    "5": (1, 5),
    "4": (2, 6),
    "3": (3, 9),
    "pre2": (4, 10),
    "pre2plus": (5, 11),
    "2": (5, 12),
    "pre1": (6, 12),
    "1": (7, 12),
}

# Initial coverage target. These are app-design bands, not official Eiken lists.
FINAL_BAND_TARGETS = {
    "5": 500,
    "4": 600,
    "3": 900,
    "pre2": 1200,
    "pre2plus": 1100,
    "2": 1800,
    "pre1": 2100,
    "1": 1800,
}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--words", type=Path, required=True)
    parser.add_argument("--wort-db", type=Path, required=True)
    parser.add_argument("--cefrj", type=Path, required=True)
    parser.add_argument("--octanove", type=Path, required=True)
    parser.add_argument("--ejdict-src", type=Path, required=True)
    parser.add_argument("--english-wordnet-gz", type=Path, required=True)
    parser.add_argument("--japanese-wordnet", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--target-total", type=int, default=6740)
    return parser.parse_args()


def normalize_headword(raw: str) -> str:
    return re.sub(r"\[[^]]*]$", "", raw).strip().lower()


def load_ejdict(directory: Path) -> dict[str, str]:
    meanings: dict[str, str] = {}
    for path in sorted(directory.glob("*.txt")):
        for line in path.read_text(encoding="utf-8").splitlines():
            if "\t" not in line:
                continue
            headwords, meaning = line.split("\t", 1)
            for raw_headword in headwords.split(", "):
                without_label = re.sub(r"\[[^]]*]$", "", raw_headword).strip()
                headword = normalize_headword(raw_headword)
                if (
                    without_label == without_label.lower()
                    and ENGLISH_WORD.fullmatch(headword)
                    and JAPANESE_TEXT.search(meaning)
                    and headword not in meanings
                ):
                    meanings[headword] = meaning.strip()
    return meanings


def load_profiles(*paths: Path) -> dict[str, list[dict[str, str]]]:
    profiles: dict[str, list[dict[str, str]]] = defaultdict(list)
    for path in paths:
        with path.open(encoding="utf-8-sig", newline="") as source:
            for row in csv.DictReader(source):
                level = row["CEFR"].strip()
                pos = row["pos"].strip().lower()
                if level not in EIKEN_FROM_CEFR or pos not in POS_NAMES:
                    continue
                for raw_headword in row["headword"].split("/"):
                    headword = raw_headword.strip().lower()
                    if ENGLISH_WORD.fullmatch(headword):
                        profiles[headword].append({"cefr": level, "pos": pos})
    return profiles


def load_wort_words(path: Path) -> dict[str, dict[str, object]]:
    result: dict[str, dict[str, object]] = {}
    query = """
        SELECT word, lemma, lower(word_type), grade_level,
               json_extract(frequency_json, '$.zipf'),
               json_extract(frequency_json, '$.per_million'),
               json_extract(metadata_json, '$.cefr_level'),
               json_extract(enrichment_json, '$.primary_lemma'),
               json_extract(enrichment_json, '$.primary_pos')
        FROM words
    """
    with sqlite3.connect(path) as database:
        for (
            raw_word, raw_lemma, pos, grade, zipf, per_million, cefr,
            primary_lemma, primary_pos,
        ) in database.execute(query):
            word = (raw_word or "").lower()
            lemma = (raw_lemma or "").lower()
            if (
                raw_word == word
                and word == lemma
                and ENGLISH_WORD.fullmatch(word or "")
                and pos in POS_NAMES
                and primary_lemma == word
                and primary_pos == pos
            ):
                result[word] = {
                    "pos": pos,
                    "grade": grade,
                    "zipf": float(zipf or 0.0),
                    "perMillion": float(per_million or 0.0),
                    "cefr": cefr if cefr in EIKEN_FROM_CEFR else None,
                }
    return result


def lmf_entries_and_ilis(source) -> tuple[
    dict[tuple[str, str], list[str]], dict[str, str]
]:
    entries: dict[tuple[str, str], list[str]] = defaultdict(list)
    synset_ilis: dict[str, str] = {}
    for _, element in ET.iterparse(source, events=("end",)):
        if element.tag == "LexicalEntry":
            lemma = element.find("Lemma")
            if lemma is not None:
                written = lemma.attrib.get("writtenForm", "")
                pos = lemma.attrib.get("partOfSpeech", "")
                key = (written, pos)
                for sense in element.findall("Sense"):
                    synset = sense.attrib.get("synset")
                    if synset:
                        entries[key].append(synset)
            element.clear()
        elif element.tag == "Synset":
            synset_id = element.attrib.get("id")
            ili = element.attrib.get("ili")
            if synset_id and ili:
                synset_ilis[synset_id] = ili
            element.clear()
    return entries, synset_ilis


def valid_japanese_lemma(text: str) -> bool:
    return (
        0 < len(text) <= 20
        and JAPANESE_TEXT.search(text) is not None
        and re.search(r"[A-Za-z+《》〈〉]", text) is None
    )


def load_wordnet_translations(
    english_gz: Path,
    japanese_xml: Path,
) -> dict[tuple[str, str], list[str]]:
    with gzip.open(english_gz, "rb") as source:
        english_entries, english_ilis = lmf_entries_and_ilis(source)
    with japanese_xml.open("rb") as source:
        japanese_entries, japanese_ilis = lmf_entries_and_ilis(source)

    japanese_by_ili: dict[str, list[str]] = defaultdict(list)
    for (written, _), synsets in japanese_entries.items():
        if not valid_japanese_lemma(written):
            continue
        for synset in synsets:
            ili = japanese_ilis.get(synset)
            if ili and written not in japanese_by_ili[ili]:
                japanese_by_ili[ili].append(written)

    translations: dict[tuple[str, str], list[str]] = defaultdict(list)
    for (written, pos), synsets in english_entries.items():
        english = written.lower()
        if written != english or not ENGLISH_WORD.fullmatch(english):
            continue
        key = (english, pos)
        for synset in synsets:
            ili = english_ilis.get(synset)
            for japanese in japanese_by_ili.get(ili, []):
                if japanese not in translations[key]:
                    translations[key].append(japanese)
    return translations


def wordnet_prompt_candidates(
    english: str,
    source_pos: str,
    translations: dict[tuple[str, str], list[str]],
) -> list[str]:
    result: list[str] = []
    for wordnet_pos in WORDNET_POS.get(source_pos, set()):
        for japanese in translations.get((english, wordnet_pos), []):
            if japanese not in result:
                result.append(japanese)
    if len(result) >= 2:
        combined = f"{result[0]}・{result[1]}"
        if len(combined) <= 40:
            result.append(combined)
    return result


def confirmed_semantic_prompts(
    dictionary_text: str,
    wordnet_prompts: list[str],
) -> list[str]:
    confirmed = [
        prompt for prompt in wordnet_prompts
        if prompt in dictionary_text and len(prompt) <= 40
    ]
    confirmed.sort(key=lambda prompt: (dictionary_text.find(prompt), len(prompt)))
    if len(confirmed) >= 2:
        combined_parts: list[str] = []
        for prompt in confirmed[:3]:
            candidate = "・".join(combined_parts + [prompt])
            if len(candidate) > 40:
                break
            combined_parts.append(prompt)
        if len(combined_parts) >= 2:
            return ["・".join(combined_parts)] + confirmed
    return confirmed


def clean_prompt_piece(text: str) -> str | None:
    cleaned = BRACKET_LABEL.sub("", text)
    cleaned = cleaned.replace("『", "").replace("』", "")
    cleaned = re.sub(r"[()（）《》〈〉\[\]+]", "", cleaned)
    cleaned = re.sub(r"^[・,，;；:/\s]+|[・,，;；:/\s]+$", "", cleaned)
    cleaned = re.sub(r"\s+", " ", cleaned).strip()
    if not cleaned or len(cleaned) > 40 or not JAPANESE_TEXT.search(cleaned):
        return None
    return cleaned


def prompt_candidates(dictionary_text: str) -> list[str]:
    raw_candidates = re.split(r" / |;|；", dictionary_text)
    raw_candidates.extend(re.split(r"[,，]", dictionary_text))
    raw_candidates.extend(EMPHASIZED.findall(dictionary_text))
    result: list[str] = []
    for raw in raw_candidates:
        cleaned = clean_prompt_piece(raw)
        if cleaned and cleaned not in result:
            result.append(cleaned)
    return result


def choose_profile(
    english: str,
    profiles: dict[str, list[dict[str, str]]],
    wort: dict[str, object] | None,
) -> tuple[str, str, str] | None:
    rows = profiles.get(english, [])
    expected_pos = str(wort["pos"]) if wort else None
    pos_rows = [row for row in rows if row["pos"] == expected_pos]
    usable = pos_rows or rows
    if usable:
        order = {level: index for index, level in enumerate(EIKEN_FROM_CEFR)}
        selected = max(usable, key=lambda row: order[row["cefr"]])
        return selected["cefr"], selected["pos"], "open_profile_cefr"
    if wort and wort.get("cefr"):
        return str(wort["cefr"]), str(wort["pos"]), "open_profile_cefr"
    if wort and wort.get("grade"):
        grade_to_cefr = {
            1: "A1",
            2: "A1",
            3: "A2",
            4: "B1",
            5: "B2",
            6: "B2",
        }
        estimated = grade_to_cefr.get(int(wort["grade"]))
        if estimated:
            return estimated, str(wort["pos"]), "wort_grade_frequency_estimate"
    return None


def distribute_split_band(
    candidates: list[dict[str, object]],
    levels: tuple[str, ...],
    needed: dict[str, int],
) -> None:
    total_needed = sum(max(0, needed[level]) for level in levels)
    usable_count = min(len(candidates), total_needed)
    allocations = {level: 0 for level in levels}
    if total_needed:
        raw = {
            level: usable_count * max(0, needed[level]) / total_needed
            for level in levels
        }
        allocations = {level: int(raw[level]) for level in levels}
        missing = usable_count - sum(allocations.values())
        remainder_order = sorted(
            levels,
            key=lambda level: (-(raw[level] - allocations[level]), levels.index(level)),
        )
        for level in remainder_order[:missing]:
            allocations[level] += 1

    offset = 0
    for level in levels:
        end = offset + allocations[level]
        for candidate in candidates[offset:end]:
            candidate["eiken"] = level
        offset = end
    # A source band may not fill an easier quota after common words were already
    # supplied by NGSL. Keep remaining candidates at the hardest compatible band.
    for candidate in candidates[offset:]:
        candidate["eiken"] = levels[-1]


def main() -> None:
    args = parse_args()
    original = json.loads(args.words.read_text(encoding="utf-8"))
    base_words = [word for word in original if word.get("wordList") != SOURCE_CODE]
    if args.target_total < len(base_words):
        raise ValueError("target total is smaller than the protected base dataset")

    existing_english = {str(word["english"]).lower() for word in base_words}
    used_prompts = {
        (word["quizMeaning"], word["quizPartOfSpeech"])
        for word in base_words
    }
    existing_counts = Counter(str(word["eikenLevel"]) for word in base_words)
    needed = {
        level: max(0, target - existing_counts[level])
        for level, target in FINAL_BAND_TARGETS.items()
    }

    dictionary = load_ejdict(args.ejdict_src)
    profiles = load_profiles(args.cefrj, args.octanove)
    wort_words = load_wort_words(args.wort_db)
    wordnet_translations = load_wordnet_translations(
        args.english_wordnet_gz,
        args.japanese_wordnet,
    )
    all_headwords = set(wort_words) | set(profiles)

    candidates: list[dict[str, object]] = []
    candidate_prompt_keys: set[tuple[str, str]] = set()
    for english in sorted(all_headwords - existing_english):
        if (
            english in BLOCKED_WORDS
            or english in QUIZ_QUALITY_EXCLUSIONS
            or len(english) <= 2
        ):
            continue
        dictionary_text = dictionary.get(english)
        if not dictionary_text:
            continue
        wort = wort_words.get(english)
        profile = choose_profile(english, profiles, wort)
        if not profile:
            continue
        cefr, source_pos, classification_basis = profile
        quiz_pos = POS_NAMES[source_pos]
        semantic_prompts = wordnet_prompt_candidates(
            english,
            source_pos,
            wordnet_translations,
        )
        if not semantic_prompts:
            continue
        display_prompts = confirmed_semantic_prompts(
            dictionary_text,
            semantic_prompts,
        )
        prompt = next(
            (
                candidate
                for candidate in display_prompts
                if (
                    (candidate, quiz_pos) not in used_prompts
                    and (candidate, quiz_pos) not in candidate_prompt_keys
                )
            ),
            None,
        )
        prompt = QUIZ_MEANING_OVERRIDES.get(english, prompt)
        if not prompt:
            continue
        candidate_prompt_keys.add((prompt, quiz_pos))
        candidates.append({
            "english": english,
            "japanese": dictionary_text,
            "quizMeaning": prompt,
            "quizPartOfSpeech": quiz_pos,
            "partOfSpeech": quiz_pos,
            "cefr": cefr,
            "classificationBasis": classification_basis,
            "zipf": float(wort["zipf"]) if wort else 0.0,
            "perMillion": float(wort["perMillion"]) if wort else 0.0,
        })

    candidates.sort(key=lambda item: (-float(item["zipf"]), str(item["english"])))
    by_cefr: dict[str, list[dict[str, object]]] = defaultdict(list)
    for candidate in candidates:
        by_cefr[str(candidate["cefr"])].append(candidate)
    distribute_split_band(by_cefr["A1"], EIKEN_FROM_CEFR["A1"], needed)
    distribute_split_band(by_cefr["A2"], EIKEN_FROM_CEFR["A2"], needed)
    for cefr in ("B1", "B2", "C1", "C2"):
        for candidate in by_cefr[cefr]:
            candidate["eiken"] = EIKEN_FROM_CEFR[cefr][0]

    # Fill each course toward its requested coverage, then use remaining
    # evidence-backed candidates by frequency until the global target is met.
    selected: list[dict[str, object]] = []
    selected_english: set[str] = set()
    for level in FINAL_BAND_TARGETS:
        pool = [item for item in candidates if item.get("eiken") == level]
        take = min(needed[level], len(pool))
        for item in pool[:take]:
            selected.append(item)
            selected_english.add(str(item["english"]))

    remaining = sorted(
        (
            item for item in candidates
            if str(item["english"]) not in selected_english
        ),
        key=lambda item: (-float(item["zipf"]), str(item["english"])),
    )
    requested_additions = args.target_total - len(base_words)
    strict_additions_requested = min(
        requested_additions,
        STRICT_REVIEWED_TOTAL - len(base_words),
    )
    selected.extend(remaining[:max(0, strict_additions_requested - len(selected))])
    selected = selected[:strict_additions_requested]

    additions: list[dict[str, object]] = []
    for source_rank, item in enumerate(selected, start=1):
        eiken = str(item["eiken"])
        level, school_grade = COURSE_METADATA[eiken]
        quiz_source = (
            "Japanese WordNet / EJDict-hand / Open Language Profiles"
            if str(item["cefr"]) in {"C1", "C2"}
            else "Japanese WordNet / EJDict-hand / WortUniversum / CEFR profile"
        )
        addition = {
            "english": item["english"],
            "quizMeaning": item["quizMeaning"],
            "quizPartOfSpeech": item["quizPartOfSpeech"],
            "quizHint": None,
            "quizSource": quiz_source,
            "japanese": item["japanese"],
            "ngslRank": None,
            "level": level,
            "schoolGrade": school_grade,
            "eikenLevel": eiken,
            "partOfSpeech": item["partOfSpeech"],
            "sfi": None,
            "frequencyPerMillion": round(float(item["perMillion"])),
            "translationSource": "Japanese WordNet / EJDict-hand",
            "wordList": SOURCE_CODE,
            "sourceRank": source_rank,
            "estimatedCefrLevel": item["cefr"],
            "eikenClassificationBasis": (
                "open_profile_frequency_split"
                if (
                    item["classificationBasis"] == "open_profile_cefr"
                    and str(item["cefr"]) in {"A1", "A2"}
                )
                else item["classificationBasis"]
            ),
        }
        prompt_key = (addition["quizMeaning"], addition["quizPartOfSpeech"])
        if prompt_key in used_prompts:
            raise ValueError(f"ambiguous generated prompt: {prompt_key}")
        used_prompts.add(prompt_key)
        additions.append(addition)

    curated_requested = requested_additions - len(additions)
    curated_available = [
        item for item in CURATED_A1_FOUNDATION_ADDITIONS
        if str(item["english"]) not in existing_english
    ]
    if curated_requested > len(curated_available):
        raise ValueError(
            f"Only {len(base_words) + len(additions) + len(curated_available)} "
            f"validated words available; target was {args.target_total}"
        )
    for item in curated_available[:curated_requested]:
        english = str(item["english"])
        eiken = str(item["eiken"])
        quiz_pos = str(item["quizPartOfSpeech"])
        profile_rows = profiles.get(english, [])
        if not any(
            row["cefr"] == "A1" and POS_NAMES[row["pos"]] == quiz_pos
            for row in profile_rows
        ):
            raise ValueError(f"curated A1 profile mismatch: {english}")
        dictionary_text = dictionary.get(english)
        if not dictionary_text:
            raise ValueError(f"curated EJDict entry missing: {english}")
        level, school_grade = COURSE_METADATA[eiken]
        addition = {
            "english": english,
            "quizMeaning": item["quizMeaning"],
            "quizPartOfSpeech": quiz_pos,
            "quizHint": None,
            "quizSource": "EJDict-hand / CEFR-J",
            "japanese": dictionary_text,
            "ngslRank": None,
            "level": level,
            "schoolGrade": school_grade,
            "eikenLevel": eiken,
            "partOfSpeech": quiz_pos,
            "sfi": None,
            "frequencyPerMillion": 0,
            "translationSource": "EJDict-hand",
            "wordList": SOURCE_CODE,
            "sourceRank": len(additions) + 1,
            "estimatedCefrLevel": "A1",
            "eikenClassificationBasis": "cefrj_foundation_split",
        }
        prompt_key = (addition["quizMeaning"], addition["quizPartOfSpeech"])
        if prompt_key in used_prompts:
            raise ValueError(f"ambiguous curated prompt: {prompt_key}")
        used_prompts.add(prompt_key)
        additions.append(addition)

    output = base_words + additions
    if len(output) != args.target_total:
        raise ValueError(
            f"Only {len(output)} validated words available; "
            f"target was {args.target_total}"
        )
    args.output.write_text(
        json.dumps(output, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(json.dumps({
        "total": len(output),
        "added": len(additions),
        "eikenCounts": dict(sorted(Counter(
            str(word["eikenLevel"]) for word in output
        ).items())),
    }, ensure_ascii=False))


if __name__ == "__main__":
    main()
