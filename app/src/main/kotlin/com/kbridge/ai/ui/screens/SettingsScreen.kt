package com.kbridge.ai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kbridge.ai.R
import com.kbridge.ai.core.domain.phrasebook.TranslationHistoryEntry
import com.kbridge.ai.ui.TranslationModelStatus
import com.kbridge.ai.ui.components.InfoPanel
import com.kbridge.ai.ui.components.ScreenHeading

@Composable
fun SettingsScreen(
    translationModelStatus: TranslationModelStatus,
    history: List<TranslationHistoryEntry>,
    rememberHistory: Boolean,
    onRememberHistoryChanged: (Boolean) -> Unit,
    onClearHistory: () -> Unit,
) {
    var showClearHistoryConfirmation by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier =
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        ScreenHeading(
            eyebrow = R.string.tab_settings,
            title = R.string.settings_title,
            subtitle = R.string.settings_subtitle,
        )
        Text(
            text = stringResource(R.string.readiness_heading),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.1.sp,
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                ReadinessRow(
                    icon = Icons.Filled.MicOff,
                    title = R.string.readiness_microphone_title,
                    body = R.string.readiness_microphone_body,
                    status = R.string.status_not_requested,
                )
                ReadinessDivider()
                ReadinessRow(
                    icon = Icons.Filled.GraphicEq,
                    title = R.string.readiness_speech_title,
                    body = R.string.readiness_speech_body,
                    status = R.string.status_not_integrated,
                )
                ReadinessDivider()
                ReadinessRow(
                    icon = Icons.Filled.Language,
                    title = R.string.readiness_translation_title,
                    body = R.string.readiness_translation_body,
                    status = translationStatusResource(translationModelStatus),
                )
                ReadinessDivider()
                ReadinessRow(
                    icon = Icons.Filled.Psychology,
                    title = R.string.readiness_agent_title,
                    body = R.string.readiness_agent_body,
                    status = R.string.status_not_integrated,
                )
                ReadinessDivider()
                ReadinessRow(
                    icon = Icons.Filled.VolumeUp,
                    title = R.string.readiness_tts_title,
                    body = R.string.readiness_tts_body,
                    status = R.string.status_not_integrated,
                )
                ReadinessDivider()
                ReadinessRow(
                    icon = Icons.Filled.Storage,
                    title = R.string.readiness_storage_title,
                    body = R.string.readiness_storage_body,
                    status = R.string.status_local_storage,
                )
            }
        }

        TranslationHistoryPanel(
            history = history,
            rememberHistory = rememberHistory,
            onRememberHistoryChanged = onRememberHistoryChanged,
            onClearHistory = { showClearHistoryConfirmation = true },
        )

        InfoPanel(
            title = stringResource(R.string.offline_notice_title),
            body = stringResource(R.string.offline_notice_body),
            leading = {
                Icon(
                    imageVector = Icons.Filled.CloudOff,
                    contentDescription = null,
                )
            },
        )
        InfoPanel(
            title = stringResource(R.string.privacy_heading),
            body = stringResource(R.string.privacy_body),
            leading = {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                )
            },
        )
    }

    if (showClearHistoryConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearHistoryConfirmation = false },
            title = { Text(stringResource(R.string.clear_history_confirmation_title)) },
            text = { Text(stringResource(R.string.clear_history_confirmation_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearHistoryConfirmation = false
                        onClearHistory()
                    },
                ) {
                    Text(stringResource(R.string.clear_history_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryConfirmation = false }) {
                    Text(stringResource(R.string.cancel_button))
                }
            },
        )
    }
}

@Composable
private fun TranslationHistoryPanel(
    history: List<TranslationHistoryEntry>,
    rememberHistory: Boolean,
    onRememberHistoryChanged: (Boolean) -> Unit,
    onClearHistory: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.history_heading),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.history_privacy_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.history_opt_in_label),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Switch(
                    checked = rememberHistory,
                    onCheckedChange = onRememberHistoryChanged,
                )
            }
            if (history.isEmpty()) {
                Text(
                    text = stringResource(R.string.history_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                history.take(5).forEach { entry ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = entry.sourceText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = entry.translatedText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                if (history.size > 5) {
                    Text(
                        text = stringResource(R.string.history_recent_limit, history.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedButton(
                    onClick = onClearHistory,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(stringResource(R.string.clear_history_button))
                }
            }
        }
    }
}

@Composable
private fun ReadinessRow(
    icon: ImageVector,
    title: Int,
    body: Int,
    status: Int,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = stringResource(status),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun ReadinessDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f),
        thickness = 1.dp,
    )
}

private fun translationStatusResource(status: TranslationModelStatus): Int =
    when (status) {
        TranslationModelStatus.CHECKING -> R.string.status_checking
        TranslationModelStatus.NEEDS_DOWNLOAD -> R.string.status_download_required
        TranslationModelStatus.DOWNLOADING -> R.string.status_downloading
        TranslationModelStatus.READY -> R.string.status_on_device_ready
        TranslationModelStatus.ERROR -> R.string.status_error
    }
