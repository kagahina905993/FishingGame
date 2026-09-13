#!/usr/bin/env python3
"""Validate word data before packaging the Android app."""

import argparse
import json
import re
from collections import Counter, defaultdict
from pathlib import Path


EXPECTED_SOURCE_COUNTS = {
    "NGSL_1_2": 2809,
    "NAWL_1_2": 957,
    "OPEN_VOCAB_EXTENDED_V1": 2974,
}
ENGLISH_WORD = re.compile(r"^[a-z]+$")
VALID_EIKEN_BASIS = {
    "cefrj",
    "cefrj_foundation_split",
    "octanove_c1c2",
    "ngsl_frequency_fallback",
    "nawl_academic_fallback",
    "open_profile_cefr",
    "open_profile_frequency_split",
    "wort_grade_frequency_estimate",
}
VALID_CEFR_TO_EIKEN = {
    "A1": {"5", "4", "3"},
    "A2": {"pre2", "pre2plus"},
    "B1": {"2"},
    "B2": {"pre1"},
    "C1": {"1"},
    "C2": {"1"},
}
REPRESENTATIVE_EIKEN_LEVELS = {
    "apple": "5",
    "ball": "5",
    "dictionary": "4",
    "academic": "2",
    "nuclear": "2",
    "domain": "pre1",
}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("words", type=Path)
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    words = json.loads(args.words.read_text(encoding="utf-8"))
    errors: list[str] = []

    normalized = [word["english"].lower() for word in words]
    duplicates = [word for word, count in Counter(normalized).items() if count > 1]
    if duplicates:
        errors.append(f"duplicate English words: {duplicates}")

    by_source = defaultdict(list)
    for word in words:
        english = word["english"]
        if not ENGLISH_WORD.fullmatch(english):
            errors.append(f"invalid English headword: {english}")
        if word.get("schoolGrade") is None or word.get("eikenLevel") is None:
            errors.append(f"missing classification: {english}")
        cefr_level = word.get("estimatedCefrLevel")
        eiken_level = word.get("eikenLevel")
        basis = word.get("eikenClassificationBasis")
        if cefr_level not in VALID_CEFR_TO_EIKEN:
            errors.append(f"invalid estimated CEFR level: {english} ({cefr_level})")
        elif eiken_level not in VALID_CEFR_TO_EIKEN[cefr_level]:
            errors.append(
                f"inconsistent CEFR/Eiken classification: "
                f"{english} ({cefr_level} -> {eiken_level})"
            )
        if basis not in VALID_EIKEN_BASIS:
            errors.append(f"invalid Eiken classification basis: {english} ({basis})")
        if not word.get("japanese"):
            errors.append(f"missing Japanese dictionary text: {english}")
        by_source[word.get("wordList")].append(word)

    for source, expected_count in EXPECTED_SOURCE_COUNTS.items():
        source_words = by_source.get(source, [])
        if len(source_words) != expected_count:
            errors.append(
                f"{source} count: expected {expected_count}, got {len(source_words)}"
            )
        ranks = sorted(word.get("sourceRank") for word in source_words)
        if ranks != list(range(1, expected_count + 1)):
            errors.append(f"{source} ranks contain duplicates or gaps")

    for word in words:
        if not word.get("quizMeaning"):
            errors.append(f"missing quiz meaning: {word['english']}")
        if not word.get("partOfSpeech") or not word.get("quizPartOfSpeech"):
            errors.append(f"missing part of speech: {word['english']}")
        if not word.get("quizSource"):
            errors.append(f"missing quiz source: {word['english']}")
        if len(word.get("quizMeaning", "")) > 40:
            errors.append(f"quiz meaning is too long: {word['english']}")

    for course_axis in ("eikenLevel", "schoolGrade", "level"):
        prompts = defaultdict(list)
        for word in words:
            key = (
                word.get(course_axis),
                word.get("quizMeaning"),
                word.get("quizPartOfSpeech"),
            )
            prompts[key].append(word["english"])
        for (course, meaning, part_of_speech), english_words in prompts.items():
            if len(english_words) > 1:
                errors.append(
                    f"ambiguous prompt in {course_axis}={course}: "
                    f"{meaning} ({part_of_speech}) -> {english_words}"
                )

    by_english = {word["english"]: word for word in words}
    for english, expected_level in REPRESENTATIVE_EIKEN_LEVELS.items():
        actual_level = by_english.get(english, {}).get("eikenLevel")
        if actual_level != expected_level:
            errors.append(
                f"representative Eiken level: expected {english}="
                f"{expected_level}, got {actual_level}"
            )

    if errors:
        for error in errors:
            print(f"ERROR: {error}")
        raise SystemExit(1)

    source_counts = {
        source: len(source_words)
        for source, source_words in sorted(by_source.items())
    }
    print(
        json.dumps(
            {
                "total": len(words),
                "sources": source_counts,
                "status": "ok",
            },
            ensure_ascii=False,
        )
    )


if __name__ == "__main__":
    main()
