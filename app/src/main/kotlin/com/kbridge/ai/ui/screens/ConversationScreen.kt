package com.kbridge.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kbridge.ai.R
import com.kbridge.ai.core.domain.conversation.TranslationDirection
import com.kbridge.ai.ui.components.DirectionSelector
import com.kbridge.ai.ui.components.InfoPanel
import com.kbridge.ai.ui.components.ScreenHeading
import com.kbridge.ai.ui.components.StatusPill

@Composable
fun ConversationScreen(
    direction: TranslationDirection,
    onDirectionChanged: (TranslationDirection) -> Unit,
    onOpenTranslator: () -> Unit,
) {
    val koreanToBangla = direction == TranslationDirection.KOREAN_TO_BANGLA
    val sourceLabel = if (koreanToBangla) R.string.korean_speaker else R.string.bangla_speaker
    val sourceLanguage = if (koreanToBangla) R.string.korean_language else R.string.bangla_language
    val resultLabel = if (koreanToBangla) R.string.bangla_meaning else R.string.korean_reply
    val resultLanguage = if (koreanToBangla) R.string.bangla_language else R.string.korean_language
    val resultPlaceholder = if (koreanToBangla) {
        R.string.translation_placeholder
    } else {
        R.string.korean_translation_placeholder
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            ScreenHeading(
                eyebrow = R.string.live_conversation_label,
                title = R.string.conversation_title,
                subtitle = R.string.conversation_subtitle,
            )
        }
        item {
            DirectionSelector(
                selected = direction,
                onSelected = onDirectionChanged,
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.microphone_status_heading),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                )
                StatusPill(label = R.string.microphone_off)
            }
        }
        item {
            SpeechPanel(
                label = sourceLabel,
                language = sourceLanguage,
                placeholder = R.string.transcript_placeholder,
            )
        }
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowDownward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
        item {
            SpeechPanel(
                label = resultLabel,
                language = resultLanguage,
                placeholder = resultPlaceholder,
            )
        }
        item {
            InfoPanel(
                title = stringResource(R.string.voice_engine_missing),
                body = stringResource(R.string.voice_engine_missing_body),
                leading = {
                    Icon(
                        imageVector = Icons.Filled.MicOff,
                        contentDescription = null,
                    )
                },
            )
        }
        item {
            InfoPanel(
                title = stringResource(R.string.reply_label),
                body = stringResource(R.string.reply_placeholder),
                leading = {
                    Icon(
                        imageVector = Icons.Filled.RecordVoiceOver,
                        contentDescription = null,
                    )
                },
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.text_fallback_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = onOpenTranslator,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 15.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.text_fallback_button),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
        item { Spacer(modifier = Modifier.height(4.dp)) }
    }
}

@Composable
private fun SpeechPanel(
    label: Int,
    language: Int,
    placeholder: Int,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(17.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.45.sp,
                )
                Spacer(modifier = Modifier.weight(1f))
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Text(
                        text = stringResource(language),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Text(
                text = stringResource(placeholder),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
