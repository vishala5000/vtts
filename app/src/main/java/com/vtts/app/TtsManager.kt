package com.vtts.app

import android.content.Context
import com.k2fsa.sherpa.onnx.GenerationConfig
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsSupertonicModelConfig
import java.io.File

class TtsManager(
    private val context: Context
) {

    companion object {

        private const val MODEL_DIR =
            "supertonic"

        private const val DURATION =
            "duration_predictor.int8.onnx"

        private const val TEXT_ENCODER =
            "text_encoder.int8.onnx"

        private const val VECTOR_ESTIMATOR =
            "vector_estimator.int8.onnx"

        private const val VOCODER =
            "vocoder.int8.onnx"

        private const val TTS_JSON =
            "tts.json"

        private const val UNICODE_INDEXER =
            "unicode_indexer.bin"

        private const val VOICE =
            "voice.bin"
    }

    private var tts: OfflineTts? = null

    private val modelDirectory: String
        get() =
            File(
                context.filesDir,
                MODEL_DIR
            ).absolutePath

    fun initialize(
        threads: Int = 2
    ) {

        if (tts != null) {
            return
        }

        extractModelIfNeeded()

        val modelPath =
            File(
                context.filesDir,
                MODEL_DIR
            )

        val supertonicConfig =
            OfflineTtsSupertonicModelConfig(
                durationPredictor =
                    File(
                        modelPath,
                        DURATION
                    ).absolutePath,

                textEncoder =
                    File(
                        modelPath,
                        TEXT_ENCODER
                    ).absolutePath,

                vectorEstimator =
                    File(
                        modelPath,
                        VECTOR_ESTIMATOR
                    ).absolutePath,

                vocoder =
                    File(
                        modelPath,
                        VOCODER
                    ).absolutePath,

                ttsJson =
                    File(
                        modelPath,
                        TTS_JSON
                    ).absolutePath,

                unicodeIndexer =
                    File(
                        modelPath,
                        UNICODE_INDEXER
                    ).absolutePath,

                voiceStyle =
                    File(
                        modelPath,
                        VOICE
                    ).absolutePath
            )

        val modelConfig =
            OfflineTtsModelConfig(
                supertonic = supertonicConfig,
                numThreads = threads,
                debug = false,
                provider = "cpu"
            )

        val config =
            OfflineTtsConfig(
                model = modelConfig,
                maxNumSentences = 1,
                silenceScale = 0.2f
            )

        tts =
            OfflineTts(
                config = config
            )
    }

    fun generate(
        text: String,
        speakerId: Int,
        speed: Float,
        language: String,
        steps: Int = 8
    ): GeneratedResult {

        val engine =
            tts
                ?: throw IllegalStateException(
                    "TTS engine is not initialized"
                )

        val cleanText =
            text.trim()

        if (cleanText.isEmpty()) {
            throw IllegalArgumentException(
                "Text cannot be empty"
            )
        }

        val generationConfig =
            GenerationConfig(
                silenceScale = 0.2f,
                speed = speed.coerceIn(
                    0.1f,
                    5.0f
                ),
                sid = speakerId.coerceIn(
                    0,
                    9
                ),
                numSteps = steps.coerceIn(
                    1,
                    30
                ),
                extra =
                    mapOf(
                        "lang" to language
                    )
            )

        val audio =
            engine.generateWithConfig(
                cleanText,
                generationConfig
            )

        return GeneratedResult(
            samples = audio.samples,
            sampleRate = audio.sampleRate
        )
    }

    fun release() {

        tts?.release()
        tts = null
    }

    private fun extractModelIfNeeded() {

        val destination =
            File(
                context.filesDir,
                MODEL_DIR
            )

        destination.mkdirs()

        val requiredFiles =
            listOf(
                DURATION,
                TEXT_ENCODER,
                VECTOR_ESTIMATOR,
                VOCODER,
                TTS_JSON,
                UNICODE_INDEXER,
                VOICE
            )

        var complete = true

        for (filename in requiredFiles) {

            val file =
                File(
                    destination,
                    filename
                )

            if (
                !file.exists() ||
                file.length() <= 0
            ) {
                complete = false
                break
            }
        }

        if (complete) {
            return
        }

        val assetNames =
            context.assets.list(MODEL_DIR)
                ?: throw IllegalStateException(
                    "Model assets are missing"
                )

        for (filename in assetNames) {

            if (!requiredFiles.contains(filename)) {
                continue
            }

            val output =
                File(
                    destination,
                    filename
                )

            context.assets
                .open(
                    "$MODEL_DIR/$filename"
                )
                .use { input ->

                    output.outputStream()
                        .use { outputStream ->

                            val buffer =
                                ByteArray(
                                    1024 * 1024
                                )

                            while (true) {

                                val count =
                                    input.read(buffer)

                                if (count <= 0) {
                                    break
                                }

                                outputStream.write(
                                    buffer,
                                    0,
                                    count
                                )
                            }

                            outputStream.flush()
                        }
                }
        }

        for (filename in requiredFiles) {

            val file =
                File(
                    destination,
                    filename
                )

            if (
                !file.exists() ||
                file.length() <= 0
            ) {

                throw IllegalStateException(
                    "Failed to extract model file: $filename"
                )
            }
        }
    }

    data class GeneratedResult(
        val samples: FloatArray,
        val sampleRate: Int
    )
}
