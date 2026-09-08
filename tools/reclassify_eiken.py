#!/usr/bin/env python3
"""Assign evidence-backed Eiken-equivalent bands to the app word data.

The Eiken Foundation does not publish a fixed vocabulary list for each grade.
This tool therefore uses licensed CEFR vocabulary profiles as the primary
word-level evidence and the Foundation's CEFR/grade correspondence only as a
course-design guide.  It never claims that the resulting bands are official.
"""

import argparse
import csv
import json
import sqlite3
from collections import Counter, defaultdict
from pathlib import Path


CEFR_ORDER = {level: index for index, level in enumerate(
    ("A1", "A2", "B1", "B2", "C1", "C2"),
)}

APP_POS_TO_SOURCE_POS = {
    "名詞": {"noun"},
    "動詞": {"verb", "be-verb", "do-verb", "have-verb"},
    "形容詞": {"adjective"},
    "副詞": {"adverb"},
    "前置詞": {"preposition"},
    "代名詞": {"pronoun"},
    "接続詞": {"conjunction"},
    "限定詞": {"determiner"},
    "冠詞": {"determiner"},
    "助動詞": {"modal auxiliary"},
    "間投詞": {"interjection"},
    "数詞": {"number"},
    "接頭辞": {"prefix"},
}

VALID_BASIS = {
    "cefrj",
    "cefrj_foundation_split",
    "octanove_c1c2",
    "ngsl_frequency_fallback",
    "nawl_academic_fallback",
}

# Re-grouping words by evidence-backed bands can place synonyms in one course.
# These concise prompts preserve the source meaning while making the expected
# English answer unambiguous.
PROMPT_DISAMBIGUATION = {
    "until": "～まで（継続の終点）",
    "till": "～まで（口語）",
    "amount": "量（総量）",
    "quantity": "数量",
    "organization": "組織・団体",
    "tissue": "生体組織",
    "gain": "得る・増やす",
    "obtain": "入手する",
    "generally": "一般的に",
    "commonly": "よく・一般に",
    "completely": "完全に・すっかり",
    "entirely": "全体として完全に",
    "appropriate": "適切な・ふさわしい",
    "proper": "正式で適切な",
    "remove": "取り除く・移動する",
    "rid": "取り除いて解放する",
    "trend": "長期的な傾向",
    "tendency": "～しがちな傾向",
    "obvious": "明白な",
    "apparent": "見たところ明らかな",
    "evident": "証拠から明らかな",
    "prison": "刑務所・収監",
    "jail": "留置場・刑務所",
    "assessment": "査定・評価",
    "evaluation": "価値の評価",
    "division": "分割・部門",
    "partition": "仕切り・分割",
    "nevertheless": "それでもなお",
    "nonetheless": "それでも",
    "coast": "海岸地帯",
    "shore": "水辺・岸",
    "port": "港・港湾都市",
    "harbor": "船を守る港",
    "spare": "予備の・余分な",
    "preliminary": "予備的な・事前の",
    "capability": "実行能力",
    "competence": "十分な技能・能力",
    "motivation": "やる気・動機づけ",
    "motive": "行動の動機",
    "voluntary": "自発的な・任意の",
    "spontaneous": "自然発生的な",
    "offspring": "子・子孫",
    "descendant": "子孫・後裔",
    "strand": "糸状の一本",
    "thread": "縫い糸",
}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--words", type=Path, required=True)
    parser.add_argument("--cefrj", type=Path, required=True)
    parser.add_argument("--octanove", type=Path, required=True)
    parser.add_argument("--foundation-db", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--report", type=Path)
    return parser.parse_args()


def normalized_headwords(raw: str) -> list[str]:
    return [part.strip().lower() for part in raw.split("/") if part.strip()]


def load_profiles(*sources: tuple[str, Path]) -> dict[str, list[dict[str, str]]]:
    profiles: dict[str, list[dict[str, str]]] = defaultdict(list)
    for source_name, path in sources:
        with path.open(encoding="utf-8-sig", newline="") as source:
            for row in csv.DictReader(source):
                level = row["CEFR"].strip()
                if level not in CEFR_ORDER:
                    raise ValueError(f"Unsupported CEFR level: {level}")
                for headword in normalized_headwords(row["headword"]):
                    profiles[headword].append({
                        "level": level,
                        "pos": row["pos"].strip().lower(),
                        "source": source_name,
                    })
    return profiles


def app_source_pos(part_of_speech: str | None) -> set[str]:
    if not part_of_speech:
        return set()
    result: set[str] = set()
    for app_pos, source_pos in APP_POS_TO_SOURCE_POS.items():
        if app_pos in part_of_speech:
            result.update(source_pos)
    return result


def select_profile(
    word: dict[str, object],
    profiles: dict[str, list[dict[str, str]]],
) -> dict[str, str] | None:
    candidates = profiles.get(str(word["english"]).lower(), [])
    if not candidates:
        return None

    expected_pos = app_source_pos(
        str(word.get("quizPartOfSpeech") or word.get("partOfSpeech") or "")
    )
    pos_matches = [row for row in candidates if row["pos"] in expected_pos]
    usable = pos_matches or candidates
    return min(
        usable,
        key=lambda row: (
            CEFR_ORDER[row["level"]],
            0 if row["source"] == "cefrj" else 1,
        ),
    )


def load_foundation_grades(
    path: Path,
) -> tuple[
    dict[tuple[str, str], int],
    dict[str, int],
    dict[tuple[str, str], str],
    dict[str, str],
]:
    by_word_pos: dict[tuple[str, str], int] = {}
    by_word: dict[str, int] = {}
    yle_by_word_pos: dict[tuple[str, str], str] = {}
    yle_by_word: dict[str, str] = {}
    with sqlite3.connect(path) as database:
        for word, word_type, grade, yle in database.execute(
            "SELECT lower(word), lower(word_type), min(grade_level), "
            "json_extract(metadata_json, '$.yle_level') "
            "FROM words WHERE grade_level IS NOT NULL "
            "GROUP BY lower(word), lower(word_type)"
        ):
            key = (word, word_type)
            by_word_pos[key] = grade
            by_word[word] = min(grade, by_word.get(word, grade))
            if yle:
                yle_by_word_pos[key] = yle
                yle_by_word.setdefault(word, yle)
    return by_word_pos, by_word, yle_by_word_pos, yle_by_word


def foundation_grade(
    word: dict[str, object],
    by_word_pos: dict[tuple[str, str], int],
    by_word: dict[str, int],
) -> int | None:
    english = str(word["english"]).lower()
    expected_pos = app_source_pos(
        str(word.get("quizPartOfSpeech") or word.get("partOfSpeech") or "")
    )
    pos_grades = [
        by_word_pos[(english, pos)]
        for pos in expected_pos
        if (english, pos) in by_word_pos
    ]
    if pos_grades:
        return min(pos_grades)
    return by_word.get(english)


def foundation_yle_level(
    word: dict[str, object],
    by_word_pos: dict[tuple[str, str], str],
    by_word: dict[str, str],
) -> str | None:
    english = str(word["english"]).lower()
    expected_pos = app_source_pos(
        str(word.get("quizPartOfSpeech") or word.get("partOfSpeech") or "")
    )
    for pos in expected_pos:
        level = by_word_pos.get((english, pos))
        if level:
            return level
    return by_word.get(english)


def normalized_source_rank(word: dict[str, object]) -> float:
    source_size = 2809 if word["wordList"] == "NGSL_1_2" else 957
    return int(word["sourceRank"]) / source_size


def classify_profiled_word(
    word: dict[str, object],
    profile: dict[str, str],
    grade: int | None,
    yle_level: str | None,
) -> tuple[str, str]:
    level = profile["level"]
    source = profile["source"]

    if level == "A1":
        if grade == 1 or yle_level == "starters":
            return "5", "cefrj_foundation_split"
        if yle_level == "movers":
            return "4", "cefrj_foundation_split"
        if yle_level == "flyers":
            return "3", "cefrj_foundation_split"
        rank = normalized_source_rank(word)
        if rank <= 0.22:
            return "5", "cefrj_foundation_split"
        if rank <= 0.50:
            return "4", "cefrj_foundation_split"
        return "3", "cefrj_foundation_split"

    if level == "A2":
        # Pre-2 Plus bridges Pre-2 and Grade 2.  CEFR alone does not split A2,
        # so the later foundation estimate (or the hardest rank third) is used.
        eiken = (
            "pre2plus"
            if normalized_source_rank(word) > 0.55
            else "pre2"
        )
        return eiken, "cefrj_foundation_split"

    direct_mapping = {
        "B1": "2",
        "B2": "pre1",
        "C1": "1",
        "C2": "1",
    }
    basis = "cefrj" if source == "cefrj" else "octanove_c1c2"
    return direct_mapping[level], basis


def classify_unprofiled_word(word: dict[str, object]) -> tuple[str, str, str]:
    rank = int(word["sourceRank"])
    if word["wordList"] == "NGSL_1_2":
        if rank <= 1200:
            return "pre2", "A2", "ngsl_frequency_fallback"
        if rank <= 2300:
            return "2", "B1", "ngsl_frequency_fallback"
        return "pre1", "B2", "ngsl_frequency_fallback"

    if rank <= 479:
        return "pre1", "B2", "nawl_academic_fallback"
    return "1", "C1", "nawl_academic_fallback"


def main() -> None:
    args = parse_args()
    words = json.loads(args.words.read_text(encoding="utf-8"))
    profiles = load_profiles(
        ("cefrj", args.cefrj),
        ("octanove", args.octanove),
    )
    (
        by_word_pos,
        by_word,
        yle_by_word_pos,
        yle_by_word,
    ) = load_foundation_grades(args.foundation_db)

    counts: Counter[str] = Counter()
    basis_counts: Counter[str] = Counter()
    cefr_counts: Counter[str] = Counter()
    for word in words:
        profile = select_profile(word, profiles)
        if profile is None:
            eiken, cefr, basis = classify_unprofiled_word(word)
        else:
            cefr = profile["level"]
            grade = foundation_grade(word, by_word_pos, by_word)
            yle_level = foundation_yle_level(
                word,
                yle_by_word_pos,
                yle_by_word,
            )
            eiken, basis = classify_profiled_word(
                word,
                profile,
                grade,
                yle_level,
            )

        if basis not in VALID_BASIS:
            raise ValueError(f"Unsupported classification basis: {basis}")
        word["eikenLevel"] = eiken
        word["estimatedCefrLevel"] = cefr
        word["eikenClassificationBasis"] = basis
        prompt = PROMPT_DISAMBIGUATION.get(str(word["english"]).lower())
        if prompt:
            word["quizMeaning"] = prompt
            word["quizSource"] = "manual review / Eiken-course disambiguation"
        counts[eiken] += 1
        basis_counts[basis] += 1
        cefr_counts[cefr] += 1

    args.output.write_text(
        json.dumps(words, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )

    report = {
        "total": len(words),
        "eikenCounts": dict(sorted(counts.items())),
        "cefrCounts": dict(sorted(cefr_counts.items())),
        "basisCounts": dict(sorted(basis_counts.items())),
    }
    if args.report:
        args.report.write_text(
            json.dumps(report, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
        )
    print(json.dumps(report, ensure_ascii=False))


if __name__ == "__main__":
    main()
