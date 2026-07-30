# AI Study Mentor remediation batches

This plan upgrades the assignment prototype without redesigning the working
application. The existing five-tab navigation, authentication/onboarding,
question and answer flow, library, quiz types, progress metrics, rewards,
themes and reminders are protected by regression tests throughout the work.

The application remains a classroom prototype. Provider API keys continue to
come from `local.properties` and are compiled into `BuildConfig`; this is
documented as a known production limitation rather than replaced with a
backend in this submission cycle.

## UI references and preservation decisions

The upgrade borrows interaction patterns, not branding or a wholesale visual
redesign:

- Quizlet's Scheduled Review and Memory Score informed the small
  Due/Learning/Mastered filters and the actionable study-plan summary:
  https://quizlet.com/in/features/spaced-repetition
- Khan Academy's Familiar/Proficient/Mastered progression informed
  subject-mastery feedback while retaining the app's existing XP, levels and
  badges:
  https://support.khanacademy.org/hc/en-us/articles/115002552631-What-are-Course-and-Unit-Mastery-
- Duolingo's separation of streak from the daily goal informed showing streak
  as motivation without changing the existing XP rules:
  https://blog.duolingo.com/improving-the-streak/
- Material Design's consistent enabled/disabled/focused/pressed states
  informed the visible queued/sending/failed feedback and retry affordance:
  https://m3.material.io/foundations/interaction/states/overview

The existing color system, card language, five-tab navigation, answer layout,
quiz flow and working settings were deliberately preserved.

## Batch 0 — Baseline and traceability

Status: complete

- Record the build, test and lint baseline.
- Protect the current user journeys with a manual regression checklist.
- Map each remediation batch to an assignment requirement and evidence.

Exit criteria:

- Debug and release builds succeed.
- 62 unit and 27 instrumented tests pass.
- The Git worktree is clean before implementation begins.

## Batch 1 — Submission safety and low-risk fixes

Status: complete

- Clear the authentication activity back stack after login/onboarding.
- Correct README build, image-processing and privacy documentation.
- Keep the client-side API-key setup but label it as prototype-only.
- Fix safe UI lint findings: hardcoded text, unused resources, redundant
  parents and simple overdraw/compound-drawable findings where they do not
  alter the visual design.
- Document release signing requirements.

Exit criteria:

- Back from the main screen cannot reveal a stale login screen.
- README matches the implementation.
- Existing tests pass and no new lint errors are introduced.

## Batch 2 — Offline queue and exact-answer cache

Status: complete

- Add Room entities and migrations for pending questions and reusable answers.
- Queue text questions while offline and retry them with WorkManager.
- Use an idempotency key to avoid duplicate answers.
- Reuse exact matching successful answers; do not silently reuse loosely
  similar answers.
- Surface queued, sending and failed states in the existing Home/Library UI.

Exit criteria:

- A question asked in airplane mode survives restart and sends once online.
- Retry cannot create duplicate history entries.
- Users can bypass a cached answer and request a fresh response.

## Batch 3 — Review plan and notification coverage

Status: complete

- Add due, learning and mastered review states.
- Build a "Today's study plan" card from bookmarks, mistakes and due reviews.
- Add review-due, recurring-topic, weekly-summary and answer-ready
  notification preferences while preserving the existing reminder switch.
- Deep-link answer-ready notifications to the saved answer and general study
  reminders to the main study workspace.

Exit criteria:

- Each notification type can be enabled independently and is not duplicated.
- Library filtering and the study plan use the same review-state source.

## Batch 4 — Mastery, streak and local leaderboard

Status: complete

- Add subject/topic mastery to the existing Progress screen.
- Extend quiz results with mastery movement and recommended next practice.
- Add a study streak and weekly activity summary without replacing current
  XP/levels.
- Add an opt-in, on-device leaderboard across local prototype accounts.

Exit criteria:

- Existing progress metrics remain unchanged.
- Leaderboard never exposes email addresses and clearly states its
  on-device scope.

## Batch 5 — Privacy, migration and accessibility polish

Status: complete

- Expand Privacy & Data controls with provider disclosure, data export,
  history deletion and image-transfer consent.
- Validate the Room v4-to-v5 migration and recover interrupted queue work.
- Verify dark/light themes, content descriptions, minimum touch targets,
  empty/loading/error/offline states and large text.

Exit criteria:

- Upgrading preserves accounts, history, bookmarks, quiz attempts and XP.
- Account deletion removes the user's local records and pending attachments.

## Batch 6 — Final verification and assignment evidence

Status: complete

- Run unit, instrumented, lint, debug and release builds.
- Test on the Pixel 7 Android 15 AVD and a min-SDK emulator where practical.
- Produce a requirement traceability matrix, risk register, before/after
  screenshots and manual test log for the assignment report.

Exit criteria:

- All legacy and new tests pass.
- No lint errors remain.
- Every requirement is linked to a screen, implementation, test and evidence.

Final result:

- 70 unit tests passed.
- 30 instrumented tests passed on Pixel 7 Android 15 (API 35).
- Debug and minified unsigned release APKs assembled successfully.
- The release APK was reduced from 3,004,303 to 2,600,651 bytes (13.43%)
  by limiting packaged locales, removing stale strings/debug logs and
  flattening the quiz-option view hierarchy.
- Android Lint completed with 0 errors and 15 non-blocking version/overdraw
  warnings; no unused-resource or redundant-layout warnings remain.
- A real airplane-mode/restart/reconnect flow produced and saved a remote
  `openai/gpt-oss-120b` answer through WorkManager.
- Traceability, limitations, manual test log and screenshots are recorded in
  `ASSIGNMENT_VERIFICATION.md` and `docs/evidence/`.
