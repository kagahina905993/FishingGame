#!/usr/bin/env python3
"""Validate authored and licensed sentence-question content."""

import argparse
import json
import re
from collections import Counter, defaultdict
from pathlib import Path


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("words", type=Path)
    parser.add_argument("questions", type=Path)
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    words = json.loads(args.words.read_text(encoding="utf-8"))
    questions = json.loads(args.questions.read_text(encoding="utf-8"))
    word_by_id = {
        f"{word['wordList'].lower()}:{word['sourceRank']:04d}": word
        for word in words
    }
    errors: list[str] = []

    def part_of_speech(word: dict) -> str:
        return word.get("quizPartOfSpeech") or word.get("partOfSpeech") or ""

    def starts_with_vowel_sound(english: str) -> bool:
        lowered = english.lower()
        if lowered.startswith(("honest", "honor", "hour", "heir")):
            return True
        if lowered.startswith(("uni", "use", "user", "usual", "one", "once")):
            return False
        return lowered.startswith(tuple("aeiou"))

    def duplicates(values: list[str]) -> list[str]:
        return [value for value, count in Counter(values).items() if count > 1]

    for label, values in (
        ("question IDs", [item.get("questionId", "") for item in questions]),
        ("word IDs", [item.get("wordId", "") for item in questions]),
        ("sentences", [item.get("sentence", "") for item in questions]),
    ):
        repeated = duplicates(values)
        if repeated:
            errors.append(f"duplicate {label}: {repeated}")

    for item in questions:
        question_id = item.get("questionId", "<missing>")
        word_id = item.get("wordId")
        word = word_by_id.get(word_id)
        if word is None:
            errors.append(f"unknown wordId: {question_id} ({word_id})")
            continue
        if item.get("sentence", "").count("___") != 1:
            errors.append(f"sentence must contain one blank: {question_id}")
        sentence = item.get("sentence", "")
        stripped_sentence = sentence.lstrip()
        if (
            stripped_sentence
            and not stripped_sentence.startswith("___")
            and not stripped_sentence[0].isupper()
        ):
            errors.append(f"sentence must start with uppercase: {question_id}")
        if not sentence.endswith((".", "?", "!")):
            errors.append(f"sentence requires ending punctuation: {question_id}")
        if re.search(r"\s{2,}|\s+[.,?!]|[（(]\s|\s[）)]", sentence):
            errors.append(f"space before punctuation: {question_id}")
        if item.get("answer") != word["english"]:
            errors.append(f"answer does not match headword: {question_id}")
        if item.get("answer", "") != item.get("answer", "").lower():
            errors.append(f"answer must be lowercase: {question_id}")
        if not item.get("japaneseSentence") or not item.get("explanation"):
            errors.append(f"missing translation or explanation: {question_id}")
        if not item.get("japaneseSentence", "").endswith(("。", "？", "！")):
            errors.append(f"translation requires ending punctuation: {question_id}")
        if not item.get("explanation", "").endswith(("。", "？", "！")):
            errors.append(f"explanation requires ending punctuation: {question_id}")
        if re.search(
            rf"\b{re.escape(item.get('answer', ''))}\b",
            sentence,
            re.IGNORECASE,
        ):
            errors.append(f"sentence reveals its answer: {question_id}")

        article_match = re.search(r"\b(a|an)\s+___", sentence, re.IGNORECASE)
        if article_match and item.get("answer"):
            expected_article = (
                "an" if starts_with_vowel_sound(item["answer"]) else "a"
            )
            if article_match.group(1).lower() != expected_article:
                errors.append(f"article does not match answer: {question_id}")

        distractors = item.get("distractorWordIds", [])
        if len(distractors) != 3 or len(set(distractors)) != 3:
            errors.append(f"exactly three distinct distractors required: {question_id}")
        if word_id in distractors:
            errors.append(f"correct word is included as distractor: {question_id}")
        unknown_distractors = [item_id for item_id in distractors if item_id not in word_by_id]
        if unknown_distractors:
            errors.append(f"unknown distractors: {question_id} {unknown_distractors}")
        known_distractor_words = [
            word_by_id[item_id]
            for item_id in distractors
            if item_id in word_by_id
        ]
        if len({part_of_speech(item) for item in known_distractor_words}) > 1:
            errors.append(f"distractor parts of speech are mixed: {question_id}")
        visible_choices = [item.get("answer", "")] + [
            candidate.get("english", "").lower()
            for candidate in known_distractor_words
        ]
        if len(visible_choices) != len(set(visible_choices)):
            errors.append(f"visible choices are duplicated: {question_id}")
        harder_distractors = [
            item_id for item_id in distractors
            if item_id in word_by_id
            and word_by_id[item_id].get("level", 99) > word.get("level", 0)
        ]
        if harder_distractors:
            errors.append(
                f"distractors must not exceed the target game level: "
                f"{question_id} {harder_distractors}"
            )

        source = item.get("source")
        license_name = item.get("license")
        source_url = item.get("sourceUrl")
        if not source or not license_name:
            errors.append(f"missing rights metadata: {question_id}")
        if source != "project-authored" and not source_url:
            errors.append(f"external content requires sourceUrl: {question_id}")
        if source == "project-authored" and license_name != "project-original":
            errors.append(f"invalid project-authored license: {question_id}")

    for course_axis in ("level", "schoolGrade", "eikenLevel"):
        course_words: dict[object, list[dict]] = defaultdict(list)
        for word in words:
            course_words[word.get(course_axis)].append(word)
        for course, members in course_words.items():
            for target in members:
                candidates = [
                    candidate for candidate in members
                    if candidate["english"] != target["english"]
                    and candidate.get("quizMeaning") != target.get("quizMeaning")
                ]
                if len(candidates) < 3:
                    errors.append(
                        f"insufficient meaning-choice candidates: "
                        f"{course_axis}={course} {target['english']}"
                    )

    if errors:
        for error in errors:
            print(f"ERROR: {error}")
        raise SystemExit(1)

    print(json.dumps({
        "total": len(questions),
        "projectAuthored": sum(
            item.get("source") == "project-authored" for item in questions
        ),
        "external": sum(
            item.get("source") != "project-authored" for item in questions
        ),
        "status": "ok",
    }, ensure_ascii=False))


if __name__ == "__main__":
    main()
