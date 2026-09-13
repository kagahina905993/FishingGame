#!/usr/bin/env python3
"""Import NAWL 1.2 words while preserving later vocabulary sources."""

import argparse
import csv
import json
import re
from pathlib import Path


NAWL_ROW = re.compile(
    r"<tr><td>(\d+)</td>\s*"
    r"<td><a [^>]*>([^<]+)</td>\s*"
    r"<td>.*?</td>\s*<td>([^<]+)</td>",
    re.IGNORECASE,
)

POS_NAMES = {
    "adj": "形容詞",
    "adv": "副詞",
    "n": "名詞",
    "noun": "名詞",
    "prep": "前置詞",
    "prefix": "接頭辞",
    "det": "限定詞",
    "pron": "代名詞",
    "v": "動詞",
    "verb": "動詞",
}

MEANING_OVERRIDES = {
    "altitude": "海抜高度",
    "bracket": "角括弧；支え金具",
    "calculation": "計算；算定",
    "cheer": "歓声を上げる；励ます",
    "millimeter": "ミリメートル",
    "syntactic": "構文上の",
}

METADATA_ALIASES = {
    "cheer": "cheers",
    "descendant": "descendent",
    "headquarters": "headquarter",
}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--words", type=Path, required=True)
    parser.add_argument("--nawl-csv", type=Path, required=True)
    parser.add_argument("--nawl-html", type=Path, required=True)
    return parser.parse_args()


def load_nawl_ranks(path: Path) -> list[tuple[int, str, str]]:
    html = path.read_text(encoding="utf-8")
    rows = [
        (int(rank), word.strip().lower(), pos.strip().lower())
        for rank, word, pos in NAWL_ROW.findall(html)
    ]
    unique_rows = sorted(set(rows))
    ranks = [rank for rank, _, _ in unique_rows]
    if ranks != list(range(1, 958)):
        raise ValueError("NAWL ranks must be unique and contiguous from 1 to 957")
    return unique_rows


def load_nawl_metadata(path: Path) -> dict[str, dict[str, str]]:
    with path.open(encoding="utf-8-sig", newline="") as source:
        rows = csv.DictReader(source)
        metadata = {}
        for row in rows:
            word = row["Meanings"].strip().lower()
            metadata[word] = {
                "japanese": row["J Translation"].strip(),
                "pos": row["POS"].strip().lower(),
            }
    return metadata


def main() -> None:
    args = parse_args()
    all_words = json.loads(args.words.read_text(encoding="utf-8"))
    later_sources = [
        word for word in all_words
        if word.get("wordList") not in {"NGSL_1_2", "NAWL_1_2"}
    ]
    words = [
        word
        for word in all_words
        if word.get("wordList", "NGSL_1_2") == "NGSL_1_2"
    ]
    if len(words) != 2809:
        raise ValueError(f"Expected 2809 NGSL words, got {len(words)}")
    ranks = load_nawl_ranks(args.nawl_html)
    metadata = load_nawl_metadata(args.nawl_csv)

    existing = {word["english"].lower() for word in words}
    additions = []
    for source_rank, english, source_pos in ranks:
        if english in existing:
            raise ValueError(f"NAWL word already exists: {english}")
        metadata_key = METADATA_ALIASES.get(english, english)
        details = metadata.get(metadata_key)
        if not details or not details["japanese"]:
            raise ValueError(f"Missing Japanese meaning: {english}")

        lower_band = source_rank <= 479
        short_meaning = MEANING_OVERRIDES.get(
            english,
            details["japanese"],
        )
        part_of_speech = POS_NAMES.get(source_pos)
        if not part_of_speech:
            raise ValueError(f"Unsupported part of speech: {english} ({source_pos})")
        additions.append(
            {
                "english": english,
                "quizMeaning": short_meaning,
                "quizPartOfSpeech": part_of_speech,
                "quizHint": None,
                "quizSource": (
                    "manual review / NAWL 1.2 Japanese metadata"
                    if english in MEANING_OVERRIDES
                    else "NAWL 1.2 Japanese metadata"
                ),
                # Keep the compatibility field short. The app no longer ships
                # or displays the source dictionary body.
                "japanese": short_meaning,
                "ngslRank": None,
                "level": 6 if lower_band else 7,
                "schoolGrade": 11 if lower_band else 12,
                "eikenLevel": "pre2plus" if lower_band else "2",
                "partOfSpeech": part_of_speech,
                "sfi": None,
                "frequencyPerMillion": None,
                "translationSource": "NAWL 1.2",
                "wordList": "NAWL_1_2",
                "sourceRank": source_rank,
            }
        )

    for word in words:
        word["wordList"] = "NGSL_1_2"
        word["sourceRank"] = word["ngslRank"]

    output = words + additions + later_sources
    expected_count = 3766 + len(later_sources)
    if len(output) != expected_count:
        raise ValueError(f"Expected {expected_count} words, got {len(output)}")
    args.words.write_text(
        json.dumps(output, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )


if __name__ == "__main__":
    main()
