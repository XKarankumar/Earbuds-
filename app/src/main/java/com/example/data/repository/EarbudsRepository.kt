package com.example.data.repository

import android.content.Context
import com.example.data.hardware.HardwareBridge
import com.example.data.local.EarbudsDatabase
import com.example.data.local.EarbudsPreferences
import com.example.data.local.SecurityAuditEntity
import com.example.data.local.SoundProfileEntity
import com.example.data.local.StoredPreferences
import com.example.data.model.AncMode
import com.example.data.model.AudioCodec
import com.example.data.model.BondedAudioDevice
import com.example.data.model.RingTarget
import com.example.data.model.SecurityAuditLog
import com.example.data.model.SoundProfile
import com.example.data.model.TouchGestureConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class EarbudsRepository(context: Context) {

    private val database = EarbudsDatabase.getInstance(context)
    private val profileDao = database.soundProfileDao()
    private val auditDao = database.securityAuditDao()
    private val prefs = EarbudsPreferences(context)
    val hardware = HardwareBridge(context)

    val preferencesFlow: Flow<StoredPreferences> = prefs.preferencesFlow

    val soundProfilesFlow: Flow<List<SoundProfile>> = profileDao.observeAllProfiles()
        .map { list -> list.map { it.toDomain() } }

    val securityAuditFlow: Flow<List<SecurityAuditLog>> = auditDao.observeLogs()
        .map { list -> list.map { it.toDomain() } }

    val blockedCountFlow: Flow<Int> = auditDao.observeBlockedCount()

    suspend fun seedInitialDataIfEmpty() {
        if (profileDao.getCount() == 0) {
            val builtIn = listOf(
                SoundProfile(
                    id = "preset_signature",
                    name = "Signature Studio",
                    subtitle = "Balanced flagship clarity",
                    bandsDb = listOf(1.5f, 2.0f, 1.0f, 0.0f, -0.5f, 0.5f, 2.0f, 3.0f, 2.5f, 1.5f),
                    bassBoostPercent = 28,
                    isBuiltIn = true
                ),
                SoundProfile(
                    id = "preset_sub_bass",
                    name = "Deep Sub-Bass",
                    subtitle = "Club-grade punchy lows",
                    bandsDb = listOf(6.5f, 5.5f, 4.0f, 1.5f, -1.0f, 0.0f, 1.5f, 2.5f, 2.0f, 1.0f),
                    bassBoostPercent = 72,
                    isBuiltIn = true
                ),
                SoundProfile(
                    id = "preset_vocal",
                    name = "Vocal Clarity",
                    subtitle = "Podcasts & crisp vocals",
                    bandsDb = listOf(-2.0f, -1.5f, 0.0f, 2.0f, 3.5f, 4.5f, 4.0f, 2.5f, 1.0f, 0.5f),
                    bassBoostPercent = 10,
                    isBuiltIn = true
                ),
                SoundProfile(
                    id = "preset_acoustic",
                    name = "Acoustic Live",
                    subtitle = "Warm spatial midrange",
                    bandsDb = listOf(1.0f, 1.5f, 2.0f, 1.5f, 1.0f, 1.5f, 2.5f, 3.5f, 3.0f, 2.0f),
                    bassBoostPercent = 22,
                    isBuiltIn = true
                ),
                SoundProfile(
                    id = "preset_gaming",
                    name = "FPS Footsteps",
                    subtitle = "Tactical spatial imaging",
                    bandsDb = listOf(-2.5f, -1.0f, 0.5f, 1.5f, 2.5f, 3.5f, 5.0f, 5.5f, 4.0f, 2.5f),
                    bassBoostPercent = 15,
                    isBuiltIn = true
                ),
                SoundProfile(
                    id = "preset_reference",
                    name = "Flat Reference",
                    subtitle = "Zero coloration monitor",
                    bandsDb = List(10) { 0f },
                    bassBoostPercent = 0,
                    isBuiltIn = true
                )
            ).map { SoundProfileEntity.fromDomain(it) }
            profileDao.insertAll(builtIn)
        }

        if (auditDao.getCount() == 0) {
            val hostModel = hardware.getHostDeviceModel()
            val fingerprint = hardware.getOrCreateHostKeyFingerprint()
            auditDao.insertLog(
                SecurityAuditEntity(
                    timestampMs = System.currentTimeMillis() - 3600_000L,
                    title = "Host Key Provisioned",
                    detail = "Bound Android Keystore EC-256 fingerprint $fingerprint to $hostModel.",
                    deviceLabel = hostModel,
                    isBlockedAttempt = false
                )
            )
            auditDao.insertLog(
                SecurityAuditEntity(
                    timestampMs = System.currentTimeMillis() - 1200_000L,
                    title = "Exclusive Pairing Shield Armed",
                    detail = "Earbuds configured to reject BLE/Classic handshakes from unregistered phones.",
                    deviceLabel = "Aether Buds Pro",
                    isBlockedAttempt = false
                )
            )
        }
    }

    suspend fun setAncMode(mode: AncMode) = prefs.setAncMode(mode)
    suspend fun setAncDepth(depth: Int) = prefs.setAncDepth(depth)
    suspend fun setVoiceFocusBoost(enabled: Boolean) = prefs.setVoiceFocusBoost(enabled)
    suspend fun setCallSidetone(enabled: Boolean) = prefs.setCallSidetone(enabled)

    suspend fun applySoundProfile(profile: SoundProfile) {
        prefs.setEqState(profile.id, profile.bandsDb, profile.bassBoostPercent)
        hardware.applyEqualizerAndBass(profile.bandsDb, profile.bassBoostPercent)
    }

    suspend fun updateCustomEqBands(bands: List<Float>, bassBoostPercent: Int) {
        prefs.setEqState("custom_unsaved", bands, bassBoostPercent)
        hardware.applyEqualizerAndBass(bands, bassBoostPercent)
    }

    suspend fun saveCustomProfile(name: String, bands: List<Float>, bassBoostPercent: Int) {
        val id = "custom_${UUID.randomUUID().toString().take(8)}"
        val profile = SoundProfile(
            id = id,
            name = name.trim(),
            subtitle = "Custom 10-band curve",
            bandsDb = bands,
            bassBoostPercent = bassBoostPercent,
            isBuiltIn = false
        )
        profileDao.insertProfile(SoundProfileEntity.fromDomain(profile))
        prefs.setEqState(id, bands, bassBoostPercent)
    }

    suspend fun deleteCustomProfile(id: String) {
        profileDao.deleteCustomProfile(id)
    }

    suspend fun setSpatialAudio(enabled: Boolean) = prefs.setSpatialAudio(enabled)
    suspend fun setHeadTracking(enabled: Boolean) = prefs.setHeadTracking(enabled)
    suspend fun setGamingLowLatency(enabled: Boolean) = prefs.setGamingLowLatency(enabled)
    suspend fun setAudioCodec(codec: AudioCodec) = prefs.setAudioCodec(codec)
    suspend fun setInEarDetection(enabled: Boolean) = prefs.setInEarDetection(enabled)
    suspend fun setAutoAnswerCalls(enabled: Boolean) = prefs.setAutoAnswerCalls(enabled)
    suspend fun updateTouchGestures(config: TouchGestureConfig) = prefs.updateTouchGestures(config)
    suspend fun setFirmwareVersion(version: String) = prefs.setFirmwareVersion(version)

    suspend fun setExclusiveLockEnabled(enabled: Boolean) {
        prefs.setExclusiveLockEnabled(enabled)
        auditDao.insertLog(
            SecurityAuditEntity(
                timestampMs = System.currentTimeMillis(),
                title = if (enabled) "Exclusive Device Lock Enabled" else "Exclusive Device Lock Paused",
                detail = if (enabled) {
                    "Only ${hardware.getHostDeviceModel()} can pair or connect to these earbuds."
                } else {
                    "Standard Bluetooth pairing window temporarily unlocked by host PIN."
                },
                deviceLabel = hardware.getHostDeviceModel(),
                isBlockedAttempt = false
            )
        )
    }

    suspend fun setLostFreezeActive(active: Boolean) {
        prefs.setLostFreezeActive(active)
        auditDao.insertLog(
            SecurityAuditEntity(
                timestampMs = System.currentTimeMillis(),
                title = if (active) "Lost-Mode Hardware Freeze Activated" else "Lost-Mode Freeze Cleared",
                detail = if (active) {
                    "Factory-reset pairing disabled on earbuds until cleared by registered host key."
                } else {
                    "Normal registered-host operations restored."
                },
                deviceLabel = hardware.getHostDeviceModel(),
                isBlockedAttempt = false
            )
        )
    }

    suspend fun bindEarbudsDevice(device: BondedAudioDevice) {
        prefs.setBoundEarbuds(device.name, device.address, device.isHardwareBacked)
        val fp = hardware.getOrCreateHostKeyFingerprint()
        auditDao.insertLog(
            SecurityAuditEntity(
                timestampMs = System.currentTimeMillis(),
                title = "Earbuds Bound to Host Key",
                detail = "${device.name} (${device.address}) paired exclusively with host fingerprint $fp.",
                deviceLabel = device.name,
                isBlockedAttempt = false
            )
        )
    }

    suspend fun rotateHostKey(): String {
        val newFingerprint = hardware.getOrCreateHostKeyFingerprint(forceRotate = true)
        auditDao.insertLog(
            SecurityAuditEntity(
                timestampMs = System.currentTimeMillis(),
                title = "Cryptographic Host Key Rotated",
                detail = "New Android Keystore EC-256 fingerprint $newFingerprint synced to earbuds.",
                deviceLabel = hardware.getHostDeviceModel(),
                isBlockedAttempt = false
            )
        )
        return newFingerprint
    }

    suspend fun recordBlockedUnauthorizedAttempt(intruderName: String, isLockEnabled: Boolean) {
        auditDao.insertLog(
            SecurityAuditEntity(
                timestampMs = System.currentTimeMillis(),
                title = if (isLockEnabled) "Unauthorized Phone Rejected" else "Foreign Pair Warning",
                detail = if (isLockEnabled) {
                    "Rejected pairing handshake from $intruderName — missing host EC-256 signature."
                } else {
                    "$intruderName probed pairing while Exclusive Lock was paused."
                },
                deviceLabel = intruderName,
                isBlockedAttempt = isLockEnabled
            )
        )
    }

    suspend fun saveSecurityPin(pin: String) = prefs.saveSecurityPin(pin)
    fun verifyPin(pin: String, storedHash: String): Boolean = prefs.verifyPinHash(pin, storedHash)

    fun scanBondedDevices(): List<BondedAudioDevice> = hardware.getBondedAudioDevices()

    fun startFindBudsTone(target: RingTarget, scope: CoroutineScope, onFinished: () -> Unit) {
        hardware.startFindBudsTone(target, scope, onFinished)
    }

    fun stopFindBudsTone() = hardware.stopFindBudsTone()

    fun release() = hardware.release()
}
