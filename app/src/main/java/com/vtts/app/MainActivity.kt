package com.vtts.app

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    companion object {
        private const val STORAGE_REQUEST_CODE = 1001

        private const val DEFAULT_SPEED = 1.25f

        private const val MIN_SPEED = 0.5f

        private const val MAX_SPEED = 2.0f

        private const val SPEED_STEP = 0.05f

        private const val CPU_THREADS = 4
    }

    private lateinit var ttsManager: TtsManager

    private val executor =
        Executors.newSingleThreadExecutor()

    private var lastSamples: FloatArray? = null

    private var lastSampleRate: Int = 24000

    private var isGenerating = false

    private lateinit var textInput: EditText

    private lateinit var languageSpinner: Spinner

    private lateinit var speakerSpinner: Spinner

    private lateinit var speedSeekBar: SeekBar

    private lateinit var speedText: TextView

    private lateinit var statusText: TextView

    private lateinit var speakButton: Button

    private lateinit var stopButton: Button

    private lateinit var playButton: Button

    private lateinit var saveButton: Button

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        ttsManager =
            TtsManager(applicationContext)

        createUi()

        initializeTts()
    }

    private fun createUi() {

        val root =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(18),
                    dp(18),
                    dp(18),
                    dp(18)
                )

                setBackgroundColor(
                    ContextCompat.getColor(
                        this@MainActivity,
                        R.color.background
                    )
                )
            }

        val scroll =
            android.widget.ScrollView(this).apply {
                addView(root)
            }

        setContentView(scroll)

        val title =
            TextView(this).apply {

                text = "VTTS"

                textSize = 30f

                setTextColor(
                    ContextCompat.getColor(
                        this@MainActivity,
                        R.color.text_primary
                    )
                )

                gravity = Gravity.CENTER

                setPadding(
                    0,
                    0,
                    0,
                    dp(8)
                )
            }

        root.addView(
            title,
            matchParams()
        )

        val subtitle =
            TextView(this).apply {

                text =
                    "Offline Supertonic-3 TTS"

                textSize = 14f

                setTextColor(
                    ContextCompat.getColor(
                        this@MainActivity,
                        R.color.text_secondary
                    )
                )

                gravity = Gravity.CENTER

                setPadding(
                    0,
                    0,
                    0,
                    dp(20)
                )
            }

        root.addView(
            subtitle,
            matchParams()
        )

        textInput =
            EditText(this).apply {

                hint =
                    "Enter text here..."

                textSize = 18f

                gravity =
                    Gravity.TOP or Gravity.START

                minLines = 7

                maxLines = 15

                setPadding(
                    dp(14),
                    dp(14),
                    dp(14),
                    dp(14)
                )

                setTextColor(
                    ContextCompat.getColor(
                        this@MainActivity,
                        R.color.text_primary
                    )
                )

                setHintTextColor(
                    ContextCompat.getColor(
                        this@MainActivity,
                        R.color.text_secondary
                    )
                )

                setBackgroundColor(
                    ContextCompat.getColor(
                        this@MainActivity,
                        R.color.surface
                    )
                )
            }

        root.addView(
            textInput,
            matchParams(
                top = 0,
                bottom = 16
            )
        )

        addLabel(
            root,
            "Language"
        )

        languageSpinner =
            Spinner(this)

        val languages =
            listOf(
                "English",
                "Hindi"
            )

        languageSpinner.adapter =
            createSpinnerAdapter(languages)

        root.addView(
            languageSpinner,
            matchParams(
                bottom = 12
            )
        )

        addLabel(
            root,
            "Voice"
        )

        speakerSpinner =
            Spinner(this)

        val speakers =
            (0..9).map {
                "Speaker $it"
            }

        speakerSpinner.adapter =
            createSpinnerAdapter(speakers)

        root.addView(
            speakerSpinner,
            matchParams(
                bottom = 12
            )
        )

        addLabel(
            root,
            "Speed"
        )

        speedText =
            TextView(this).apply {

                text =
                    formatSpeed(DEFAULT_SPEED)

                textSize = 16f

                setTextColor(
                    ContextCompat.getColor(
                        this@MainActivity,
                        R.color.text_primary
                    )
                )
            }

        root.addView(
            speedText,
            matchParams(
                bottom = 4
            )
        )

        speedSeekBar =
            SeekBar(this).apply {

                max =
                    ((MAX_SPEED - MIN_SPEED) /
                            SPEED_STEP)
                        .toInt()

                progress =
                    ((DEFAULT_SPEED - MIN_SPEED) /
                            SPEED_STEP)
                        .toInt()

                setOnSeekBarChangeListener(
                    object :
                        SeekBar.OnSeekBarChangeListener {

                        override fun onProgressChanged(
                            seekBar: SeekBar?,
                            progress: Int,
                            fromUser: Boolean
                        ) {
                            speedText.text =
                                formatSpeed(
                                    getSelectedSpeed()
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
            }

        root.addView(
            speedSeekBar,
            matchParams(
                bottom = 20
            )
        )

        speakButton =
            createButton(
                "Generate & Speak"
            )

        root.addView(
            speakButton,
            matchParams(
                bottom = 10
            )
        )

        speakButton.setOnClickListener {
            generateSpeech()
        }

        stopButton =
            createButton(
                "Stop"
            )

        root.addView(
            stopButton,
            matchParams(
                bottom = 10
            )
        )

        stopButton.setOnClickListener {
            stopGeneration()
        }

        playButton =
            createButton(
                "Play Last Audio"
            )

        root.addView(
            playButton,
            matchParams(
                bottom = 10
            )
        )

        playButton.setOnClickListener {
            playLastAudio()
        }

        saveButton =
            createButton(
                "Save WAV to Downloads"
            )

        root.addView(
            saveButton,
            matchParams(
                bottom = 16
            )
        )

        saveButton.setOnClickListener {
            saveLastAudio()
        }

        statusText =
            TextView(this).apply {

                text =
                    "Initializing..."

                textSize = 14f

                gravity = Gravity.CENTER

                setTextColor(
                    ContextCompat.getColor(
                        this@MainActivity,
                        R.color.text_secondary
                    )
                )

                setPadding(
                    0,
                    dp(8),
                    0,
                    dp(8)
                )
            }

        root.addView(
            statusText,
            matchParams()
        )
    }

    private fun initializeTts() {

        setBusy(true)

        statusText.text =
            "Loading Supertonic-3..."

        executor.execute {

            try {

                ttsManager.initialize(
                    threads = CPU_THREADS
                )

                runOnUiThread {

                    statusText.text =
                        "Ready • 4 steps • 4 CPU threads"

                    setBusy(false)
                }

            } catch (e: Exception) {

                runOnUiThread {

                    statusText.text =
                        "Initialization failed"

                    setBusy(false)

                    showError(
                        e
                    )
                }
            }
        }
    }

    private fun generateSpeech() {

        if (isGenerating) {
            return
        }

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

        val language =
            when (
                languageSpinner
                    .selectedItemPosition
            ) {
                0 -> "en"
                1 -> "hi"
                else -> "en"
            }

        val speaker =
            speakerSpinner
                .selectedItemPosition
                .coerceIn(0, 9)

        val speed =
            getSelectedSpeed()

        isGenerating = true

        setBusy(true)

        statusText.text =
            "Generating speech..."

        executor.execute {

            try {

                ttsManager.resetStop()

                val result =
                    ttsManager.generate(
                        text = text,
                        speakerId = speaker,
                        speed = speed,
                        language = language,
                        steps = TtsManager.DEFAULT_STEPS
                    )

                lastSamples =
                    result.samples

                lastSampleRate =
                    result.sampleRate

                runOnUiThread {

                    statusText.text =
                        "Generation complete"

                    setBusy(false)

                    isGenerating = false

                    playSamples(
                        result.samples,
                        result.sampleRate
                    )
                }

            } catch (e: Exception) {

                runOnUiThread {

                    statusText.text =
                        "Generation failed"

                    setBusy(false)

                    isGenerating = false

                    showError(
                        e
                    )
                }
            }
        }
    }

    private fun stopGeneration() {

        ttsManager.stop()

        isGenerating = false

        statusText.text =
            "Stopped"

        setBusy(false)
    }

    private fun playLastAudio() {

        val samples =
            lastSamples

        if (
            samples == null ||
            samples.isEmpty()
        ) {

            Toast.makeText(
                this,
                "No generated audio.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        playSamples(
            samples,
            lastSampleRate
        )
    }

    private fun playSamples(
        samples: FloatArray,
        sampleRate: Int
    ) {

        executor.execute {

            try {

                val pcm =
                    ShortArray(
                        samples.size
                    )

                for (i in samples.indices) {

                    val value =
                        samples[i]
                            .coerceIn(
                                -1.0f,
                                1.0f
                            )

                    pcm[i] =
                        if (value < 0f) {
                            (value * 32768f)
                                .toInt()
                                .coerceIn(
                                    -32768,
                                    32767
                                )
                                .toShort()
                        } else {
                            (value * 32767f)
                                .toInt()
                                .coerceIn(
                                    -32768,
                                    32767
                                )
                                .toShort()
                        }
                }

                val minBuffer =
                    AudioTrack.getMinBufferSize(
                        sampleRate,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT
                    )

                val bufferSize =
                    maxOf(
                        minBuffer,
                        pcm.size * 2
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
                            AudioTrack.MODE_STATIC
                        )
                        .build()

                track.write(
                    pcm,
                    0,
                    pcm.size
                )

                track.play()

                while (
                    track.playState ==
                    AudioTrack.PLAYSTATE_PLAYING
                ) {

                    Thread.sleep(50)

                    if (
                        track.playbackHeadPosition >=
                        pcm.size
                    ) {
                        break
                    }
                }

                track.stop()

                track.release()

            } catch (e: Exception) {

                runOnUiThread {
                    Toast.makeText(
                        this,
                        "Playback failed: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun saveLastAudio() {

        val samples =
            lastSamples

        if (
            samples == null ||
            samples.isEmpty()
        ) {

            Toast.makeText(
                this,
                "Generate audio first.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (
            Build.VERSION.SDK_INT <=
            Build.VERSION_CODES.P
        ) {

            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) !=
                PackageManager.PERMISSION_GRANTED
            ) {

                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ),
                    STORAGE_REQUEST_CODE
                )

                return
            }

            saveWavLegacy(
                samples,
                lastSampleRate
            )

        } else {

            executor.execute {

                try {

                    val uri =
                        saveWavUsingMediaStore(
                            samples,
                            lastSampleRate
                        )

                    runOnUiThread {

                        Toast.makeText(
                            this,
                            "Saved to Downloads/VTTS",
                            Toast.LENGTH_LONG
                        ).show()

                        statusText.text =
                            "Saved: $uri"
                    }

                } catch (e: Exception) {

                    runOnUiThread {

                        showError(
                            e
                        )
                    }
                }
            }
        }
    }

    private fun saveWavLegacy(
        samples: FloatArray,
        sampleRate: Int
    ) {

        executor.execute {

            try {

                val downloads =
                    Environment
                        .getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOWNLOADS
                        )

                val directory =
                    File(
                        downloads,
                        "VTTS"
                    )

                if (
                    !directory.exists() &&
                    !directory.mkdirs()
                ) {
                    throw IOException(
                        "Could not create Downloads/VTTS"
                    )
                }

                val file =
                    File(
                        directory,
                        createFileName()
                    )

                WaveWriter.write(
                    file = file,
                    samples = samples,
                    sampleRate = sampleRate
                )

                runOnUiThread {

                    Toast.makeText(
                        this,
                        "Saved to Downloads/VTTS",
                        Toast.LENGTH_LONG
                    ).show()

                    statusText.text =
                        "Saved: ${file.absolutePath}"
                }

            } catch (e: Exception) {

                runOnUiThread {
                    showError(e)
                }
            }
        }
    }

    private fun saveWavUsingMediaStore(
        samples: FloatArray,
        sampleRate: Int
    ): Uri {

        val resolver =
            contentResolver

        val fileName =
            createFileName()

        val values =
            ContentValues().apply {

                put(
                    MediaStore.Downloads.DISPLAY_NAME,
                    fileName
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
                ?: throw IOException(
                    "Could not create Downloads file"
                )

        try {

            val output =
                resolver.openOutputStream(uri)
                    ?: throw IOException(
                        "Could not open Downloads file"
                    )

            output.use {

                WaveWriter.write(
                    output = it,
                    samples = samples,
                    sampleRate = sampleRate
                )
            }

            val finishedValues =
                ContentValues().apply {

                    put(
                        MediaStore.Downloads.IS_PENDING,
                        0
                    )
                }

            resolver.update(
                uri,
                finishedValues,
                null,
                null
            )

            return uri

        } catch (e: Exception) {

            resolver.delete(
                uri,
                null,
                null
            )

            throw e
        }
    }

    private fun createFileName(): String {

        val date =
            SimpleDateFormat(
                "yyyyMMdd_HHmmss",
                Locale.US
            ).format(
                Date()
            )

        return "VTTS_$date.wav"
    }

    private fun getSelectedSpeed(): Float {

        return MIN_SPEED +
                speedSeekBar.progress *
                SPEED_STEP
    }

    private fun formatSpeed(
        speed: Float
    ): String {

        return String.format(
            Locale.US,
            "%.2fx",
            speed
        )
    }

    private fun createSpinnerAdapter(
        items: List<String>
    ): ArrayAdapter<String> {

        return ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            items
        ).apply {

            setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
            )
        }
    }

    private fun addLabel(
        root: LinearLayout,
        text: String
    ) {

        val label =
            TextView(this).apply {

                this.text = text

                textSize = 15f

                setTextColor(
                    ContextCompat.getColor(
                        this@MainActivity,
                        R.color.text_primary
                    )
                )

                setPadding(
                    0,
                    0,
                    0,
                    dp(5)
                )
            }

        root.addView(
            label,
            matchParams()
        )
    }

    private fun createButton(
        text: String
    ): Button {

        return Button(this).apply {

            this.text = text

            textSize = 16f

            isAllCaps = false

            minHeight = dp(52)

            setTextColor(
                ContextCompat.getColor(
                    this@MainActivity,
                    R.color.white
                )
            )

            setBackgroundColor(
                ContextCompat.getColor(
                    this@MainActivity,
                    R.color.primary
                )
            )
        }
    }

    private fun setBusy(
        busy: Boolean
    ) {

        speakButton.isEnabled =
            !busy

        stopButton.isEnabled =
            busy

        playButton.isEnabled =
            !busy &&
            lastSamples != null

        saveButton.isEnabled =
            !busy &&
            lastSamples != null

        textInput.isEnabled =
            !busy

        languageSpinner.isEnabled =
            !busy

        speakerSpinner.isEnabled =
            !busy

        speedSeekBar.isEnabled =
            !busy
    }

    private fun showError(
        error: Exception
    ) {

        Toast.makeText(
            this,
            error.message
                ?: error.javaClass.simpleName,
            Toast.LENGTH_LONG
        ).show()
    }

    private fun matchParams(
        top: Int = 0,
        bottom: Int = 0
    ): LinearLayout.LayoutParams {

        return LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {

            topMargin =
                dp(top)

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
            STORAGE_REQUEST_CODE
        ) {

            if (
                grantResults.isNotEmpty() &&
                grantResults[0] ==
                PackageManager.PERMISSION_GRANTED
            ) {

                saveLastAudio()

            } else {

                Toast.makeText(
                    this,
                    "Storage permission is required to save WAV.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onDestroy() {

        try {
            ttsManager.stop()
            ttsManager.release()
        } catch (_: Exception) {
        }

        executor.shutdownNow()

        super.onDestroy()
    }
}

".github/workflows/build.yml"

:::writing{variant="document" id="74106" title="build.yml"}

name: Build VTTS APK

on:
  workflow_dispatch:
  push:
    branches:
      - main

permissions:
  contents: read

jobs:

  build:

    name: Build VTTS Android APK

    runs-on: ubuntu-latest

    timeout-minutes: 90

    steps:

      - name: Checkout repository
        uses: actions/checkout@v4

      - name: Setup Java 17
        uses: actions/setup-java@v5
        with:
          distribution: temurin
          java-version: "17"

      - name: Setup Android SDK
        uses: android-actions/setup-android@v3

      - name: Install Android SDK
        shell: bash
        run: |
          set -e

          yes | sdkmanager --licenses >/dev/null || true

          sdkmanager \
            "platform-tools" \
            "platforms;android-35" \
            "build-tools;35.0.0"

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4
        with:
          gradle-version: "8.10.2"

      - name: Download sherpa-onnx 1.13.7
        shell: bash
        run: |
          set -e

          mkdir -p app/libs

          AAR_URL="https://github.com/k2-fsa/sherpa-onnx/releases/download/v1.13.7/sherpa-onnx-1.13.7.aar"

          curl -L \
            --fail \
            --retry 5 \
            --retry-delay 3 \
            --connect-timeout 30 \
            --max-time 1800 \
            -o app/libs/sherpa-onnx-1.13.7.aar \
            "$AAR_URL"

          test -s app/libs/sherpa-onnx-1.13.7.aar

          ls -lh app/libs/sherpa-onnx-1.13.7.aar

      - name: Download Supertonic-3 INT8 model
        shell: bash
        run: |
          set -e

          MODEL_DIR="app/src/main/assets/supertonic"

          mkdir -p "$MODEL_DIR"

          BASE_URL="https://github.com/vishala5000/vtts/releases/download/vtts"

          files=(
            "duration_predictor.int8.onnx"
            "text_encoder.int8.onnx"
            "vector_estimator.int8.onnx"
            "vocoder.int8.onnx"
            "tts.json"
            "unicode_indexer.bin"
            "voice.bin"
          )

          for file in "${files[@]}"; do

            echo "Downloading $file"

            curl -L \
              --fail \
              --retry 5 \
              --retry-delay 5 \
              --connect-timeout 30 \
              --max-time 1800 \
              -o "$MODEL_DIR/$file" \
              "$BASE_URL/$file"

            test -s "$MODEL_DIR/$file"

            ls -lh "$MODEL_DIR/$file"

          done

      - name: Verify Supertonic model
        shell: bash
        run: |
          set -e

          MODEL_DIR="app/src/main/assets/supertonic"

          required=(
            "duration_predictor.int8.onnx"
            "text_encoder.int8.onnx"
            "vector_estimator.int8.onnx"
            "vocoder.int8.onnx"
            "tts.json"
            "unicode_indexer.bin"
            "voice.bin"
          )

          for file in "${required[@]}"; do

            if [ ! -f "$MODEL_DIR/$file" ]; then
              echo "ERROR: Missing $file"
              exit 1
            fi

            if [ ! -s "$MODEL_DIR/$file" ]; then
              echo "ERROR: Empty $file"
              exit 1
            fi

          done

          echo "All Supertonic model files verified."

      - name: Verify sherpa-onnx AAR
        shell: bash
        run: |
          set -e

          test -s app/libs/sherpa-onnx-1.13.7.aar

          ls -lh app/libs/sherpa-onnx-1.13.7.aar

      - name: Show project files
        shell: bash
        run: |
          echo "===== MAIN PROJECT FILES ====="

          find app/src/main \
            -type f \
            -print \
            | sort

          echo "===== LIBRARIES ====="

          find app/libs \
            -type f \
            -exec ls -lh {} \;

      - name: Clean Gradle project
        shell: bash
        run: |
          set -e

          gradle \
            --no-daemon \
            --stacktrace \
            clean

      - name: Build debug APK
        shell: bash
        run: |
          set -e

          gradle \
            --no-daemon \
            --stacktrace \
            --info \
            assembleDebug

      - name: Inspect debug build
        if: always()
        shell: bash
        run: |
          echo "===== APK FILES ====="

          if [ -d "app/build" ]; then

            find app/build \
              -type f \
              -name "*.apk" \
              -print \
              || true

            echo "===== OUTPUT FILES ====="

            find app/build/outputs \
              -type f \
              -print \
              2>/dev/null \
              || true

          else

            echo "app/build does not exist."

          fi

      - name: Verify debug APK
        shell: bash
        run: |
          set -e

          DEBUG_APK=""

          while IFS= read -r file; do

            if [[ "$file" == *debug*.apk ]]; then

              DEBUG_APK="$file"

              break

            fi

          done < <(
            find app/build \
              -type f \
              -name "*.apk" \
              -print
          )

          if [ -z "$DEBUG_APK" ]; then

            echo "ERROR: DEBUG APK WAS NOT GENERATED."

            find app/build \
              -maxdepth 10 \
              -type f \
              -print \
              2>/dev/null \
              || true

            exit 1

          fi

          echo "DEBUG APK FOUND:"
          echo "$DEBUG_APK"

          ls -lh "$DEBUG_APK"

      - name: Build release APK
        shell: bash
        run: |
          set -e

          gradle \
            --no-daemon \
            --stacktrace \
            --info \
            assembleRelease

      - name: Inspect release build
        if: always()
        shell: bash
        run: |
          echo "===== RELEASE APK FILES ====="

          if [ -d "app/build" ]; then

            find app/build \
              -type f \
              -name "*.apk" \
              -print \
              || true

          else

            echo "app/build does not exist."

          fi

      - name: Verify release APK
        shell: bash
        run: |
          set -e

          RELEASE_APK=""

          while IFS= read -r file; do

            if [[ "$file" == *release*.apk ]]; then

              RELEASE_APK="$file"

              break

            fi

          done < <(
            find app/build \
              -type f \
              -name "*.apk" \
              -print
          )

          if [ -z "$RELEASE_APK" ]; then

            echo "ERROR: RELEASE APK WAS NOT GENERATED."

            find app/build \
              -maxdepth 10 \
              -type f \
              -print \
              2>/dev/null \
              || true

            exit 1

          fi

          echo "RELEASE APK FOUND:"
          echo "$RELEASE_APK"

          ls -lh "$RELEASE_APK"

      - name: List final APKs
        shell: bash
        run: |
          echo "===== FINAL APKs ====="

          find app/build \
            -type f \
            -name "*.apk" \
            -exec ls -lh {} \;

      - name: Upload debug APK
        uses: actions/upload-artifact@v4
        with:
          name: VTTS-debug
          path: |
            app/build/outputs/**/*.apk
          if-no-files-found: error

      - name: Upload release APK
        uses: actions/upload-artifact@v4
        with:
          name: VTTS-release
          path: |
            app/build/outputs/**/*.apk
          if-no-files-found: error

No changes are needed to "TtsManager.kt", "WaveWriter.kt", "app/build.gradle.kts", "AndroidManifest.xml", or the resource files.
