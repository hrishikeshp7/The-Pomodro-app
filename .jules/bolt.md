## 2024-05-24 - Audio Synthesis Bottlenecks
**Learning:** Instantiating `AudioAttributes` and `AudioFormat` builders repeatedly, combined with calculating constant values inside high-frequency audio buffer generation loops (like `sin` frequency multipliers), causes unnecessary garbage collection and CPU overhead which can lead to audio stuttering, especially when playing sequences of notes.
**Action:** Always pre-compute loop invariants outside of buffer generation loops and cache static configuration objects like `AudioAttributes` and `AudioFormat` as class-level properties when synthesizing audio.

## 2024-03-22 - Avoid Over-Querying DB via Flow Combinations
**Learning:** Using `combine` on multiple Room database flows to compute related metrics causes redundant SQL queries and multiple re-emissions for every DB change. `AnalyticsViewModel` was issuing 5 separate queries whenever a session updated.
**Action:** When computing multiple related statistics (e.g., today vs. week metrics), fetch the largest necessary dataset in a single Room query and derive the subsets in memory using `.map { }`.

## 2024-03-20 - Compose State Collection in Android
**Learning:** In Android, collecting flows in Compose using `collectAsState()` does not respect the app lifecycle. This can cause the flow to continue emitting and wasting resources when the app is in the background. Jetpack Compose lint explicitly recommends against using `collectAsState()` for performance.
**Action:** Always use `collectAsStateWithLifecycle()` from `androidx.lifecycle:lifecycle-runtime-compose` when collecting StateFlows in Jetpack Compose to ensure UI collection stops when the lifecycle drops below `STARTED`.
