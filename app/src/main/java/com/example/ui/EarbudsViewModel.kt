package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.StoredPreferences
import com.example.data.model.AncMode
import com.example.data.model.AudioCodec
import com.example.data.model.BondedAudioDevice
import com.example.data.model.EarFitResult
import com.example.data.model.FirmwareStage
import com.example.data.model.FirmwareState
import com.example.data.model.GestureSlot
import com.example.data.model.RingTarget
import com.example.data.model.SecurityAuditLog
import com.example.data.model.SecurityLockState
import com.example.data.model.SoundProfile
import com.example.data.model.TouchAction
import com.example.data.model.TouchGestureConfig
import com.example.data.repository.EarbudsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PendingSecurityAction {
    DISABLE_EXCLUSIVE_LOCK,
    CLEAR_LOST_FREEZE,
    ROTATE_HOST_KEY
}

data class PinDialogState(
    val visible: Boolean = false,
    val isSetupMode: Boolean = false,
    val errorMessage: String? = null,
    val pendingAction: PendingSecurityAction? = null
)

data class EarbudsUiState(
    val prefs: StoredPreferences = StoredPreferences(),
    val soundProfiles: List<SoundProfile> = emptyList(),
    val auditLogs: List<SecurityAuditLog> = emptyList(),
    val securityLock: SecurityLockState = SecurityLockState(),
    val leftBattery: Int = 92,
    val rightBattery: Int = 94,
    val caseBattery: Int = 78,
    val hasBluetoothPermission: Boolean = false,
    val isBluetoothEnabled: Boolean = false,
    val hasBluetoothRadio: Boolean = true,
    val bondedDevices: List<BondedAudioDevice> = emptyList(),
    val firmware: FirmwareState = FirmwareState(),
    val earFitResult: EarFitResult = EarFitResult.IDLE,
    val activeRingTarget: RingTarget = RingTarget.NONE,
    val pinDialog: PinDialogState = PinDialogState(),
    val statusBannerMessage: String? = null
)

private data class TransientState(
    val hostModel: String = "",
    val hostFingerprint: String = "",
    val hasBluetoothPermission: Boolean = false,
    val isBluetoothEnabled: Boolean = false,
    val hasBluetoothRadio: Boolean = true,
    val bondedDevices: List<BondedAudioDevice> = emptyList(),
    val firmware: FirmwareState = FirmwareState(),
    val earFitResult: EarFitResult = EarFitResult.IDLE,
    val activeRingTarget: RingTarget = RingTarget.NONE,
    val pinDialog: PinDialogState = PinDialogState(),
    val statusBannerMessage: String? = null
)

class EarbudsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = EarbudsRepository(application.applicationContext)

    private val transientState = MutableStateFlow(
        TransientState(
            hostModel = repository.hardware.getHostDeviceModel(),
            hostFingerprint = repository.hardware.getOrCreateHostKeyFingerprint(),
            hasBluetoothPermission = repository.hardware.hasBluetoothPermission(),
            isBluetoothEnabled = repository.hardware.isBluetoothEnabled(),
            hasBluetoothRadio = repository.hardware.hasBluetoothRadio(),
            bondedDevices = repository.scanBondedDevices()
        )
    )

    private var firmwareJob: Job? = null
    private var fitTestJob: Job? = null

    val uiState: StateFlow<EarbudsUiState> = combine(
        repository.preferencesFlow,
        repository.soundProfilesFlow,
        repository.securityAuditFlow,
        repository.blockedCountFlow,
        transientState
    ) { prefs, profiles, logs, blockedCount, transient ->
        val fwStage = if (prefs.firmwareVersion == transient.firmware.latestVersion &&
            transient.firmware.stage == FirmwareStage.IDLE
        ) {
            FirmwareStage.UP_TO_DATE
        } else {
            transient.firmware.stage
        }

        EarbudsUiState(
            prefs = prefs,
            soundProfiles = profiles,
            auditLogs = logs,
            securityLock = SecurityLockState(
                isExclusiveLockEnabled = prefs.exclusiveLockEnabled,
                isLostFreezeActive = prefs.lostFreezeActive,
                registeredHostModel = transient.hostModel,
                registeredHostFingerprint = transient.hostFingerprint,
                boundBudsName = prefs.boundBudsName,
                boundBudsAddress = prefs.boundBudsAddress,
                isHardwareBtBound = prefs.isHardwareBtBound,
                hasPinConfigured = prefs.pinHash.isNotEmpty(),
                blockedAttemptsCount = blockedCount
            ),
            hasBluetoothPermission = transient.hasBluetoothPermission,
            isBluetoothEnabled = transient.isBluetoothEnabled,
            hasBluetoothRadio = transient.hasBluetoothRadio,
            bondedDevices = transient.bondedDevices,
            firmware = transient.firmware.copy(
                currentVersion = prefs.firmwareVersion,
                stage = fwStage
            ),
            earFitResult = transient.earFitResult,
            activeRingTarget = transient.activeRingTarget,
            pinDialog = transient.pinDialog,
            statusBannerMessage = transient.statusBannerMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = EarbudsUiState()
    )

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }
    }

    fun refreshBluetoothHardware() {
        val hasPerm = repository.hardware.hasBluetoothPermission()
        val isEnabled = repository.hardware.isBluetoothEnabled()
        val hasRadio = repository.hardware.hasBluetoothRadio()
        val devices = repository.scanBondedDevices()
        transientState.update {
            it.copy(
                hasBluetoothPermission = hasPerm,
                isBluetoothEnabled = isEnabled,
                hasBluetoothRadio = hasRadio,
                bondedDevices = devices
            )
        }
    }

    // --- ANC & Ambient ---
    fun setAncMode(mode: AncMode) {
        viewModelScope.launch { repository.setAncMode(mode) }
    }

    fun setAncDepth(depth: Int) {
        viewModelScope.launch { repository.setAncDepth(depth) }
    }

    fun setVoiceFocusBoost(enabled: Boolean) {
        viewModelScope.launch { repository.setVoiceFocusBoost(enabled) }
    }

    fun setCallSidetone(enabled: Boolean) {
        viewModelScope.launch { repository.setCallSidetone(enabled) }
    }

    fun runEarTipFitTest() {
        fitTestJob?.cancel()
        fitTestJob = viewModelScope.launch {
            transientState.update { it.copy(earFitResult = EarFitResult.TESTING) }
            delay(1800)
            transientState.update { it.copy(earFitResult = EarFitResult.GOOD_SEAL) }
        }
    }

    fun resetEarTipFitTest() {
        fitTestJob?.cancel()
        transientState.update { it.copy(earFitResult = EarFitResult.IDLE) }
    }

    fun toggleFindMyBuds(target: RingTarget) {
        val current = transientState.value.activeRingTarget
        if (current == target || target == RingTarget.NONE) {
            repository.stopFindBudsTone()
            transientState.update { it.copy(activeRingTarget = RingTarget.NONE) }
        } else {
            transientState.update { it.copy(activeRingTarget = target) }
            repository.startFindBudsTone(target, viewModelScope) {
                transientState.update { it.copy(activeRingTarget = RingTarget.NONE) }
            }
        }
    }

    // --- Sound Profiles & EQ ---
    fun selectSoundProfile(profile: SoundProfile) {
        viewModelScope.launch { repository.applySoundProfile(profile) }
    }

    fun updateEqBand(index: Int, newDb: Float) {
        val currentBands = uiState.value.prefs.activeBandsDb.toMutableList()
        if (index in currentBands.indices) {
            currentBands[index] = newDb.coerceIn(-12f, 12f)
            viewModelScope.launch {
                repository.updateCustomEqBands(currentBands, uiState.value.prefs.bassBoostPercent)
            }
        }
    }

    fun updateBassBoost(percent: Int) {
        val bands = uiState.value.prefs.activeBandsDb
        viewModelScope.launch {
            repository.updateCustomEqBands(bands, percent.coerceIn(0, 100))
        }
    }

    fun resetEqFlat() {
        val flat = List(10) { 0f }
        viewModelScope.launch {
            repository.updateCustomEqBands(flat, 0)
        }
    }

    fun saveCustomProfile(name: String) {
        if (name.isBlank()) return
        val prefs = uiState.value.prefs
        viewModelScope.launch {
            repository.saveCustomProfile(name, prefs.activeBandsDb, prefs.bassBoostPercent)
            showBanner("Saved sound profile \"$name\"")
        }
    }

    fun deleteCustomProfile(id: String) {
        viewModelScope.launch {
            repository.deleteCustomProfile(id)
        }
    }

    fun setSpatialAudio(enabled: Boolean) {
        viewModelScope.launch { repository.setSpatialAudio(enabled) }
    }

    fun setHeadTracking(enabled: Boolean) {
        viewModelScope.launch { repository.setHeadTracking(enabled) }
    }

    fun setGamingLowLatency(enabled: Boolean) {
        viewModelScope.launch { repository.setGamingLowLatency(enabled) }
    }

    fun setAudioCodec(codec: AudioCodec) {
        viewModelScope.launch { repository.setAudioCodec(codec) }
    }

    // --- Controls & Sensors ---
    fun setInEarDetection(enabled: Boolean) {
        viewModelScope.launch { repository.setInEarDetection(enabled) }
    }

    fun setAutoAnswerCalls(enabled: Boolean) {
        viewModelScope.launch { repository.setAutoAnswerCalls(enabled) }
    }

    fun updateGesture(isLeft: Boolean, slot: GestureSlot, action: TouchAction) {
        val g = uiState.value.prefs.gestures
        val updated: TouchGestureConfig = when {
            isLeft && slot == GestureSlot.SINGLE_TAP -> g.copy(leftSingle = action)
            isLeft && slot == GestureSlot.DOUBLE_TAP -> g.copy(leftDouble = action)
            isLeft && slot == GestureSlot.TRIPLE_TAP -> g.copy(leftTriple = action)
            isLeft && slot == GestureSlot.PRESS_HOLD -> g.copy(leftHold = action)
            !isLeft && slot == GestureSlot.SINGLE_TAP -> g.copy(rightSingle = action)
            !isLeft && slot == GestureSlot.DOUBLE_TAP -> g.copy(rightDouble = action)
            !isLeft && slot == GestureSlot.TRIPLE_TAP -> g.copy(rightTriple = action)
            else -> g.copy(rightHold = action)
        }
        viewModelScope.launch { repository.updateTouchGestures(updated) }
    }

    // --- Firmware OTA ---
    fun checkForFirmwareUpdate() {
        if (firmwareJob?.isActive == true) return
        firmwareJob = viewModelScope.launch {
            transientState.update {
                it.copy(
                    firmware = it.firmware.copy(
                        stage = FirmwareStage.CHECKING,
                        progress = 0.25f
                    )
                )
            }
            delay(1000)
            val currentVersion = uiState.value.prefs.firmwareVersion
            val latest = transientState.value.firmware.latestVersion
            val nextStage = if (currentVersion == latest) {
                FirmwareStage.UP_TO_DATE
            } else {
                FirmwareStage.AVAILABLE
            }
            transientState.update {
                it.copy(
                    firmware = it.firmware.copy(
                        stage = nextStage,
                        progress = if (nextStage == FirmwareStage.UP_TO_DATE) 1f else 0f,
                        lastCheckedLabel = "Checked just now"
                    )
                )
            }
        }
    }

    fun startFirmwareOtaUpdate() {
        if (firmwareJob?.isActive == true) return
        firmwareJob = viewModelScope.launch {
            val steps = listOf(
                FirmwareStage.DOWNLOADING to 0.22f,
                FirmwareStage.VERIFYING to 0.40f,
                FirmwareStage.FLASHING_LEFT to 0.65f,
                FirmwareStage.FLASHING_RIGHT to 0.88f,
                FirmwareStage.REBOOTING to 0.96f
            )
            for ((stage, targetProgress) in steps) {
                transientState.update {
                    it.copy(firmware = it.firmware.copy(stage = stage, progress = targetProgress))
                }
                delay(900)
            }
            val latest = transientState.value.firmware.latestVersion
            repository.setFirmwareVersion(latest)
            transientState.update {
                it.copy(
                    firmware = it.firmware.copy(
                        currentVersion = latest,
                        stage = FirmwareStage.UP_TO_DATE,
                        progress = 1f,
                        lastCheckedLabel = "Updated just now"
                    )
                )
            }
            showBanner("Firmware updated to $latest")
        }
    }

    // --- Exclusive Device Pairing Lock & Security ---
    fun toggleExclusiveLock(enable: Boolean) {
        if (enable) {
            viewModelScope.launch {
                repository.setExclusiveLockEnabled(true)
                showBanner("Exclusive Device Lock armed for ${transientState.value.hostModel}")
            }
            return
        }
        // Disabling lock requires PIN verification (or PIN setup if none exists)
        val hasPin = uiState.value.prefs.pinHash.isNotEmpty()
        transientState.update {
            it.copy(
                pinDialog = PinDialogState(
                    visible = true,
                    isSetupMode = !hasPin,
                    errorMessage = null,
                    pendingAction = PendingSecurityAction.DISABLE_EXCLUSIVE_LOCK
                )
            )
        }
    }

    fun toggleLostFreeze(enable: Boolean) {
        if (enable) {
            viewModelScope.launch {
                repository.setLostFreezeActive(true)
                showBanner("Lost-Mode Hardware Freeze enabled")
            }
            return
        }
        val hasPin = uiState.value.prefs.pinHash.isNotEmpty()
        transientState.update {
            it.copy(
                pinDialog = PinDialogState(
                    visible = true,
                    isSetupMode = !hasPin,
                    errorMessage = null,
                    pendingAction = PendingSecurityAction.CLEAR_LOST_FREEZE
                )
            )
        }
    }

    fun requestKeyRotation() {
        val hasPin = uiState.value.prefs.pinHash.isNotEmpty()
        transientState.update {
            it.copy(
                pinDialog = PinDialogState(
                    visible = true,
                    isSetupMode = !hasPin,
                    errorMessage = null,
                    pendingAction = PendingSecurityAction.ROTATE_HOST_KEY
                )
            )
        }
    }

    fun openPinSetupDialog() {
        transientState.update {
            it.copy(
                pinDialog = PinDialogState(
                    visible = true,
                    isSetupMode = true,
                    errorMessage = null,
                    pendingAction = null
                )
            )
        }
    }

    fun dismissPinDialog() {
        transientState.update { it.copy(pinDialog = PinDialogState()) }
    }

    fun submitPin(pin: String) {
        if (pin.length != 4 || !pin.all { it.isDigit() }) {
            transientState.update {
                it.copy(pinDialog = it.pinDialog.copy(errorMessage = "Enter a 4-digit numeric PIN"))
            }
            return
        }
        val dialog = transientState.value.pinDialog
        viewModelScope.launch {
            if (dialog.isSetupMode) {
                repository.saveSecurityPin(pin)
                executePendingSecurityAction(dialog.pendingAction)
                transientState.update { it.copy(pinDialog = PinDialogState()) }
                if (dialog.pendingAction == null) {
                    showBanner("4-digit Security PIN saved")
                }
            } else {
                val storedHash = uiState.value.prefs.pinHash
                if (repository.verifyPin(pin, storedHash)) {
                    executePendingSecurityAction(dialog.pendingAction)
                    transientState.update { it.copy(pinDialog = PinDialogState()) }
                } else {
                    transientState.update {
                        it.copy(pinDialog = it.pinDialog.copy(errorMessage = "Incorrect PIN. Try again."))
                    }
                }
            }
        }
    }

    private suspend fun executePendingSecurityAction(action: PendingSecurityAction?) {
        when (action) {
            PendingSecurityAction.DISABLE_EXCLUSIVE_LOCK -> {
                repository.setExclusiveLockEnabled(false)
                showBanner("Exclusive Device Lock paused")
            }
            PendingSecurityAction.CLEAR_LOST_FREEZE -> {
                repository.setLostFreezeActive(false)
                showBanner("Lost-Mode Freeze cleared")
            }
            PendingSecurityAction.ROTATE_HOST_KEY -> {
                val fp = repository.rotateHostKey()
                transientState.update { it.copy(hostFingerprint = fp) }
                showBanner("Host EC-256 key rotated")
            }
            null -> Unit
        }
    }

    fun bindEarbuds(device: BondedAudioDevice) {
        viewModelScope.launch {
            repository.bindEarbudsDevice(device)
            showBanner("Bound ${device.name} exclusively to this phone")
        }
    }

    fun simulateUnauthorizedPhoneAttempt() {
        val isLocked = uiState.value.prefs.exclusiveLockEnabled
        val sampleForeignPhones = listOf(
            "Unknown Galaxy S24 (MAC 7C:2A:9F:11)",
            "Unregistered Pixel 9 (MAC 3A:8B:44:E2)",
            "Foreign iPhone 16 Pro (MAC 98:01:A7:5D)"
        )
        val intruder = sampleForeignPhones.random()
        viewModelScope.launch {
            repository.recordBlockedUnauthorizedAttempt(intruder, isLocked)
            if (isLocked) {
                showBanner("Blocked pairing handshake from $intruder")
            } else {
                showBanner("Warning: Exclusive Lock is paused — foreign probe detected")
            }
        }
    }

    fun clearBanner() {
        transientState.update { it.copy(statusBannerMessage = null) }
    }

    private fun showBanner(message: String) {
        transientState.update { it.copy(statusBannerMessage = message) }
    }

    override fun onCleared() {
        super.onCleared()
        repository.release()
    }
}
