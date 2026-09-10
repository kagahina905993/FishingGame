#!/usr/bin/env python3
"""Report sentence-question wording and multiple-choice quality risks."""

import argparse
import json
import re
from collections import Counter
from pathlib import Path


def starts_with_vowel_sound(english: str) -> bool:
    lowered = english.lower()
    if lowered.startswith(("honest", "honor", "hour", "heir")):
        return True
    if lowered.startswith(("uni", "use", "user", "usual", "one", "once")):
        return False
    return lowered.startswith(tuple("aeiou"))


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("words", type=Path)
    parser.add_argument("questions", type=Path)
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    words = json.loads(args.words.read_text(encoding="utf-8"))
    questions = json.loads(args.questions.read_text(encoding="utf-8"))
    words_by_id = {
        f"{word['wordList'].lower()}:{word['sourceRank']:04d}": word
        for word in words
    }
    findings: dict[str, list[str]] = {
        "sentence_start": [],
        "sentence_punctuation": [],
        "japanese_punctuation": [],
        "explanation_punctuation": [],
        "spacing": [],
        "answer_leak": [],
        "article_mismatch": [],
        "mixed_distractor_pos": [],
        "duplicate_visible_choice": [],
    }

    for item in questions:
        question_id = item["questionId"]
        sentence = item["sentence"]
        japanese = item["japaneseSentence"]
        explanation = item["explanation"]
        answer = item["answer"]
        target = words_by_id[item["wordId"]]

        stripped = sentence.lstrip()
        if not stripped.startswith("___") and (
            not stripped or not stripped[0].isupper()
        ):
            findings["sentence_start"].append(question_id)
        if not sentence.endswith((".", "?", "!")):
            findings["sentence_punctuation"].append(question_id)
        if not japanese.endswith(("。", "？", "！")):
            findings["japanese_punctuation"].append(question_id)
        if not explanation.endswith(("。", "？", "！")):
            findings["explanation_punctuation"].append(question_id)
        if re.search(r"\s{2,}|\s+[.,?!]|[（(]\s|\s[）)]", sentence):
            findings["spacing"].append(question_id)
        if re.search(rf"\b{re.escape(answer)}\b", sentence, re.IGNORECASE):
            findings["answer_leak"].append(question_id)

        article_match = re.search(r"\b(a|an)\s+___", sentence, re.IGNORECASE)
        if article_match:
            expected = "an" if starts_with_vowel_sound(answer) else "a"
            if article_match.group(1).lower() != expected:
                findings["article_mismatch"].append(question_id)

        distractors = [words_by_id[item_id] for item_id in item["distractorWordIds"]]
        distractor_pos = [
            word.get("quizPartOfSpeech") or word.get("partOfSpeech")
            for word in distractors
        ]
        if len(set(distractor_pos)) > 1:
            findings["mixed_distractor_pos"].append(question_id)
        visible_choices = [answer] + [word["english"].lower() for word in distractors]
        if len(set(visible_choices)) != 4:
            findings["duplicate_visible_choice"].append(question_id)

    summary = {name: len(ids) for name, ids in findings.items()}
    print(json.dumps(summary, ensure_ascii=False, indent=2))
    for name, ids in findings.items():
        if ids:
            print(f"\n{name} ({len(ids)})")
            print(" ".join(ids[:80]))

    sentence_starters = Counter(
        item["sentence"].split("___", 1)[0].strip()
        for item in questions
    )
    print("\nmost_common_blank_prefixes")
    for prefix, count in sentence_starters.most_common(20):
        print(f"{count:4d} {prefix!r}")


if __name__ == "__main__":
    main()
