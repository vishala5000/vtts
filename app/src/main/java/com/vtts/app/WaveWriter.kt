package com.vtts.app

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object WaveWriter {

    fun write(
        file: File,
        samples: FloatArray,
        sampleRate: Int
    ) {
        file.parentFile?.mkdirs()

        BufferedOutputStream(
            FileOutputStream(file)
        ).use { output ->

            val pcmBytes = samples.size * 2

            writeAscii(output, "RIFF")
            writeIntLE(output, 36 + pcmBytes)

            writeAscii(output, "WAVE")

            writeAscii(output, "fmt ")
            writeIntLE(output, 16)

            // PCM
            writeShortLE(output, 1)

            // Mono
            writeShortLE(output, 1)

            writeIntLE(output, sampleRate)

            val byteRate = sampleRate * 2

            writeIntLE(output, byteRate)

            // Block align
            writeShortLE(output, 2)

            // 16-bit
            writeShortLE(output, 16)

            writeAscii(output, "data")
            writeIntLE(output, pcmBytes)

            val buffer = ByteBuffer
                .allocate(2)
                .order(ByteOrder.LITTLE_ENDIAN)

            for (sample in samples) {

                val clipped = sample.coerceIn(-1.0f, 1.0f)

                val pcm =
                    if (clipped < 0f) {
                        (clipped * 32768f).toInt()
                    } else {
                        (clipped * 32767f).toInt()
                    }

                buffer.clear()
                buffer.putShort(pcm.toShort())

                output.write(buffer.array())
            }
        }
    }

    private fun writeAscii(
        output: BufferedOutputStream,
        value: String
    ) {
        output.write(value.toByteArray(Charsets.US_ASCII))
    }

    private fun writeIntLE(
        output: BufferedOutputStream,
        value: Int
    ) {
        output.write(value and 0xFF)
        output.write((value shr 8) and 0xFF)
        output.write((value shr 16) and 0xFF)
        output.write((value shr 24) and 0xFF)
    }

    private fun writeShortLE(
        output: BufferedOutputStream,
        value: Int
    ) {
        output.write(value and 0xFF)
        output.write((value shr 8) and 0xFF)
    }
}
