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
            text = "このアプリの単語データ3,766語は、" +
                "NGSL 1.2（2,809語）とNAWL 1.2（957語）を基にしています。"
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
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onClose
        ) {
            Text(text = "戻る")
        }
    }
}
