package com.vtts.app

import android.content.Context
import com.k2fsa.sherpa.onnx.GenerationConfig
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsSupertonicModelConfig
import java.util.concurrent.atomic.AtomicBoolean

class TtsManager(
    private val context: Context
) {

    companion object {

        private const val MODEL_DIR = "supertonic"

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

        /*
         * 4 steps = faster generation.
         *
         * 8 steps = higher quality but slower.
         *
         * Supertonic supports changing this value.
         */
        const val DEFAULT_STEPS = 4
    }

    private var tts: OfflineTts? = null

    private val cancelled =
        AtomicBoolean(false)

    @Synchronized
    fun initialize(
        threads: Int = 4
    ) {

        if (tts != null) {
            return
        }

        verifyAssets()

        val supertonicConfig =
            OfflineTtsSupertonicModelConfig(

                durationPredictor =
                    "$MODEL_DIR/$DURATION",

                textEncoder =
                    "$MODEL_DIR/$TEXT_ENCODER",

                vectorEstimator =
                    "$MODEL_DIR/$VECTOR_ESTIMATOR",

                vocoder =
                    "$MODEL_DIR/$VOCODER",

                ttsJson =
                    "$MODEL_DIR/$TTS_JSON",

                unicodeIndexer =
                    "$MODEL_DIR/$UNICODE_INDEXER",

                voiceStyle =
                    "$MODEL_DIR/$VOICE"
            )

        val modelConfig =
            OfflineTtsModelConfig(

                supertonic =
                    supertonicConfig,

                /*
                 * More CPU threads can improve speed on
                 * multi-core Android phones.
                 *
                 * 4 is a good default without aggressively
                 * consuming the CPU.
                 */
                numThreads =
                    threads.coerceIn(
                        1,
                        8
                    ),

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
                assetManager =
                    context.assets,

                config = config
            )
    }

    fun generate(
        text: String,
        speakerId: Int,
        speed: Float,
        language: String,
        steps: Int = DEFAULT_STEPS
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

        cancelled.set(false)

        /*
         * Supertonic has 10 speakers:
         *
         * 0,1,2,3,4,5,6,7,8,9
         */
        val safeSpeaker =
            speakerId.coerceIn(
                0,
                9
            )

        val safeSpeed =
            speed.coerceIn(
                0.5f,
                2.0f
            )

        val safeSteps =
            steps.coerceIn(
                1,
                30
            )

        val generationConfig =
            GenerationConfig(

                silenceScale = 0.2f,

                speed = safeSpeed,

                sid = safeSpeaker,

                numSteps = safeSteps,

                extra =
                    mapOf(
                        "lang" to language
                    )
            )

        val audio =
            engine.generateWithConfigAndCallback(

                text = cleanText,

                config = generationConfig,

                callback = { _ ->

                    if (cancelled.get()) {
                        0
                    } else {
                        1
                    }
                }
            )

        return GeneratedResult(
            samples = audio.samples,
            sampleRate = audio.sampleRate
        )
    }

    fun stop() {

        cancelled.set(true)
    }

    fun resetStop() {

        cancelled.set(false)
    }

    fun release() {

        try {

            tts?.release()

        } finally {

            tts = null
        }
    }

    private fun verifyAssets() {

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

        val files =
            context.assets.list(
                MODEL_DIR
            )
                ?: throw IllegalStateException(
                    "Missing model assets: $MODEL_DIR"
                )

        for (file in requiredFiles) {

            if (!files.contains(file)) {

                throw IllegalStateException(
                    "Missing Supertonic model file: $file"
                )
            }
        }
    }

    data class GeneratedResult(

        val samples: FloatArray,

        val sampleRate: Int
    )
}
