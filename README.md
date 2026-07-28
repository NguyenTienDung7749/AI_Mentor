# AI Study Mentor

AI Study Mentor is an Android Java application created for the BrightPath
Learning scenario in BTEC Unit 22: Application Development. It combines a
Groq-powered study assistant with on-device OCR, personalised quizzes, local
history, progress tracking and scheduled study reminders.

This repository contains the application implementation. Assignment analysis,
test evidence, screenshots, peer review and evaluation belong in the submitted
report rather than in the source tree.

## Features

| Area | Current implementation |
|---|---|
| Accounts | Registration, login, onboarding and local session handling |
| AI answers | Structured answers in the question language with subject and difficulty classification |
| Model routing | Local selection between `llama-3.1-8b-instant` and `llama-3.3-70b-versatile` |
| Reliability | One bounded alternate-model attempt, a 60-second total deadline and rule-based offline fallback |
| OCR | Photo Picker or camera input, on-device ML Kit OCR and editable extracted text |
| History | Search, subject filter, bookmarks and reviewed state backed by Room |
| Quizzes | Topic/history personalisation, adaptive difficulty, multiple choice, true/false, short answer, fill-in-the-blank, explanations and retrying mistakes |
| Progress | Real Room statistics, review time, 7/30-day accuracy trends, repeated topics, subject totals, XP, levels and badges |
| Notifications | User-controlled daily WorkManager reminder, selectable time and a test notification |
| Presentation | Material UI, light/dark mode, unlockable avatars/accent themes, loading/error/empty states and state restoration |

Only successful remote answers are saved to Room. Offline fallback content is
temporary and does not enter history, progress or XP calculations. Submitting a
question always starts a fresh request instead of reusing an old answer.

## Technology

- Java 11 source compatibility
- Android SDK: min 24, compile/target 36
- Android Gradle Plugin 8.11.2 and Gradle 8.13
- AndroidX, Material Components and ViewPager2
- Room for local persistence
- Retrofit, OkHttp and Gson for the Groq OpenAI-compatible endpoint
- ML Kit Text Recognition for on-device OCR
- WorkManager for persistent, inexact daily reminders
- JUnit, MockWebServer, AndroidX Test and Espresso

## Project structure

```text
app/src/main/java/com/example/aimentor/
|-- activities/    Login, signup, onboarding, answer and quiz screens
|-- Fragments/     Home, history, quiz setup and settings
|-- adapters/      ViewPager and question-list adapters
|-- ai/            Remote/local engines, parsing, classification and quiz models
|-- data/          Room entities, DAOs, database and migrations
|-- network/       Groq Retrofit service and request/response models
|-- repo/          Asynchronous user and study repositories
|-- util/          Security, validation, gamification and reminder helpers
`-- worker/        Scheduled study reminder worker
```

## Local configuration

The application does not ask students for an API key. The developer supplies
the key locally at build time.

Create or update the root `local.properties` file:

```properties
sdk.dir=/absolute/path/to/Android/Sdk
GROQ_API_KEY=replace-with-your-own-groq-key
MISTRAL_API_KEY=replace-with-your-own-mistral-key
```

`local.properties` is ignored by Git. Never put a real key in Java, XML,
screenshots, issues, reports or commits.

The debug prototype reads keys into `BuildConfig`. Text-only questions use:

```text
https://api.groq.com/openai/v1/chat/completions
```

Questions with one attached image use:

```text
https://api.mistral.ai/v1/chat/completions
```

This is acceptable only for a controlled classroom prototype using fictional
data. A value compiled into an APK can be extracted. A production version must
keep the provider credential on a trusted backend/proxy and authenticate app
users before forwarding requests.

If `GROQ_API_KEY` is empty, the app uses `LocalAiEngine`. When a configured
remote request fails with an eligible transient error, the app may try the
alternate Groq model once within the same 60-second deadline and then show the
offline fallback without crashing.

## Build and run

Prerequisites:

- Android Studio with Android SDK/platform 36
- JDK 17 or the JBR bundled with Android Studio
- An Android device or emulator running API 24 or newer

Windows PowerShell:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
.\gradlew.bat verifyReleaseConfiguration assembleRelease
```

macOS/Linux:

```bash
./gradlew testDebugUnitTest assembleDebug lintDebug
```

The debug APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Release builds require a valid local `GROQ_API_KEY`, enable R8 code
optimisation/obfuscation and remove unused resources. The generated release APK
is unsigned unless a developer supplies a private signing configuration outside
the repository.

Open the project in Android Studio, select a device and run the `app`
configuration. The normal first-use flow is:

```text
Sign up -> onboarding -> Home
```

## Testing

JVM tests cover:

- response parsing and bilingual structured output
- local model selection and retry/error mapping
- the shared 60-second fallback deadline
- authentication, password hashing and validation
- subject classification, quiz scoring, XP, levels and badges
- input/OCR limits and reminder-time calculation

Instrumented tests cover:

- Room repository operations and user isolation
- online-only answer persistence and offline non-persistence
- atomic XP/review/quiz mutations
- asynchronous repository callbacks
- ViewModel state restoration and duplicate-request prevention
- unique WorkManager scheduling, cancellation and notification channel setup

Run instrumented tests with a connected device:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

Use a dedicated test device/profile: the Android Gradle test task may reinstall
or uninstall the debug package and therefore remove its local accounts and
Room history.

## Data and security notes

- Passwords are stored as per-user salted PBKDF2 hashes; legacy hashes are
  upgraded after a successful login.
- Room main-thread access is disabled. Repository work runs on background
  executors and returns results to the main thread.
- Photos remain on the device. OCR runs locally and only the edited text is
  sent to the AI provider.
- Question input is limited to 6,000 characters.
- App backup is disabled and the Settings screen provides account/data deletion.
- Daily reminders are optional, use one unique periodic job and are cancelled
  on normal logout or account deletion.
- The application is an educational prototype and should use fictional data.

## Known prototype limitations

- Authentication and all user data are device-local; there is no cloud account
  service or multi-device synchronisation.
- The Groq key is recoverable from a built APK because there is no backend proxy.
- WorkManager reminders are battery-friendly and persistent but not exact alarms;
  Android may deliver them later than the selected time.
- Offline answers are deterministic study guidance, not a replacement for the
  remote language models.
- Release signing, production monitoring and store deployment are outside the
  assignment prototype scope.
