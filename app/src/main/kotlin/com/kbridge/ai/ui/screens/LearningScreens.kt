package com.kbridge.ai.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kbridge.ai.R
import com.kbridge.ai.core.domain.phrasebook.PhraseEntry
import com.kbridge.ai.core.domain.phrasebook.PhraseScenario
import com.kbridge.ai.core.domain.phrasebook.PracticeQuestion
import com.kbridge.ai.core.domain.phrasebook.PracticeQuiz
import com.kbridge.ai.core.domain.phrasebook.QuizDirection
import com.kbridge.ai.ui.components.InfoPanel
import com.kbridge.ai.ui.components.ScreenHeading

@Composable
fun PracticeScreen(
    savedPhrases: List<PhraseEntry>,
    onOpenSaved: () -> Unit,
) {
    var phraseId by rememberSaveable { mutableStateOf<String?>(null) }
    var directionName by rememberSaveable { mutableStateOf(QuizDirection.KOREAN_TO_BANGLA.name) }
    var answerVisible by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(savedPhrases) {
        if (savedPhrases.none { it.id == phraseId }) {
            val next = PracticeQuiz.nextQuestion(savedPhrases)
            phraseId = next?.phraseId
            directionName = next?.direction?.name ?: QuizDirection.KOREAN_TO_BANGLA.name
            answerVisible = false
        }
    }

    val direction = QuizDirection.entries.firstOrNull { it.name == directionName } ?: QuizDirection.KOREAN_TO_BANGLA
    val phrase = savedPhrases.firstOrNull { it.id == phraseId }

    Column(
        modifier =
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        ScreenHeading(
            eyebrow = R.string.tab_practice,
            title = R.string.practice_title,
            subtitle = R.string.practice_subtitle,
        )
        if (phrase == null) {
            InfoPanel(
                title = stringResource(R.string.practice_empty_title),
                body = stringResource(R.string.practice_empty_body),
                leading = { Icon(imageVector = Icons.Filled.AutoStories, contentDescription = null) },
            )
            Button(onClick = onOpenSaved, shape = RoundedCornerShape(16.dp)) {
                Text(stringResource(R.string.browse_phrasebook))
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        } else {
            PracticeCard(
                phrase = phrase,
                direction = direction,
                answerVisible = answerVisible,
                onToggleAnswer = { answerVisible = !answerVisible },
            )
            Button(
                onClick = {
                    val previous =
                        PracticeQuestion(
                            phraseId = phrase.id,
                            direction = direction,
                        )
                    val next =
                        PracticeQuiz.nextQuestion(
                            savedPhrases = savedPhrases,
                            previousPhraseId = previous.phraseId,
                            previousDirection = previous.direction,
                        )
                    phraseId = next?.phraseId
                    directionName = next?.direction?.name ?: direction.name
                    answerVisible = false
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(vertical = 15.dp),
            ) {
                Text(stringResource(R.string.quiz_next_question))
            }
            Text(
                text = stringResource(R.string.quiz_privacy_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PracticeCard(
    phrase: PhraseEntry,
    direction: QuizDirection,
    answerVisible: Boolean,
    onToggleAnswer: () -> Unit,
) {
    val prompt =
        if (direction == QuizDirection.KOREAN_TO_BANGLA) {
            phrase.korean
        } else {
            phrase.bangla
        }
    val answer =
        if (direction == QuizDirection.KOREAN_TO_BANGLA) {
            phrase.bangla
        } else {
            phrase.korean
        }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp),
        ) {
            Text(
                text =
                    stringResource(
                        if (direction == QuizDirection.KOREAN_TO_BANGLA) {
                            R.string.quiz_prompt_korean_to_bangla
                        } else {
                            R.string.quiz_prompt_bangla_to_korean
                        },
                    ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = prompt,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (answerVisible) {
                Text(
                    text = answer,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(onClick = onToggleAnswer, shape = RoundedCornerShape(14.dp)) {
                Text(stringResource(if (answerVisible) R.string.quiz_hide_answer else R.string.quiz_show_answer))
            }
        }
    }
}

@Composable
fun SavedScreen(
    phrasebook: List<PhraseEntry>,
    savedPhrases: List<PhraseEntry>,
    onToggleSaved: (PhraseEntry, Boolean) -> Unit,
) {
    var showSavedOnly by rememberSaveable { mutableStateOf(false) }
    var scenarioId by rememberSaveable { mutableStateOf(ALL_SCENARIOS) }
    val source = if (showSavedOnly) savedPhrases else phrasebook
    val visiblePhrases =
        if (scenarioId == ALL_SCENARIOS) {
            source
        } else {
            source.filter { it.scenario.id == scenarioId }
        }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            ScreenHeading(
                eyebrow = R.string.tab_saved,
                title = R.string.saved_title,
                subtitle = R.string.saved_subtitle,
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !showSavedOnly,
                    onClick = { showSavedOnly = false },
                    label = { Text(stringResource(R.string.phrasebook_filter_all)) },
                )
                FilterChip(
                    selected = showSavedOnly,
                    onClick = { showSavedOnly = true },
                    label = { Text(stringResource(R.string.phrasebook_filter_saved)) },
                )
            }
        }
        item {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ScenarioChip(ALL_SCENARIOS, scenarioId, R.string.scenario_all) { scenarioId = it }
                PhraseScenario.entries.forEach { scenario ->
                    ScenarioChip(scenario.id, scenarioId, scenario.resourceId()) { scenarioId = it }
                }
            }
        }
        if (showSavedOnly && savedPhrases.isEmpty()) {
            item {
                InfoPanel(
                    title = stringResource(R.string.saved_empty_title),
                    body = stringResource(R.string.saved_empty_body),
                    leading = { Icon(imageVector = Icons.Filled.BookmarkBorder, contentDescription = null) },
                )
            }
        } else if (!showSavedOnly && phrasebook.isEmpty()) {
            item {
                InfoPanel(
                    title = stringResource(R.string.phrasebook_loading_title),
                    body = stringResource(R.string.phrasebook_loading_body),
                    leading = { Icon(imageVector = Icons.Filled.AutoStories, contentDescription = null) },
                )
            }
        } else if (visiblePhrases.isEmpty()) {
            item {
                InfoPanel(
                    title = stringResource(R.string.phrasebook_no_matches_title),
                    body = stringResource(R.string.phrasebook_no_matches_body),
                    leading = { Icon(imageVector = Icons.Filled.Search, contentDescription = null) },
                )
            }
        } else {
            items(visiblePhrases, key = PhraseEntry::id) { phrase ->
                PhraseCard(
                    phrase = phrase,
                    onToggleSaved = { onToggleSaved(phrase, !phrase.isSaved) },
                )
            }
        }
    }
}

@Composable
private fun ScenarioChip(
    id: String,
    selectedId: String,
    label: Int,
    onSelected: (String) -> Unit,
) {
    FilterChip(
        selected = selectedId == id,
        onClick = { onSelected(id) },
        label = { Text(stringResource(label)) },
    )
}

@Composable
private fun PhraseCard(
    phrase: PhraseEntry,
    onToggleSaved: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(phrase.scenario.resourceId()),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onToggleSaved) {
                    Icon(
                        imageVector = if (phrase.isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        contentDescription =
                            stringResource(
                                if (phrase.isSaved) R.string.remove_saved_phrase else R.string.save_phrase,
                            ),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Text(
                text = phrase.korean,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = phrase.bangla,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun PhraseScenario.resourceId(): Int =
    when (this) {
        PhraseScenario.EVERYDAY -> R.string.scenario_everyday
        PhraseScenario.SHOPPING -> R.string.scenario_shopping
        PhraseScenario.TRANSPORT -> R.string.scenario_transport
        PhraseScenario.HEALTH -> R.string.scenario_clinic
        PhraseScenario.WORKPLACE -> R.string.scenario_workplace
        PhraseScenario.HOUSING -> R.string.scenario_housing
    }

private const val ALL_SCENARIOS = "all"
