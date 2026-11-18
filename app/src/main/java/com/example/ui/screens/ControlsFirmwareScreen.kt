package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Earbuds
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.FirmwareStage
import com.example.data.model.GestureSlot
import com.example.data.model.TouchAction
import com.example.ui.EarbudsUiState
import com.example.ui.components.AetherCard
import com.example.ui.theme.EmeraldStatus

@Composable
fun ControlsFirmwareScreen(
    state: EarbudsUiState,
    onUpdateGesture: (isLeft: Boolean, slot: GestureSlot, action: TouchAction) -> Unit,
    onInEarDetectionToggle: (Boolean) -> Unit,
    onAutoAnswerToggle: (Boolean) -> Unit,
    onCheckFirmware: () -> Unit,
    onInstallFirmware: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedLeftSide by remember { mutableStateOf(true) }
    var editingSlot by remember { mutableStateOf<GestureSlot?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Touch & Squeeze Gestures
        item {
            AetherCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.TouchApp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.gestures_section),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Customize Left & Right bud capacitive tap actions",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = selectedLeftSide,
                        onClick = { selectedLeftSide = true },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        modifier = Modifier.testTag("earbud_side_left")
                    ) {
                        Text(stringResource(R.string.left_earbud))
                    }
                    SegmentedButton(
                        selected = !selectedLeftSide,
                        onClick = { selectedLeftSide = false },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        modifier = Modifier.testTag("earbud_side_right")
                    ) {
                        Text(stringResource(R.string.right_earbud))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val gestures = state.prefs.gestures
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GestureSlot.entries.forEach { slot ->
                        val assignedAction = when {
                            selectedLeftSide && slot == GestureSlot.SINGLE_TAP -> gestures.leftSingle
                            selectedLeftSide && slot == GestureSlot.DOUBLE_TAP -> gestures.leftDouble
                            selectedLeftSide && slot == GestureSlot.TRIPLE_TAP -> gestures.leftTriple
                            selectedLeftSide && slot == GestureSlot.PRESS_HOLD -> gestures.leftHold
                            !selectedLeftSide && slot == GestureSlot.SINGLE_TAP -> gestures.rightSingle
                            !selectedLeftSide && slot == GestureSlot.DOUBLE_TAP -> gestures.rightDouble
                            !selectedLeftSide && slot == GestureSlot.TRIPLE_TAP -> gestures.rightTriple
                            else -> gestures.rightHold
                        }
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .clickable { editingSlot = slot }
                                .testTag("gesture_slot_${slot.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = slot.label,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = assignedAction.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Filled.ChevronRight,
                                    contentDescription = "Edit gesture",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Smart Sensors
        item {
            AetherCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Sensors,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = stringResource(R.string.smart_sensors_section),
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

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
                            imageVector = Icons.Filled.Earbuds,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.in_ear_detection),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = stringResource(R.string.in_ear_detection_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = state.prefs.inEarDetection,
                        onCheckedChange = onInEarDetectionToggle,
                        modifier = Modifier.testTag("in_ear_detection_switch")
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
                            imageVector = Icons.Filled.Call,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.auto_answer_calls),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = stringResource(R.string.auto_answer_calls_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = state.prefs.autoAnswerCalls,
                        onCheckedChange = onAutoAnswerToggle,
                        modifier = Modifier.testTag("auto_answer_switch")
                    )
                }
            }
        }

        // Firmware OTA Update Center
        item {
            val fw = state.firmware
            val isFlashing = fw.stage in listOf(
                FirmwareStage.CHECKING,
                FirmwareStage.DOWNLOADING,
                FirmwareStage.VERIFYING,
                FirmwareStage.FLASHING_LEFT,
                FirmwareStage.FLASHING_RIGHT,
                FirmwareStage.REBOOTING
            )

            AetherCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SystemUpdate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.firmware_section),
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "Current: ${fw.currentVersion} • ${fw.lastCheckedLabel}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = if (fw.hasUpdate) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.tertiaryContainer
                        }
                    ) {
                        Text(
                            text = if (fw.hasUpdate) "Update Ready" else "Latest",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (fw.hasUpdate) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                EmeraldStatus
                            },
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (isFlashing) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = fw.stage.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${(fw.progress * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                            LinearProgressIndicator(
                                progress = { fw.progress },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "Keep both buds out of case or lid open during dual-MCU flash.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else if (!fw.hasUpdate) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f)
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
                                text = "Aether Buds Pro running latest ${fw.currentVersion} firmware.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    FilledTonalButton(
                        onClick = onCheckFirmware,
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("check_updates_button")
                    ) {
                        Text(stringResource(R.string.check_updates))
                    }
                } else {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Release ${fw.latestVersion}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${fw.packageSizeMb} • ${fw.releaseDate}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            fw.releaseNotes.forEach { note ->
                                Text(
                                    text = "• $note",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val batterySafe = state.leftBattery >= 30 && state.rightBattery >= 30 && state.caseBattery >= 30
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilledTonalButton(
                            onClick = onCheckFirmware,
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("check_updates_button")
                        ) {
                            Text("Re-check")
                        }
                        Button(
                            onClick = onInstallFirmware,
                            enabled = batterySafe,
                            modifier = Modifier
                                .weight(1.4f)
                                .minimumInteractiveComponentSize()
                                .testTag("install_firmware_button")
                        ) {
                            Text(stringResource(R.string.install_update))
                        }
                    }
                }
            }
        }
    }

    editingSlot?.let { slot ->
        val gestures = state.prefs.gestures
        val currentAction = when {
            selectedLeftSide && slot == GestureSlot.SINGLE_TAP -> gestures.leftSingle
            selectedLeftSide && slot == GestureSlot.DOUBLE_TAP -> gestures.leftDouble
            selectedLeftSide && slot == GestureSlot.TRIPLE_TAP -> gestures.leftTriple
            selectedLeftSide && slot == GestureSlot.PRESS_HOLD -> gestures.leftHold
            !selectedLeftSide && slot == GestureSlot.SINGLE_TAP -> gestures.rightSingle
            !selectedLeftSide && slot == GestureSlot.DOUBLE_TAP -> gestures.rightDouble
            !selectedLeftSide && slot == GestureSlot.TRIPLE_TAP -> gestures.rightTriple
            else -> gestures.rightHold
        }
        GesturePickerDialog(
            sideLabel = if (selectedLeftSide) "Left Earbud" else "Right Earbud",
            slotLabel = slot.label,
            selectedAction = currentAction,
            onDismiss = { editingSlot = null },
            onSelect = { action ->
                onUpdateGesture(selectedLeftSide, slot, action)
                editingSlot = null
            }
        )
    }
}

@Composable
private fun GesturePickerDialog(
    sideLabel: String,
    slotLabel: String,
    selectedAction: TouchAction,
    onDismiss: () -> Unit,
    onSelect: (TouchAction) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$sideLabel • $slotLabel") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TouchAction.entries.forEach { action ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .clickable { onSelect(action) }
                            .padding(vertical = 4.dp)
                            .testTag("gesture_action_${action.name.lowercase()}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = action == selectedAction,
                            onClick = { onSelect(action) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = action.label,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Text(stringResource(R.string.close))
            }
        }
    )
}
