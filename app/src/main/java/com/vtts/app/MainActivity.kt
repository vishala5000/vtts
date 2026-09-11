package com.vtts.app

import android.content.ContentValues
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Gravity
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

class MainActivity : AppCompatActivity() {

    private lateinit var textInput: EditText

    private lateinit var languageSpinner: Spinner

    private lateinit var speakerSpinner: Spinner

    private lateinit var speedSeekBar: SeekBar

    private lateinit var speedLabel: TextView

    private lateinit var statusText: TextView

    private lateinit var speakButton: Button

    private lateinit var stopButton: Button

    private lateinit var playButton: Button

    private lateinit var saveButton: Button

    private val executor:
        ExecutorService =
        Executors.newSingleThreadExecutor()

    private var generationFuture:
        Future<*>? = null

    private var ttsManager:
        TtsManager? = null

    private var engineReady =
        false

    private var lastAudio:
        TtsManager.GeneratedResult? = null

    private var audioTrack:
        AudioTrack? = null

    private var userStopped =
        false

    /*
     * ONLY languages supported by this app.
     *
     * Supertonic-3 supports English and Hindi.
     *
     * Kannada is intentionally NOT included because
     * Supertonic-3 does not currently list "kn".
     */
    private val languages =
        listOf(

            Language(
                name = "English",
                code = "en"
            ),

            Language(
                name = "Hindi",
                code = "hi"
            )
        )

    /*
     * Keep ALL 10 Supertonic voices.
     */
    private val speakers =
        (0..9).map {
            "Speaker $it"
        }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        createUi()

        initializeTts()
    }

    private fun createUi() {

        val scroll =
            ScrollView(this)

        val root =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(20),
                    dp(20),
                    dp(20),
                    dp(30)
                )
            }

        scroll.addView(root)

        val title =
            TextView(this).apply {

                text = "VTTS"

                textSize = 30f

                gravity =
                    Gravity.CENTER

                setTextColor(
                    getColor(
                        R.color.text_primary
                    )
                )

                setPadding(
                    0,
                    0,
                    0,
                    dp(8)
                )
            }

        root.addView(
            title,
            match()
        )

        val subtitle =
            TextView(this).apply {

                text =
                    "Supertonic-3 INT8 • Fast • Fully Offline"

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
                    0,
                    0,
                    dp(20)
                )
            }

        root.addView(
            subtitle,
            match()
        )

        /*
         * TEXT
         */
        textInput =
            EditText(this).apply {

                hint =
                    "Enter text to speak..."

                textSize = 18f

                minLines = 7

                gravity =
                    Gravity.TOP or
                        Gravity.START

                setPadding(
                    dp(14),
                    dp(14),
                    dp(14),
                    dp(14)
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

        root.addView(
            textInput,
            matchWrap(16)
        )

        /*
         * LANGUAGE
         */
        addLabel(
            root,
            "Language"
        )

        languageSpinner =
            Spinner(this)

        languageSpinner.adapter =
            ArrayAdapter(
                this,

                android.R.layout.simple_spinner_dropdown_item,

                languages.map {
                    it.name
                }
            )

        root.addView(
            languageSpinner,
            matchWrap(8)
        )

        /*
         * SPEAKER
         */
        addLabel(
            root,
            "Voice"

        )

        speakerSpinner =
            Spinner(this)

        speakerSpinner.adapter =
            ArrayAdapter(
                this,

                android.R.layout.simple_spinner_dropdown_item,

                speakers
            )

        root.addView(
            speakerSpinner,
            matchWrap(8)
        )

        /*
         * SPEED
         */
        addLabel(
            root,
            "Speed"
        )

        speedLabel =
            TextView(this).apply {

                text =
                    "1.25x"

                textSize =
                    16f

                setTextColor(
                    getColor(
                        R.color.text_primary
                    )
                )
            }

        root.addView(
            speedLabel,
            matchWrap(4)
        )

        /*
         * Default speed = 1.25x.
         *
         * Range:
         *
         * 0.50x -> 2.00x
         */
        speedSeekBar =
            SeekBar(this).apply {

                max = 150

                progress = 75
            }

        root.addView(
            speedSeekBar,
            matchWrap(12)
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
                            progress / 100f

                    speedLabel.text =
                        String.format(
                            Locale.US,
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

        /*
         * SPEAK
         */
        speakButton =
            makeButton(
                "Speak"
            )

        root.addView(
            speakButton,
            matchWrap(8)
        )

        speakButton.setOnClickListener {
            speak()
        }

        /*
         * STOP
         */
        stopButton =
            makeButton(
                "Stop"
            )

        stopButton.isEnabled =
            false

        root.addView(
            stopButton,
            matchWrap(8)
        )

        stopButton.setOnClickListener {
            stopGeneration()
        }

        /*
         * PLAY
         */
        playButton =
            makeButton(
                "Play Last Audio"
            )

        playButton.isEnabled =
            false

        root.addView(
            playButton,
            matchWrap(8)
        )

        playButton.setOnClickListener {
            playLastAudio()
        }

        /*
         * SAVE
         */
        saveButton =
            makeButton(
                "Save WAV to Downloads"
            )

        saveButton.isEnabled =
            false

        root.addView(
            saveButton,
            matchWrap(8)
        )

        saveButton.setOnClickListener {
            saveLastAudio()
        }

        /*
         * STATUS
         */
        statusText =
            TextView(this).apply {

                text =
                    "Loading TTS model..."

                textSize = 14f

                gravity =
                    Gravity.CENTER

                setTextColor(
                    getColor(
                        R.color.text_secondary
                    )
                )

                setPadding(
                    dp(4),
                    dp(20),
                    dp(4),
                    0
                )
            }

        root.addView(
            statusText,
            matchWrap(8)
        )

        speakButton.isEnabled =
            false

        setContentView(scroll)
    }

    private fun initializeTts() {

        executor.execute {

            try {

                val manager =
                    TtsManager(
                        applicationContext
                    )

                /*
                 * Use 4 CPU threads.
                 *
                 * This can improve inference speed on
                 * modern multi-core Android phones.
                 */
                manager.initialize(
                    threads = 4
                )

                ttsManager =
                    manager

                engineReady =
                    true

                runOnUiThread {

                    statusText.text =
                        "Ready • Offline • Fast mode"

                    speakButton.isEnabled =
                        true

                    stopButton.isEnabled =
                        false
                }

            } catch (
                error: Throwable
            ) {

                engineReady =
                    false

                runOnUiThread {

                    statusText.text =
                        "TTS initialization failed: ${
                            error.message
                                ?: "Unknown error"
                        }"

                    speakButton.isEnabled =
                        false
                }
            }
        }
    }

    private fun speak() {

        if (!engineReady) {

            toast(
                "TTS is still loading"
            )

            return
        }

        val text =
            textInput.text
                .toString()
                .trim()

        if (text.isEmpty()) {

            textInput.error =
                "Enter text"

            return
        }

        /*
         * Stop previous generation/playback.
         */
        stopGeneration()

        val manager =
            ttsManager
                ?: return

        userStopped =
            false

        val language =
            languages[
                languageSpinner.selectedItemPosition
                    .coerceIn(
                        0,
                        languages.lastIndex
                    )
            ].code

        val speaker =
            speakerSpinner.selectedItemPosition
                .coerceIn(
                    0,
                    9
                )

        val speed =
            0.5f +
                speedSeekBar.progress / 100f

        lastAudio =
            null

        speakButton.isEnabled =
            false

        stopButton.isEnabled =
            true

        playButton.isEnabled =
            false

        saveButton.isEnabled =
            false

        statusText.text =
            "Generating..."

        generationFuture =
            executor.submit {

                try {

                    manager.resetStop()

                    /*
                     * 4 steps = faster mode.
                     */
                    val result =
                        manager.generate(

                            text = text,

                            speakerId =
                                speaker,

                            speed =
                                speed,

                            language =
                                language,

                            steps =
                                TtsManager.DEFAULT_STEPS
                        )

                    if (userStopped) {
                        return@submit
                    }

                    if (
                        Thread.currentThread()
                            .isInterrupted
                    ) {
                        return@submit
                    }

                    lastAudio =
                        result

                    runOnUiThread {

                        if (userStopped) {
                            return@runOnUiThread
                        }

                        statusText.text =
                            "Generated • ${
                                formatDuration(
                                    result
                                )
                            }"

                        speakButton.isEnabled =
                            true

                        stopButton.isEnabled =
                            false

                        playButton.isEnabled =
                            true

                        saveButton.isEnabled =
                            true

                        /*
                         * Automatically play generated speech.
                         */
                        playLastAudio()
                    }

                } catch (
                    error: Throwable
                ) {

                    if (!userStopped) {

                        runOnUiThread {

                            statusText.text =
                                "Error: ${
                                    error.message
                                        ?: "Generation failed"
                                }"

                            speakButton.isEnabled =
                                engineReady

                            stopButton.isEnabled =
                                false
                        }
                    }

                } finally {

                    generationFuture =
                        null
                }
            }
    }

    private fun stopGeneration() {

        userStopped =
            true

        ttsManager?.stop()

        generationFuture?.cancel(
            true
        )

        generationFuture =
            null

        stopAudioPlayback()

        speakButton.isEnabled =
            engineReady

        stopButton.isEnabled =
            false

        statusText.text =
            if (engineReady) {
                "Ready"
            } else {
                "Loading TTS model..."
            }
    }

    private fun playLastAudio() {

        val audio =
            lastAudio
                ?: return

        stopAudioPlayback()

        try {

            val minBuffer =
                AudioTrack.getMinBufferSize(
                    audio.sampleRate,

                    AudioFormat.CHANNEL_OUT_MONO,

                    AudioFormat.ENCODING_PCM_FLOAT
                )

            val bufferSize =
                maxOf(
                    minBuffer,
                    audio.samples.size * 4
                )

            audioTrack =
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
                                audio.sampleRate
                            )

                            .setEncoding(
                                AudioFormat.ENCODING_PCM_FLOAT
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
                        AudioTrack.MODE_STATIC
                    )

                    .build()

            val written =
                audioTrack?.write(

                    audio.samples,

                    0,

                    audio.samples.size,

                    AudioTrack.WRITE_BLOCKING
                )

            if (
                written == null ||
                written < 0
            ) {

                throw IllegalStateException(
                    "AudioTrack write failed"
                )
            }

            audioTrack?.play()

        } catch (
            _: Throwable
        ) {

            playPcm16Fallback(
                audio
            )
        }
    }

    private fun playPcm16Fallback(
        audio: TtsManager.GeneratedResult
    ) {

        stopAudioPlayback()

        try {

            val pcm =
                ShortArray(
                    audio.samples.size
                )

            for (
                i in audio.samples.indices
            ) {

                val sample =
                    audio.samples[i]
                        .coerceIn(
                            -1f,
                            1f
                        )

                pcm[i] =
                    (
                        sample * 32767f
                    )
                        .toInt()
                        .toShort()
            }

            val minBuffer =
                AudioTrack.getMinBufferSize(

                    audio.sampleRate,

                    AudioFormat.CHANNEL_OUT_MONO,

                    AudioFormat.ENCODING_PCM_16BIT
                )

            val bufferSize =
                maxOf(
                    minBuffer,
                    pcm.size * 2
                )

            audioTrack =
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
                                audio.sampleRate
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
                        AudioTrack.MODE_STATIC
                    )

                    .build()

            audioTrack?.write(
                pcm,
                0,
                pcm.size
            )

            audioTrack?.play()

        } catch (
            error: Throwable
        ) {

            toast(
                "Playback failed: ${
                    error.message
                        ?: "Unknown error"
                }"
            )
        }
    }

    private fun stopAudioPlayback() {

        try {
            audioTrack?.pause()
        } catch (_: Throwable) {
        }

        try {
            audioTrack?.flush()
        } catch (_: Throwable) {
        }

        try {
            audioTrack?.release()
        } catch (_: Throwable) {
        }

        audioTrack =
            null
    }

    /*
     * ============================================================
     * SAVE WAV TO PUBLIC DOWNLOADS
     * ============================================================
     *
     * Android 10+:
     *
     * Download/
     *     VTTS/
     *         vtts_....wav
     *
     * MediaStore is used so no storage permission is needed.
     */
    private fun saveLastAudio() {

        val audio =
            lastAudio
                ?: return

        try {

            val filename =
                "vtts_${
                    System.currentTimeMillis()
                }.wav"

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.Q
            ) {

                saveWavUsingMediaStore(
                    filename = filename,
                    audio = audio
                )

            } else {

                saveWavLegacy(
                    filename = filename,
                    audio = audio
                )
            }

        } catch (
            error: Throwable
        ) {

            toast(
                "Save failed: ${
                    error.message
                        ?: "Unknown error"
                }"
            )
        }
    }

    /*
     * Android 10+
     *
     * Uses MediaStore Downloads.
     */
    private fun saveWavUsingMediaStore(
        filename: String,
        audio: TtsManager.GeneratedResult
    ) {

        val resolver =
            contentResolver

        val values =
            ContentValues().apply {

                put(
                    MediaStore.Downloads.DISPLAY_NAME,
                    filename
                )

                put(
                    MediaStore.Downloads.MIME_TYPE,
                    "audio/wav"
                )

                put(
                    MediaStore.Downloads.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS +
                        "/VTTS"
                )

                put(
                    MediaStore.Downloads.IS_PENDING,
                    1
                )
            }

        val uri =
            resolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                values
            )
                ?: throw IllegalStateException(
                    "Could not create Downloads file"
                )

        try {

            resolver.openOutputStream(
                uri
            ).use { output ->

                if (output == null) {

                    throw IllegalStateException(
                        "Could not open output file"
                    )
                }

                WaveWriter.write(
                    output = output,
                    samples = audio.samples,
                    sampleRate = audio.sampleRate
                )
            }

            val completed =
                ContentValues().apply {

                    put(
                        MediaStore.Downloads.IS_PENDING,
                        0
                    )
                }

            resolver.update(
                uri,
                completed,
                null,
                null
            )

            statusText.text =
                "Saved to Downloads/VTTS"

            toast(
                "WAV saved to Downloads/VTTS"
            )

        } catch (
            error: Throwable
        ) {

            resolver.delete(
                uri,
                null,
                null
            )

            throw error
        }
    }

    /*
     * Android 9 and older.
     */
    private fun saveWavLegacy(
        filename: String,
        audio: TtsManager.GeneratedResult
    ) {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.M
        ) {

            if (
                checkSelfPermission(
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {

                requestPermissions(
                    arrayOf(
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ),
                    REQUEST_STORAGE_PERMISSION
                )

                toast(
                    "Allow storage permission and press Save again."
                )

                return
            }
        }

        val downloads =
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            )

        val directory =
            File(
                downloads,
                "VTTS"
            )

        if (!directory.exists()) {
            directory.mkdirs()
        }

        val file =
            File(
                directory,
                filename
            )

        WaveWriter.write(
            file = file,
            samples = audio.samples,
            sampleRate = audio.sampleRate
        )

        statusText.text =
            "Saved to Downloads/VTTS"

        toast(
            "WAV saved to Downloads/VTTS"
        )
    }

    private fun formatDuration(
        audio: TtsManager.GeneratedResult
    ): String {

        val seconds =
            audio.samples.size.toFloat() /
                audio.sampleRate

        return String.format(
            Locale.US,
            "%.1f sec",
            seconds
        )
    }

    private fun addLabel(
        root: LinearLayout,
        text: String
    ) {

        val label =
            TextView(this).apply {

                this.text =
                    text

                textSize =
                    15f

                setTextColor(
                    getColor(
                        R.color.text_primary
                    )
                )

                setPadding(
                    dp(2),
                    dp(12),
                    dp(2),
                    dp(4)
                )
            }

        root.addView(
            label,
            matchWrap(4)
        )
    }

    private fun makeButton(
        text: String
    ): Button {

        return Button(this).apply {

            this.text =
                text

            textSize =
                16f

            isAllCaps =
                false

            minHeight =
                dp(52)

            setTextColor(
                getColor(
                    R.color.white
                )
            )

            setBackgroundColor(
                getColor(
                    R.color.primary
                )
            )
        }
    }

    private fun match():
        LinearLayout.LayoutParams {

        return LinearLayout.LayoutParams(

            LinearLayout.LayoutParams.MATCH_PARENT,

            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }

    private fun matchWrap(
        bottom: Int
    ): LinearLayout.LayoutParams {

        return LinearLayout.LayoutParams(

            LinearLayout.LayoutParams.MATCH_PARENT,

            LinearLayout.LayoutParams.WRAP_CONTENT

        ).apply {

            bottomMargin =
                dp(bottom)
        }
    }

    private fun dp(
        value: Int
    ): Int {

        return (
            value *
                resources.displayMetrics.density
            ).toInt()
    }

    private fun toast(
        message: String
    ) {

        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (
            requestCode ==
            REQUEST_STORAGE_PERMISSION
        ) {

            if (
                grantResults.isNotEmpty() &&
                grantResults[0] ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {

                toast(
                    "Storage permission granted. Press Save WAV again."
                )

            } else {

                toast(
                    "Storage permission denied."
                )
            }
        }
    }

    override fun onDestroy() {

        userStopped =
            true

        ttsManager?.stop()

        generationFuture?.cancel(
            true
        )

        stopAudioPlayback()

        ttsManager?.release()

        executor.shutdownNow()

        super.onDestroy()
    }

    private data class Language(
        val name: String,
        val code: String
    )

    companion object {

        private const val REQUEST_STORAGE_PERMISSION =
            1001
    }
}
