## 2024-05-24 - Audio Synthesis Bottlenecks
**Learning:** Instantiating `AudioAttributes` and `AudioFormat` builders repeatedly, combined with calculating constant values inside high-frequency audio buffer generation loops (like `sin` frequency multipliers), causes unnecessary garbage collection and CPU overhead which can lead to audio stuttering, especially when playing sequences of notes.
**Action:** Always pre-compute loop invariants outside of buffer generation loops and cache static configuration objects like `AudioAttributes` and `AudioFormat` as class-level properties when synthesizing audio.

## 2024-03-22 - Avoid Over-Querying DB via Flow Combinations
**Learning:** Using `combine` on multiple Room database flows to compute related metrics causes redundant SQL queries and multiple re-emissions for every DB change. `AnalyticsViewModel` was issuing 5 separate queries whenever a session updated.
**Action:** When computing multiple related statistics (e.g., today vs. week metrics), fetch the largest necessary dataset in a single Room query and derive the subsets in memory using `.map { }`.

## 2025-02-18 - Lifecycle-aware Flow Collection in Compose
**Learning:** Using `collectAsState()` for Kotlin Flows in Jetpack Compose UI components causes the flow collection to continue even when the application is in the background, consuming CPU, memory, and battery unnecessarily.
**Action:** Always prefer `collectAsStateWithLifecycle()` from `androidx.lifecycle.compose` over `collectAsState()` in Compose UI code, to ensure flow collection adheres to the UI's lifecycle (pausing when not STARTED). Note that `collectAsState(initial = ...)` becomes `collectAsStateWithLifecycle(initialValue = ...)`.
