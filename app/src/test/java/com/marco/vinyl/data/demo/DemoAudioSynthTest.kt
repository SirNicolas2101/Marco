package com.marco.vinyl.data.demo

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class DemoAudioSynthTest {

    @Test
    fun rendersRequestedLengthDeterministically() {
        val a = DemoAudioSynth.render(seed = 42, durationMs = 2_000)
        val b = DemoAudioSynth.render(seed = 42, durationMs = 2_000)
        assertEquals(DemoAudioSynth.SAMPLE_RATE * 2, a.size)
        assertArrayEquals(a, b)
        assertTrue("should not be silent", a.any { it.toInt() != 0 })
    }

    @Test
    fun wavHeaderIsValid() {
        val pcm = shortArrayOf(1, -1, 1000, -1000)
        val wav = DemoAudioSynth.wav(pcm)
        val buf = ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN)

        assertEquals(44 + pcm.size * 2, wav.size)
        assertEquals("RIFF", String(wav, 0, 4))
        assertEquals(wav.size - 8, buf.getInt(4))
        assertEquals("WAVE", String(wav, 8, 4))
        assertEquals(1, buf.getShort(20).toInt()) // PCM
        assertEquals(1, buf.getShort(22).toInt()) // Mono
        assertEquals(DemoAudioSynth.SAMPLE_RATE, buf.getInt(24))
        assertEquals(16, buf.getShort(34).toInt())
        assertEquals("data", String(wav, 36, 4))
        assertEquals(pcm.size * 2, buf.getInt(40))
        assertEquals(-1000, buf.getShort(44 + 6).toInt())
    }
}
