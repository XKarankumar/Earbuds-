package com.example.data.model

enum class AncMode(
    val title: String,
    val shortLabel: String,
    val description: String,
    val maxAttenuationDb: Int
) {
    ANC_MAX(
        title = "ANC Max",
        shortLabel = "Max",
        description = "Deep -48 dB isolation for flights & transit",
        maxAttenuationDb = 48
    ),
    ADAPTIVE(
        title = "Adaptive",
        shortLabel = "Adaptive",
        description = "Dynamically adjusts to ambient noise levels",
        maxAttenuationDb = 42
    ),
    TRANSPARENCY(
        title = "Transparency",
        shortLabel = "Ambient",
        description = "Natural pass-through for conversations",
        maxAttenuationDb = 0
    ),
    WIND_REDUCTION(
        title = "Wind Shield",
        shortLabel = "Wind",
        description = "Suppresses high-velocity airflow on outdoor mics",
        maxAttenuationDb = 36
    ),
    OFF(
        title = "Off",
        shortLabel = "Off",
        description = "Passive silicone seal only for max battery",
        maxAttenuationDb = 18
    )
}

enum class AudioCodec(
    val label: String,
    val bitrate: String,
    val specDetail: String
) {
    LDAC("LDAC", "990 kbps", "24-bit / 96 kHz Hi-Res"),
    APTX_ADAPTIVE("aptX Adaptive", "420 kbps", "24-bit / 48 kHz Dynamic"),
    AAC("AAC", "256 kbps", "Low Power Balanced"),
    SBC("SBC", "328 kbps", "Universal Compatibility")
}

enum class TouchAction(val label: String) {
    PLAY_PAUSE("Play / Pause"),
    NEXT_TRACK("Next Track"),
    PREV_TRACK("Previous Track"),
    CYCLE_ANC("Cycle ANC / Transparency"),
    VOICE_ASSISTANT("Voice Assistant"),
    VOLUME_UP("Volume Up"),
    VOLUME_DOWN("Volume Down"),
    SPATIAL_TOGGLE("Toggle Spatial Audio")
}

enum class GestureSlot(val label: String) {
    SINGLE_TAP("Single Tap"),
    DOUBLE_TAP("Double Tap"),
    TRIPLE_TAP("Triple Tap"),
    PRESS_HOLD("Press & Hold")
}

data class TouchGestureConfig(
    val leftSingle: TouchAction = TouchAction.PLAY_PAUSE,
    val leftDouble: TouchAction = TouchAction.PREV_TRACK,
    val leftTriple: TouchAction = TouchAction.VOLUME_DOWN,
    val leftHold: TouchAction = TouchAction.CYCLE_ANC,
    val rightSingle: TouchAction = TouchAction.PLAY_PAUSE,
    val rightDouble: TouchAction = TouchAction.NEXT_TRACK,
    val rightTriple: TouchAction = TouchAction.VOLUME_UP,
    val rightHold: TouchAction = TouchAction.VOICE_ASSISTANT
)

val EQ_BAND_LABELS = listOf(
    "32", "64", "125", "250", "500", "1K", "2K", "4K", "8K", "16K"
)

data class SoundProfile(
    val id: String,
    val name: String,
    val subtitle: String,
    val bandsDb: List<Float>,
    val bassBoostPercent: Int,
    val isBuiltIn: Boolean
)

data class BondedAudioDevice(
    val name: String,
    val address: String,
    val isConnected: Boolean,
    val isHardwareBacked: Boolean,
    val deviceClassLabel: String
)

enum class FirmwareStage(val label: String) {
    IDLE("Ready"),
    CHECKING("Checking Aether Cloud Manifest..."),
    AVAILABLE("Update Ready to Install"),
    DOWNLOADING("Downloading Signed Firmware Image"),
    VERIFYING("Verifying ECDSA & SHA-256 Signature"),
    FLASHING_LEFT("Flashing Left Earbud (MCU + DSP)"),
    FLASHING_RIGHT("Flashing Right Earbud (MCU + DSP)"),
    REBOOTING("Rebooting & Re-verifying Host Lock"),
    UP_TO_DATE("Up to Date")
}

data class FirmwareState(
    val currentVersion: String = "v4.2.1",
    val latestVersion: String = "v4.3.0",
    val releaseDate: String = "2026-11-10",
    val packageSizeMb: String = "14.8 MB",
    val releaseNotes: List<String> = listOf(
        "Adaptive Wind Shield v2 cuts 40 km/h wind gusts by +9 dB",
        "Spatial Audio 360° low-latency head-tracking calibration",
        "Hardened Exclusive Host Key handshake against relay spoofing",
        "Optimized LDAC 990 kbps stability in congested RF zones"
    ),
    val stage: FirmwareStage = FirmwareStage.IDLE,
    val progress: Float = 0f,
    val lastCheckedLabel: String = "Tap to check"
) {
    val hasUpdate: Boolean get() = currentVersion != latestVersion
}

enum class EarFitResult {
    IDLE,
    TESTING,
    GOOD_SEAL,
    ADJUST_LEFT,
    ADJUST_RIGHT
}

enum class RingTarget {
    NONE,
    LEFT,
    RIGHT,
    BOTH
}

data class SecurityAuditLog(
    val id: Long = 0,
    val timestampMs: Long,
    val title: String,
    val detail: String,
    val deviceLabel: String,
    val isBlockedAttempt: Boolean
)

data class SecurityLockState(
    val isExclusiveLockEnabled: Boolean = true,
    val isLostFreezeActive: Boolean = false,
    val registeredHostModel: String = "",
    val registeredHostKeyAlias: String = "aether_host_ec_v1",
    val registeredHostFingerprint: String = "",
    val boundBudsName: String = "Aether Buds Pro (Bound)",
    val boundBudsAddress: String = "E4:90:7E:A1:4C:02",
    val isHardwareBtBound: Boolean = false,
    val hasPinConfigured: Boolean = false,
    val blockedAttemptsCount: Int = 0
)
