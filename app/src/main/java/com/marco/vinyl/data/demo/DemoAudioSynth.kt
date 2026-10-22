package com.marco.vinyl.data.demo

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Erzeugt für die Demo-Platten kurze Lo-Fi-Stücke (Pad, Arpeggio, Kick, Knistern),
 * damit die App ohne mitgelieferte Audiodateien und ohne Netz etwas abspielen kann.
 */
object DemoAudioSynth {
    const val SAMPLE_RATE = 22_050

    // Moll-Progression i – VI – III – VII als Halbtonabstände zum Grundton.
    private val progression = listOf(
        intArrayOf(0, 3, 7),
        intArrayOf(-4, 0, 3),
        intArrayOf(3, 7, 10),
        intArrayOf(-2, 2, 5),
    )

    fun render(seed: Long, durationMs: Long): ShortArray {
        val random = Random(seed)
        val rootMidi = 45 + random.nextInt(0, 10)
        val bpm = 72 + random.nextInt(0, 36)
        val arpPattern = IntArray(8) { random.nextInt(0, 3) }
        val octaveUp = IntArray(8) { if (random.nextFloat() < 0.35f) 12 else 0 }

        val samples = (durationMs * SAMPLE_RATE / 1000).toInt()
        val out = ShortArray(samples)
        val beatSamples = SAMPLE_RATE * 60 / bpm
        val eighth = beatSamples / 2
        val barSamples = beatSamples * 4
        val fade = SAMPLE_RATE * 2
        val crackle = Random(seed xor 0x5EED)

        for (i in 0 until samples) {
            val t = i.toDouble() / SAMPLE_RATE
            val chord = progression[(i / barSamples) % progression.size]

            var pad = 0.0
            for (interval in chord) pad += sin(2 * PI * midiToHz(rootMidi + 12 + interval) * t)
            pad *= 0.07

            val step = (i / eighth) % 8
            val sinceNote = (i % eighth).toDouble() / SAMPLE_RATE
            val arpHz = midiToHz(rootMidi + 24 + chord[arpPattern[step]] + octaveUp[step])
            val arp = sin(2 * PI * arpHz * sinceNote) * exp(-sinceNote * 6) * 0.16

            val sinceBeat = (i % beatSamples).toDouble() / SAMPLE_RATE
            val kickHz = 50 + 90 * exp(-sinceBeat * 30)
            val kick = sin(2 * PI * kickHz * sinceBeat) * exp(-sinceBeat * 9) * 0.32

            val noise = (crackle.nextDouble() - 0.5) * 0.012 +
                if (crackle.nextInt(0, 2_500) == 0) (crackle.nextDouble() - 0.5) * 0.5 else 0.0

            val envelope = minOf(1.0, i.toDouble() / fade, (samples - i).toDouble() / fade)
            val mixed = ((pad + arp + kick) * envelope + noise).coerceIn(-1.0, 1.0)
            out[i] = (mixed * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    /** Verpackt 16-Bit-Mono-PCM als WAV-Datei. */
    fun wav(pcm: ShortArray, sampleRate: Int = SAMPLE_RATE): ByteArray {
        val dataSize = pcm.size * 2
        val buffer = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray()).putInt(36 + dataSize).put("WAVE".toByteArray())
        buffer.put("fmt ".toByteArray()).putInt(16)
            .putShort(1.toShort()) // PCM
            .putShort(1.toShort()) // Mono
            .putInt(sampleRate)
            .putInt(sampleRate * 2)
            .putShort(2.toShort())
            .putShort(16.toShort())
        buffer.put("data".toByteArray()).putInt(dataSize)
        buffer.asShortBuffer().put(pcm)
        return buffer.array()
    }

    private fun midiToHz(midi: Int): Double = 440.0 * 2.0.pow((midi - 69) / 12.0)
}
