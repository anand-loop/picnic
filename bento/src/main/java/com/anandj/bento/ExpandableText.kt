// Copyright (C) 2026 Anand Jesudason
// SPDX-License-Identifier: Apache-2.0

package com.anandj.bento

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A text block that collapses to three lines with a "show more" affordance.
 * Automatically enlarges emoji-only strings.
 *
 * @param text The text to display.
 */
@Composable
fun ExpandableText(
    text: String,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var overflows by remember { mutableStateOf(false) }
    val isEmojiOnly = remember(text) { text.isNotBlank() && isEmojiOnly(text) }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .animateContentSize(
                    spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
                )
                .padding(horizontal = 16.dp).padding(top = 12.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontSize =
                if (isEmojiOnly) {
                    (MaterialTheme.typography.bodyMedium.fontSize.value * 1.5f).sp
                } else {
                    MaterialTheme.typography.bodyMedium.fontSize
                },
            maxLines = if (expanded) Int.MAX_VALUE else 3,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { if (!expanded) overflows = it.hasVisualOverflow },
        )
        if (overflows && !expanded) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = stringResource(R.string.bento_show_more),
                modifier =
                    Modifier
                        .padding(top = 4.dp)
                        .clickable { expanded = true },
            )
        }
    }
}

private fun isEmojiOnly(text: String): Boolean {
    val trimmed = text.trim()
    var i = 0
    while (i < trimmed.length) {
        val cp = Character.codePointAt(trimmed, i)
        val charCount = Character.charCount(cp)
        // Allow whitespace between emojis
        if (Character.isWhitespace(cp)) {
            i += charCount
            continue
        }
        val type = Character.getType(cp)
        val isEmoji =
            type == Character.OTHER_SYMBOL.toInt() ||
                type == Character.SURROGATE.toInt() ||
                cp == 0xFE0F || // variation selector
                cp == 0x200D || // zero-width joiner
                cp in 0x1F3FB..0x1F3FF // skin tone modifiers
        if (!isEmoji) return false
        i += charCount
    }
    return true
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun ExpandableTextEmojiPreview() {
    CompositionLocalProvider(LocalContentColor provides Color.White) {
        ExpandableText(text = "\uD83C\uDF1F\uD83C\uDF0A\u2728")
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun ExpandableTextShortPreview() {
    CompositionLocalProvider(LocalContentColor provides Color.White) {
        ExpandableText(text = "A short caption that fits on one line.")
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun ExpandableTextLongPreview() {
    CompositionLocalProvider(LocalContentColor provides Color.White) {
        ExpandableText(
            text =
                "This is a really long caption that goes on and on and will definitely overflow the three line limit. " +
                    "It keeps going with even more text here to make absolutely sure the expand arrow shows up in the preview.",
        )
    }
}
