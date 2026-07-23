# AI Study Mentor (Android)

An offline-first Android study assistant built for the **BrightPath Learning – "AI Study
Mentor"** scenario (BTEC Unit 22: Application Development). Students register, ask academic
questions, receive structured AI-style explanations, build a personal question library,
practise with auto-generated quizzes and track their progress through XP, levels and badges.

> This repository is the **software package deliverable for Assignment Part 2, Activity 2
> (P5 + M4)** – "Develop a functional business application using the preferred tools,
> techniques and methodologies".

---

## 1. Highlights / feature map

| Requirement (from the brief) | Where it is implemented |
|---|---|
| Register with email + secure login | `activities/LoginActivity`, `activities/SignUpActivity`, `repo/UserRepository` |
| First-use setup (level, subjects, explanation style) | `activities/OnboardingActivity` |
| Ask questions (text) | `Fragments/HomeFragment` → `AnswerActivity` |
| AI answer generation (subject + difficulty, step-by-step, key concepts, common mistakes, follow-ups) | `ai/RemoteAiEngine`, `ai/LocalAiEngine`, `ai/AiAnswer` |
| Resilient AI (bounded retry, token-limit recovery, offline fallback, answer provenance) | `ai/RemoteAiEngine`, `ai/FallbackAiEngine`, `data/Question` |
| Real computed results for maths | `ai/MathEvaluator` |
| Question history & personal library (search, bookmark, filter by subject, review suggestions) | `Fragments/CategoryFragment`, `adapters/QuestionAdapter` |
| AI practice quizzes (MCQ) with instant feedback | `Fragments/QuizFragment`, `activities/QuizActivity`, `ai/LocalAiEngine#generateQuiz` |
| Progress tracking / dashboard + insights | `repo/StudyRepository#getProgress`, `HomeFragment`, `SettingsFragment` |
| Gamification (XP, levels, badges) | `util/Gamification`, `repo/StudyRepository` |
| Notifications (review reminders, level-up) | `util/NotificationHelper` |
| Security & safety (password strength, salted hashing, abuse detection) | `util/PasswordValidator`, `util/SecurityUtils`, `util/ContentModerator` |
| Offline access | Everything is stored locally in Room (`data/`) |
| AI cost management (reuse similar/identical questions) | `repo/StudyRepository#ask` (similar-question cache) |
| Light / dark theme | `util/SessionManager`, `AiMentorApp`, Material 3 day/night themes |

See the pull request description for the full requirement-by-requirement checklist,
including items intentionally deferred as future work (e.g. real 2FA, cloud sync).

## 2. Tech stack & tools

* **Language:** Java 11
* **IDE / build:** Android Studio, Gradle 8.13, Android Gradle Plugin 8.11.2
* **UI:** AndroidX AppCompat + Material 3 (Material Components), ViewPager2, RecyclerView, CardView
* **Persistence:** Room (SQLite) – single local source of truth for offline use
* **Min / target SDK:** 24 / 36
* **Package / applicationId:** `com.example.aimentor` (unchanged)

## 3. Project structure

```
app/src/main/java/com/example/aimentor/
├── AiMentorApp.java            # Application – applies saved theme
├── activities/                 # Login, SignUp, Onboarding, Menu, Answer, Quiz
├── Fragments/                  # Home (ask + dashboard), Category (library),
│                               #   Quiz, Settings
├── adapters/                   # ViewPagerAdapter, QuestionAdapter
├── ai/                         # AiEngine + offline LocalAiEngine, MathEvaluator,
│                               #   SubjectClassifier, models
├── data/                       # Room: entities, DAOs, AppDatabase
├── repo/                       # UserRepository, StudyRepository
└── util/                       # SessionManager, SecurityUtils, PasswordValidator,
                                #   Validators, Gamification, ContentModerator,
                                #   NotificationHelper
```

## 4. AI engine configuration

Text answers use the OpenAI-compatible HCNSEC chat-completions endpoint when
`HCNSEC_API_KEY` is present in the gitignored `local.properties` file:

```properties
HCNSEC_API_KEY=replace-with-a-local-demo-key
```

`RemoteAiEngine` requests model `auto` and maps structured JSON into `AiAnswer`. The request
runs through `StudyRepository`'s IO executor, so it never blocks Android's main thread. The
first request allows 1,200 completion tokens. If a reasoning model reports
`finish_reason=length`, the app retries once with 2,400 tokens and never stores the partial
answer. Temporary network, HTTP 408/429 and 5xx failures are also retried at most once. Only
the final `content` is displayed; provider `reasoning_content` is never shown or persisted.

When no key is configured, `AiEngineFactory` selects `LocalAiEngine`, a deterministic,
rule-based study coach that computes arithmetic and returns structured offline guidance.
When a key is configured but the remote provider still fails after its bounded retry,
`FallbackAiEngine` returns clearly labelled offline guidance instead of crashing. Saved
questions record whether the answer came from online AI, local mode, offline fallback or the
cache, together with the model name and response time. Practice quizzes currently remain local.

There is no API-key field in the app UI: the assignment developer configures the key at build
time and students simply install the APK. For this assignment demo the local key is compiled
into `BuildConfig`. It must never be committed, logged or shown in screenshots. A production
application must keep the provider key on a backend/proxy because values compiled into an APK
can be extracted.

## 5. Build & run

Requirements: Android Studio (Giraffe+) or command-line Android SDK with **platform 36** and
**build-tools 36**, plus a **JDK 17+**.

```bash
# 1. Point the build at your SDK (local.properties is gitignored)
echo "sdk.dir=/path/to/Android/sdk" > local.properties

# 2. Build / test / lint
chmod +x gradlew
./gradlew test           # JVM unit tests for the core logic
./gradlew lint           # static analysis
./gradlew assembleDebug  # produces app/build/outputs/apk/debug/app-debug.apk
```

Then install `app-debug.apk` on a device/emulator, or open the project in Android Studio and
press **Run**. First launch: **Sign up → onboarding → Home**.

## 6. Testing

JVM tests under `app/src/test/java` cover the pure-Java study logic plus remote response
parsing, authentication headers, bounded retry, token-limit recovery, timeout/error handling
and offline fallback. Run them with `./gradlew test`.

## 7. Security notes

Passwords are **never stored in plain text** – they are hashed with SHA-256 over a per-user
random salt (`util/SecurityUtils`). For production this should be upgraded to a slow KDF
(bcrypt / Argon2 / PBKDF2) together with encryption-at-rest (EncryptedSharedPreferences /
SQLCipher). No secrets, keys or tokens are stored in this repository.
