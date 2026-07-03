# OfflineForms

An offline-first Android app for building, filling, and sharing data collection forms — built with Kotlin, Jetpack Compose, MVVM, and Firebase Firestore.

---

## Demo & Screenshots

> _Add screenshots and screen recordings here_

| Login | Home | Form Builder |
|:-----:|:----:|:------------:|
| `[ screenshot ]` | `[ screenshot ]` | `[ screenshot ]` |

| Fill Form | Responses | Imports |
|:---------:|:---------:|:-------:|
| `[ screenshot ]` | `[ screenshot ]` | `[ screenshot ]` |

---

## What It Does

- **Build forms** with multiple field types — text, number, date, dropdown, checkbox, radio
- **Collect responses** on the same device — no internet needed
- **Sync to cloud** automatically via Firebase Firestore when internet is available
- **Share forms** as `.json` files via WhatsApp, email, or Bluetooth
- **Import shared forms** from other devices into a dedicated Imports section
- **No mandatory login** — works fully offline from first launch, sign in is optional for cloud backup

---

## How It Works

```
You build a form
        |
        v
Tap Share on the form card
        |
        v
App exports form as a .json file
        |
        v
Send via WhatsApp / Email / Bluetooth
        |
        v  (on recipient's device)
Recipient taps file -> Open with OfflineForms
        |
        v
Form appears in their Imports screen
        |
        v
They fill it offline -> response saved locally
        |
        v
Internet returns -> auto-syncs to cloud
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material Design 3 |
| Architecture | MVVM |
| Database | Firebase Firestore (offline-first, native persistence) |
| Authentication | Firebase Auth (Anonymous + Email/Password) |
| Navigation | Jetpack Navigation Compose |
| Async | Kotlin Coroutines + StateFlow |
| Min SDK | API 24 (Android 7.0) |

---

## Architecture

```
  +--------------------------------------------------+
  |              UI Layer                            |
  |        Jetpack Compose Screens                   |
  +--------------------------------------------------+
                        |
                        v
  +--------------------------------------------------+
  |              ViewModel                           |
  |   FormViewModel - holds state, survives rotation |
  +--------------------------------------------------+
                        |
                        v
  +--------------------------------------------------+
  |              Repository                          |
  |   FormRepository - only class that calls Firebase|
  +--------------------------------------------------+
                        |
                        v
  +--------------------------------------------------+
  |           Firebase Firestore                     |
  |   Local disk cache -> auto-syncs to cloud online |
  +--------------------------------------------------+
```

---

## Project Structure

```
com.example.offlineforms/
│
├── MyApp.kt                           # Application class — initializes Firebase
├── MainActivity.kt                    # Single Activity — hosts Compose + handles file imports
│
├── Navigation/
│   └── NavGraph.kt                    # All routes and screen connections
│
├── data/
│   ├── model/
│   │   ├── Form.kt                    # Form template data class
│   │   ├── FormField.kt               # One question — FieldType enum (TEXT, NUMBER, DATE...)
│   │   ├── FormSubmission.kt          # Filled response — answers as Map<fieldId, answer>
│   │   └── ImportedForm.kt            # Form received via file sharing
│   └── repository/
│       └── FormRepository.kt          # All Firestore + Auth operations
│
├── ui/
│   ├── viewmodel/
│   │   └── FormViewModel.kt           # App state as StateFlow — shared by all screens
│   ├── screens/
│   │   ├── StartupScreen.kt           # Auth check on launch (invisible to user)
│   │   ├── NoInternetScreen.kt        # First launch with no internet
│   │   ├── LoginScreen.kt             # Optional sign in / sign up
│   │   ├── HomeScreen.kt              # Form list + sidebar + FAB
│   │   ├── FormBuilderScreen.kt       # Create and edit forms
│   │   ├── FormPreviewScreen.kt       # Read-only layout preview
│   │   ├── FillFormScreen.kt          # Active form filling
│   │   ├── ResponsesScreen.kt         # All responses for a form
│   │   ├── ResponseDetailScreen.kt    # Single response detail
│   │   ├── ImportsScreen.kt           # Imported forms from file sharing
│   │   └── FillImportedFormScreen.kt  # Fill an imported form
│   ├── components/
│   │   └── FormFields.kt              # Reusable composable components
│   └── themes/
│       ├── Color.kt
│       ├── Theme.kt                   # Light + dark color schemes
│       └── Type.kt
│
└── res/
    └── xml/
        └── file_paths.xml             # FileProvider paths for secure file sharing
```

---

## Offline Behavior

| Action | Offline | Online |
|---|---|---|
| Create form | Saved to local cache instantly | Saves to cache + syncs to cloud |
| Fill form | Response saved locally | Saves + syncs to cloud |
| Share form | Exports JSON — no internet needed | Works identically |
| Import form | Saves to local cache | Saves + syncs to cloud |
| View forms | Reads from local Firestore cache | Live from server |
| Sync badge | Shows: Saved locally | Shows: Synced to cloud |
| Login | Requires internet (Firebase Auth) | Works normally |
| Stay logged in | Works — session persists from prior launch | Works normally |

---

## Firebase Setup

1. Go to [console.firebase.google.com](https://console.firebase.google.com) and create a project
2. Register your Android app with package name `com.example.offlineforms`
3. Download `google-services.json` and place it in the `app/` folder
4. Enable **Authentication** → Email/Password + Anonymous
5. Create a **Firestore Database** in test mode
6. Add your debug SHA-1 to Firebase (from `./gradlew signingReport`) — required for email sign-up

---

## Getting Started

### Run

```bash
# Clone the repo
git clone https://github.com/yourusername/offlineforms.git

# Open in Android Studio
# Place google-services.json in app/
# Sync Gradle
# Run on device or emulator
```

---

## Key Dependencies

```toml
# libs.versions.toml
agp           = "9.2.1"
kotlin        = "2.2.10"
composeBom    = "2026.02.01"
firebaseBom   = "34.15.0"
```

```kotlin
// build.gradle.kts (module)
implementation(platform(libs.firebase.bom))
implementation(libs.firebase.firestore)
implementation(libs.firebase.auth)
implementation(libs.firebase.analytics)
implementation("androidx.navigation:navigation-compose:2.7.7")
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
implementation("androidx.compose.material:material-icons-extended:1.6.7")
implementation("com.google.android.gms:play-services-auth:21.2.0")
```

---

## Firestore Collections

```
forms/          id, title, fields[], createdAt, updatedAt, isSynced, userId
submissions/    id, formId, formTitle, answers{}, submittedAt, isSynced, userId
imports/        id, title, fields[], importedAt, originalFormId, originalCreatorId, userId
```

---

## Planned Features

- [ ] Share responses as PDF
- [ ] Google Sign-In
- [ ] Cross-device response syncing (responses sync back to original form creator)
- [ ] Local Room database for data persistence across sign-outs
- [ ] Shared workspace / team code model for multi-device anonymous response collection

---

## License

For personal and educational use.
