# WA Gateway - Android

WhatsApp automation gateway app using Android Accessibility Service + local web UI.

## Architecture

- **Backend:** Kotlin, Accessibility Service, Ktor HTTP server (embedded), Room SQLite
- **Frontend:** React + TypeScript + Vite (served from APK assets)
- **Automation:** Accessibility Service for WhatsApp interaction
- **Data Source:** Google Sheets (read-only via Service Account)

## Features

- Send WhatsApp messages automatically via Accessibility Service
- Human-like typing behavior (configurable)
- Google Sheets integration (fetch targets from spreadsheet)
- Local web UI on port 8888 (Dashboard, Settings, History, Logs)
- Scheduled sending (daily/weekly, via AlarmManager)
- Persists across reboots (BootReceiver)

## Prerequisites

1. Android 10+ device
2. Node.js 18+ (untuk build Web UI — otomatis via Gradle)
3. Google Service Account key with Sheets API access
4. Google Sheet with columns: NOMOR_HP, NAMA_PEMILIK, NOMOR_KENDARAAN, MASA_BERLAKU

## Build

Web UI dibuild otomatis oleh Gradle task `copyWebUiAssets` (npm ci → npm run build → copy ke assets). Cukup jalankan:

```bash
./gradlew assembleRelease
```

APK: `app/build/outputs/apk/release/app-release.apk`

### Install on Device

Install `app/build/outputs/apk/release/app-release.apk` on your Android device.

### 4. First Run

1. Open WA Gateway app
2. Grant Accessibility Service permission (Settings → Accessibility → WA Gateway)
3. Grant overlay permission when prompted
4. Open http://localhost:8888 in browser
5. Upload your Service Account key JSON
6. Enter Sheet ID and Sheet Tab name
7. Click "Test Connection" to verify
8. Configure message template
9. Click "Sync Now" to fetch targets

## Google Service Account Setup

1. Go to [Google Cloud Console](https://console.cloud.google.com)
2. Create a project (or select existing)
3. Enable Google Sheets API
4. Create Service Account → download JSON key
5. Share your Google Sheet with the service account email (Viewer role)

## Building for Release (GitHub Actions)

1. Store secrets in GitHub:
   - `KEYSTORE_BASE64`: base64-encoded keystore file
   - `KEYSTORE_PASSWORD`: keystore password
   - `KEY_ALIAS`: key alias
   - `KEY_PASSWORD`: key password
2. Push a tag: `git tag v1.0.0 && git push origin v1.0.0`
3. Workflow builds, signs, and uploads APK to Releases

## Security

- SA key encrypted with AES256-GCM (EncryptedSharedPreferences)
- Web UI only accessible on localhost
- SA key never logged (redacted in logs)
- ProGuard obfuscation for release builds
- Read-only Google Sheets scope

## License

Internal tool.
