## 2024-05-24 - Audio Synthesis Bottlenecks
**Learning:** Instantiating `AudioAttributes` and `AudioFormat` builders repeatedly, combined with calculating constant values inside high-frequency audio buffer generation loops (like `sin` frequency multipliers), causes unnecessary garbage collection and CPU overhead which can lead to audio stuttering, especially when playing sequences of notes.
**Action:** Always pre-compute loop invariants outside of buffer generation loops and cache static configuration objects like `AudioAttributes` and `AudioFormat` as class-level properties when synthesizing audio.

## 2024-03-22 - Avoid Over-Querying DB via Flow Combinations
**Learning:** Using `combine` on multiple Room database flows to compute related metrics causes redundant SQL queries and multiple re-emissions for every DB change. `AnalyticsViewModel` was issuing 5 separate queries whenever a session updated.
**Action:** When computing multiple related statistics (e.g., today vs. week metrics), fetch the largest necessary dataset in a single Room query and derive the subsets in memory using `.map { }`.

## 2026-03-27 - Jetpack Compose Flow Collection Performance
**Learning:** Using `collectAsState()` in Jetpack Compose UI continues to collect from flows even when the app is backgrounded and the UI is not visible, leading to unnecessary resource usage and potential memory leaks.
**Action:** Prefer using `collectAsStateWithLifecycle()` from `androidx.lifecycle.compose` (available transitively) which automatically stops flow collection when the lifecycle drops below STARTED, preventing unnecessary re-renders when the app is backgrounded.
