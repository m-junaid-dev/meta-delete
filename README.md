# WA Saver (MVP)

Android app jo WhatsApp ke incoming messages, voice notes, videos aur documents
sender ke delete karne se pehle apne paas save kar leti hai.

## Kaise kaam karti hai
- **Text:** `MessageListener` WhatsApp ki notification padhta hai aur text local database mein save karta hai.
  "This message was deleted" notification aaye to purana message `[DELETED]` mark ho jata hai.
- **Media:** `MediaWatcherService` WhatsApp ke media folder ko har 2 second scan karta hai aur nayi file
  `/sdcard/WASaver/<category>/` mein copy kar deta hai.

## Limits
- WhatsApp mein **Media auto-download ON** hona chahiye, warna file phone par aati hi nahi.
- Agar sender file download hone se pehle delete kar de to wo save nahi ho sakti.
- Sirf Android. iPhone par possible nahi.
- Battery optimization mein is app ko "Unrestricted" karo, warna Android service band kar deta hai.

## APK GitHub Actions se banana (signed)
1. Repo > Settings > Secrets and variables > Actions mein 4 secrets banao: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
2. Files push/upload karo. Actions tab mein "Build APK" chalega.
3. Build ke baad APK **Releases** mein aa jati hai. Seedha download link:
   `https://github.com/<user>/<repo>/releases/latest/download/WASaver.apk`

## Phone par setup
1. Chrome se link kholo, APK download karke install karo.
2. App kholo, **Notification access** aur **All files access** do.
3. **Start** dabao. Battery settings mein WA Saver ko Unrestricted karo.

## Next steps
Cloud sync (backend + S3), web dashboard, files ko app ke andar open karna, search, login/payments.
