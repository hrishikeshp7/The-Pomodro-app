# Pomodoro Focus — Product Specification

## 1. Competitive Research Summary

### Existing Apps Analyzed
- **Forest** — Gamified focus timer with tree-growing mechanic. Strong engagement but heavy gamification.
- **Focus To-Do** — Combines Pomodoro with task management. Feature-rich but cluttered UI.
- **Tide** — Minimalist focus timer with ambient sounds. Beautiful but limited analytics.
- **Be Focused** — Simple Pomodoro timer. Clean but lacks modern design language.

### Key Insights
- **Most apps over-gamify focus**, creating new distractions rather than reducing them.
- **Analytics are often an afterthought** — basic session counts without meaningful patterns.
- **Onboarding is commonly skipped**, leaving new users confused about the technique.
- **Task integration is weak** — most apps treat tasks and timers as separate features.
- **Break transitions are abrupt** — jarring notifications break the calm focus state.

### Our Differentiators
- Calm, distraction-free design as the primary feature
- Thoughtful session transitions (gentle, not jarring)
- Integrated task-timer workflow (select task → start session → track automatically)
- Meaningful analytics that show patterns, not just numbers
- Quick presets for different work styles (Classic 25/5, Deep Work 50/10, Quick Focus 15/3)

---

## 2. Feature Prioritization

### Must Have (P0)
- [x] Large, central countdown timer (primary focus element)
- [x] Start / Pause / Reset controls
- [x] Automatic Pomodoro cycle (25min focus / 5min break / 15min long break after 4 sessions)
- [x] Customizable timer durations
- [x] Task creation and selection before starting a session
- [x] Session tracking and history
- [x] Daily and weekly analytics
- [x] Light and dark mode
- [x] Bottom navigation (Timer, History, Analytics, Settings)

### Should Have (P1)
- [x] Quick preset modes (Classic 25/5, Deep Work 50/10, Quick Focus 15/3)
- [x] Streak tracking (consecutive days of focus)
- [x] Session completion dialog with gentle transition
- [x] Minimal onboarding (3 screens)
- [x] Session progress indicators (dots showing completed sessions in cycle)

### Optional (P2)
- [ ] Session reflection notes
- [ ] Sound/haptic feedback on session complete
- [ ] Focus mode (DND integration)
- [ ] Ambient sounds during focus
- [ ] Widget for home screen
- [ ] Export session data

---

## 3. UX Flow

### First-Time User Flow
```
App Launch → Onboarding (3 screens) → Main Timer Screen
```

### Daily User Flow
```
App Launch → Timer Screen → Select Task (optional) → Start Timer → Focus
→ Session Complete Dialog → Break Timer → Repeat Cycle
```

### Navigation Flow
```
Bottom Navigation:
├── Timer (default, primary screen)
│   └── Task Selection (push screen)
├── History (session list)
├── Analytics (dashboard)
└── Settings (preferences)
```

### Timer State Machine
```
IDLE → [Start] → RUNNING → [Pause] → PAUSED → [Start] → RUNNING
  ↑                  |                              |
  |              [Complete]                      [Reset]
  |                  ↓                              |
  |         SESSION_COMPLETE ──────────────────────→
  |                  |
  |              [Continue]
  |                  ↓
  └──── BREAK_RUNNING → [Complete] → IDLE (next cycle)
```

---

## 4. Screen-by-Screen UI Breakdown

### 4.1 Onboarding Screen (3 pages)
- **Layout**: Full-screen, centered content, bottom navigation button
- **Page 1**: Timer icon + "Focus Timer" — Explains Pomodoro technique
- **Page 2**: Fitness icon + "Build Habits" — Streaks and task management
- **Page 3**: Trending icon + "See Progress" — Analytics and growth
- **Components**: Icon circle (120dp), title, description, page dots, Next/Get Started button
- **Interaction**: Swipe or tap Next, animated content transition

### 4.2 Main Timer Screen (Primary)
- **Layout**: Vertical stack — top bar, session indicators, timer circle, task chip, controls
- **Top Bar**: App title (left), streak counter with fire icon (right)
- **Session Indicators**: 4 horizontal bars showing cycle progress (filled = completed)
- **Timer Display**: 280dp circular arc with large time text (64sp), phase label below
- **Task Chip**: AssistChip showing selected task, or "Select a task" text button
- **Controls**: 3-button row — Stop (tonal), Play/Pause (large FAB), Skip (tonal)
- **Session Complete**: AlertDialog with congratulations and session count

### 4.3 Task Management Screen
- **Layout**: Title, text input with add button, scrollable task list
- **Input**: OutlinedTextField with RoundedCornerShape(12dp) + FilledIconButton
- **Task Items**: Card with checkbox icon, title text, delete icon
- **Interaction**: Tap checkbox to toggle, tap card to select for timer, tap delete to remove
- **Animation**: Color transition on completion state

### 4.4 Session History Screen
- **Layout**: Title + scrollable session list
- **Empty State**: Centered message "No sessions yet. Start your first Pomodoro!"
- **Session Items**: Card with task title, date/time, duration badge
- **Duration Badge**: PrimaryContainer chip showing minutes

### 4.5 Analytics Dashboard
- **Layout**: Scrollable column — Today stats, Week stats, Weekly chart
- **Stat Cards**: 2-up grid with large number + label, PrimaryContainer background
- **Weekly Chart**: Custom Canvas bar chart with 7 days, rounded bars
- **Day Labels**: Single-letter day abbreviations below chart

### 4.6 Settings Screen
- **Layout**: Scrollable column — Presets, Timer Durations, Preferences, App info
- **Quick Presets**: Row of FilterChips (Classic, Deep Work, Quick Focus)
- **Duration Settings**: Card with label, value, Slider (Material 3)
- **Preferences**: Card with toggle rows (Dark Mode, Sound Effects)
- **Ranges**: Focus 5-90min, Short Break 1-30min, Long Break 5-60min, Sessions 2-8

---

## 5. Design System

### Color Palette
| Token | Light | Dark | Usage |
|-------|-------|------|-------|
| Primary | #2D6A4F (Sage) | #95D5B2 | Focus state, progress, CTAs |
| Secondary | #8B6914 (Amber) | #E8C46A | Streaks, accents |
| Tertiary | #AD5C51 (Rose) | #FFB4A8 | Break state, pause |
| Background | #F8FAF6 | #1A1C19 | App background |
| Surface | #F8FAF6 | #1A1C19 | Cards, dialogs |
| SurfaceVariant | #E0E4DC | #434840 | Tracks, disabled |

### Typography Scale
| Style | Weight | Size | Usage |
|-------|--------|------|-------|
| displayLarge | Light | 72sp | Timer digits |
| headlineLarge | SemiBold | 28sp | Screen titles |
| headlineMedium | Medium | 24sp | Stat values |
| titleLarge | Medium | 20sp | App title |
| titleMedium | Medium | 16sp | Section headers |
| bodyLarge | Normal | 16sp | Content text |
| bodyMedium | Normal | 14sp | Secondary text |
| labelLarge | Medium | 14sp | Button text |
| labelMedium | Medium | 12sp | Chips, tabs |

### Spacing System
- **4dp**: Minimal spacing (icon-text gaps)
- **8dp**: Tight spacing (within components)
- **12dp**: Component spacing
- **16dp**: Section padding
- **24dp**: Screen padding, major sections
- **32dp**: Large section gaps

### Component Library
- **TimerDisplay**: Custom Canvas circle + text overlay
- **TimerControls**: FAB + 2 tonal icon buttons
- **TaskItem**: Card with icon button + text + delete
- **StatCard**: Card with large number + label
- **WeeklyChart**: Custom Canvas bar chart
- **DurationSetting**: Card with slider
- **SettingToggle**: Row with switch

### Interaction States
| State | Visual |
|-------|--------|
| Default | Standard colors |
| Pressed | Ripple effect (Material 3 default) |
| Disabled | 38% opacity |
| Active/Selected | Primary color fill |
| Running | Animated arc progress |
| Break | Tertiary color palette |
| Paused | Secondary color accent |

### Micro-Interactions
- Timer arc: `animateFloatAsState` with 300ms tween
- Button color: `animateColorAsState` between primary/tertiary
- Task completion: `animateColorAsState` background transition
- Page transitions: `AnimatedContent` for onboarding
- Session indicators: Instant fill with color change

---

## 6. Engineering Notes

### Architecture
- **Pattern**: MVVM (Model-View-ViewModel)
- **UI**: Jetpack Compose with Material 3
- **State**: StateFlow + collectAsState
- **Navigation**: Jetpack Navigation Compose
- **Persistence**: Room (sessions, tasks) + DataStore (preferences)
- **Timer**: CountDownTimer in ViewModel

### Tech Stack
| Component | Technology | Version |
|-----------|-----------|---------|
| Language | Kotlin | 2.0.21 |
| UI Framework | Jetpack Compose | BOM 2024.12.01 |
| Design System | Material 3 | Via Compose BOM |
| Database | Room | 2.6.1 |
| Preferences | DataStore | 1.1.1 |
| Navigation | Navigation Compose | 2.8.5 |
| Lifecycle | ViewModel Compose | 2.8.7 |
| Build | AGP | 8.7.3 |
| Annotation Processing | KSP | 2.0.21-1.0.28 |
| Min SDK | 26 (Android 8.0) | |
| Target SDK | 35 (Android 15) | |

### Key Design Decisions
1. **AndroidViewModel** — Used for database/preferences access without DI framework
2. **No Hilt/Dagger** — Kept simple for initial version; manual DI via Application
3. **CountDownTimer** — Native Android timer; future: WorkManager for background
4. **Room + DataStore** — Room for structured data, DataStore for simple key-value prefs
5. **Shared TimerViewModel** — Passed through navigation to maintain timer state across screens
6. **Edge-to-Edge** — Modern Android look with transparent status/navigation bars

### Future Considerations
- Background timer service with notification
- WorkManager for session reminders
- Hilt dependency injection
- Compose animations library for richer transitions
- Widget support with Glance
- Data export/backup

---

## 7. QA Acceptance Criteria

### Timer Screen
- [ ] Timer displays 25:00 by default
- [ ] Tapping Start begins countdown and shows Pause button
- [ ] Timer counts down accurately (±1 second)
- [ ] Tapping Pause stops countdown and shows Start button
- [ ] Tapping Reset returns to initial duration
- [ ] Session indicators show correct number of completed sessions
- [ ] After focus session completes, session complete dialog appears
- [ ] After dismissing dialog, break timer is set (5min or 15min)
- [ ] After 4 focus sessions, long break (15min) is triggered
- [ ] Skip button advances to next phase
- [ ] Streak counter updates after completing a session

### Task Management
- [ ] Can add a new task with non-empty title
- [ ] Empty title does not create a task
- [ ] Tasks appear in reverse chronological order
- [ ] Can toggle task completion (checkbox + strikethrough)
- [ ] Can delete a task
- [ ] Selecting a task returns to timer with task displayed

### History
- [ ] Completed focus sessions appear in history
- [ ] Sessions show task title, date/time, and duration
- [ ] Empty state shows appropriate message
- [ ] Sessions are ordered newest first

### Analytics
- [ ] Today's session count and minutes are accurate
- [ ] Weekly session count and minutes are accurate
- [ ] Bar chart shows correct daily distribution
- [ ] Values update after completing a session

### Settings
- [ ] Quick presets update all duration values
- [ ] Sliders change duration values within valid ranges
- [ ] Dark mode toggle switches theme immediately
- [ ] Sound toggle persists across app restarts
- [ ] All settings persist across app restarts

### Onboarding
- [ ] Shows on first launch
- [ ] Does not show on subsequent launches
- [ ] Can navigate through 3 pages
- [ ] "Get Started" on last page navigates to Timer
- [ ] Page indicators update correctly

### General
- [ ] Bottom navigation works between all 4 tabs
- [ ] Back navigation behaves correctly
- [ ] Dark mode applies to all screens
- [ ] No visual glitches or text overflow
- [ ] Smooth animations without jank
- [ ] App handles configuration changes (rotation)
