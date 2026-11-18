package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NoiseAware
import androidx.compose.material.icons.filled.NoiseControlOff
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AncMode
import com.example.data.model.EarFitResult
import com.example.data.model.RingTarget
import com.example.ui.EarbudsUiState
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.AzureAccent
import com.example.ui.theme.EmeraldStatus
import com.example.ui.components.AetherCard
import com.example.ui.components.BatteryRingGauge

@Composable
fun DeviceAncHubScreen(
    state: EarbudsUiState,
    onAncModeSelected: (AncMode) -> Unit,
    onAncDepthChanged: (Int) -> Unit,
    onVoiceFocusToggle: (Boolean) -> Unit,
    onSidetoneToggle: (Boolean) -> Unit,
    onRunFitTest: () -> Unit,
    onRingBuds: (RingTarget) -> Unit,
    onOpenSecurityTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Earbuds Card
        item {
            AetherCard(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(186.dp)
                        .clip(MaterialTheme.shapes.medium)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_earbuds_hero),
                        contentDescription = "Aether Buds Pro hardware render",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.background.copy(alpha = 0.88f)
                                    )
                                )
                            )
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            border = BorderStroke(
                                1.dp,
                                if (state.securityLock.isExclusiveLockEnabled) {
                                    EmeraldStatus.copy(alpha = 0.6f)
                                } else {
                                    AmberAlert.copy(alpha = 0.6f)
                                }
                            ),
                            modifier = Modifier
                                .clickable(onClick = onOpenSecurityTab)
                                .testTag("hero_security_badge")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Shield,
                                    contentDescription = "Host Lock Status",
                                    tint = if (state.securityLock.isExclusiveLockEnabled) {
                                        EmeraldStatus
                                    } else {
                                        AmberAlert
                                    },
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (state.securityLock.isExclusiveLockEnabled) {
                                        stringResource(R.string.status_locked_to_host)
                                    } else {
                                        "Lock Paused"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                        ) {
                            Text(
                                text = "${state.prefs.audioCodec.label} • ${state.firmware.currentVersion}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(14.dp)
                    ) {
                        Text(
                            text = state.securityLock.boundBudsName,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.BluetoothConnected,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (state.securityLock.isHardwareBtBound) {
                                    "Bound Hardware • ${state.securityLock.boundBudsAddress}"
                                } else {
                                    "Exclusive Host Link • ${state.securityLock.registeredHostModel}"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tri-Gauge Battery Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    BatteryRingGauge(
                        label = stringResource(R.string.battery_left),
                        percent = state.leftBattery,
                        accentColor = MaterialTheme.colorScheme.primary
                    )
                    BatteryRingGauge(
                        label = stringResource(R.string.battery_right),
                        percent = state.rightBattery,
                        accentColor = AzureAccent
                    )
                    BatteryRingGauge(
                        label = stringResource(R.string.battery_case),
                        percent = state.caseBattery,
                        accentColor = EmeraldStatus
                    )
                }
            }
        }

        // Active Noise Cancellation Section
        item {
            AetherCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.section_anc),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = state.prefs.ancMode.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        val effectiveDb = if (state.prefs.ancMode == AncMode.ANC_MAX ||
                            state.prefs.ancMode == AncMode.ADAPTIVE ||
                            state.prefs.ancMode == AncMode.WIND_REDUCTION
                        ) {
                            "-${(state.prefs.ancMode.maxAttenuationDb * (state.prefs.ancDepth / 100f)).toInt()} dB"
                        } else if (state.prefs.ancMode == AncMode.TRANSPARENCY) {
                            "+Pass"
                        } else {
                            "-18 dB"
                        }
                        Text(
                            text = effectiveDb,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(AncMode.entries) { mode ->
                        val selected = state.prefs.ancMode == mode
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            border = BorderStroke(
                                width = if (selected) 1.5.dp else 1.dp,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                }
                            ),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clickable { onAncModeSelected(mode) }
                                .testTag("anc_mode_${mode.name.lowercase()}")
                        ) {
                            Column(
                                modifier = Modifier
                                    .width(96.dp)
                                    .padding(horizontal = 10.dp, vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = ancModeIcon(mode),
                                    contentDescription = mode.title,
                                    tint = if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = mode.shortLabel,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = state.prefs.ancMode != AncMode.OFF
                ) {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (state.prefs.ancMode == AncMode.TRANSPARENCY) {
                                    "Ambient Pass-Through Level"
                                } else {
                                    stringResource(R.string.anc_depth_label)
                                },
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "${state.prefs.ancDepth}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = state.prefs.ancDepth.toFloat(),
                            onValueChange = { onAncDepthChanged(it.toInt()) },
                            valueRange = 10f..100f,
                            modifier = Modifier.testTag("anc_depth_slider")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Voice Focus & Sidetone Toggles
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.RecordVoiceOver,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.transparency_voice_focus),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = stringResource(R.string.transparency_voice_focus_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = state.prefs.voiceFocusBoost,
                        onCheckedChange = onVoiceFocusToggle,
                        modifier = Modifier.testTag("voice_focus_switch")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.GraphicEq,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.sidetone_label),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = stringResource(R.string.sidetone_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = state.prefs.callSidetone,
                        onCheckedChange = onSidetoneToggle,
                        modifier = Modifier.testTag("sidetone_switch")
                    )
                }
            }
        }

        // Ear Tip Seal Test & Find My Earbuds
        item {
            AetherCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.ear_tip_fit_test),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = stringResource(R.string.ear_tip_fit_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalButton(
                        onClick = onRunFitTest,
                        enabled = state.earFitResult != EarFitResult.TESTING,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("run_fit_test_button")
                    ) {
                        if (state.earFitResult == EarFitResult.TESTING) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Testing...")
                        } else {
                            Text("Start Test")
                        }
                    }
                }

                if (state.earFitResult == EarFitResult.GOOD_SEAL) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = EmeraldStatus
                            )
                            Text(
                                text = "Optimal Acoustic Seal (L: -47 dB • R: -48 dB)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }
        }

        // Find My Earbuds Stereo Tone Generator
        item {
            AetherCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.find_my_buds),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Plays targeted acoustic chime to Left or Right driver",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RingTargetButton(
                        label = if (state.activeRingTarget == RingTarget.LEFT) {
                            stringResource(R.string.stop_ring)
                        } else {
                            stringResource(R.string.find_left)
                        },
                        isActive = state.activeRingTarget == RingTarget.LEFT,
                        onClick = { onRingBuds(RingTarget.LEFT) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("find_left_button")
                    )
                    RingTargetButton(
                        label = if (state.activeRingTarget == RingTarget.RIGHT) {
                            stringResource(R.string.stop_ring)
                        } else {
                            stringResource(R.string.find_right)
                        },
                        isActive = state.activeRingTarget == RingTarget.RIGHT,
                        onClick = { onRingBuds(RingTarget.RIGHT) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("find_right_button")
                    )
                    RingTargetButton(
                        label = if (state.activeRingTarget == RingTarget.BOTH) {
                            stringResource(R.string.stop_ring)
                        } else {
                            stringResource(R.string.find_both)
                        },
                        isActive = state.activeRingTarget == RingTarget.BOTH,
                        onClick = { onRingBuds(RingTarget.BOTH) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("find_both_button")
                    )
                }
            }
        }
    }
}

@Composable
private fun RingTargetButton(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isActive) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
            contentColor = if (isActive) {
                MaterialTheme.colorScheme.onError
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        ),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
        modifier = modifier.minimumInteractiveComponentSize()
    ) {
        Icon(
            imageVector = if (isActive) Icons.Filled.Stop else Icons.AutoMirrored.Filled.VolumeUp,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelLarge)
    }
}

private fun ancModeIcon(mode: AncMode): ImageVector {
    return when (mode) {
        AncMode.ANC_MAX -> Icons.Filled.VerifiedUser
        AncMode.ADAPTIVE -> Icons.Filled.NoiseAware
        AncMode.TRANSPARENCY -> Icons.Filled.Hearing
        AncMode.WIND_REDUCTION -> Icons.Filled.Air
        AncMode.OFF -> Icons.Filled.NoiseControlOff
    }
}
