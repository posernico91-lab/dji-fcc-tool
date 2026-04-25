package com.example.djifcctool

/**
 * DJI FCC-Protokoll – verifizierte „magic bytes".
 *
 * Quelle der Bytes: M4TH1EU/DJI-FCC-HACK auf GitHub
 * (basierend auf Reverse-Engineering von @galbb auf MavicPilots,
 *  Thread „Mavic Air 2 – switch to FCC mode using an Android app").
 *
 * Bestätigt funktionierend für:
 *  - DJI Mini 4K
 *  - DJI Mavic Air 2 / Air 2S
 *  - DJI Mini 2 / Mini 2 SE
 *  - DJI Neo 2
 *  - DJI Flip
 *
 * Ablauf: Zwei DUML-Frames werden direkt an den UNTEREN USB-Port der
 * DJI-N1/N2-Remote geschickt (CDC-ACM, 19200 8N1). Anschließend Telefon
 * abziehen und an den oberen USB-Port stecken, um die DJI-Fly-App zu nutzen.
 *
 * Reset auf CE existiert NICHT als Software-Befehl – nur per Power-Cycle.
 */
object DjiFccProtocol {

    /**
     * SAFE_MODE = true → es werden KEINE Bytes an die Hardware geschickt
     * (nur Logs für Trockenlauf). Default false, da die Bytes verifiziert sind.
     */
    const val SAFE_MODE: Boolean = false

    /** DJI Standard-USB-VID (0x2CA3 = 11427). */
    const val DJI_VENDOR_ID: Int = 0x2CA3

    /** PID der initialisierten N1-Remote (0x1020 = 4128). */
    const val DJI_N1_REMOTE_PID: Int = 0x1020

    /** Serielle Parameter laut Quell-Implementierung. */
    const val BAUD_RATE: Int = 19200
    const val DATA_BITS: Int = 8
    const val STOP_BITS: Int = 1

    /** DUML-Start-Magic-Byte. Jeder gültige Frame muss damit beginnen. */
    const val DUML_MAGIC: Byte = 0x55

    /**
     * 1. Frame – Region-Unlock (13 Bytes inkl. CRC).
     * Hex: 55 0D 04 21 2A 1F 00 00 00 00 01 86 20
     */
    val FCC_PATCH_FRAME_1: ByteArray = byteArrayOf(
        0x55, 0x0D, 0x04, 0x21, 0x2A, 0x1F,
        0x00, 0x00, 0x00, 0x00, 0x01,
        0x86.toByte(), 0x20
    )

    /**
     * 2. Frame – FCC-Aktivierung (24 Bytes inkl. CRC).
     * Hex: 55 18 04 20 02 09 00 00 40 09 27 00 02 48 00 FF FF 02 00 00 00 00 81 1F
     */
    val FCC_PATCH_FRAME_2: ByteArray = byteArrayOf(
        0x55, 0x18, 0x04, 0x20, 0x02, 0x09,
        0x00, 0x00, 0x40, 0x09, 0x27, 0x00,
        0x02, 0x48, 0x00, 0xFF.toByte(), 0xFF.toByte(), 0x02,
        0x00, 0x00, 0x00, 0x00,
        0x81.toByte(), 0x1F
    )

    /**
     * Reihenfolge der zu sendenden Pakete. Defensive Kopien, damit
     * UI-Code die Frames niemals versehentlich mutieren kann.
     */
    val fccPatchSequence: List<ByteArray>
        get() = listOf(FCC_PATCH_FRAME_1.copyOf(), FCC_PATCH_FRAME_2.copyOf())

    /** Summe der Bytes, die im Patch-Vorgang an die Remote gehen. */
    val totalPatchByteCount: Int = FCC_PATCH_FRAME_1.size + FCC_PATCH_FRAME_2.size

    /** Aktuell unterstützte Modus-Anzeige. */
    enum class FccState { UNKNOWN, CE, FCC }

    /**
     * Strikter Geräte-Filter. Nur exakte VID + PID einer initialisierten
     * DJI-N1-Remote werden akzeptiert. Kein Vendor-Only-Fallback,
     * keine Wildcards – verhindert Schreibzugriff auf falsche USB-Geräte.
     */
    fun matchesDjiN1Remote(vendorId: Int, productId: Int): Boolean =
        vendorId == DJI_VENDOR_ID && productId == DJI_N1_REMOTE_PID

    /**
     * Sicherheits-Selbsttest der Frames vor dem Senden.
     * Wird vor jedem Schreibvorgang ausgeführt; schlägt eine Prüfung fehl,
     * werden GAR keine Bytes an die Hardware geschickt.
     */
    fun validateFrame(frame: ByteArray): FrameValidation {
        if (frame.isEmpty()) return FrameValidation.Invalid("leer")
        if (frame[0] != DUML_MAGIC) return FrameValidation.Invalid("kein DUML 0x55")
        // Byte 1 in DUML = Gesamtlänge des Frames inkl. Header und CRC.
        val declaredLen = frame[1].toInt() and 0xFF
        if (declaredLen != frame.size) {
            return FrameValidation.Invalid(
                "Längen-Byte $declaredLen ≠ Frame-Länge ${frame.size}"
            )
        }
        return FrameValidation.Ok
    }

    sealed interface FrameValidation {
        data object Ok : FrameValidation
        data class Invalid(val reason: String) : FrameValidation
    }
}
