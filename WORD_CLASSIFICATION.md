# 単語の学年分類

## 分類の意味

`schoolGrade` は、文部科学省が学年ごとに指定した単語を表すものではありません。
このゲームでの出題順を決めるための「ゲーム内推奨学年」です。

学習指導要領が示す語数の目安、NGSL・NAWL頻度順位、公開CEFR語彙資料を使って、
6,700語を一意に分類しています。追加語の学年はゲーム内部の推定値であり、教科書準拠ではありません。

## 分類表

| `schoolGrade` | 表示 | 出典と順位 | 語数 |
|---:|---|---:|---:|
| 5 | 小学5年 | 基礎語彙推定 | 339 |
| 6 | 小学6年 | 基礎語彙推定 | 349 |
| 7 | 中学1年 | NGSL基礎帯 | 550 |
| 8 | 中学2年 | NGSL基礎帯 | 550 |
| 9 | 中学3年 | NGSL＋追加A1語彙 | 611 |
| 10 | 高校1年 | NGSL＋追加A2語彙 | 670 |
| 11 | 高校2年 | NAWL＋追加A2語彙 | 662 |
| 12 | 高校3年以降 | NGSL・NAWL＋追加B1〜C2語彙 | 2,969 |

追加語を含むため、この表は学習指導要領の語数配分を再現するものではありません。
通常プレイでは学年ではなく英検相当推定級を目標として使用します。

## 根拠と限界

文部科学省の現行学習指導要領解説では、小学校で600〜700語程度、
中学校で小学校の語に1,600〜1,800語程度の新語を加え、
高等学校でさらに1,800〜2,500語程度を扱う目安が示されています。

- [小学校学習指導要領解説（外国語活動・外国語編）](https://www.mext.go.jp/component/a_menu/education/micro_detail/__icsFiles/afieldfile/2019/03/18/1387017_011.pdf)
- [中学校学習指導要領解説（外国語編）](https://www.mext.go.jp/content/20210531-mxt_kyoiku01-100002608_010.pdf)
- [高等学校学習指導要領解説（外国語編・英語編）](https://www.mext.go.jp/component/a_menu/education/micro_detail/__icsFiles/afieldfile/2019/03/28/1407073_09_1_1.pdf)

学習指導要領は学年別の固定単語リストを定めていないため、
各学校や教科書で実際に扱う学年とは異なる場合があります。
厳密な教科書準拠分類へ切り替える場合は、使用する教科書と版を決めたうえで
別の分類元を追加してください。

## データ保全ルール

- `schoolGrade` は `5`〜`12` の定義済みコードだけを使用する。
- 読み込み時に未知のコードがあればエラーにする。
- 未分類語が1語でもあれば読み込みを失敗させる。
- 英検級や独自レベルとは別の軸として保持する。

## 英検級分類

`eikenLevel` は英検協会が指定した単語リストではありません。
英検協会は各級の能力・教育段階の目安を公開していますが、級別の固定単語表は
公開していないため、本アプリでは「英検相当の推定級」として扱います。
値は累積語彙ではなく、その級で新しく学ぶ目安の語を表します。

| `eikenLevel` | 表示 | 主な推定範囲 | 新出語数 |
|---|---|---:|---:|
| `5` | 英検5級相当 | CEFR A1基礎帯 | 334 |
| `4` | 英検4級相当 | CEFR A1中間帯 | 321 |
| `3` | 英検3級相当 | CEFR A1後半帯 | 192 |
| `pre2` | 英検準2級相当 | CEFR A2前半帯 | 634 |
| `pre2plus` | 英検準2級プラス相当 | CEFR A2後半帯 | 461 |
| `2` | 英検2級相当 | CEFR B1 | 1,707 |
| `pre1` | 英検準1級相当 | CEFR B2 | 2,139 |
| `1` | 英検1級相当 | CEFR C1/C2 | 912 |

英検協会の推奨目安は、5級が中学初級、4級が中学中級、3級が中学卒業、
準2級が高校中級、準2級プラスが高校上級、2級が高校卒業程度です。

https://www.eiken.or.jp/eiken/exam/about/

### 分類根拠

1. CEFR-J Vocabulary Profile 1.5を語・品詞単位で照合する。
2. C1/C2はOctanove Vocabulary Profile C1/C2 1.0で補う。
3. A1内の5級・4級・3級、A2内の準2級・準2級プラスは、
   CC BY-SA 4.0のWortUniversum English Vocabulary Databaseに含まれる
   基礎学年・YLE情報と、元語彙リスト内順位を補助的に使って分割する。
4. CEFR語彙表に一致しない既存433語は、NGSL語48語を一般頻度帯、
   NAWL語385語を学術語彙帯として保守的に推定する。
5. 追加2,934語はOpen English WordNetとJapanese WordNetの同一概念・同一品詞を照合し、
   その日本語語義がEJDict本文にも存在する場合だけ採用する。

各語は`estimatedCefrLevel`と`eikenClassificationBasis`を保持し、
CEFR根拠とフォールバックを区別できるようにする。

CEFR-J / C1・C2語彙データ：
https://github.com/openlanguageprofiles/olp-en-cefrj

WortUniversum English Vocabulary Database：
https://huggingface.co/datasets/cstr/grundwortschatz-voc-en

使用スナップショット（2026-09-01取得）：

- `cefrj-vocabulary-profile-1.5.csv`:
  `b0dd3c635f1c9a4fdf1490c7e5b7c48e8bbe55b652ad0c9860a95f98e10ae498`
- `octanove-vocabulary-profile-c1c2-1.0.csv`:
  `18c33a407f2f89f7b8de9671c6d45fe3ea0bce45e7d2d7dcaab48d73e0f7b380`
- `grundwortschatz_en.db.gz`:
  `dd0639b3f7dc96469a644cd4fff525a49f531ef2b58a3d6670601357422c9867`

分類は`tools/reclassify_eiken.py`で再生成できる。

### 語彙リストと順位

- `wordList` は `NGSL_1_2`、`NAWL_1_2`、`OPEN_VOCAB_EXTENDED_V1`のいずれかを保持する。
- `sourceRank` は各語彙リスト内の順位であり、異なるリスト間では比較しない。
- `ngslRank` はNGSL語だけが持ち、NAWL語と追加語では `null` とする。
- 詳細な出典とライセンスは `THIRD_PARTY_DATA.md` に記載する。

### 英検級データの保全ルール

- 定義済みコードだけを使用する。
- 読み込み時に未知のコードがあればエラーにする。
- 未分類語が1語でもあれば読み込みを失敗させる。
- 全語にCEFR推定値と分類根拠を保存する。
- CEFRと英検相当級の対応矛盾をビルド前監査で拒否する。
- 公式単語表・公式認定ではないことを画面と文書で明示する。
