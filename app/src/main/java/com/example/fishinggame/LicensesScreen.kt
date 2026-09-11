package com.example.fishinggame

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp

private const val NGSL_URL =
    "https://www.newgeneralservicelist.com/new-general-service-list"
private const val NAWL_URL =
    "https://www.newgeneralservicelist.com/new-academic-word-list"
private const val CC_BY_SA_URL =
    "https://creativecommons.org/licenses/by-sa/4.0/"
private const val EJDICT_URL =
    "https://github.com/kujirahand/EJDict"
private const val CC0_URL =
    "https://creativecommons.org/publicdomain/zero/1.0/"
private const val CEFR_J_URL =
    "https://github.com/openlanguageprofiles/olp-en-cefrj"
private const val WORT_UNIVERSUM_URL =
    "https://huggingface.co/datasets/cstr/grundwortschatz-voc-en"
private const val OPEN_ENGLISH_WORDNET_URL =
    "https://en-word.net/downloads"
private const val JAPANESE_WORDNET_URL =
    "https://bond-lab.github.io/wnja/"
private const val SOUND_EFFECT_LAB_URL =
    "https://soundeffect-lab.info/"
private const val SOUND_EFFECT_LAB_TERMS_URL =
    "https://soundeffect-lab.info/agreement/"

@Composable
fun LicensesScreen(
    modifier: Modifier = Modifier,
    onClose: () -> Unit
) {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "単語データの権利情報",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "このアプリの単語データ6,700語は、" +
                "NGSL 1.2（2,809語）、NAWL 1.2（957語）、" +
                "公開語彙資料を照合した追加2,934語で構成しています。"
        )
        Text(
            text = "文章穴埋め問題と文章4択の英文・和文・解説は、" +
                "外部例文を転載せず、このプロジェクト用に新規作成しています。"
        )
        Text(
            text = "NGSL 1.2 / NAWL 1.2\n" +
                "Authors: Charles Browne, Brent Culligan, Joseph Phillips\n" +
                "License: CC BY-SA 4.0"
        )
        Text(
            text = "変更内容：JSON形式への変換、日本語訳の付加、" +
                "ゲーム内推奨学年・英検級・独自レベルへの分類。" +
                "変更後の単語データもCC BY-SA 4.0で提供します。" +
                "原著作者がこのアプリを推奨していることを示すものではありません。"
        )
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { uriHandler.openUri(NGSL_URL) }
        ) {
            Text(text = "NGSL公式ページ")
        }
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { uriHandler.openUri(NAWL_URL) }
        ) {
            Text(text = "NAWL公式ページ")
        }
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { uriHandler.openUri(CC_BY_SA_URL) }
        ) {
            Text(text = "CC BY-SA 4.0")
        }

        Text(
            text = "EJDict-hand\n" +
                "Maintainer: kujirahand\n" +
                "License: CC0 1.0 / Public Domain"
        )
        Text(
            text = "英和辞書本文の付与に使用しています。" +
                "原著作者がこのアプリを推奨していることを示すものではありません。"
        )
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { uriHandler.openUri(EJDICT_URL) }
        ) {
            Text(text = "EJDict-hand配布元")
        }
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { uriHandler.openUri(CC0_URL) }
        ) {
            Text(text = "CC0 1.0")
        }
        Text(
            text = "英検相当級の推定\n" +
                "CEFR-J Vocabulary Profile 1.5: " +
                "東京外国語大学 投野由紀夫研究室\n" +
                "Octanove Vocabulary Profile C1/C2 1.0: " +
                "CC BY-SA 4.0"
        )
        Text(
            text = "英検協会の公式単語表ではありません。" +
                "CEFR語彙レベル、基礎語彙データ、" +
                "NGSL・NAWL順位から本アプリが推定した分類です。" +
                "英検協会による承認・推奨を示すものではありません。"
        )
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { uriHandler.openUri(CEFR_J_URL) }
        ) {
            Text(text = "CEFR-J語彙データと利用条件")
        }
        Text(
            text = "追加語彙の照合元\n" +
                "WortUniversum: CC BY-SA 4.0\n" +
                "Open English WordNet 2024: CC BY 4.0\n" +
                "Japanese Wordnet 2.0: WordNet形式のライセンス"
        )
        Text(
            text = "Japanese Wordnet 2.0 © 2009-2011 NICT, " +
                "2012-2015 Francis Bond, 2016-2024 Francis Bond and " +
                "Takayuki Kuribayashi"
        )
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { uriHandler.openUri(WORT_UNIVERSUM_URL) }
        ) {
            Text(text = "WortUniversum")
        }
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { uriHandler.openUri(OPEN_ENGLISH_WORDNET_URL) }
        ) {
            Text(text = "Open English WordNet")
        }
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { uriHandler.openUri(JAPANESE_WORDNET_URL) }
        ) {
            Text(text = "Japanese Wordnet")
        }
        Text(
            text = "効果音\n" +
                "提供：効果音ラボ（Sound Effect Lab）\n" +
                "投入・着水・捕獲などの操作・演出音として使用しています。"
        )
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { uriHandler.openUri(SOUND_EFFECT_LAB_URL) }
        ) {
            Text(text = "効果音ラボ")
        }
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { uriHandler.openUri(SOUND_EFFECT_LAB_TERMS_URL) }
        ) {
            Text(text = "効果音ラボ利用規約")
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onClose
        ) {
            Text(text = "戻る")
        }
    }
}
