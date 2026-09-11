package com.vtts.app

import android.app.Activity
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.Future
import kotlin.math.roundToInt

class MainActivity : Activity() {

    private lateinit var textInput: EditText
    private lateinit var languageSpinner: Spinner
    private lateinit var speakerSpinner: Spinner
    private lateinit var speedSeekBar: SeekBar
    private lateinit var speedValue: TextView
    private lateinit var statusText: TextView

    private lateinit var speakButton: Button
    private lateinit var stopButton: Button
    private lateinit var saveButton: Button

    private lateinit var ttsManager: TtsManager

    private val executor =
        Executors.newSingleThreadExecutor()

    private val mainHandler =
        Handler(Looper.getMainLooper())

    private var currentTask: Future<*>? = null

    private var latestSamples: FloatArray? = null
    private var latestSampleRate: Int = 24000

    private var audioTrack: AudioTrack? = null

    private val languages =
        listOf(
            Language("English", "en"),
            Language("Korean", "ko"),
            Language("Japanese", "ja"),
            Language("Arabic", "ar"),
            Language("Bulgarian", "bg"),
            Language("Czech", "cs"),
            Language("Danish", "da"),
            Language("German", "de"),
            Language("Greek", "el"),
            Language("Spanish", "es"),
            Language("Estonian", "et"),
            Language("Finnish", "fi"),
            Language("French", "fr"),
            Language("Hindi", "hi"),
            Language("Croatian", "hr"),
            Language("Hungarian", "hu"),
            Language("Indonesian", "id"),
            Language("Italian", "it"),
            Language("Lithuanian", "lt"),
            Language("Latvian", "lv"),
            Language("Dutch", "nl"),
            Language("Polish", "pl"),
            Language("Portuguese", "pt"),
            Language("Romanian", "ro"),
            Language("Russian", "ru"),
            Language("Slovak", "sk"),
            Language("Slovenian", "sl"),
            Language("Swedish", "sv"),
            Language("Turkish", "tr"),
            Language("Ukrainian", "uk"),
            Language("Vietnamese", "vi")
        )

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        ttsManager =
            TtsManager(applicationContext)

        createUi()

        initializeEngine()
    }

    private fun createUi() {

        val root =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(20),
                    dp(20),
                    dp(20),
                    dp(20)
                )

                setBackgroundColor(
                    getColor(
                        R.color.background
                    )
                )
            }

        val scroll =
            ScrollView(this)

        val content =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
            }

        scroll.addView(content)

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                -1,
                0,
                1f
            )
        )

        val title =
            TextView(this).apply {

                text = "VTTS"

                textSize = 30f

                setTextColor(
                    getColor(
                        R.color.text_primary
                    )
                )

                gravity =
                    Gravity.CENTER

                setPadding(
                    0,
                    0,
                    0,
                    dp(8)
                )
            }

        content.addView(title)

        val subtitle =
            TextView(this).apply {

                text =
                    "Supertonic-3 • Offline TTS"

                textSize = 15f

                gravity =
                    Gravity.CENTER

                setTextColor(
                    getColor(
                        R.color.text_secondary
                    )
                )

                setPadding(
                    0,
                    0,
                    0,
                    dp(20)
                )
            }

        content.addView(subtitle)

        textInput =
            EditText(this).apply {

                hint =
                    "Enter text to speak..."

                textSize = 18f

                gravity =
                    Gravity.TOP

                minLines = 7

                maxLines = 14

                setPadding(
                    dp(16),
                    dp(16),
                    dp(16),
                    dp(16)
                )

                setTextColor(
                    getColor(
                        R.color.text_primary
                    )
                )

                setHintTextColor(
                    getColor(
                        R.color.text_secondary
                    )
                )

                setBackgroundColor(
                    getColor(
                        R.color.surface
                    )
                )
            }

        content.addView(
            textInput,
            marginParams(
                0,
                0,
                0,
                18
            )
        )

        addLabel(
            content,
            "Language"
        )

        languageSpinner =
            Spinner(this)

        val languageAdapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                languages.map {
                    it.name
                }
            )

        languageSpinner.adapter =
            languageAdapter

        content.addView(
            languageSpinner,
            marginParams(
                0,
                0,
                0,
                14
            )
        )

        addLabel(
            content,
            "Speaker"
        )

        speakerSpinner =
            Spinner(this)

        val speakers =
            (0..9).map {
                "Speaker $it"
            }

        val speakerAdapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                speakers
            )

        speakerSpinner.adapter =
            speakerAdapter

        content.addView(
            speakerSpinner,
            marginParams(
                0,
                0,
                0,
                14
            )
        )

        addLabel(
            content,
            "Speed"
        )

        val speedLayout =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL
            }

        speedSeekBar =
            SeekBar(this).apply {

                max = 150

                progress = 50
            }

        speedLayout.addView(
            speedSeekBar,
            LinearLayout.LayoutParams(
                0,
                -2,
                1f
            )
        )

        speedValue =
            TextView(this).apply {

                text = "1.00x"

                textSize = 16f

                setTextColor(
                    getColor(
                        R.color.text_primary
                    )
                )

                gravity =
                    Gravity.CENTER
            }

        speedLayout.addView(
            speedValue,
            LinearLayout.LayoutParams(
                dp(70),
                -2
            )
        )

        content.addView(
            speedLayout,
            marginParams(
                0,
                0,
                0,
                18
            )
        )

        speedSeekBar.setOnSeekBarChangeListener(
            object :
                SeekBar.OnSeekBarChangeListener {

                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {

                    val speed =
                        0.5f +
                            progress /
                            100f

                    speedValue.text =
                        String.format(
                            "%.2fx",
                            speed
                        )
                }

                override fun onStartTrackingTouch(
                    seekBar: SeekBar?
                ) {
                }

                override fun onStopTrackingTouch(
                    seekBar: SeekBar?
                ) {
                }
            }
        )

        speakButton =
            createButton(
                "SPEAK"
            )

        stopButton =
            createButton(
                "STOP"
            )

        saveButton =
            createButton(
                "SAVE WAV"
            )

        content.addView(
            speakButton,
            marginParams(
                0,
                0,
                0,
                10
            )
        )

        content.addView(
            stopButton,
            marginParams(
                0,
                0,
                0,
                10
            )
        )

        content.addView(
            saveButton,
            marginParams(
                0,
                0,
                0,
                18
            )
        )

        speakButton.isEnabled = false
        stopButton.isEnabled = false
        saveButton.isEnabled = false

        speakButton.setOnClickListener {
            speak()
        }

        stopButton.setOnClickListener {
            stop()
        }

        saveButton.setOnClickListener {
            saveLatestAudio()
        }

        statusText =
            TextView(this).apply {

                text =
                    "Loading offline model..."

                textSize = 14f

                gravity =
                    Gravity.CENTER

                setTextColor(
                    getColor(
                        R.color.text_secondary
                    )
                )

                setPadding(
                    0,
                    dp(10),
                    0,
                    dp(20)
                )
            }

        content.addView(statusText)

        setContentView(root)
    }

    private fun initializeEngine() {

        currentTask =
            executor.submit {

                try {

                    ttsManager.initialize(
                        threads = 2
                    )

                    mainHandler.post {

                        statusText.text =
                            "Ready • 100% offline"

                        speakButton.isEnabled =
                            true
                    }

                } catch (error: Throwable) {

                    mainHandler.post {

                        statusText.text =
                            "Model initialization failed"

                        showError(
                            error
                        )
                    }
                }
            }
    }

    private fun speak() {

        val text =
            textInput.text
                .toString()
                .trim()

        if (text.isEmpty()) {

            Toast.makeText(
                this,
                "Enter some text first.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        stopPlayback()

        currentTask?.cancel(
            true
        )

        speakButton.isEnabled =
            false

        stopButton.isEnabled =
            true

        saveButton.isEnabled =
            false

        statusText.text =
            "Generating speech..."

        val language =
            languages[
                languageSpinner.selectedItemPosition
            ].code

        val speaker =
            speakerSpinner
                .selectedItemPosition

        val speed =
            0.5f +
                speedSeekBar.progress /
                100f

        currentTask =
            executor.submit {

                try {

                    val result =
                        ttsManager.generate(
                            text = text,
                            speakerId = speaker,
                            speed = speed,
                            language = language,
                            steps = 8
                        )

                    latestSamples =
                        result.samples

                    latestSampleRate =
                        result.sampleRate

                    mainHandler.post {

                        statusText.text =
                            "Playing • ${result.sampleRate} Hz"

                        saveButton.isEnabled =
                            true
                    }

                    playAudio(
                        result.samples,
                        result.sampleRate
                    )

                    mainHandler.post {

                        if (
                            !Thread.currentThread()
                                .isInterrupted
                        ) {

                            statusText.text =
                                "Ready • 100% offline"
                        }

                        speakButton.isEnabled =
                            true

                        stopButton.isEnabled =
                            false
                    }

                } catch (error: Throwable) {

                    mainHandler.post {

                        speakButton.isEnabled =
                            true

                        stopButton.isEnabled =
                            false

                        statusText.text =
                            "Generation failed"

                        showError(
                            error
                        )
                    }
                }
            }
    }

    private fun playAudio(
        samples: FloatArray,
        sampleRate: Int
    ) {

        val minBuffer =
            AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

        if (minBuffer <= 0) {
            throw IllegalStateException(
                "Audio output is not supported."
            )
        }

        val bufferSize =
            maxOf(
                minBuffer,
                4096
            )

        val track =
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(
                            AudioAttributes.USAGE_MEDIA
                        )
                        .setContentType(
                            AudioAttributes.CONTENT_TYPE_SPEECH
                        )
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(
                            sampleRate
                        )
                        .setEncoding(
                            AudioFormat.ENCODING_PCM_16BIT
                        )
                        .setChannelMask(
                            AudioFormat.CHANNEL_OUT_MONO
                        )
                        .build()
                )
                .setBufferSizeInBytes(
                    bufferSize
                )
                .setTransferMode(
                    AudioTrack.MODE_STREAM
                )
                .build()

        audioTrack =
            track

        track.play()

        val pcm =
            ShortArray(
                minOf(
                    4096,
                    samples.size
                )
            )

        var position = 0

        try {

            while (
                position < samples.size &&
                !Thread.currentThread()
                    .isInterrupted
            ) {

                val count =
                    minOf(
                        pcm.size,
                        samples.size - position
                    )

                for (i in 0 until count) {

                    val sample =
                        samples[position + i]
                            .coerceIn(
                                -1f,
                                1f
                            )

                    pcm[i] =
                        (
                            if (sample < 0f) {
                                sample * 32768f
                            } else {
                                sample * 32767f
                            }
                        )
                            .roundToInt()
                            .toShort()
                }

                track.write(
                    pcm,
                    0,
                    count
                )

                position += count
            }

        } finally {

            try {
                track.stop()
            } catch (_: Throwable) {
            }

            track.release()

            audioTrack = null
        }
    }

    private fun stop() {

        currentTask?.cancel(
            true
        )

        stopPlayback()

        speakButton.isEnabled =
            ttsManagerIsReady()

        stopButton.isEnabled =
            false

        statusText.text =
            if (ttsManagerIsReady()) {
                "Ready • 100% offline"
            } else {
                "Loading..."
            }
    }

    private fun stopPlayback() {

        try {

            audioTrack?.pause()

            audioTrack?.flush()

            audioTrack?.stop()

        } catch (_: Throwable) {
        }

        try {
            audioTrack?.release()
        } catch (_: Throwable) {
        }

        audioTrack = null
    }

    private fun saveLatestAudio() {

        val samples =
            latestSamples

        if (
            samples == null ||
            samples.isEmpty()
        ) {

            Toast.makeText(
                this,
                "Generate speech first.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        try {

            val outputDir =
                File(
                    getExternalFilesDir(null),
                    "VTTS"
                )

            outputDir.mkdirs()

            val filename =
                "vtts_${System.currentTimeMillis()}.wav"

            val output =
                File(
                    outputDir,
                    filename
                )

            WaveWriter.write(
                output,
                samples,
                latestSampleRate
            )

            Toast.makeText(
                this,
                "Saved:\n${output.absolutePath}",
                Toast.LENGTH_LONG
            ).show()

        } catch (error: Throwable) {

            showError(
                error
            )
        }
    }

    private fun ttsManagerIsReady(): Boolean {
        return try {
            speakButton.isEnabled ||
                statusText.text
                    .toString()
                    .startsWith("Ready")
        } catch (_: Throwable) {
            false
        }
    }

    private fun showError(
        error: Throwable
    ) {

        val message =
            error.message
                ?: error.javaClass.simpleName

        Toast.makeText(
            this,
            message,
            Toast.LENGTH_LONG
        ).show()
    }

    private fun addLabel(
        parent: LinearLayout,
        text: String
    ) {

        val label =
            TextView(this).apply {

                this.text = text

                textSize = 15f

                setTextColor(
                    getColor(
                        R.color.text_secondary
                    )
                )

                setPadding(
                    0,
                    dp(4),
                    0,
                    dp(6)
                )
            }

        parent.addView(label)
    }

    private fun createButton(
        text: String
    ): Button {

        return Button(this).apply {

            this.text = text

            textSize = 15f

            isAllCaps = false

            setTextColor(
                getColor(
                    R.color.white
                )
            )
        }
    }

    private fun marginParams(
        left: Int,
        top: Int,
        right: Int,
        bottom: Int
    ): LinearLayout.LayoutParams {

        return LinearLayout.LayoutParams(
            -1,
            -2
        ).apply {

            setMargins(
                dp(left),
                dp(top),
                dp(right),
                dp(bottom)
            )
        }
    }

    private fun dp(
        value: Int
    ): Int {

        return (
            value *
                resources.displayMetrics.density
            ).roundToInt()
    }

    override fun onDestroy() {

        stopPlayback()

        currentTask?.cancel(
            true
        )

        executor.shutdownNow()

        try {
            ttsManager.release()
        } catch (_: Throwable) {
        }

        super.onDestroy()
    }

    data class Language(
        val name: String,
        val code: String
    )
}
