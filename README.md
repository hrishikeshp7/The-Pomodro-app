# Pomodoro Focus

A modern, minimal Pomodoro productivity app for Android built with Jetpack Compose and Material 3.

## Features

- **Focus Timer** — Large, central countdown timer with circular progress indicator
- **Automatic Cycles** — 25min focus / 5min break / 15min long break after 4 sessions
- **Task Management** — Create and select tasks before starting focus sessions
- **Session History** — Track all completed focus sessions
- **Analytics Dashboard** — Daily and weekly stats with visual bar chart
- **Quick Presets** — Classic (25/5), Deep Work (50/10), Quick Focus (15/3)
- **Streak Tracking** — Track consecutive days of focus
- **Customizable Durations** — Adjust all timer durations via sliders
- **Dark Mode** — Full light and dark theme support
- **Onboarding** — 3-screen introduction for new users

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Kotlin 2.0.21 |
| UI | Jetpack Compose (Material 3) |
| Database | Room 2.6.1 |
| Preferences | DataStore |
| Navigation | Navigation Compose 2.8.5 |
| Architecture | MVVM |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 35 (Android 15) |

## Building

```bash
./gradlew assembleDebug
```

## Project Structure

```
app/src/main/java/com/pomodoro/app/
├── data/
│   ├── db/          # Room database, DAOs
│   ├── model/       # Entity classes, presets
│   └── repository/  # Repository pattern
├── navigation/      # Jetpack Navigation setup
├── ui/
│   ├── components/  # Reusable UI components
│   ├── screens/     # Screen composables + ViewModels
│   └── theme/       # Material 3 theme, colors, typography
└── util/            # Preferences manager
```

## Design Principles

- Minimal cognitive load
- Generous whitespace
- Clear visual hierarchy
- One dominant action per screen
- Calm, focus-oriented color palette
- Accessibility compliant

## License

Apache License 2.0 — see [LICENSE](LICENSE)
