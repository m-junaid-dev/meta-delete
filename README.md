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

## APK GitHub Actions se banana
1. GitHub par naya repo banao aur is poore folder ki files upload/push karo (branch `main`).
2. Repo ka **Actions** tab kholo. Build khud shuru hogi (ya "Build APK" > "Run workflow").
3. Build green hone par run kholo, neeche **Artifacts** se `wa-saver-debug-apk` download karo.
4. Zip kholo, `app-debug.apk` phone par install karo (Unknown sources allow karna hoga).

## Phone par setup
1. App kholo, **Notification access do** > WA Saver ON karo.
2. **All files access do** > WA Saver ON karo.
3. **Saving start karo**.
4. Battery settings mein WA Saver ko Unrestricted karo.

## Next steps
Cloud sync (backend + S3), web dashboard, files ko app ke andar open karna, search, login/payments.
