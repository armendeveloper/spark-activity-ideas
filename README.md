# Spark ✨ — activity ideas for Android

Bored? Pick a few filters, tap **Spark me**, and get something fun to do right now.
Spark is a small offline Android app written in Kotlin with Jetpack Compose.

## Features

- **Discover**: optional filters for time (15 min / up to 1 h), budget (free / cheap), place (indoor / outdoor),
  people (alone / with others) and energy (chill / active). Each tap gives a random match.
  Ideas you did in the last 14 days come up about 5× less often, and you never get the same idea twice in a row.
- **160 built-in ideas** across Creative, Outdoors, Social, Learning, Food, Mindful, Games and Home.
- **Saved**: heart the ideas you like and add your own, with the same tags.
- **History**: everything you marked as done, grouped by day.
- **Daily idea**: an optional notification at the time you choose. Tapping it opens that idea.
- Fully offline. No account and no tracking.

## Tech

Kotlin 2.2 · Jetpack Compose + Material 3 (dynamic colour) · Navigation Compose · Room · DataStore ·
WorkManager · kotlinx.serialization · MVVM with manual DI. minSdk 26, targetSdk 36.

```
app/src/main/java/com/armen/spark/
  domain/   Idea model, Filters, IdeaPicker (weighted random pick, pure Kotlin)
  data/     Room entities/DAOs, IdeaRepository, SettingsRepository, SeedParser
  notify/   DailyIdeaScheduler, DailyIdeaWorker, Notifications
  ui/       discover, saved, edit, history, settings, nav, components, theme
app/src/main/assets/activities.json   the bundled ideas
```

## Build and run

You need JDK 17+ and the Android SDK (platform 36). Then:

```bash
./gradlew testDebugUnitTest   # 34 JVM unit tests
./gradlew installDebug        # install on a connected device or emulator
```

Or open the folder in Android Studio and press Run.

## Adding ideas

Add an entry to `app/src/main/assets/activities.json`:

```json
{"title": "Sketch your view", "description": "Draw what's in front of you for 15 minutes.", "emoji": "✏️",
 "category": "Creative", "duration": "QUICK", "cost": "FREE", "place": "ANYWHERE", "people": "SOLO", "energy": "CHILL"}
```

`SeedParserTest` checks that every entry parses, titles are unique, and each filter still matches at least 10 ideas.
The list is only imported on first launch, so to see new entries you need to clear the app's data.

## Design docs

The spec and implementation plan are in [`docs/superpowers`](docs/superpowers).
