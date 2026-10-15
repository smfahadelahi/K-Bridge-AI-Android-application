package com.kbridge.ai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kbridge.ai.R
import com.kbridge.ai.core.domain.conversation.TranslationDirection
import com.kbridge.ai.ui.navigation.AppDestination
import com.kbridge.ai.ui.screens.ConversationScreen
import com.kbridge.ai.ui.screens.PracticeScreen
import com.kbridge.ai.ui.screens.SavedScreen
import com.kbridge.ai.ui.screens.SettingsScreen
import com.kbridge.ai.ui.screens.TextTranslatorScreen

@Composable
fun KBridgeApp(viewModel: KBridgeViewModel = viewModel()) {
    var selectedDestination by rememberSaveable { mutableStateOf(AppDestination.CONVERSATION) }
    var direction by rememberSaveable { mutableStateOf(TranslationDirection.KOREAN_TO_BANGLA) }

    val translationState by viewModel.translationState.collectAsStateWithLifecycle()
    val phrasebook by viewModel.phrasebook.collectAsStateWithLifecycle()
    val savedPhrases by viewModel.savedPhrases.collectAsStateWithLifecycle()
    val recentHistory by viewModel.recentHistory.collectAsStateWithLifecycle()
    val rememberHistory by viewModel.rememberHistory.collectAsStateWithLifecycle()
    val onDirectionChanged: (TranslationDirection) -> Unit = { updatedDirection ->
        if (updatedDirection != direction) {
            viewModel.clearTranslationResult()
            direction = updatedDirection
        }
    }

    LaunchedEffect(selectedDestination, direction) {
        if (selectedDestination == AppDestination.TRANSLATE || selectedDestination == AppDestination.SETTINGS) {
            viewModel.refreshTranslationModel(direction)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppHeader() },
        bottomBar = {
            AppNavigationBar(
                selected = selectedDestination,
                onSelected = { selectedDestination = it },
            )
        },
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            when (selectedDestination) {
                AppDestination.CONVERSATION ->
                    ConversationScreen(
                        direction = direction,
                        onDirectionChanged = onDirectionChanged,
                        onOpenTranslator = { selectedDestination = AppDestination.TRANSLATE },
                    )

                AppDestination.TRANSLATE ->
                    TextTranslatorScreen(
                        direction = direction,
                        state = translationState,
                        onDirectionChanged = onDirectionChanged,
                        onDraftChanged = viewModel::updateDraft,
                        onPhraseScenarioChanged = viewModel::selectPhraseScenario,
                        onDownloadModel = { viewModel.downloadTranslationModel(direction) },
                        onTranslate = { viewModel.translate(direction) },
                        onSavePhrase = { viewModel.saveCurrentTranslation() },
                        onRetryModelCheck = { viewModel.refreshTranslationModel(direction) },
                    )

                AppDestination.PRACTICE ->
                    PracticeScreen(
                        savedPhrases = savedPhrases,
                        onOpenSaved = { selectedDestination = AppDestination.SAVED },
                    )

                AppDestination.SAVED ->
                    SavedScreen(
                        phrasebook = phrasebook,
                        savedPhrases = savedPhrases,
                        onToggleSaved = viewModel::setPhraseSaved,
                    )

                AppDestination.SETTINGS ->
                    SettingsScreen(
                        translationModelStatus = translationState.modelStatus,
                        history = recentHistory,
                        rememberHistory = rememberHistory,
                        onRememberHistoryChanged = viewModel::setRememberHistory,
                        onClearHistory = viewModel::clearHistory,
                    )
            }
        }
    }
}

@Composable
private fun AppHeader() {
    Column {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "K",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Column(
                modifier = Modifier.padding(start = 11.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Text(
                    text = "K-BRIDGE AI",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.05.sp,
                )
                Text(
                    text = stringResource(R.string.app_tagline),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Text(
                    text = stringResource(R.string.foundation_badge),
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.35.sp,
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
    }
}

@Composable
private fun AppNavigationBar(
    selected: AppDestination,
    onSelected: (AppDestination) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 4.dp,
    ) {
        AppDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = selected == destination,
                onClick = { onSelected(destination) },
                icon = {
                    androidx.compose.material3.Icon(
                        imageVector = destination.icon,
                        contentDescription = null,
                    )
                },
                label = { Text(stringResource(destination.label)) },
                colors =
                    NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
            )
        }
    }
}
