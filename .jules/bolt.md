## 2024-05-24 - Audio Synthesis Bottlenecks
**Learning:** Instantiating `AudioAttributes` and `AudioFormat` builders repeatedly, combined with calculating constant values inside high-frequency audio buffer generation loops (like `sin` frequency multipliers), causes unnecessary garbage collection and CPU overhead which can lead to audio stuttering, especially when playing sequences of notes.
**Action:** Always pre-compute loop invariants outside of buffer generation loops and cache static configuration objects like `AudioAttributes` and `AudioFormat` as class-level properties when synthesizing audio.

## 2024-03-22 - Avoid Over-Querying DB via Flow Combinations
**Learning:** Using `combine` on multiple Room database flows to compute related metrics causes redundant SQL queries and multiple re-emissions for every DB change. `AnalyticsViewModel` was issuing 5 separate queries whenever a session updated.
**Action:** When computing multiple related statistics (e.g., today vs. week metrics), fetch the largest necessary dataset in a single Room query and derive the subsets in memory using `.map { }`.

## 2024-12-23 - Lifecycle-Aware State Flow Collection in Compose
**Learning:** Collecting flows using `collectAsState()` in Jetpack Compose keeps active subscriptions to underlying resources (like DataStore or Room DB) even when the UI screen is not visible or the app is sent to the background. This results in unnecessary CPU wakeups, battery drain, and memory usage.
**Action:** Always collect flows in Jetpack Compose using `collectAsStateWithLifecycle()` from `androidx.lifecycle:lifecycle-runtime-compose` so that collection automatically stops when the lifecycle falls below `Lifecycle.State.STARTED`.
