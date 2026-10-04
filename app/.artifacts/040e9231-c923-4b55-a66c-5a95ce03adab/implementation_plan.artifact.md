# Goal Description

Implement a comprehensive update to the Deutsch Dictionary app, focusing on UI/UX fixes (fixing race conditions, improving input states), introducing a robust masked input component for "Skeleton Words", adding a Quick Start flow, implementing gamification via a daily quota and Heatmap calendar, adding a persistent foreground service for progress tracking, and updating the core engine logic to enforce a strict 30-word WIP limit with "Spaced Fillers".

## User Review Required

- **Foreground Service Permissions:** Implementing a foreground service requires the `FOREGROUND_SERVICE` and `POST_NOTIFICATIONS` permissions in the AndroidManifest. The user will be prompted for notification permissions on Android 13+.
- **Masked Input Interaction:** The Skeleton format will now be typed directly into the masked string (e.g., `d _ r   A _ f _ e _ l`). Protected characters (spaces, hyphens) will be automatically skipped by the cursor.
- **WIP Limit Location:** Currently, the "Drip-Feeding" (WIP limit) logic resides inside `PracticeSessionViewModel`. I will move/consolidate this logic so the Engine and ViewModel strictly enforce the 30-word limit and 8-word spaced filler threshold.

## Proposed Changes

---

### UI State Fixes & 3-State Input

- **Fix Incorrect (X) Icon Flash:** Update `PracticeSessionViewModel` so that the `AnswerState` and `writtenAnswer` are NOT reset at the beginning of `generateNextQuestion()`. Instead, they will be reset atomically at the exact moment the new `_currentQuestion` is emitted, preventing the UI from rapidly recomposing the old word with an empty state.
- **3-State Input Logic:**
    - Modify the Article selection buttons in `PracticeSessionScreen` to act as toggles (clicking an already selected article sets it to `null`).
    - Update `PracticeSessionViewModel.checkAnswer()`: If the word is evaluated and does NOT have an article in the DB (regex doesn't match), but the user *did* select an article in the UI, it will be marked as an Incorrect answer and penalized.

### Skeleton Words (Masked Input)

- **Remove 3-State Buttons:** Disable/hide the Der/Die/Das buttons when the format is `SKELETON`.
- **Masked TextField Component:** Create a new custom composable (e.g., `MaskedSkeletonTextField`) in `PracticeSessionScreen` (or a dedicated component file).
    - It will generate a skeleton string *including* the article.
    - It will intercept `TextFieldValue` changes to enforce typing only into the `_` slots, automatically advancing the cursor and skipping over spaces (` `) and hyphens (`-`).
    - Backspace will restore the `_` and move the cursor back appropriately.

### Quick Start Flow & Custom Practice Updates

- **Main Menu Updates:** Add a large "Quick Start" button in `MainMenuScreen`. When clicked, it will generate a `PracticeConfig` selecting all lessons (except those containing "Test"), enable all formats, set endless mode (`wordCount = -1`), and navigate to `PracticeSessionScreen`.
- **Custom Practice Setup:** Rename/repurpose `PracticeSetupScreen` to "Custom Practice". Ensure settings are saved and applied correctly.

### Gamification & Foreground Service

- **Mastery Data Persistence:** Update `MasteryData` in `models/MasteryManager` to track `dailyPointsGained` using an epoch day key.
- **Daily Progress & Heatmap:**
    - Add a progress bar for the daily quota (default 300 pts) in `MainMenuScreen` and `PracticeSessionScreen`.
    - Create a new `HeatmapCalendarScreen` showing an infinitely scrollable GitHub-style heatmap, coloring days where the user hit 100% of their quota.
- **Foreground Service:**
    - Create `DailyProgressService` (Foreground Service) that displays a persistent notification with a progress bar.
    - Bind the service to update dynamically as the user gains mastery points.
    - Add a PendingIntent to launch the app directly into the "Quick Start" mode.

### Engine Logic (Spaced Fillers & WIP limit)

- **WIP Limit:** Update the logic (currently in `PracticeSessionViewModel.generateNextQuestion`) to strictly maintain a maximum of 30 active words (`NEW` + `IN_PROGRESS`). If active < 30, pull `LOCKED` words from selected lessons.
- **Spaced Fillers:** If the active pool of unfinished words (`NEW` + `IN_PROGRESS` + `MASTERED < 95`) drops below 8, the engine will query words with 100% Mastery, sort them by oldest `lastTestedAtEpochMilli`, and inject them into the active pool with a massive weight boost to force them into the roulette.

## Verification Plan

### Automated Tests
- N/A - Manual verification required.

### Manual Verification
1. Play a practice session and verify there is no "X" flash between correct answers.
2. Verify article buttons toggle on/off. Verify penalization for selecting an article on an article-less word.
3. Test Skeleton format: type directly into the mask, verify cursor skips spaces, and backspace restores `_`.
4. Click "Quick Start" on the main menu and ensure a session starts with all lessons.
5. Check the Heatmap screen to ensure today is lit up (if 300 points are gained).
6. Verify the persistent notification appears and updates when points are gained.