package com.kbridge.ai.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.ui.graphics.vector.ImageVector
import com.kbridge.ai.R

enum class AppDestination(
    @StringRes val label: Int,
    val icon: ImageVector,
) {
    CONVERSATION(R.string.tab_conversation, Icons.Filled.ChatBubbleOutline),
    TRANSLATE(R.string.tab_translate, Icons.Filled.Translate),
    PRACTICE(R.string.tab_practice, Icons.Filled.School),
    SAVED(R.string.tab_saved, Icons.Filled.BookmarkBorder),
    SETTINGS(R.string.tab_settings, Icons.Filled.Settings),
}
