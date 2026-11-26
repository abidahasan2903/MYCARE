# MYCARE

**MYCARE** is an offline-first Android companion for personal health, wellness, and everyday organization. It includes an English and Bangla interface and keeps user-created records on the device.

> MYCARE is an organization tool, not a medical device. It does not diagnose conditions or replace a qualified healthcare professional.

## Download and install

1. Open [Latest MYCARE Release](https://github.com/abidahasan2903/MYCARE/releases/latest).
2. Download **MYCARE.apk** to an Android phone.
3. Open the downloaded APK. If Android asks, allow your browser or file manager to install unknown apps, then confirm **Install**.
4. Launch **MYCARE** and choose English or বাংলা.

The GitHub Actions workflow builds a debug-signed APK on each push to `main` and publishes it to the rolling **latest** release. If you already installed a MYCARE APK signed with a different key, Android may require uninstalling it before installing this release; uninstalling removes that installation's local app data, so export a backup first.

## What it includes

- **Health and medical records:** medicine lists and details, reminders, health hub, medical folders, and prescription/report attachments.
- **Daily planning:** tasks, exercise plans, grocery lists, travel plans, and nutrition tracking.
- **Personal tools:** income/expense wallet, calculator, emergency contacts and profile, memory albums, photo/video/document viewing, and voice notes.
- **Search and device actions:** doctor specialty lookup, nearby search, map/dialer links, reminders, and notifications.
- **Personalization:** English and Bangla, light/dark themes, accent themes, font sizing, and local backup/restore.

Some device features ask for permission when you use them. Internet access is used for online search or opening external map/browser content; core records are designed to work offline. The current repository does not configure Gemini or Firebase cloud features.

## Privacy and data

MYCARE does not require an account. Records and preferences are stored in the app's local Room/SQLite database and app storage. There is no cloud sync configured in this build. Use **Settings → Backup & Restore** before changing devices; imported or restored backups replace local app data. The app does not provide medical diagnosis or treatment.

## Technology

- Kotlin and Jetpack Compose with Material 3
- Android Navigation Compose
- Room / SQLite and Preferences DataStore
- Android notifications, alarms, media playback, and app storage APIs
- Coroutines, Coil, and Kotlin Serialization
- Gradle Kotlin DSL and GitHub Actions for Android APK releases

## Build locally

### Requirements

- JDK 17
- Android SDK Platform **36.1** and Build Tools **36.1.0**
- Android Studio (recommended) or the included Gradle wrapper

Clone the repository and build a debug APK:

```bash
git clone https://github.com/abidahasan2903/MYCARE.git
cd MYCARE
./gradlew assembleDebug
```

On Windows PowerShell use:

```powershell
.\gradlew.bat assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Open the project in Android Studio to run it on an emulator or a connected device.

## Repository layout

```text
app/src/main/java/com/example/
├── core/       # database, navigation, permissions, storage, reminders, theme
├── data/       # local repositories
└── feature/    # dashboard, health, medicines, records, wallet, tasks, and more
app/src/main/res/       # launcher icons, English/Bangla strings, and resources
app/src/test/           # existing unit and Robolectric test sources
gradle/                 # version catalog and Gradle wrapper
.github/workflows/      # Android APK build and latest-release workflow
```

## Releases

Every push to `main` builds `MYCARE.apk`, stores a short-lived workflow artifact, and updates the repository's [latest GitHub Release](https://github.com/abidahasan2903/MYCARE/releases/latest). Builds are debug-signed for easy installation and evaluation; a Play Store production release should use a maintainer-controlled signing key and a stable release process.

## Developer

**Abid Ahasan** · Software Engineering student · Mobile and web app developer
