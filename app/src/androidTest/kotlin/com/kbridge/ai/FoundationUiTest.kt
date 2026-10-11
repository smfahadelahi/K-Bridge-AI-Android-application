package com.kbridge.ai

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FoundationUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun conversationHomeTruthfullyShowsThatMicrophoneIsOff() {
        val context = composeRule.activity

        composeRule.onNodeWithText(context.getString(R.string.microphone_off)).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.transcript_placeholder)).assertExists()
    }

    @Test
    fun translatorShowsUnavailableModelInsteadOfInventedOutput() {
        val context = composeRule.activity

        composeRule.onNodeWithText(context.getString(R.string.tab_translate)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.translation_unavailable_title)).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.translate_button)).assertIsNotEnabled()
    }
}
