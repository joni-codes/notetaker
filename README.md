# Notetaker
Focus on the conversation, not the transcript.

## Philosophy
Notetaker is built on the premise that capturing details shouldn't come at the cost of being present in a discussion. By utilizing on-device generative AI, the app handles the cognitive load of transcription and summarization so you can focus on the actual interaction. 

Security and privacy are central to the design. All processing happens locally on the device via Gemini Nano and Android's SpeechRecognizer. There are zero cloud dependencies, meaning your conversations never leave your hardware.

## Features
- **Live Transcription**: Real-time conversion of speech to text using continuous Android SpeechRecognizer sessions.
- **Paste Transcript**: Dedicated screen for typing or pasting pre-existing transcripts without recording.
- **AI Summarization**: Local generation of concise titles and bulleted summaries using ML Kit GenAI (Gemini Nano).
- **AI-Generated Titles**: Separate 1-point summarization call to produce an ultra-concise title, with heuristic fallback.
- **Contextual Inference**: Automatic identification of participants and discussion themes within the generated summary.
- **Editable Metadata**: Post-recording screen for refining auto-generated titles and adding/removing participants.
- **Comprehensive History**: Persistent storage of all sessions with full-text search across titles, summaries, and transcripts.
- **Flexible Organization**: Sort by date or filter records by specific participants.
- **Session Management**: Swipe-to-delete functionality for history management and a dedicated mic permission flow.
- **Three-Tab Navigation**: Bottom navigation bar with Paste, Record (center), and History tabs.
- **Hardware Verification**: Automatic detection of Google AICore support with a dedicated fallback for unsupported devices.

## Architecture
The project follows **Clean Architecture** principles and the **MVVM** (Model-View-ViewModel) pattern to ensure separation of concerns and testability.

- **Domain Layer**: Contains the core business logic, including `NoteSummary` models, repository interfaces, and specific use cases for recording, summarization, and data persistence.
- **Data Layer**: Implements repository interfaces. It manages data flow between the Room local database (via `SummaryDao`), ML Kit's summarization engine, and the Android SpeechRecognizer.
- **Presentation Layer**: Built with **Jetpack Compose**. ViewModels follow a **State + Effect** pattern, where the state represents the UI data and effects handle one-time events like navigation or permission requests.
- **Dependency Injection**: Managed by **Hilt** to provide scoped instances of repositories, use cases, and database components.

## Tech Stack
- **UI**: Jetpack Compose with Material 3 design system.
- **Logic**: Kotlin Coroutines and Flow for asynchronous operations.
- **DI**: Hilt (Dagger).
- **Database**: Room for local persistence.
- **AI**: ML Kit GenAI Summarization (Gemini Nano) via Google AICore.
- **Speech**: Android SpeechRecognizer.
- **Build**: Gradle Kotlin DSL with Version Catalogs (KSP enabled).

## Project Structure
```text
com.jonicodes.notetaker/
├── data/
│   ├── local/          # Room database and DAO definitions
│   ├── mapper/         # Conversion logic between Data and Domain models
│   ├── repository/     # Concrete implementations of domain repositories
│   └── source/         # Data source abstractions (Speech, AI)
├── di/                 # Hilt modules for database and repository injection
├── domain/
│   ├── model/          # Pure business logic data classes (NoteSummary)
│   ├── repository/     # Repository interfaces
│   └── usecase/        # Individual business logic units
└── presentation/
    ├── components/     # Reusable UI widgets
    ├── history/        # History list and filtering UI
    ├── navigation/     # Bottom bar and screen routing logic
    ├── paste/          # Manual transcript paste/type input UI
    ├── recording/      # Live recording and transcription UI
    ├── summary/        # Post-recording and detail summary screens
    ├── theme/          # Material 3 color schemes and typography
    └── unsupported/    # Fallback UI for devices without AICore
```

## Device Requirements
- **OS**: Android 14+ (API 34).
- **Hardware**: Device must support **Google AICore** (e.g., Pixel 8 series or newer, Samsung S24 series or newer).

## How to Build
1. Clone the repository.
2. Open the project in the latest version of **Android Studio**.
3. Ensure you have the **Android 14 (API 34)** or higher SDK installed.
4. Sync the project with Gradle files.
5. Build and run the `app` module on a compatible physical device (Gemini Nano features are generally not available on emulators).
