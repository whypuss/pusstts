package com.pusstts

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import com.google.android.material.slider.Slider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

const val TAG = "PussTTS"

class MainActivity : AppCompatActivity() {
    private var tts: OfflineTts? = null
    private var ttsReady = false
    private var numSpeakers = 0

    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var loadModelBtn: Button
    private lateinit var mainContent: View

    private lateinit var speakerSpinner: Spinner
    private lateinit var speedSlider: Slider
    private lateinit var podcastScript: EditText
    private lateinit var speakBtn: Button
    private lateinit var podcastBtn: Button
    private lateinit var stopBtn: Button
    private lateinit var logView: TextView

    private var playerService: PodcastPlayerService? = null
    private var serviceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val svc = binder as PodcastPlayerService.LocalBinder
            playerService = svc.getService()
            serviceBound = true
            appendLog("[SERVICE] PodcastPlayerService connected")
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            playerService = null
            serviceBound = false
        }
    }

    // vits-melo-tts-zh_en speaker names (curated top voices)
    private val speakerNames = listOf(
        "🎙️ 新聞男聲",       // 0
        "🎙️ 新聞女聲",       // 1
        "🎙️ 故事男聲",       // 2
        "🎙️ 故事女聲",       // 3
        "🎙️ 旁白男聲",       // 4
        "🎙️ 旁白女聲",       // 5
        "🎙️ 輕柔女聲",       // 6
        "🎙️ 活力男聲",       // 7
        "🎙️ 溫柔女聲",       // 8
        "🎙️ 磁性男聲",       // 9
        "🎙️ 甜美女聲",       // 10
        "🎙️ 成熟男聲",       // 11
        "🎙️ 清亮女聲",       // 12
        "🎙️ 沉穩男聲",       // 13
        "🎙️ 活力女聲",       // 14
        "🎙️ 知性男聲",       // 15
        "🎙️ 知性女聲",       // 16
        "🎙️ 陽光男聲",       // 17
        "🎙️ 陽光女聲",       // 18
        "🎙️ 陽剛男聲",       // 19
        "🎙️ 柔美女聲",       // 20
        "🎙️ 陽光男孩",       // 21
        "🎙️ 陽光女孩",       // 22
        "🎙️ 成熟女性",       // 23
        "🎙️ 成熟男性",       // 24
        "🎙️ 年輕女性",       // 25
        "🎙️ 年輕男性",       // 26
        "🎙️ 專業女主播",     // 27
        "🎙️ 專業男主播",     // 28
        "🎙️ 新聞主播女",     // 29
        "🎙️ 新聞主播男",     // 30
        "🎙️ 教育女聲",       // 31
        "🎙️ 教育男聲",       // 32
        "🎙️ 客服女聲",       // 33
        "🎙️ 客服男聲",       // 34
        "🎙️ 促銷女聲",       // 35
        "🎙️ 促銷男聲",       // 36
        "🎙️ 旁白專業",       // 37
        "🎙️ 朗讀女聲",       // 38
        "🎙️ 朗讀男聲",       // 39
        "🎙️ 說書男聲",       // 40
        "🎙️ 說書女聲",       // 41
        "🎙️ 動漫男聲",       // 42
        "🎙️ 動漫女聲",       // 43
        "🎙️ 遊戲男聲",       // 44
        "🎙️ 遊戲女聲",       // 45
        "🎙️ 廣播男聲",       // 46
        "🎙️ 廣播女聲",       // 47
        "🎙️ 電台男DJ",       // 48
        "🎙️ 電台女DJ",       // 49
        "🎙️ 標準普通話",     // 50
        "🎙️ 台灣國語",       // 51
        "🎙️ 北京口音",       // 52
        "🎙️ 東北口音",       // 53
        "🎙️ 四川口音",       // 54
        "🎙️ 廣東口音",       // 55
        "🎙️ 上海口音",       // 56
        "🎙️ 天津口音",       // 57
        "🎙️ 山東口音",       // 58
        "🎙️ 河南口音",       // 59
        "🎙️ 武漢口音",       // 60
        "🎙️ 長沙口音",       // 61
        "🎙️ 南京口音",       // 62
        "🎙️ 西安口音",       // 63
        "🎙️ 重慶口音",       // 64
        "🎙️ 武漢女性",       // 65
        "🎙️ 武漢男性",       // 66
        "🎙️ 成都女性",       // 67
        "🎙️ 成都男性",       // 68
        "🎙️ 哈爾濱女性",     // 69
        "🎙️ 哈爾濱男性",     // 70
        "🎙️ 上海女性",       // 71
        "🎙️ 上海男性",       // 72
        "🎙️ 杭州女性",       // 73
        "🎙️ 杭州男性",       // 74
        "🎙️ 蘇州女性",       // 75
        "🎙️ 蘇州男性",       // 76
        "🎙️ 福州女性",       // 77
        "🎙️ 福州男性",       // 78
        "🎙️ 廈門女性",       // 79
        "🎙️ 廈門男性",       // 80
        "🎙️ 廣州女性",       // 81
        "🎙️ 廣州男性",       // 82
        "🎙️ 深圳女性",       // 83
        "🎙️ 深圳男性",       // 84
        "🎙️ 香港女性",       // 85
        "🎙️ 香港男性",       // 86
        "🎙️ 台灣女性",       // 87
        "🎙️ 台灣男性",       // 88
        "🎙️ 新加坡女性",     // 89
        "🎙️ 新加坡男性",     // 90
        "🎙️ 標準央視",       // 91
        "🎙️ 標準新華",       // 92
        "🎙️ 晨間新聞女",     // 93
        "🎙️ 晨間新聞男",     // 94
        "🎙️ 晚間新聞女",     // 95
        "🎙️ 晚間新聞男",     // 96
        "🎙️ 財經頻道女",     // 97
        "🎙️ 財經頻道男",     // 98
        "🎙️ 體育頻道女",     // 99
        "🎙️ 體育頻道男",     // 100
        "🎙️ 娛樂頻道女",     // 101
        "🎙️ 娛樂頻道男",     // 102
        "🎙️ 科技頻道女",     // 103
        "🎙️ 科技頻道男",     // 104
        "🎙️ 軍事頻道女",     // 105
        "🎙️ 軍事頻道男",     // 106
        "🎙️ 健康頻道女",     // 107
        "🎙️ 健康頻道男",     // 108
        "🎙️ 美食頻道女",     // 109
        "🎙️ 美食頻道男",     // 110
        "🎙️ 旅遊頻道女",     // 111
        "🎙️ 旅遊頻道男",     // 112
        "🎙️ 教育課堂女",     // 113
        "🎙️ 教育課堂男",     // 114
        "🎙️ 兒童故事女",     // 115
        "🎙️ 兒童故事男",     // 116
        "🎙️ 童話朗讀女",     // 117
        "🎙️ 童話朗讀男",     // 118
        "🎙️ 小說男性",       // 119
        "🎙️ 小說女性",       // 120
        "🎙️ 武俠男聲",       // 121
        "🎙️ 武俠女俠",       // 122
        "🎙️ 科幻男聲",       // 123
        "🎙️ 科幻女聲",       // 124
        "🎙️ 歷史男聲",       // 125
        "🎙️ 歷史女聲",       // 126
        "🎙️ 推理男聲",       // 127
        "🎙️ 推理女聲",       // 128
        "🎙️ 愛情男聲",       // 129
        "🎙️ 愛情女聲",       // 130
        "🎙️ 喜劇男聲",       // 131
        "🎙️ 喜劇女聲",       // 132
        "🎙️ 懸疑男聲",       // 133
        "🎙️ 懸疑女聲",       // 134
        "🎙️ 紀錄片男",       // 135
        "🎙️ 紀錄片女",       // 136
        "🎙️ 訪談女主持",     // 137
        "🎙️ 訪談男主持",     // 138
        "🎙️ 脫口秀女",       // 139
        "🎙️ 脫口秀男",       // 140
        "🎙️ 配音女演員",     // 141
        "🎙️ 配音男演員",     // 142
        "🎙️ 有聲書女",       // 143
        "🎙️ 有聲書男",       // 144
        "🎙️ 詩歌朗誦女",     // 145
        "🎙️ 詩歌朗誦男",     // 146
        "🎙️ 散文朗讀女",     // 147
        "🎙️ 散文朗讀男",     // 148
        "🎙️ 天氣預報女",     // 149
        "🎙️ 天氣預報男",     // 150
        "🎙️ 鐵路廣播女",     // 151
        "🎙️ 鐵路廣播男",     // 152
        "🎙️ 地鐵廣播女",     // 153
        "🎙️ 地鐵廣播男",     // 154
        "🎙️ 機場廣播女",     // 155
        "🎙️ 機場廣播男",     // 156
        "🎙️ 展廳導覽女",     // 157
        "🎙️ 展廳導覽男",     // 158
        "🎙️ 博物館導覽",     // 159
        "🎙️ 展覽解說女",     // 160
        "🎙️ 產品介紹女",     // 161
        "🎙️ 產品介紹男",     // 162
        "🎙️ 企業培訓",       // 163
        "🎙️ 會議記錄女",     // 164
        "🎙️ 會議記錄男",     // 165
        "🎙️ 語音助手女",     // 166
        "🎙️ 語音助手男",     // 167
        "🎙️ 導航女聲",       // 168
        "🎙️ 導航男聲",       // 169
        "🎙️ 智能音箱女",     // 170
        "🎙️ 智能音箱男",     // 171
        "🎙️ 遊戲解說女",     // 172
        "🎙️ 遊戲解說男"      // 173
    )

    private val modelDirName = "vits-melo-tts-zh_en"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        bindService()
        checkPermissions()
    }

    private fun initViews() {
        statusText     = findViewById(R.id.statusText)
        progressBar    = findViewById(R.id.progressBar)
        loadModelBtn   = findViewById(R.id.loadModelBtn)
        mainContent    = findViewById(R.id.mainContent)

        speakerSpinner = findViewById(R.id.speakerSpinner)
        speedSlider    = findViewById(R.id.speedSlider)
        podcastScript  = findViewById(R.id.podcastScript)
        speakBtn       = findViewById(R.id.speakBtn)
        podcastBtn     = findViewById(R.id.podcastBtn)
        stopBtn        = findViewById(R.id.stopBtn)
        logView        = findViewById(R.id.logView)

        mainContent.visibility = View.GONE

        speedSlider.value = 1.0f

        // Speaker adapter — show speaker names
        val adapter = ArrayAdapter(this,
            android.R.layout.simple_spinner_dropdown_item, speakerNames)
        speakerSpinner.adapter = adapter

        // Default script
        podcastScript.setText("""[主播A]：歡迎收聽本期 Podcast，我是今天的主持人。
[主播B]：大家好，今天我們來聊聊最新的 AI 新聞。
[主播A]：ChatGPT 用戶數量突破新里程碑，達到了新的高度。
[主播B]：xAI 的 Grok 3 模型也正式發布了，引起了業界廣泛關注。
[主播A]：Google 推出了新的 Gemini 2.0，性能大幅提升。
[主播B]：本期內容就到這裡，感謝大家收聽，我們下期再見！""".trimIndent())

        loadModelBtn.setOnClickListener {
            loadModelBtn.isEnabled = false
            loadModelBtn.text = "初始化中..."
            lifecycleScope.launch(Dispatchers.Main) {
                initModel()
            }
        }

        speakBtn.setOnClickListener   { speak() }
        podcastBtn.setOnClickListener { playPodcast() }
        stopBtn.setOnClickListener   { stop() }
    }

    private suspend fun initModel() {
        val modelDir = File(filesDir, modelDirName)

        withContext(Dispatchers.Main) { setProgress(5, "準備中...") }

        // 第一次：從 assets 複製模型到 filesDir
        if (!modelDir.exists() || !File(modelDir, "model.onnx").exists()) {
            withContext(Dispatchers.Main) { setProgress(10, "複製模型檔案...") }
            appendLog("首次使用，正在複製模型...")
            try {
                modelDir.mkdirs()
                copyAssetRecursively("models/$modelDirName", modelDir)
                appendLog("模型複製完成: ${modelDir.absolutePath}")
            } catch (e: Exception) {
                appendLog("[ERROR] 複製模型失敗: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "模型初始化失敗:\n${e.message}", Toast.LENGTH_LONG).show()
                    loadModelBtn.isEnabled = true
                    loadModelBtn.text = "重試"
                }
                return
            }
        }

        loadTtsFromDir(modelDir.absolutePath)
    }

    private fun copyAssetRecursively(assetPath: String, destDir: File) {
        val assetManager: AssetManager = assets
        try {
            val list = assetManager.list(assetPath) ?: emptyArray()
            if (list.isEmpty()) {
                // 是檔案，複製它
                destDir.parentFile?.mkdirs()
                assetManager.open(assetPath).use { input ->
                    FileOutputStream(destDir).use { output ->
                        input.copyTo(output)
                    }
                }
            } else {
                // 是目錄，遞迴處理
                destDir.mkdirs()
                for (name in list) {
                    copyAssetRecursively("$assetPath/$name", File(destDir, name))
                }
            }
        } catch (e: Exception) {
            throw RuntimeException("複製 $assetPath 失敗: ${e.message}", e)
        }
    }

    private suspend fun loadTtsFromDir(modelPath: String) {
        withContext(Dispatchers.Main) { setProgress(90, "載入 TTS 引擎...") }
        appendLog("modelPath: $modelPath")

        try {
            // VITS melo — 只需要 model.onnx + tokens.txt，不需要 ruleFsts / lexicon
            val config = getOfflineTtsConfig(
                modelDir = modelPath,
                modelName = "model.onnx",
                acousticModelName = "",
                vocoder = "",
                voices = "",
                lexicon = "",
                dataDir = "",
                dictDir = "",
                ruleFsts = "",
                ruleFars = "",
                numThreads = 4,
                isKitten = false,
            )

            withContext(Dispatchers.Main) { setProgress(95, "建立 TTS...") }
            appendLog("Creating OfflineTts (VITS melo)...")

            tts = OfflineTts(config = config)
            numSpeakers = tts?.numSpeakers() ?: 0
            appendLog("OfflineTts created!")
            appendLog("Sample rate: ${tts?.sampleRate()} Hz")
            appendLog("Num speakers: $numSpeakers")

            withContext(Dispatchers.Main) {
                setProgress(100, "就緒 ✓")
                appendLog("TTS ready! ✓")
                statusText.text = "就緒 ✓  ($numSpeakers 音色)"
                ttsReady = true

                mainContent.visibility = View.VISIBLE
                loadModelBtn.visibility = View.GONE

                speakBtn.isEnabled = true
                podcastBtn.isEnabled = true
            }

        } catch (e: Exception) {
            val msg = "[ERROR] ${e.message}\n${e.stackTraceToString().take(500)}"
            appendLog(msg)
            withContext(Dispatchers.Main) {
                statusText.text = "載入失敗: ${e.message}"
                Toast.makeText(this@MainActivity, "載入失敗:\n${e.message}", Toast.LENGTH_LONG).show()
                loadModelBtn.isEnabled = true
                loadModelBtn.text = "重試"
            }
        }
    }

    private fun setProgress(pct: Int, msg: String = "") {
        runOnUiThread {
            progressBar.progress = pct
            if (msg.isNotEmpty()) statusText.text = msg
        }
    }

    private fun bindService() {
        Intent(this, PodcastPlayerService::class.java).also { intent ->
            startService(intent)
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.INTERNET), 1)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun speak() {
        val engine = tts
        if (!ttsReady || engine == null) {
            Toast.makeText(this, "TTS 尚未就緒", Toast.LENGTH_SHORT).show()
            return
        }

        val text = podcastScript.text.toString().trim()
        if (text.isEmpty()) {
            Toast.makeText(this, "請輸入內容", Toast.LENGTH_SHORT).show()
            return
        }

        val sid   = speakerSpinner.selectedItemPosition
        val speed = speedSlider.value

        speakBtn.isEnabled = false
        appendLog("[SPEAK] sid=$sid speed=$speed")

        Thread {
            try {
                val audio = engine.generate(text = text, sid = sid, speed = speed)
                appendLog("[SPEAK] Generated ${audio.samples.size} samples")
                playAudioSync(audio.samples, audio.sampleRate)
                runOnUiThread { speakBtn.isEnabled = true }
            } catch (e: Exception) {
                appendLog("[SPEAK] ERROR: ${e.message}")
                runOnUiThread { speakBtn.isEnabled = true }
            }
        }.start()
    }

    private fun playPodcast() {
        val engine = tts
        if (!ttsReady || engine == null) {
            Toast.makeText(this, "TTS 尚未就緒", Toast.LENGTH_SHORT).show()
            return
        }
        val service = playerService
        if (service == null) {
            Toast.makeText(this, "服務未就緒", Toast.LENGTH_SHORT).show()
            return
        }

        val script = podcastScript.text.toString().trim()
        if (script.isEmpty()) {
            Toast.makeText(this, "請輸入 Podcast 腳本", Toast.LENGTH_SHORT).show()
            return
        }

        val speed   = speedSlider.value
        val chunker = TextChunker(maxChars = 80)
        val chunks  = chunker.split(script)

        appendLog("[PODCAST] ${chunks.size} chunks, speed=$speed")
        podcastBtn.isEnabled = false

        service.playChunks(chunks, engine, speed)
    }

    private fun stop() {
        playerService?.stop()
        podcastBtn.isEnabled = true
        appendLog("[STOP] stopped")
    }

    private fun playAudioSync(samples: FloatArray, sampleRate: Int) {
        val minBuffer = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val track = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .setUsage(AudioAttributes.USAGE_MEDIA).build())
            .setAudioFormat(AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setBufferSizeInBytes(minBuffer * 4)
            .setTransferMode(AudioTrack.MODE_STREAM).build()

        val pcm = ShortArray(samples.size)
        for (i in samples.indices) {
            pcm[i] = (samples[i].coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
        }

        track.play()
        track.write(pcm, 0, pcm.size)
        track.stop()
        track.release()
    }

    private fun appendLog(msg: String) {
        Log.i(TAG, msg)
        runOnUiThread { logView.append("$msg\n") }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
        try { tts?.release() } catch (_: Exception) {}
    }
}
