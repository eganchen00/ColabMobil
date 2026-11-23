# Colab Mobile (Android / Kotlin / Jetpack Compose)

原生、離線的 `.ipynb` 筆記本編輯器。支援開啟、建立、編輯、儲存、另存新檔、程式碼/Markdown 儲存格、上下移動及刪除儲存格。透過外部瀏覽器開啟官方 Google Colab。

## 編譯與安裝
1. 安裝 Android Studio（建議使用內建 JDK 17），開啟此資料夾。
2. 連上網路，讓 Gradle 下載 Android Gradle Plugin、Kotlin 和 Compose 依賴。
3. 安裝 Android SDK Platform 35（如 Android Studio 提示）。
4. 點選 **Build > Build APK(s)**，APK 位於 `app/build/outputs/apk/debug/app-debug.apk`。
5. 傳送 APK 到 Android 手機並安裝；Android 8.0 以上可用。

## 功能界線
- 本版**不會在 App 內執行 Python 或直接連線 Colab GPU**。點擊「開啟 Colab」會在系統瀏覽器開啟官方網站，登入後可上傳 `.ipynb` 執行。
- 匯入 `.ipynb` 時只讀取 code/markdown 儲存格及原始碼；**重新儲存會移除原有輸出、執行計數、儲存格 metadata、筆記本 metadata**。請先備份原始檔。raw cells 會當作 code 載入。
- 本版尚未實作同步 Google Drive、語法高亮、真正的游標位置插入快捷鍵、Notebook 輸出顯示及雲端 kernel 控制。
- Android 的檔案存取透過 Storage Access Framework，不需全磁碟權限。
- Google Colab 與 Google 為第三方服務，本專案非 Google 官方產品。

## 專案設定
- Android Gradle Plugin 8.7.3
- Kotlin 2.0.21 + Compose compiler plugin
- Compose BOM 2024.12.01
- compileSdk / targetSdk 35；minSdk 26
- JDK 17

此原始碼未在目前環境執行 Android Gradle 編譯，首次 Sync/Build 可能需要依 Android Studio 的 SDK/依賴提示調整。
