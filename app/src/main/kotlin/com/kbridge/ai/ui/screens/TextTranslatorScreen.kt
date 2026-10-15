package com.kbridge.ai.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.kbridge.ai.R
import com.kbridge.ai.core.domain.conversation.TranslationDirection
import com.kbridge.ai.core.domain.phrasebook.PhraseScenario
import com.kbridge.ai.ui.TranslationError
import com.kbridge.ai.ui.TranslationModelStatus
import com.kbridge.ai.ui.TranslationUiState
import com.kbridge.ai.ui.components.DirectionSelector
import com.kbridge.ai.ui.components.ScreenHeading

@Composable
fun TextTranslatorScreen(
    direction: TranslationDirection,
    state: TranslationUiState,
    onDirectionChanged: (TranslationDirection) -> Unit,
    onDraftChanged: (String) -> Unit,
    onPhraseScenarioChanged: (PhraseScenario) -> Unit,
    onDownloadModel: () -> Unit,
    onTranslate: () -> Unit,
    onSavePhrase: () -> Unit,
    onRetryModelCheck: () -> Unit,
) {
    var showDownloadConsent by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier =
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        ScreenHeading(
            eyebrow = R.string.tab_translate,
            title = R.string.translator_title,
            subtitle = R.string.translator_subtitle,
        )
        DirectionSelector(
            selected = direction,
            onSelected = onDirectionChanged,
        )
        Text(
            text = stringResource(R.string.phrase_situation_label),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PhraseScenario.entries.forEach { scenario ->
                FilterChip(
                    selected = state.selectedScenario == scenario,
                    onClick = { onPhraseScenarioChanged(scenario) },
                    label = { Text(stringResource(scenario.translatorResourceId())) },
                )
            }
        }
        OutlinedTextField(
            value = state.draft,
            onValueChange = onDraftChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.source_text_label)) },
            minLines = 4,
            maxLines = 8,
            shape = RoundedCornerShape(18.dp),
            keyboardOptions =
                KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
            trailingIcon =
                if (state.draft.isNotEmpty()) {
                    {
                        IconButton(
                            onClick = { onDraftChanged("") },
                            modifier = Modifier.padding(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = stringResource(R.string.clear_button),
                            )
                        }
                    }
                } else {
                    null
                },
        )

        ModelAvailabilityPanel(
            status = state.modelStatus,
            onRequestDownload = { showDownloadConsent = true },
            onRetryCheck = onRetryModelCheck,
        )

        state.error?.let { error ->
            Text(
                text = stringResource(error.messageResource()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Button(
            onClick = onTranslate,
            enabled =
                state.modelStatus == TranslationModelStatus.READY &&
                    state.draft.isNotBlank() &&
                    !state.isTranslating,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 15.dp),
        ) {
            if (state.isTranslating) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(end = 10.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(imageVector = Icons.Filled.Translate, contentDescription = null)
            }
            Text(
                text = stringResource(if (state.isTranslating) R.string.translating else R.string.translate_button),
                modifier = Modifier.padding(start = 8.dp),
                fontWeight = FontWeight.SemiBold,
            )
        }

        state.result?.let { result ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.translation_result_label),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = result,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.translation_quality_notice),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                    ) {
                        if (state.resultSaved) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = stringResource(R.string.translation_saved),
                                modifier = Modifier.padding(start = 6.dp),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        } else {
                            TextButton(
                                onClick = onSavePhrase,
                                enabled = !state.isSavingPhrase,
                            ) {
                                if (state.isSavingPhrase) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                    )
                                } else {
                                    Icon(imageVector = Icons.Filled.BookmarkAdd, contentDescription = null)
                                }
                                Text(
                                    text =
                                        stringResource(
                                            if (state.isSavingPhrase) {
                                                R.string.saving_phrase
                                            } else {
                                                R.string.save_translation_phrase
                                            },
                                        ),
                                    modifier = Modifier.padding(start = 6.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDownloadConsent) {
        AlertDialog(
            onDismissRequest = { showDownloadConsent = false },
            title = { Text(stringResource(R.string.model_download_consent_title)) },
            text = { Text(stringResource(R.string.model_download_consent_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDownloadConsent = false
                        onDownloadModel()
                    },
                ) {
                    Text(stringResource(R.string.continue_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDownloadConsent = false }) {
                    Text(stringResource(R.string.cancel_button))
                }
            },
        )
    }
}

@Composable
private fun ModelAvailabilityPanel(
    status: TranslationModelStatus,
    onRequestDownload: () -> Unit,
    onRetryCheck: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when (status) {
                TranslationModelStatus.CHECKING -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 12.dp), strokeWidth = 2.dp)
                        Text(stringResource(R.string.translation_model_checking))
                    }
                }

                TranslationModelStatus.NEEDS_DOWNLOAD -> {
                    Text(
                        text = stringResource(R.string.translation_model_missing_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(R.string.translation_model_missing_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = onRequestDownload,
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(imageVector = Icons.Filled.CloudDownload, contentDescription = null)
                        Text(
                            text = stringResource(R.string.download_translation_model),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }

                TranslationModelStatus.DOWNLOADING -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 12.dp), strokeWidth = 2.dp)
                        Text(stringResource(R.string.translation_model_downloading))
                    }
                }

                TranslationModelStatus.READY -> {
                    Text(
                        text = stringResource(R.string.translation_model_ready),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                TranslationModelStatus.ERROR -> {
                    Text(
                        text = stringResource(R.string.translation_model_check_error),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    TextButton(onClick = onRetryCheck) {
                        Icon(imageVector = Icons.Filled.Refresh, contentDescription = null)
                        Text(
                            text = stringResource(R.string.retry_button),
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

private fun PhraseScenario.translatorResourceId(): Int =
    when (this) {
        PhraseScenario.EVERYDAY -> R.string.scenario_everyday
        PhraseScenario.SHOPPING -> R.string.scenario_shopping
        PhraseScenario.TRANSPORT -> R.string.scenario_transport
        PhraseScenario.HEALTH -> R.string.scenario_clinic
        PhraseScenario.WORKPLACE -> R.string.scenario_workplace
        PhraseScenario.HOUSING -> R.string.scenario_housing
    }

private fun TranslationError.messageResource(): Int =
    when (this) {
        TranslationError.MODEL_CHECK -> R.string.translation_model_check_error
        TranslationError.DOWNLOAD -> R.string.translation_download_error
        TranslationError.TRANSLATION -> R.string.translation_failed
        TranslationError.SAVE_PHRASE -> R.string.save_phrase_failed
    }
