# AI Study Mentor assignment verification

Verification date: 4 August 2026
Primary device: Pixel 7 Android 15 (API 35) AVD  
Implementation branch: `codex/assignment-remediation-batches`

## Outcome

The upgraded submission preserves the existing authentication, onboarding,
five-tab navigation, AI answer, Library, quiz, progress, rewards, appearance
and reminder flows. The final audit adds real optional TOTP two-factor sign-in,
Android-Keystore protection for its setup key, a clearer branded app bar,
cleaner quiz dropdowns and response validation that rejects an AI quiz when an
explanation does not support its own answer. The final visual pass also refreshes
the full-page sign-in/sign-up presentation with a BTEC identity plate, semantic
field icons and an elevated destination card without changing any user flow.

The app remains intentionally scoped as a classroom prototype. API keys are
read from `local.properties` and compiled into `BuildConfig`, as agreed for
this assignment. That is acceptable for a private demonstration build but is
not a production secret-management design.

The final minified release APK is 2,612,862 bytes (2.49 MiB), 13.03% smaller
than the verified 3,004,303-byte pre-cleanup build. Adding genuine 2FA,
Keystore encryption, quiz validation and the final UI resources increased the
previous optimized APK by only 12,211 bytes (0.47%).

Assessment verdict: ready for the Android assignment demonstration and report.
The required MVP (AI question/answer, history and basic progress) is complete,
and the implementation also covers the assignment's onboarding, mixed-format
quiz, review, personalization, rewards, reminders, offline and privacy goals.
The local leaderboard and unsigned APK remain honestly documented prototype
boundaries rather than being presented as an online production service.

## Requirement traceability

| Area | User-visible evidence | Main implementation | Automated evidence |
|---|---|---|---|
| Registration and onboarding | Sign-up enforces password rules, then collects education level, subjects and explanation style | `SignUpActivity`, `OnboardingActivity`, `PasswordUtils` | Authentication and repository tests plus final fresh-account test |
| Authentication safety | Back cannot reveal a stale sign-in/onboarding activity after entering the app | `LoginActivity`, `OnboardingActivity` clear the task | Existing activity tests plus manual registration |
| Optional two-factor sign-in | Settings can enable/disable a standard 6-digit authenticator code; login pauses before creating the session | `SecondFactorManager`, `TotpUtils`, `LoginActivity`, `SettingsFragment` | RFC 6238 unit vectors, Android Keystore instrumented test and full manual sign-in cycle |
| Personalized AI | Education level, subject and explanation style continue to shape requests | `HomeFragment`, `RemoteAiEngine`, `StudyRepository` | AI engine and repository tests |
| Text and image questions | Home supports academic text, gallery image and camera input with subject/style controls | `HomeFragment`, Mistral vision path and image consent | AI parsing tests and manual consent/picker tests |
| Structured learning answer | Direct answer, steps, simplified explanation, concepts, common mistakes and follow-ups render in a readable answer screen | `AiAnswer`, `RemoteAiEngine`, `AnswerActivity` | Structured-response tests plus a real online Mathematics answer |
| Exact saved-answer reuse | Home contains an explicit opt-in checkbox; only the exact normalized question/profile key is reused | `AnswerCacheKey`, `StudyRepository` | `AnswerCacheKeyTest` and Room repository test |
| Offline question queue | Home and Library show pending state; the question survives process restart and sends when connected | `PendingQuestion`, Room DAO, WorkManager worker | Room queue tests and real airplane-mode test |
| Duplicate prevention | Each queued request has a durable request key and successful sync checks it before XP/history insertion | `PendingQuestion.requestKey`, `Question.requestKey` | Repository idempotency tests |
| Review workflow | Library filters All, Due, Learning and Mastered from one shared classifier | `ReviewState`, `CategoryFragment` | `ReviewStateTest` |
| Search, subjects and bookmarks | Library searches saved prompts/answers, filters subjects and opens/bookmarks answers | `CategoryFragment`, `QuestionDao`, `AnswerActivity` | Room isolation tests and final online-answer workflow |
| Study plan | Home shows a focused plan based on due and learning counts | `HomeFragment`, repository `Progress` | Repository progress tests and UI test |
| Contextual reminders | Review-due, recurring-topic and weekly content respects the existing master reminder; answer-ready opens the saved answer | `StudyReminderWorker`, `NotificationHelper` | Worker/scheduler tests and notification capability tests |
| Progress quality | Current streak, per-subject mastery, activity, trends and review state appear alongside existing XP and badges | `ProgressMetrics`, `ProgressFragment` | `ProgressMetricsTest` and repository analytics tests |
| Four-format quiz and feedback | AI/local quizzes include multiple choice, true/false, short answer and fill-in-the-blank with immediate explanation, score, XP and retry-mistakes | `QuizActivity`, `RemoteAiEngine`, `LocalAiEngine` | Quiz tests plus live five-question generation |
| Quiz answer integrity | A remote quiz is retried/falls back if an explanation appears to belong to another answer | `RemoteAiEngine.explanationSupportsAnswer` | Dedicated mismatched-explanation regression test and live corrected feedback |
| Leaderboard privacy | Ranking is opt-in, contains name/XP only and clearly says it is limited to opted-in accounts on this device | `SessionManager`, safe Room projection, `ProgressFragment` | Room projection compile validation and manual test |
| Image privacy | First gallery/camera use explains provider transfer and offline pending storage | `HomeFragment`, user-scoped consent preference | Manual dialog test |
| Local data protection | App records use Android app-private credential-encrypted storage; the TOTP seed adds AES-256-GCM with a non-exportable Keystore key; backup is disabled | manifest, Room, `SecondFactorManager` | Android Keystore instrumented test and device encryption check |
| Data controls | Settings can export a text summary, clear study history/XP while keeping the account, or delete all account data with password confirmation | `SettingsFragment`, repository transaction | Instrumented clear/delete isolation tests |
| Upgrade safety | Room v4-to-v5 migration adds cache/queue fields and tables without destructive migration | `AppDatabase.MIGRATION_4_5` | 31 connected tests on a real Android Room runtime |
| Youthful professional UI | Existing gradient/cards/navigation remain; BTEC-branded full-page authentication, elevated destination card, consistent outlined dropdowns and a polished security card remove duplicated/rough states | Material layouts/styles | Emulator review at 1080 x 2400, light/dark and 100%/130% font scale |

## Real-device workflow log

The following sequence was performed against the installed debug APK, not a
mock UI:

1. Registered `manual.20260730@example.com`, completed onboarding with Science
   and Programming, then reached Home with a cleared authentication back stack.
2. Verified the new Home study-plan card and the unchanged five-tab navigation.
3. Verified the Library subject/bookmark controls and
   All/Due/Learning/Mastered chips at 1080 x 2400.
4. Opened Add image and confirmed the provider/offline-storage consent dialog
   appears before the Android photo picker.
5. Enabled airplane mode, submitted “Explain offline queue in Android”, and
   observed “1 question is waiting for a connection”.
6. Force-stopped and relaunched the app while offline. Library still reported
   one queued question, proving Room durability.
7. Disabled airplane mode. WorkManager sent the queued item and Library showed
   the resulting Programming answer. The remote source was
   `openai/gpt-oss-120b`, with a measured response time of approximately
   6 seconds.
8. Opened the saved answer, bookmarked it and marked it reviewed. XP moved to
   12 (10 ask XP plus 2 review XP).
9. Verified Progress subject mastery, weekly activity/streak data and the
   opt-in-only on-device leaderboard.
10. Verified Settings reminder categories, export, leaderboard opt-in, clear
    history, logout and password-protected account deletion controls.

Final regression on 4 August 2026:

1. Created a fresh account, completed High School onboarding and selected
   Mathematics and Programming.
2. Sent a real Mathematics question. `openai/gpt-oss-120b` returned a saved,
   structured answer in 6.1 seconds; bookmarking and review both persisted.
3. Generated a five-question personalized quiz through the live provider. A
   cross-question explanation mismatch found during the audit was converted
   into a regression test and a provider-response guard. The repeated live
   test returned answer-consistent feedback.
4. Enabled authenticator verification, logged out, proved that password-only
   sign-in was blocked, entered a current TOTP code, reached Home, and then
   disabled the factor with another current code.
5. Repeated the main Home review in dark mode and at 130% system font size;
   navigation, cards and primary actions remained readable and reachable.
6. Finished with 75 JVM tests and 31 Android instrumented tests passing, 0
   skipped/failed, Android Lint at 0 errors and the minified release build
   succeeding.

## Screenshot evidence

- `docs/evidence/01-login.png`
- `docs/evidence/02-home-study-plan.png`
- `docs/evidence/03-library-review-filters.png`
- `docs/evidence/04-offline-queue-synced-answer.png`
- `docs/evidence/05-progress-mastery-leaderboard.png`
- `docs/evidence/06-settings-privacy.png`
- `docs/evidence/07-final-home.png`
- `docs/evidence/08-account-security.png`
- `docs/evidence/10-online-ai-answer.png`

## Known prototype limitations

- Provider keys in a client APK can be extracted. A production release needs a
  server-side proxy, key rotation, quotas and abuse monitoring.
- Room and preferences use Android's app-private credential-encrypted storage
  and are excluded from backup. This is platform-layer protection rather than
  a separate SQLCipher layer. Passwords are PBKDF2 salted hashes, and the TOTP
  seed has an additional AES-GCM layer backed by Android Keystore.
- The leaderboard is deliberately local and opt-in; there is no multi-device
  account synchronization or online ranking service.
- The release APK produced locally is unsigned. Submission/publishing requires
  the owner’s keystore and signing configuration.
- Network answer quality and availability still depend on the configured
  third-party AI provider.
