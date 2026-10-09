# Spark — Activity Ideas App: Design

Date: 2026-10-09
Status: Approved in chat, pending written-spec review

## 1. Intent

A personal, just-for-fun Android app (Kotlin) that answers "what should I do right now?".
The user sets a few quick filters, taps once, and gets a random activity that fits their
time, budget, place, company and energy. Works fully offline.

**Success:** open app → at most two taps → a suggestion that fits the situation.

**Non-goals:** accounts, cloud sync, Play Store release, localization, monetization, online APIs.

## 2. Platform & stack

| Item | Choice |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3, single Activity, Navigation Compose |
| Architecture | MVVM: one `ViewModel` per screen exposing `StateFlow<UiState>` |
| Persistence | Room (activities, done log); Preferences DataStore (settings) |
| Background | WorkManager (daily notification) |
| DI | Manual: an `AppContainer` created in the `Application` class |
| Serialization | kotlinx.serialization (seed JSON) |
| SDK | minSdk 26, targetSdk 36, compileSdk 36 |
| Build | Gradle (Kotlin DSL) with version catalog, wrapper included |

Package: `com.armen.spark`. App label: **Spark**.

## 3. Domain model

```kotlin
enum class Duration { QUICK /* ≤15 min */, HOUR /* ~1 h */, LONG /* 2 h+ */ }
enum class Cost { FREE, CHEAP, PRICEY }
enum class Place { INDOOR, OUTDOOR, ANYWHERE }
enum class People { SOLO, GROUP, ANY }
enum class Energy { CHILL, ACTIVE }

data class Activity(
    val id: Long,
    val title: String,
    val description: String,
    val emoji: String,
    val category: String,      // e.g. "Creative", "Outdoors", "Social", "Learning", "Food", "Mindful", "Games"
    val duration: Duration,
    val cost: Cost,
    val place: Place,
    val people: People,
    val energy: Energy,
    val isCustom: Boolean,
    val isFavorite: Boolean,
)

data class DoneEntry(val id: Long, val activityId: Long, val doneAt: Long /* epoch millis */)
```

### Filters

```kotlin
data class Filters(
    val duration: Duration? = null,   // null = any
    val maxCost: Cost? = null,        // null = any; FREE matches FREE only, CHEAP matches FREE+CHEAP
    val place: Place? = null,         // INDOOR/OUTDOOR; activity ANYWHERE always matches
    val people: People? = null,       // SOLO/GROUP; activity ANY always matches
    val energy: Energy? = null,
)
```

Matching rules:
- `duration`: activity duration must be ≤ the selected one (a QUICK idea fits in an hour).
- `maxCost`: activity cost ordinal ≤ selected ordinal.
- `place`: activity place equals the selection, or the activity is `ANYWHERE`.
- `people`: activity people equals the selection, or the activity is `ANY`.
- `energy`: exact match.

## 4. Picking logic — `ActivityPicker`

A pure Kotlin class with no Android dependencies. It takes a `Random` and a clock so tests can control both.

```kotlin
class ActivityPicker(private val random: Random, private val now: () -> Long) {
    fun pick(
        candidates: List<Activity>,
        filters: Filters,
        recentlyDone: Map<Long, Long>,   // activityId -> last doneAt
        excludeId: Long?,                 // last shown id; avoid immediate repeat
    ): Activity?
}
```

1. Keep the candidates that match the filters.
2. Remove `excludeId`, unless it is the only match left.
3. Weight each remaining activity: `0.2` if it was done within the last 14 days, otherwise `1.0`.
4. Make a weighted random choice. Return `null` when nothing matches.

## 5. Data layer

- `SparkDatabase` (Room) with `ActivityEntity`, `DoneEntryEntity`, `ActivityDao`, `DoneDao`.
- `ActivityRepository` exposes:
  - `activities: Flow<List<Activity>>`, `favorites: Flow<List<Activity>>`
  - `history: Flow<List<HistoryItem>>` (DoneEntry joined with Activity)
  - `toggleFavorite(id)`, `markDone(id)`, `upsertCustom(activity)`, `deleteCustom(id)`
  - `lastDoneMap(): Map<Long, Long>`
  - `seedIfEmpty()`
- **Seeding:** `assets/activities.json` holds about 150 hand-written ideas. `SeedParser` decodes the
  JSON into domain objects. `seedIfEmpty()` inserts them on first launch, when the table is empty.
- **Deletion:** only custom activities can be deleted, and their done entries are removed with them (cascade).
- `SettingsRepository` (DataStore) stores:
  - `notificationsEnabled: Boolean` (default false)
  - `notificationTime: LocalTime` (default 09:00)
  - `lastFilters: Filters`

## 6. Screens

Bottom navigation: **Discover · Saved · History · Settings**. The add/edit form is a separate destination.

- **Discover:** a row of filter chips for each dimension; tapping the selected chip again clears it. A big
  **"Spark me"** button below the chips. The result card shows the emoji, title, category and description,
  plus chips for its tags. Card actions: ♥ Save (toggle), ✓ Done (logs it and shows a snackbar),
  ↻ Another (picks again with the current id excluded).
  - **Empty state:** "No ideas fit — loosen a filter" with a **Reset filters** button.
  - Filters are saved to DataStore and restored on the next launch.
- **Saved:** a list of favorites. Custom ideas show a "Mine" badge and an edit/delete menu.
  A FAB (+) opens **Edit idea**.
- **Edit idea:** fields for title (required), description, and an emoji (single field, default ✨), plus a
  category dropdown and a choice for each of the five tags. Save is disabled while the title is blank.
  New custom ideas are favorited automatically so they show up in Saved.
- **History:** the done log, newest first, grouped by date ("Today", "Yesterday", then dates).
  Empty state: "Nothing done yet — go spark something!"
- **Settings:** a toggle for "Daily idea" and a time picker, plus an About line.

## 7. Daily notification

- `DailyIdeaWorker` (a `CoroutineWorker`): picks an idea with empty filters and the done-history weighting,
  then posts a notification on the `daily_idea` channel. Tapping it opens Discover with that idea showing
  (via an intent extra `activityId`).
- `DailyIdeaScheduler`: schedules a unique `PeriodicWorkRequest` (24 h) whose initial delay lands on the
  chosen time. It reschedules (REPLACE) whenever the time changes, and cancels when the toggle is turned off.
- **Permission:** on Android 13+, turning the toggle on requests `POST_NOTIFICATIONS`. If the user denies it,
  the toggle goes back to off and a short explanation is shown.

## 8. Error handling

- **Seed parse failure:** logged. The app still runs with an empty list, and Discover shows its empty state.
- **No filter matches:** the empty state described above.
- **Worker:** if no activity exists, it returns `Result.success()` without posting.
- **Database:** all I/O runs on `Dispatchers.IO` through Room's suspend and Flow APIs. A single
  `SparkDatabase` instance is shared by the whole app.

## 9. Testing

JVM unit tests (JUnit 4):
- `ActivityPickerTest`
  - each filter rule, including the ANYWHERE/ANY wildcards and the cost/duration "≤" rules
  - excludes the last shown id, but returns it when it is the only match
  - recent activities are weighted down (statistical check with a seeded `Random`)
  - returns null when nothing matches
- `SeedParserTest`
  - parses a sample JSON
  - the bundled `activities.json` parses and has at least 150 entries with unique titles
- `DailyIdeaSchedulerTest`: initial-delay calculation for times before and after now.

Verification: `gradlew testDebugUnitTest assembleDebug` succeeds.

## 10. Project layout

```
app/src/main/java/com/armen/spark/
  SparkApp.kt, MainActivity.kt, AppContainer.kt
  domain/      Activity.kt, Filters.kt, ActivityPicker.kt
  data/        SparkDatabase.kt, entities, daos, ActivityRepository.kt, SettingsRepository.kt, SeedParser.kt
  notify/      DailyIdeaWorker.kt, DailyIdeaScheduler.kt, Notifications.kt
  ui/          theme/, nav/SparkNavHost.kt,
               discover/, saved/, edit/, history/, settings/  (Screen + ViewModel each)
app/src/main/assets/activities.json
app/src/test/java/com/armen/spark/...
```
