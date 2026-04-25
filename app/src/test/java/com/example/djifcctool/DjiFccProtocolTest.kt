package com.example.djifcctool

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifiziert, dass die FCC-Patch-Bytes BYTE-IDENTISCH zur Quelle
 * (M4TH1EU/DJI-FCC-HACK) sind. Schlägt dieser Test fehl, darf die App
 * NICHT veröffentlicht werden — die Bytes wären fremd / unverifiziert.
 */
class DjiFccProtocolBytesTest {

    private val EXPECTED_FRAME_1 = byteArrayOf(
        0x55, 0x0D, 0x04, 0x21, 0x2A, 0x1F,
        0x00, 0x00, 0x00, 0x00, 0x01,
        0x86.toByte(), 0x20
    )

    private val EXPECTED_FRAME_2 = byteArrayOf(
        0x55, 0x18, 0x04, 0x20, 0x02, 0x09,
        0x00, 0x00, 0x40, 0x09, 0x27, 0x00,
        0x02, 0x48, 0x00, 0xFF.toByte(), 0xFF.toByte(), 0x02,
        0x00, 0x00, 0x00, 0x00,
        0x81.toByte(), 0x1F
    )

    @Test fun `frame 1 byte-identical to upstream M4TH1EU source`() {
        assertTrue(EXPECTED_FRAME_1.contentEquals(DjiFccProtocol.FCC_PATCH_FRAME_1))
    }

    @Test fun `frame 2 byte-identical to upstream M4TH1EU source`() {
        assertTrue(EXPECTED_FRAME_2.contentEquals(DjiFccProtocol.FCC_PATCH_FRAME_2))
    }

    @Test fun `total payload is exactly 37 bytes`() {
        assertEquals(37, DjiFccProtocol.totalPatchByteCount)
        assertEquals(37, DjiFccProtocol.FCC_PATCH_FRAME_1.size + DjiFccProtocol.FCC_PATCH_FRAME_2.size)
    }

    @Test fun `each frame starts with DUML magic 0x55`() {
        assertEquals(DjiFccProtocol.DUML_MAGIC, DjiFccProtocol.FCC_PATCH_FRAME_1[0])
        assertEquals(DjiFccProtocol.DUML_MAGIC, DjiFccProtocol.FCC_PATCH_FRAME_2[0])
    }

    @Test fun `each frame length byte matches array size (DUML invariant)`() {
        assertEquals(13, DjiFccProtocol.FCC_PATCH_FRAME_1[1].toInt() and 0xFF)
        assertEquals(13, DjiFccProtocol.FCC_PATCH_FRAME_1.size)
        assertEquals(24, DjiFccProtocol.FCC_PATCH_FRAME_2[1].toInt() and 0xFF)
        assertEquals(24, DjiFccProtocol.FCC_PATCH_FRAME_2.size)
    }

    @Test fun `serial parameters match upstream 19200 8N1`() {
        assertEquals(19200, DjiFccProtocol.BAUD_RATE)
        assertEquals(8, DjiFccProtocol.DATA_BITS)
        assertEquals(1, DjiFccProtocol.STOP_BITS)
    }

    @Test fun `vendor and product ids are exactly DJI N1 remote`() {
        assertEquals(0x2CA3, DjiFccProtocol.DJI_VENDOR_ID)
        assertEquals(11427, DjiFccProtocol.DJI_VENDOR_ID)
        assertEquals(0x1020, DjiFccProtocol.DJI_N1_REMOTE_PID)
        assertEquals(4128, DjiFccProtocol.DJI_N1_REMOTE_PID)
    }

    @Test fun `safe mode default is false (verified bytes)`() {
        assertEquals(false, DjiFccProtocol.SAFE_MODE)
    }

    @Test fun `patch sequence has exactly two frames in correct order`() {
        val seq = DjiFccProtocol.fccPatchSequence
        assertEquals(2, seq.size)
        assertTrue(seq[0].contentEquals(EXPECTED_FRAME_1))
        assertTrue(seq[1].contentEquals(EXPECTED_FRAME_2))
    }
}

/**
 * Pentest-/Sicherheitstests: harte Garantien gegen versehentliches oder
 * böswilliges Senden von Bytes an falsche Geräte oder mutierte Frames.
 */
class DjiFccSecurityTest {

    // ---- Strict device matcher ---------------------------------------------

    @Test fun `matcher accepts exact DJI N1 remote VID+PID`() {
        assertTrue(DjiFccProtocol.matchesDjiN1Remote(0x2CA3, 0x1020))
    }

    @Test fun `matcher rejects correct VID but wrong PID (different DJI device)`() {
        // z. B. DJI-Drohne im Direct-Mode oder DJI-RC mit Display
        assertFalse(DjiFccProtocol.matchesDjiN1Remote(0x2CA3, 0x0000))
        assertFalse(DjiFccProtocol.matchesDjiN1Remote(0x2CA3, 0x1021))
        assertFalse(DjiFccProtocol.matchesDjiN1Remote(0x2CA3, 0x101F))
        assertFalse(DjiFccProtocol.matchesDjiN1Remote(0x2CA3, 0xFFFF))
    }

    @Test fun `matcher rejects wrong VID even with matching PID`() {
        // Spoofing-Schutz: ein nicht-DJI-Gerät mit gleicher PID darf nicht matchen
        assertFalse(DjiFccProtocol.matchesDjiN1Remote(0x0000, 0x1020))
        assertFalse(DjiFccProtocol.matchesDjiN1Remote(0x18D1, 0x1020)) // Google
        assertFalse(DjiFccProtocol.matchesDjiN1Remote(0x05AC, 0x1020)) // Apple
        assertFalse(DjiFccProtocol.matchesDjiN1Remote(0x1A86, 0x1020)) // CH340
    }

    @Test fun `matcher rejects all-zero IDs`() {
        assertFalse(DjiFccProtocol.matchesDjiN1Remote(0, 0))
    }

    @Test fun `matcher rejects negative IDs`() {
        assertFalse(DjiFccProtocol.matchesDjiN1Remote(-1, -1))
        assertFalse(DjiFccProtocol.matchesDjiN1Remote(-1, 0x1020))
        assertFalse(DjiFccProtocol.matchesDjiN1Remote(0x2CA3, -1))
    }

    // ---- Frame validator ---------------------------------------------------

    @Test fun `validator accepts both verified frames`() {
        assertEquals(
            DjiFccProtocol.FrameValidation.Ok,
            DjiFccProtocol.validateFrame(DjiFccProtocol.FCC_PATCH_FRAME_1)
        )
        assertEquals(
            DjiFccProtocol.FrameValidation.Ok,
            DjiFccProtocol.validateFrame(DjiFccProtocol.FCC_PATCH_FRAME_2)
        )
    }

    @Test fun `validator rejects empty frame`() {
        val r = DjiFccProtocol.validateFrame(ByteArray(0))
        assertTrue(r is DjiFccProtocol.FrameValidation.Invalid)
    }

    @Test fun `validator rejects frame without DUML 0x55`() {
        val bad = byteArrayOf(0xAA.toByte(), 0x0D, 0x04, 0x21, 0x2A, 0x1F, 0x00, 0x00, 0x00, 0x00, 0x01, 0x86.toByte(), 0x20)
        val r = DjiFccProtocol.validateFrame(bad)
        assertTrue(r is DjiFccProtocol.FrameValidation.Invalid)
    }

    @Test fun `validator rejects frame with wrong length-byte (truncation attack)`() {
        // Längen-Byte sagt 99, Array hat aber 13 — typischer Smuggling-Versuch.
        val bad = DjiFccProtocol.FCC_PATCH_FRAME_1.copyOf()
        bad[1] = 0x63 // 99
        val r = DjiFccProtocol.validateFrame(bad)
        assertTrue(r is DjiFccProtocol.FrameValidation.Invalid)
    }

    @Test fun `validator rejects truncated frame`() {
        val truncated = DjiFccProtocol.FCC_PATCH_FRAME_2.copyOfRange(0, 10)
        val r = DjiFccProtocol.validateFrame(truncated)
        assertTrue(r is DjiFccProtocol.FrameValidation.Invalid)
    }

    @Test fun `validator rejects frame extended with extra bytes (injection)`() {
        val extended = DjiFccProtocol.FCC_PATCH_FRAME_1 + byteArrayOf(0xDE.toByte(), 0xAD.toByte())
        val r = DjiFccProtocol.validateFrame(extended)
        assertTrue(r is DjiFccProtocol.FrameValidation.Invalid)
    }

    // ---- Immutability ------------------------------------------------------

    @Test fun `patch sequence returns defensive copies (cannot mutate originals)`() {
        val first = DjiFccProtocol.fccPatchSequence
        val second = DjiFccProtocol.fccPatchSequence
        // Different array instances each call:
        assertNotSame(first[0], second[0])
        assertNotSame(first[1], second[1])
        // Mutating one copy must not affect future copies:
        first[0][5] = 0x00
        val third = DjiFccProtocol.fccPatchSequence
        assertEquals(0x1F.toByte(), third[0][5])
    }

    @Test fun `original frame constants are not corrupted by mutation of a copy`() {
        val copy = DjiFccProtocol.fccPatchSequence[0]
        copy.fill(0)
        assertEquals(0x55.toByte(), DjiFccProtocol.FCC_PATCH_FRAME_1[0])
        assertEquals(0x0D.toByte(), DjiFccProtocol.FCC_PATCH_FRAME_1[1])
    }

    // ---- Sanity: no hidden second protocol --------------------------------

    @Test fun `only two patch frames exist (no hidden third write)`() {
        assertEquals(2, DjiFccProtocol.fccPatchSequence.size)
    }
}
