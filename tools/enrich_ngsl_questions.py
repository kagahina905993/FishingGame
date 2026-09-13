#!/usr/bin/env python3
"""Add official NGSL Japanese quiz meanings and parts of speech."""

import argparse
import html
import json
import re
from pathlib import Path


ROW = re.compile(
    r"<tr><td>(\d+)</td>\s*"
    r"<td><a [^>]*>([^<]+)</a></td>\s*"
    r"<td>.*?</td>\s*<td>([^<]+)</td>\s*"
    r"<td>(.*?)</td></tr>",
    re.IGNORECASE,
)
TAG = re.compile(r"<[^>]+>")
JAPANESE = re.compile(r"[ぁ-んァ-ヶ一-龯]")

POS_NAMES = {
    "adj": "形容詞",
    "adv": "副詞",
    "adj/noun": "形容詞・名詞",
    "aux": "助動詞",
    "conj": "接続詞",
    "det": "限定詞",
    "intj": "間投詞",
    "noun": "名詞",
    "num": "数詞",
    "prep": "前置詞",
    "pron": "代名詞",
    "verb": "動詞",
    "verb, noun": "動詞・名詞",
}

OFFICIAL_ALIASES = {
    "email": "e-mail",
}

# The official Japanese learning dictionary intentionally uses short glosses.
# These reviewed overrides fix clear source errors and distinguish words that
# would otherwise have the same visible meaning and part of speech in a course.
MEANING_OVERRIDES = {
    "watch": "注意して見る",
    "discover": "初めて発見する",
    "guy": "男の人（口語）",
    "little": "小さい；少ししかない",
    "speak": "言葉を話す；発言する",
    "finish": "終える；仕上げる",
    "sort": "種類；分類する",
    "significant": "重要で意味のある",
    "okay": "大丈夫；よい",
    "task": "課題・作業",
    "apart": "互いに離れて",
    "propose": "正式に提案する",
    "film": "映画；フィルム",
    "major": "主要な；専攻",
    "somebody": "誰か（口語）",
    "pick": "選び取る",
    "particularly": "特定して；特に",
    "behavior": "振る舞い・行動",
    "chance": "偶然の機会；可能性",
    "indeed": "実際に；本当に",
    "fast": "速い；しっかり固定した",
    "sector": "産業・社会の部門",
    "debate": "賛否を論じる討論",
    "handle": "手で扱う；対処する",
    "tour": "各地を巡る旅行",
    "none": "一つもない；誰もいない",
    "mistake": "間違い",
    "notion": "考え；概念",
    "consequence": "結果；重大な影響",
    "employ": "雇用する；用いる",
    "odd": "変わった；奇数の",
    "wood": "木材；森",
    "fee": "サービスへの料金",
    "reduction": "減少；削減",
    "sick": "病気の；吐き気がする",
    "transform": "大きく変形・変質させる",
    "frighten": "ひどく怖がらせる",
    "pro": "プロ；専門家",
    "remote": "遠隔の；人里離れた",
    "tale": "物語；作り話",
    "immigrant": "移民した人",
    "phrase": "句；言い回し",
    "segment": "区分；切片",
    "exact": "厳密に正確な",
    "pour": "注ぐ",
}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--words", type=Path, required=True)
    parser.add_argument("--ngsl-japanese-html", type=Path, required=True)
    return parser.parse_args()


def parse_official_entries(path: Path) -> dict[str, dict[str, object]]:
    source = path.read_text(encoding="utf-8")
    entries = {}
    ranks = []
    for rank_text, word_text, pos_text, definition_html in ROW.findall(source):
        rank = int(rank_text)
        word = html.unescape(word_text).strip().lower()
        pos_code = pos_text.strip().lower()
        definition = html.unescape(TAG.sub("", definition_html)).strip()
        japanese, separator, _ = definition.rpartition(" - ")
        if not separator:
            raise ValueError(f"Missing definition separator: {word}")
        japanese = japanese.strip()
        if not JAPANESE.search(japanese):
            raise ValueError(f"Missing Japanese definition: {word}")
        part_of_speech = POS_NAMES.get(pos_code)
        if not part_of_speech:
            raise ValueError(f"Unsupported part of speech: {word} ({pos_code})")
        if word in entries:
            raise ValueError(f"Duplicate official NGSL word: {word}")
        entries[word] = {
            "rank": rank,
            "meaning": japanese,
            "partOfSpeech": part_of_speech,
        }
        ranks.append(rank)

    if sorted(ranks) != list(range(1, 2810)):
        raise ValueError("Official NGSL ranks must be contiguous from 1 to 2809")
    return entries


def main() -> None:
    args = parse_args()
    words = json.loads(args.words.read_text(encoding="utf-8"))
    official = parse_official_entries(args.ngsl_japanese_html)

    ngsl_words = [word for word in words if word["wordList"] == "NGSL_1_2"]
    if len(ngsl_words) != 2809:
        raise ValueError(f"Expected 2809 NGSL words, got {len(ngsl_words)}")

    for word in ngsl_words:
        english = word["english"].lower()
        official_word = OFFICIAL_ALIASES.get(english, english)
        entry = official.get(official_word)
        if not entry:
            raise ValueError(f"NGSL word not found in official dictionary: {english}")
        if word["sourceRank"] != entry["rank"]:
            raise ValueError(f"NGSL rank mismatch: {english}")

        if english in MEANING_OVERRIDES:
            word["quizMeaning"] = MEANING_OVERRIDES[english]
            word["quizSource"] = (
                "manual review / NGSL 1.2 Japanese Learning Dictionary"
            )
        elif word.get("quizSource") == "manual review":
            # Preserve the original hand-reviewed questions already in the app.
            pass
        else:
            word["quizMeaning"] = entry["meaning"]
            word["quizSource"] = "NGSL 1.2 Japanese Learning Dictionary"
        # `japanese` remains only as a backwards-compatible short-meaning
        # field. Do not restore a long dictionary body here.
        word["japanese"] = word["quizMeaning"]
        word["partOfSpeech"] = entry["partOfSpeech"]
        if not word.get("quizPartOfSpeech"):
            word["quizPartOfSpeech"] = entry["partOfSpeech"]

    args.words.write_text(
        json.dumps(words, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )


if __name__ == "__main__":
    main()
