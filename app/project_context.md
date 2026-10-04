# Deutsch Dictionary CVUT FS - Project Context

This file serves as a reference document detailing the project's structure and functionality. It is designed to provide immediate context for AI prompting and future modifications.

## About the Project
An Android application built with **Jetpack Compose**, designed for teaching and practicing German vocabulary, primarily targeted at students of ČVUT FS.

## Architecture and Structure
The app avoids overly complex standard architectures (like strict MVVM with Repositories/UseCases/Hilt). Instead, it relies on a pragmatic split between Compose UI screens and dedicated business logic managers (`Manager` and `Engine` classes).

### Tech Stack & Libraries
- **UI:** Jetpack Compose (Material 3)
- **Navigation:** Compose Navigation (`androidx.navigation.compose.NavHost`)
- **Serialization:** `kotlinx.serialization.json` (for reading assets and local storage persistence)
- **Language:** Kotlin

### Key Components & File Structure

#### UI & Navigation
- **`MainActivity.kt`**: Minimal entry point, sets up the Compose `AppNavigation`.
- **`ui/navigation/AppNavigation.kt`**: Defines the navigation graph, connects screens, and handles route parameters.
- **`ui/screens/`**: Organized logically by feature:
  - `dictionary/`: Screens for listing and detailing dictionary/lessons (`DictionaryListScreen`, `DictionaryDetailScreen`).
  - `mastery/`: Screens for displaying user progress (`MasteryScreen`, `MasteryDetailScreen`).
  - `practice/`: Screens for vocabulary practice setup, session, and results (`PracticeSetupScreen`, `PracticeSessionScreen`, `PracticeResultScreen`).
  - `MainMenuScreen.kt`: The main dashboard.
- **`ui/screens/practice/PracticeSessionViewModel.kt`**: The only prominent ViewModel. Manages the state of the active practice session (current question, score, answers) to decouple complex test logic from `PracticeSessionScreen`.
- **`ui/components/`**: Reusable UI parts (e.g., `SetupCheckboxRow.kt`).

#### Data Models
- **`models.kt` & `ui/models/PracticeModels.kt`**: Core data models (`Lesson`, `Category`, `Word`/`VocabItem`) and models tied purely to the practice execution state (`PracticeConfig`, `SessionResult`, etc.).

#### Business Logic & Data Management
- **`DictionaryManager.kt`**: Parses JSON files containing lessons from the `assets/` folder. Automatically discovers and loads files like `lesson1.json`.
- **`MasteryManager.kt`**: Handles the persistence of user progress (Mastery levels) directly to the device's internal storage (`mastery_data.json`). Manages `WordStatus` (NEW, IN_PROGRESS, MASTERED) and fallback legacy mappings.
- **`Domain.kt`**: Contains the core, framework-independent business logic:
  - **`AnswerValidator`**: Evaluates user answers against primary words and synonyms, intelligently ignoring German articles (der/die/das) when typing.
  - **`MasteryUpdater`**: Calculates changes to the "Mastery" score based on answer correctness, question format, and translation direction.
  - **`PracticeSessionEngine`**: Advanced spaced-repetition algorithm selecting the next word. It factors in current mastery and time elapsed (forgetting curve), and dynamically scales question difficulty (from Multiple Choice up to Written format based on mastery thresholds).

## Practice Session Flow
1. **Setup:** User defines the scope (lessons), question formats, and duration in `PracticeSetupScreen`.
2. **Initialization:** `PracticeSessionViewModel` initializes the `PracticeSessionEngine` with vocabulary from `DictionaryManager`.
3. **Execution:** The Engine selects optimal words and question formats. The UI updates dynamically based on the current question.
4. **Validation:** User answers are processed via `AnswerValidator` and `MasteryUpdater`. Progress is immediately persisted via `MasteryManager`.
5. **Results:** After the session finishes, `PracticeResultScreen` displays success rates and Mastery changes per lesson.

## Guidelines for Future Prompting
- **UI/Screens:** For new screens, create them in the appropriate `ui/screens/` subdirectory and register the route in `AppNavigation.kt`. Keep standard UI state inside Compose components using `remember`/`mutableStateOf`.
- **Practice Logic:** To adjust point systems, difficulty scaling, or question types, modify `Domain.kt` (specifically `MasteryUpdater` or `PracticeSessionEngine`). To change what happens in the UI mid-session, update `PracticeSessionViewModel`.
- **Dictionary Expansion:** No code changes are required to add new vocabulary. Simply add a properly formatted `lessonXX.json` to the `src/main/assets/` folder, and `DictionaryManager` will handle it automatically.
