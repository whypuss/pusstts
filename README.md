# PussTTS

Android 離線 Podcast TTS 應用基於 Sherpa-ONNX VITS，無需網路，174 種音色開箱即用。

![Platform](https://img.shields.io/badge/Platform-Android-blue)
![License](https://img.shields.io/badge/License-MIT-green)

## 功能

- **完全離線** — 模型內建，無需網路
- **174 種音色** — 涵蓋新聞主播、朗讀、播客、各地口音
- **雙人 Podcast** — 使用 `[主播A]：` / `[主播B]：` 標記切換說話者
- **語速調節** — 0.5x ~ 2.0x
- **背景播放** — Foreground Service + 通知欄控制
- **Material Design 3** — 現代化 UI

## 支援型號

- vits-melo-tts-zh_en（主力，174 音色）

## 安裝

APK 直接安裝即可，首次啟動會從 app 內複製模型（約 163MB）。

## 從源碼編譯

```bash
cd android/SherpaOnnxTts
./gradlew assembleDebug
```

## 技術棧

- **引擎**: Sherpa-ONNX VITS
- **模型**: vits-melo-tts-zh_en（Coqui/TTS 導出）
- **UI**: Material Design 3 / Kotlin
- **最低 Android**: API 24 (Android 7.0)
