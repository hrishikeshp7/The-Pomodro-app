## 2024-05-24 - Audio Synthesis Bottlenecks
**Learning:** Instantiating `AudioAttributes` and `AudioFormat` builders repeatedly, combined with calculating constant values inside high-frequency audio buffer generation loops (like `sin` frequency multipliers), causes unnecessary garbage collection and CPU overhead which can lead to audio stuttering, especially when playing sequences of notes.
**Action:** Always pre-compute loop invariants outside of buffer generation loops and cache static configuration objects like `AudioAttributes` and `AudioFormat` as class-level properties when synthesizing audio.

## 2024-03-22 - Avoid Over-Querying DB via Flow Combinations
**Learning:** Using `combine` on multiple Room database flows to compute related metrics causes redundant SQL queries and multiple re-emissions for every DB change. `AnalyticsViewModel` was issuing 5 separate queries whenever a session updated.
**Action:** When computing multiple related statistics (e.g., today vs. week metrics), fetch the largest necessary dataset in a single Room query and derive the subsets in memory using `.map { }`.

## 2024-12-04 - Unnecessary Flow Collection in Background
**Learning:** Using `collectAsState()` in Jetpack Compose to collect flows keeps the collection active even when the app or screen drops below the `STARTED` lifecycle state (e.g., when the app goes into the background or another activity is opened). This can lead to wasted CPU cycles, unnecessary recompositions, and memory leaks.
**Action:** Use `collectAsStateWithLifecycle()` from `androidx.lifecycle.compose.collectAsStateWithLifecycle` instead of `collectAsState()`. This automatically pauses collection when the lifecycle state is below `STARTED`, ensuring UI collection only happens when the UI is actually visible or interactive. Remember that when migrating, the `initial` parameter becomes `initialValue`.
