#!/usr/bin/env python3
"""Normalize authored sentence-question distractors by grammatical role."""

import argparse
import json
import re
from collections import Counter
from pathlib import Path


# These headwords are used with a different part of speech from the primary
# dictionary entry in their authored sentence. The value describes the blank.
EFFECTIVE_POS_OVERRIDES = {
    "early": "副詞",
    "today": "副詞",
    "together": "副詞",
    "finish": "動詞",
    "plus": "前置詞",
    "whereas": "接続詞",
    "carefully": "副詞",
    "totally": "副詞",
    "onto": "前置詞",
    "anybody": "代名詞",
    "fairly": "副詞",
    "tear": "名詞",
    "ought": "助動詞",
    "tourist": "名詞",
    "lawyer": "名詞",
    "typical": "形容詞",
    "plenty": "名詞",
    "moral": "形容詞",
    "shot": "名詞",
    "till": "接続詞",
    "seriously": "副詞",
    "increasingly": "副詞",
    "ourselves": "代名詞",
    "anywhere": "副詞",
    "besides": "前置詞",
    "via": "前置詞",
    "hi": "間投詞",
    "emphasize": "動詞",
    "bother": "動詞",
    "diet": "名詞",
    "nearby": "形容詞",
    "intellectual": "形容詞",
    "solid": "形容詞",
    "cloud": "名詞",
    "beside": "前置詞",
    "anger": "名詞",
    "beneath": "前置詞",
    "stroke": "名詞",
    "versus": "前置詞",
}

# Sparse grammatical categories need choices selected for the exact sentence,
# rather than merely taking the nearest dictionary ranks.
MANUAL_DISTRACTOR_WORDS = {
    "ngsl-1372-sentence-1": ("because", "if", "unless"),
    "ngsl-1431-sentence-1": ("he", "she", "they"),
    "ngsl-1477-sentence-1": ("plan", "refuse", "forget"),
    "ngsl-1571-sentence-1": ("because", "if", "unless"),
    "ngsl-1615-sentence-1": ("himself", "herself", "themselves"),
    "ngsl-1826-sentence-1": ("yes", "sorry", "okay"),
    "ngsl-2283-sentence-1": ("crazy", "parallel", "false"),
}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("words", type=Path)
    parser.add_argument("questions", type=Path)
    parser.add_argument("--write", action="store_true")
    return parser.parse_args()


def word_id(word: dict) -> str:
    return f"{word['wordList'].lower()}:{word['sourceRank']:04d}"


def part_of_speech(word: dict) -> str:
    return word.get("quizPartOfSpeech") or word.get("partOfSpeech") or ""


def uses_vowel_article(sentence: str) -> bool | None:
    match = re.search(r"\b(a|an)\s+___", sentence, re.IGNORECASE)
    if not match:
        return None
    return match.group(1).lower() == "an"


def starts_with_vowel_sound(english: str) -> bool:
    # The question set currently needs only these common written exceptions.
    consonant_sound_vowels = ("uni", "use", "user", "usual", "one", "once")
    silent_h = ("honest", "honor", "hour", "heir")
    lowered = english.lower()
    if lowered.startswith(silent_h):
        return True
    if lowered.startswith(consonant_sound_vowels):
        return False
    return lowered.startswith(tuple("aeiou"))


def main() -> None:
    args = parse_args()
    words = json.loads(args.words.read_text(encoding="utf-8"))
    questions = json.loads(args.questions.read_text(encoding="utf-8"))
    words_by_id = {word_id(word): word for word in words}
    words_by_list_and_english = {
        (word["wordList"].lower(), word["english"].lower()): word
        for word in words
    }
    replacements = 0
    changed_questions = 0

    for question in questions:
        target = words_by_id[question["wordId"]]
        manual_words = MANUAL_DISTRACTOR_WORDS.get(question["questionId"])
        if manual_words:
            selected = [
                words_by_list_and_english[
                    (target["wordList"].lower(), english)
                ]
                for english in manual_words
            ]
            if any(
                word.get("level", 99) > target.get("level", 0)
                for word in selected
            ):
                raise ValueError(
                    f"Manual distractor is too hard: {question['questionId']}"
                )
            old_ids = question["distractorWordIds"]
            new_ids = [word_id(word) for word in selected]
            if old_ids != new_ids:
                replacements += sum(item_id not in old_ids for item_id in new_ids)
                changed_questions += 1
                question["distractorWordIds"] = new_ids
            continue
        target_pos = part_of_speech(target)
        current = [words_by_id[item_id] for item_id in question["distractorWordIds"]]
        current_pos_counts = Counter(part_of_speech(word) for word in current)
        effective_pos = EFFECTIVE_POS_OVERRIDES.get(question["answer"])
        if effective_pos is None:
            target_matches = current_pos_counts[target_pos]
            if target_matches >= 2:
                effective_pos = target_pos
            else:
                effective_pos = current_pos_counts.most_common(1)[0][0]

        kept = [word for word in current if part_of_speech(word) == effective_pos]
        if len(kept) == 3:
            continue

        article_requires_vowel = uses_vowel_article(question["sentence"])
        used_ids = {question["wordId"]} | {word_id(word) for word in kept}
        candidates = [
            word for word in words
            if word_id(word) not in used_ids
            and word.get("level", 99) <= target.get("level", 0)
            and part_of_speech(word) == effective_pos
            and word.get("quizMeaning") != target.get("quizMeaning")
            and (
                article_requires_vowel is None
                or starts_with_vowel_sound(word["english"])
                == article_requires_vowel
            )
        ]
        candidates.sort(
            key=lambda word: (
                abs(word["sourceRank"] - target["sourceRank"]),
                word["sourceRank"],
                word["english"],
            )
        )
        needed = 3 - len(kept)
        if len(candidates) < needed:
            raise ValueError(
                f"Not enough {effective_pos} distractors for {question['questionId']}"
            )
        selected = kept + candidates[:needed]
        old_ids = question["distractorWordIds"]
        new_ids = [word_id(word) for word in selected]
        replacements += sum(item_id not in old_ids for item_id in new_ids)
        changed_questions += 1
        question["distractorWordIds"] = new_ids

    result = {
        "questions": len(questions),
        "changedQuestions": changed_questions,
        "replacedDistractors": replacements,
    }
    print(json.dumps(result, ensure_ascii=False))
    if args.write:
        output = json.dumps(questions, ensure_ascii=False, indent=2) + "\n"
        output = re.sub(
            r'"distractorWordIds": \[\n'
            r'\s+"([^"]+)",\n'
            r'\s+"([^"]+)",\n'
            r'\s+"([^"]+)"\n'
            r'\s+\]',
            r'"distractorWordIds": ["\1", "\2", "\3"]',
            output,
        )
        args.questions.write_text(
            output,
            encoding="utf-8",
        )


if __name__ == "__main__":
    main()
