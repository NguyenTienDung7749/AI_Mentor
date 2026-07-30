# AI Study Mentor assignment verification

Verification date: 30 July 2026  
Primary device: Pixel 7 Android 15 (API 35) AVD  
Implementation branch: `codex/assignment-remediation-batches`

## Outcome

The upgraded submission preserves the existing authentication, onboarding,
five-tab navigation, AI answer, Library, quiz, progress, rewards, appearance
and reminder flows. The remediation work adds the previously missing
submission-grade behavior without replacing working screens.

The app remains intentionally scoped as a classroom prototype. API keys are
read from `local.properties` and compiled into `BuildConfig`, as agreed for
this assignment. That is acceptable for a private demonstration build but is
not a production secret-management design.

## Requirement traceability

| Area | User-visible evidence | Main implementation | Automated evidence |
|---|---|---|---|
| Authentication safety | Back cannot reveal a stale sign-in/onboarding activity after entering the app | `LoginActivity`, `OnboardingActivity` clear the task | Existing activity tests plus manual registration |
| Personalized AI | Education level, subject and explanation style continue to shape requests | `HomeFragment`, `RemoteAiEngine`, `StudyRepository` | AI engine and repository tests |
| Exact saved-answer reuse | Home contains an explicit opt-in checkbox; only the exact normalized question/profile key is reused | `AnswerCacheKey`, `StudyRepository` | `AnswerCacheKeyTest` and Room repository test |
| Offline question queue | Home and Library show pending state; the question survives process restart and sends when connected | `PendingQuestion`, Room DAO, WorkManager worker | Room queue tests and real airplane-mode test |
| Duplicate prevention | Each queued request has a durable request key and successful sync checks it before XP/history insertion | `PendingQuestion.requestKey`, `Question.requestKey` | Repository idempotency tests |
| Review workflow | Library filters All, Due, Learning and Mastered from one shared classifier | `ReviewState`, `CategoryFragment` | `ReviewStateTest` |
| Study plan | Home shows a focused plan based on due and learning counts | `HomeFragment`, repository `Progress` | Repository progress tests and UI test |
| Contextual reminders | Review-due, recurring-topic and weekly content respects the existing master reminder; answer-ready opens the saved answer | `StudyReminderWorker`, `NotificationHelper` | Worker/scheduler tests and notification capability tests |
| Progress quality | Current streak, per-subject mastery, activity, trends and review state appear alongside existing XP and badges | `ProgressMetrics`, `ProgressFragment` | `ProgressMetricsTest` and repository analytics tests |
| Quiz feedback | Completion result includes a mastery signal while retaining score, XP and retry-mistakes | `QuizActivity` | Existing quiz unit/instrumented tests |
| Leaderboard privacy | Ranking is opt-in, contains name/XP only and clearly says it is limited to opted-in accounts on this device | `SessionManager`, safe Room projection, `ProgressFragment` | Room projection compile validation and manual test |
| Image privacy | First gallery/camera use explains provider transfer and offline pending storage | `HomeFragment`, user-scoped consent preference | Manual dialog test |
| Data controls | Settings can export a text summary, clear study history/XP while keeping the account, or delete all account data with password confirmation | `SettingsFragment`, repository transaction | Instrumented clear/delete isolation tests |
| Upgrade safety | Room v4-to-v5 migration adds cache/queue fields and tables without destructive migration | `AppDatabase.MIGRATION_4_5` | 30 connected tests on a real Android Room runtime |

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

## Screenshot evidence

- `docs/evidence/01-login.png`
- `docs/evidence/02-home-study-plan.png`
- `docs/evidence/03-library-review-filters.png`
- `docs/evidence/04-offline-queue-synced-answer.png`
- `docs/evidence/05-progress-mastery-leaderboard.png`
- `docs/evidence/06-settings-privacy.png`

## Known prototype limitations

- Provider keys in a client APK can be extracted. A production release needs a
  server-side proxy, key rotation, quotas and abuse monitoring.
- The Room database is private to the Android app and excluded from backup, but
  the whole database is not encrypted at rest. Passwords themselves are salted
  and hashed.
- The leaderboard is deliberately local and opt-in; there is no multi-device
  account synchronization or online ranking service.
- The release APK produced locally is unsigned. Submission/publishing requires
  the owner’s keystore and signing configuration.
- Network answer quality and availability still depend on the configured
  third-party AI provider.
