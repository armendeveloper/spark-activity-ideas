# Spark Activity Ideas App Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build "Spark", an offline Android app (Kotlin, Jetpack Compose) that suggests a random activity matching quick filters, with favorites, a done log, custom ideas and an optional daily notification.

**Architecture:** A single-Activity Compose app with four bottom-nav tabs and one ViewModel per screen exposing `StateFlow`. Room stores the ideas and the done log, and DataStore stores settings. A pure-Kotlin `IdeaPicker` holds the selection logic. WorkManager posts the daily notification. Dependencies are wired by hand through an `AppContainer` owned by the `Application`.

**Tech Stack:** Kotlin 2.2.21, AGP 8.13.0, Gradle 8.14.3, Compose BOM 2025.10.01 (Material 3 1.4.0), Navigation Compose 2.9.5, Room 2.8.3 + KSP 2.2.21-2.0.4, WorkManager 2.10.5, DataStore 1.1.7, kotlinx.serialization 1.9.0, JUnit 4.13.2, kotlinx-coroutines-test 1.10.2.

**Spec:** `docs/superpowers/specs/2026-10-09-spark-activity-ideas-design.md`

**Naming note:** the spec's `Activity` / `ActivityPicker` / `ActivityRepository` are named `Idea` / `IdeaPicker` / `IdeaRepository` in code, so they don't collide with `android.app.Activity`.

## Global Constraints

- Package and applicationId `com.armen.spark`; app label `Spark`.
- minSdk 26, targetSdk 36, compileSdk 36; Java/Kotlin JVM target 17 (built with the installed JDK 21).
- No network access, no accounts, English only.
- The Android SDK is at `C:\Users\armen\AppData\Local\Android\Sdk` (put it in `local.properties`, which is gitignored).
- At least 150 seed ideas, using exactly these categories: Creative, Outdoors, Social, Learning, Food, Mindful, Games, Home.
- Recently done = within 14 days → weight 0.2; otherwise 1.0.
- The default daily-notification time is 09:00, and it is off by default.
- All commands run from the project root; on Windows use `./gradlew` in Git Bash (or `.\gradlew.bat` in PowerShell).

## Review Focus

1. A whitespace-only title for a custom idea → Save stays disabled; title, description and emoji are trimmed on save (Task 6, `IdeaDraftTest`).
2. A double-tap on Save in the edit screen → exactly one idea is stored, not two copies (Task 6, `EditIdeaViewModelTest`).
3. Deleting a custom idea that is showing on the Discover card → the card clears instead of showing stale data (Task 5, `DiscoverViewModelTest`).
4. A daily-notification time that has already passed today (or is exactly now) → the first run is tomorrow, and the delay stays correct across a DST jump (Task 8, `DailyIdeaSchedulerTest`).
5. History entries just either side of midnight → the "Today"/"Yesterday" labels follow the local time zone, not UTC (Task 7, `HistoryGroupingTest`).

Also covered: one malformed entry in the seed JSON is skipped and the rest still load (Task 3, `SeedParserTest`).

---

### Task 1: Project scaffold, Gradle wrapper, theme, launcher

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, `local.properties`, `.gitignore`
- Create: `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/res/values/themes.xml`, `app/src/main/res/values-night/themes.xml`, `app/src/main/res/values/colors.xml`
- Create: `app/src/main/res/drawable/ic_launcher_foreground.xml`, `app/src/main/res/drawable/ic_notification.xml`
- Create: `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`, `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`
- Create: `app/src/main/java/com/armen/spark/ui/theme/Theme.kt`
- Create: `app/src/main/java/com/armen/spark/MainActivity.kt` (placeholder content; Task 5 replaces it)
- Generate: `gradlew`, `gradlew.bat`, `gradle/wrapper/*`

**Interfaces:**
- Produces: `@Composable fun SparkTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit)`; drawable `R.drawable.ic_notification`; version-catalog aliases used by later tasks.

- [ ] **Step 1: Write `settings.gradle.kts`**

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Spark"
include(":app")
```

- [ ] **Step 2: Write `gradle/libs.versions.toml`**

```toml
[versions]
agp = "8.13.0"
kotlin = "2.2.21"
ksp = "2.2.21-2.0.4"
composeBom = "2025.10.01"
coreKtx = "1.17.0"
activityCompose = "1.11.0"
lifecycle = "2.9.4"
navigation = "2.9.5"
room = "2.8.3"
work = "2.10.5"
datastore = "1.1.7"
serialization = "1.9.0"
coroutines = "1.10.2"
junit = "4.13.2"

[libraries]
androidx-core-ktx = { module = "androidx.core:core-ktx", version.ref = "coreKtx" }
androidx-activity-compose = { module = "androidx.activity:activity-compose", version.ref = "activityCompose" }
androidx-lifecycle-runtime-compose = { module = "androidx.lifecycle:lifecycle-runtime-compose", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-compose = { module = "androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-navigation-compose = { module = "androidx.navigation:navigation-compose", version.ref = "navigation" }
compose-bom = { module = "androidx.compose:compose-bom", version.ref = "composeBom" }
compose-ui = { module = "androidx.compose.ui:ui" }
compose-ui-tooling = { module = "androidx.compose.ui:ui-tooling" }
compose-ui-tooling-preview = { module = "androidx.compose.ui:ui-tooling-preview" }
compose-material3 = { module = "androidx.compose.material3:material3" }
compose-material-icons-core = { module = "androidx.compose.material:material-icons-core" }
room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
room-ktx = { module = "androidx.room:room-ktx", version.ref = "room" }
room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }
work-runtime-ktx = { module = "androidx.work:work-runtime-ktx", version.ref = "work" }
datastore-preferences = { module = "androidx.datastore:datastore-preferences", version.ref = "datastore" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "serialization" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines" }
junit = { module = "junit:junit", version.ref = "junit" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

- [ ] **Step 3: Write root `build.gradle.kts`, `gradle.properties`, `local.properties`, `.gitignore`**

`build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
}
```

`gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
android.nonTransitiveRClass=true
kotlin.code.style=official
```

`local.properties`:
```properties
sdk.dir=C\:\\Users\\armen\\AppData\\Local\\Android\\Sdk
```

`.gitignore`:
```
.gradle/
.kotlin/
build/
local.properties
.idea/
*.iml
captures/
```

- [ ] **Step 4: Write `app/build.gradle.kts`**

```kotlin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.armen.spark"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.armen.spark"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

ksp {
    arg("room.generateKotlin", "true")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.core)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.work.runtime.ktx)
    implementation(libs.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
```

- [ ] **Step 5: Write the manifest and resources**

`app/src/main/AndroidManifest.xml` (the `.SparkApp` application class is added in Task 5; leave `android:name` out until then):
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="Spark"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.Spark">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:launchMode="singleTop"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

`res/values/colors.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="spark_primary">#B4451F</color>
</resources>
```

`res/values/themes.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.Spark" parent="android:Theme.Material.Light.NoActionBar" />
</resources>
```

`res/values-night/themes.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.Spark" parent="android:Theme.Material.NoActionBar" />
</resources>
```

`res/drawable/ic_launcher_foreground.xml`:
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
    <path android:fillColor="#FFFFFF"
        android:pathData="M54,28 C56,44 64,52 80,54 C64,56 56,64 54,80 C52,64 44,56 28,54 C44,52 52,44 54,28 Z" />
    <path android:fillColor="#FFE0B2"
        android:pathData="M74,30 C74.6,34 76,35.4 80,36 C76,36.6 74.6,38 74,42 C73.4,38 72,36.6 68,36 C72,35.4 73.4,34 74,30 Z" />
</vector>
```

`res/drawable/ic_notification.xml`:
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FFFFFF"
        android:pathData="M12,2 C12.6,8 16,11.4 22,12 C16,12.6 12.6,16 12,22 C11.4,16 8,12.6 2,12 C8,11.4 11.4,8 12,2 Z" />
</vector>
```

`res/mipmap-anydpi-v26/ic_launcher.xml` and `ic_launcher_round.xml` (identical):
```xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/spark_primary" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
```

- [ ] **Step 6: Write `ui/theme/Theme.kt`**

```kotlin
package com.armen.spark.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Color(0xFFB4451F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBCF),
    onPrimaryContainer = Color(0xFF3A0B00),
    secondary = Color(0xFF77574C),
    tertiary = Color(0xFF6B5E2F),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB59C),
    onPrimary = Color(0xFF5E1700),
    primaryContainer = Color(0xFF8F2F0A),
    onPrimaryContainer = Color(0xFFFFDBCF),
    secondary = Color(0xFFE7BDB0),
    tertiary = Color(0xFFD8C68D),
)

@Composable
fun SparkTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val context = LocalContext.current
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, content = content)
}
```

- [ ] **Step 7: Write placeholder `MainActivity.kt`**

```kotlin
package com.armen.spark

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import com.armen.spark.ui.theme.SparkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { SparkTheme { Text("Spark") } }
    }
}
```

- [ ] **Step 8: Generate the Gradle wrapper**

Download `https://services.gradle.org/distributions/gradle-8.14.3-bin.zip` into the scratchpad, unzip it, then run from the project root:
`<scratchpad>/gradle-8.14.3/bin/gradle wrapper --gradle-version 8.14.3 --distribution-type bin`
Expected: `BUILD SUCCESSFUL`; `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar` and `gradle-wrapper.properties` now exist.

- [ ] **Step 9: Build**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`, and `app/build/outputs/apk/debug/app-debug.apk` exists.

- [ ] **Step 10: Commit**

```bash
git add -A
git commit -m "chore: scaffold Spark Android project"
```

---

### Task 2: Domain model, filters and `IdeaPicker`

**Files:**
- Create: `app/src/main/java/com/armen/spark/domain/Idea.kt`
- Create: `app/src/main/java/com/armen/spark/domain/Filters.kt`
- Create: `app/src/main/java/com/armen/spark/domain/IdeaPicker.kt`
- Test: `app/src/test/java/com/armen/spark/TestIdeas.kt`, `app/src/test/java/com/armen/spark/domain/IdeaPickerTest.kt`

**Interfaces:**
- Produces:
  - `enum class Duration/Cost/Place/People/Energy` (each has `val label: String`)
  - `val CATEGORIES: List<String>`
  - `data class Idea(...)`, `data class HistoryItem(entryId: Long, idea: Idea, doneAt: Long)`
  - `data class Filters(...)` with `fun matches(idea: Idea): Boolean`
  - `class IdeaPicker(random: Random = Random.Default, now: () -> Long = System::currentTimeMillis)` with `fun pick(candidates: List<Idea>, filters: Filters, lastDone: Map<Long, Long>, excludeId: Long? = null): Idea?`
  - test helper `fun testIdea(id: Long, ...)`

- [ ] **Step 1: Write the domain types**

`domain/Idea.kt`:
```kotlin
package com.armen.spark.domain

enum class Duration(val label: String) { QUICK("15 min"), HOUR("About 1 h"), LONG("2 h+") }
enum class Cost(val label: String) { FREE("Free"), CHEAP("Cheap"), PRICEY("Pricey") }
enum class Place(val label: String) { INDOOR("Indoor"), OUTDOOR("Outdoor"), ANYWHERE("Anywhere") }
enum class People(val label: String) { SOLO("Alone"), GROUP("With others"), ANY("Alone or together") }
enum class Energy(val label: String) { CHILL("Chill"), ACTIVE("Active") }

val CATEGORIES = listOf("Creative", "Outdoors", "Social", "Learning", "Food", "Mindful", "Games", "Home")

data class Idea(
    val id: Long = 0,
    val title: String,
    val description: String,
    val emoji: String,
    val category: String,
    val duration: Duration,
    val cost: Cost,
    val place: Place,
    val people: People,
    val energy: Energy,
    val isCustom: Boolean = false,
    val isFavorite: Boolean = false,
)

data class HistoryItem(val entryId: Long, val idea: Idea, val doneAt: Long)
```

`domain/Filters.kt`:
```kotlin
package com.armen.spark.domain

/** A null field means "any". Durations and costs match anything up to the selected value. */
data class Filters(
    val duration: Duration? = null,
    val maxCost: Cost? = null,
    val place: Place? = null,
    val people: People? = null,
    val energy: Energy? = null,
) {
    fun matches(idea: Idea): Boolean =
        (duration == null || idea.duration <= duration) &&
            (maxCost == null || idea.cost <= maxCost) &&
            (place == null || idea.place == place || idea.place == Place.ANYWHERE) &&
            (people == null || idea.people == people || idea.people == People.ANY) &&
            (energy == null || idea.energy == energy)
}
```

- [ ] **Step 2: Write the test helper and the failing picker tests**

`test/.../TestIdeas.kt`:
```kotlin
package com.armen.spark

import com.armen.spark.domain.Cost
import com.armen.spark.domain.Duration
import com.armen.spark.domain.Energy
import com.armen.spark.domain.Idea
import com.armen.spark.domain.People
import com.armen.spark.domain.Place

fun testIdea(
    id: Long,
    title: String = "Idea $id",
    duration: Duration = Duration.QUICK,
    cost: Cost = Cost.FREE,
    place: Place = Place.ANYWHERE,
    people: People = People.ANY,
    energy: Energy = Energy.CHILL,
    isCustom: Boolean = false,
    isFavorite: Boolean = false,
) = Idea(
    id = id, title = title, description = "Something to do", emoji = "🎯", category = "Games",
    duration = duration, cost = cost, place = place, people = people, energy = energy,
    isCustom = isCustom, isFavorite = isFavorite,
)
```

`test/.../domain/IdeaPickerTest.kt`:
```kotlin
package com.armen.spark.domain

import com.armen.spark.testIdea
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class IdeaPickerTest {
    private val now = 1_800_000_000_000L
    private val day = 24L * 60 * 60 * 1000
    private val picker = IdeaPicker(Random(42)) { now }

    private fun pickedIds(ideas: List<Idea>, filters: Filters, excludeId: Long? = null): Set<Long> =
        (1..300).mapNotNull { picker.pick(ideas, filters, emptyMap(), excludeId)?.id }.toSet()

    @Test
    fun returnsNullWhenNothingMatches() {
        val ideas = listOf(testIdea(1, energy = Energy.CHILL))
        assertNull(picker.pick(ideas, Filters(energy = Energy.ACTIVE), emptyMap()))
    }

    @Test
    fun durationFilterIncludesShorterIdeas() {
        val ideas = listOf(
            testIdea(1, duration = Duration.QUICK),
            testIdea(2, duration = Duration.HOUR),
            testIdea(3, duration = Duration.LONG),
        )
        assertEquals(setOf(1L, 2L), pickedIds(ideas, Filters(duration = Duration.HOUR)))
        assertEquals(setOf(1L), pickedIds(ideas, Filters(duration = Duration.QUICK)))
    }

    @Test
    fun costFilterIncludesCheaperIdeas() {
        val ideas = listOf(
            testIdea(1, cost = Cost.FREE),
            testIdea(2, cost = Cost.CHEAP),
            testIdea(3, cost = Cost.PRICEY),
        )
        assertEquals(setOf(1L), pickedIds(ideas, Filters(maxCost = Cost.FREE)))
        assertEquals(setOf(1L, 2L), pickedIds(ideas, Filters(maxCost = Cost.CHEAP)))
    }

    @Test
    fun anywhereIdeasMatchEitherPlace() {
        val ideas = listOf(
            testIdea(1, place = Place.INDOOR),
            testIdea(2, place = Place.OUTDOOR),
            testIdea(3, place = Place.ANYWHERE),
        )
        assertEquals(setOf(1L, 3L), pickedIds(ideas, Filters(place = Place.INDOOR)))
        assertEquals(setOf(2L, 3L), pickedIds(ideas, Filters(place = Place.OUTDOOR)))
    }

    @Test
    fun anyPeopleIdeasMatchSoloAndGroup() {
        val ideas = listOf(
            testIdea(1, people = People.SOLO),
            testIdea(2, people = People.GROUP),
            testIdea(3, people = People.ANY),
        )
        assertEquals(setOf(1L, 3L), pickedIds(ideas, Filters(people = People.SOLO)))
        assertEquals(setOf(2L, 3L), pickedIds(ideas, Filters(people = People.GROUP)))
    }

    @Test
    fun energyMatchesExactly() {
        val ideas = listOf(testIdea(1, energy = Energy.CHILL), testIdea(2, energy = Energy.ACTIVE))
        assertEquals(setOf(2L), pickedIds(ideas, Filters(energy = Energy.ACTIVE)))
    }

    @Test
    fun neverRepeatsTheExcludedIdea() {
        val ideas = listOf(testIdea(1), testIdea(2))
        assertEquals(setOf(2L), pickedIds(ideas, Filters(), excludeId = 1L))
    }

    @Test
    fun returnsExcludedIdeaWhenItIsTheOnlyMatch() {
        val ideas = listOf(testIdea(1))
        assertEquals(1L, picker.pick(ideas, Filters(), emptyMap(), excludeId = 1L)?.id)
    }

    @Test
    fun recentlyDoneIdeasAreAboutFiveTimesLessLikely() {
        val ideas = listOf(testIdea(1), testIdea(2))
        val lastDone = mapOf(1L to now - day)
        val picks = (1..10_000).map { picker.pick(ideas, Filters(), lastDone)!!.id }
        val ones = picks.count { it == 1L }
        // Expected share 0.2 / 1.2 ≈ 1667 of 10 000.
        assertTrue("picked recent idea $ones times", ones in 1400..1950)
    }

    @Test
    fun ideasDoneMoreThanTwoWeeksAgoAreNotPenalized() {
        val ideas = listOf(testIdea(1), testIdea(2))
        val lastDone = mapOf(1L to now - 15 * day)
        val ones = (1..10_000).count { picker.pick(ideas, Filters(), lastDone)!!.id == 1L }
        assertTrue("picked old idea $ones times", ones in 4600..5400)
    }
}
```

- [ ] **Step 3: Run the tests to verify they fail**

Run: `./gradlew testDebugUnitTest --tests "com.armen.spark.domain.IdeaPickerTest"`
Expected: compilation FAILS with `Unresolved reference 'IdeaPicker'`.

- [ ] **Step 4: Implement `IdeaPicker`**

`domain/IdeaPicker.kt`:
```kotlin
package com.armen.spark.domain

import kotlin.random.Random

class IdeaPicker(
    private val random: Random = Random.Default,
    private val now: () -> Long = System::currentTimeMillis,
) {
    /**
     * Picks a random idea that matches [filters], weighting down ideas done in the last 14 days.
     * [excludeId] (the idea currently shown) is skipped unless it is the only match.
     */
    fun pick(
        candidates: List<Idea>,
        filters: Filters,
        lastDone: Map<Long, Long>,
        excludeId: Long? = null,
    ): Idea? {
        val matching = candidates.filter(filters::matches)
        if (matching.isEmpty()) return null
        val pool = matching.filter { it.id != excludeId }.ifEmpty { matching }

        val cutoff = now() - RECENT_WINDOW_MS
        val weights = pool.map { idea ->
            val doneAt = lastDone[idea.id]
            if (doneAt != null && doneAt >= cutoff) RECENT_WEIGHT else 1.0
        }
        var roll = random.nextDouble() * weights.sum()
        for (i in pool.indices) {
            roll -= weights[i]
            if (roll < 0) return pool[i]
        }
        return pool.last()
    }

    companion object {
        const val RECENT_WEIGHT = 0.2
        const val RECENT_WINDOW_MS = 14L * 24 * 60 * 60 * 1000
    }
}
```

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./gradlew testDebugUnitTest --tests "com.armen.spark.domain.IdeaPickerTest"`
Expected: PASS (10 tests).

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "feat: add idea domain model, filters and weighted picker"
```

---

### Task 3: Seed parser and the bundled idea list

**Files:**
- Create: `app/src/main/java/com/armen/spark/data/SeedParser.kt`
- Create: `app/src/main/assets/activities.json`
- Test: `app/src/test/java/com/armen/spark/data/SeedParserTest.kt`

**Interfaces:**
- Consumes: `Idea`, the enums, `CATEGORIES`, `Filters` (Task 2).
- Produces: `object SeedParser { fun parse(text: String): List<Idea> }`. It throws if the top level isn't a JSON array, and skips entries that fail to decode or have a blank title.

- [ ] **Step 1: Write the failing tests**

```kotlin
package com.armen.spark.data

import com.armen.spark.domain.CATEGORIES
import com.armen.spark.domain.Cost
import com.armen.spark.domain.Duration
import com.armen.spark.domain.Energy
import com.armen.spark.domain.Filters
import com.armen.spark.domain.People
import com.armen.spark.domain.Place
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SeedParserTest {
    private fun entry(title: String, duration: String = "HOUR") =
        """{"title":"$title","description":"d","emoji":"📚","category":"Learning",
            "duration":"$duration","cost":"FREE","place":"INDOOR","people":"SOLO","energy":"CHILL"}"""

    @Test
    fun parsesValidEntries() {
        val ideas = SeedParser.parse("[${entry("Read a chapter")}]")
        assertEquals(1, ideas.size)
        with(ideas.single()) {
            assertEquals("Read a chapter", title)
            assertEquals(Duration.HOUR, duration)
            assertEquals(Cost.FREE, cost)
            assertEquals(Place.INDOOR, place)
            assertEquals(People.SOLO, people)
            assertEquals(Energy.CHILL, energy)
            assertFalse(isCustom)
            assertFalse(isFavorite)
        }
    }

    @Test
    fun skipsMalformedEntriesAndKeepsTheRest() {
        val missingTitle = """{"description":"d","emoji":"x","category":"Games","duration":"QUICK",
            "cost":"FREE","place":"INDOOR","people":"SOLO","energy":"CHILL"}"""
        val text = "[${entry("Good")}, ${entry("Bad enum", duration = "FOREVER")}, $missingTitle, ${entry("  ")}]"
        assertEquals(listOf("Good"), SeedParser.parse(text).map { it.title })
    }

    @Test
    fun bundledSeedIsCompleteAndValid() {
        val text = File("src/main/assets/activities.json").readText()
        val rawCount = Json.parseToJsonElement(text).jsonArray.size
        val ideas = SeedParser.parse(text)

        assertEquals("every bundled entry must parse", rawCount, ideas.size)
        assertTrue("need at least 150 ideas, have ${ideas.size}", ideas.size >= 150)
        assertEquals("titles must be unique", ideas.size, ideas.map { it.title.lowercase() }.toSet().size)
        assertEquals(CATEGORIES.toSet(), ideas.map { it.category }.toSet())
        assertTrue(ideas.all { it.emoji.isNotBlank() && it.description.isNotBlank() })

        val uiFilters = listOf(
            Filters(duration = Duration.QUICK), Filters(duration = Duration.HOUR),
            Filters(maxCost = Cost.FREE), Filters(maxCost = Cost.CHEAP),
            Filters(place = Place.INDOOR), Filters(place = Place.OUTDOOR),
            Filters(people = People.SOLO), Filters(people = People.GROUP),
            Filters(energy = Energy.CHILL), Filters(energy = Energy.ACTIVE),
        )
        uiFilters.forEach { f ->
            val count = ideas.count(f::matches)
            assertTrue("$f matches only $count ideas", count >= 10)
        }
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew testDebugUnitTest --tests "com.armen.spark.data.SeedParserTest"`
Expected: compilation FAILS with `Unresolved reference 'SeedParser'`.

- [ ] **Step 3: Implement `SeedParser`**

```kotlin
package com.armen.spark.data

import com.armen.spark.domain.Cost
import com.armen.spark.domain.Duration
import com.armen.spark.domain.Energy
import com.armen.spark.domain.Idea
import com.armen.spark.domain.People
import com.armen.spark.domain.Place
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray

object SeedParser {
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class SeedIdea(
        val title: String,
        val description: String,
        val emoji: String,
        val category: String,
        val duration: Duration,
        val cost: Cost,
        val place: Place,
        val people: People,
        val energy: Energy,
    )

    /** Throws if [text] is not a JSON array; skips individual entries that are malformed. */
    fun parse(text: String): List<Idea> =
        json.parseToJsonElement(text).jsonArray
            .mapNotNull { element -> runCatching { json.decodeFromJsonElement(SeedIdea.serializer(), element) }.getOrNull() }
            .filter { it.title.isNotBlank() }
            .map {
                Idea(
                    title = it.title.trim(), description = it.description.trim(), emoji = it.emoji,
                    category = it.category, duration = it.duration, cost = it.cost,
                    place = it.place, people = it.people, energy = it.energy,
                )
            }
}
```

- [ ] **Step 4: Run the parser unit tests**

Run: `./gradlew testDebugUnitTest --tests "com.armen.spark.data.SeedParserTest.parsesValidEntries" --tests "com.armen.spark.data.SeedParserTest.skipsMalformedEntriesAndKeepsTheRest"`
Expected: PASS. `bundledSeedIsCompleteAndValid` still FAILS (file not found).

- [ ] **Step 5: Write `app/src/main/assets/activities.json`**

Write a JSON array of **at least 150** hand-written ideas. Each entry has exactly these keys and value sets:

```json
{"title": "Sketch your view", "description": "Grab any pen and draw what's in front of you for 15 minutes — no erasing allowed.", "emoji": "✏️", "category": "Creative", "duration": "QUICK", "cost": "FREE", "place": "ANYWHERE", "people": "SOLO", "energy": "CHILL"}
```

- `category` ∈ Creative, Outdoors, Social, Learning, Food, Mindful, Games, Home. Use every category, with roughly 15–25 ideas each.
- `duration` ∈ QUICK, HOUR, LONG. `cost` ∈ FREE, CHEAP, PRICEY. `place` ∈ INDOOR, OUTDOOR, ANYWHERE. `people` ∈ SOLO, GROUP, ANY. `energy` ∈ CHILL, ACTIVE.
- Spread the tags so that every filter in the test matches at least 10 ideas. Aim for at least 25 QUICK, at least 40 GROUP, at least 30 OUTDOOR and at least 40 ACTIVE, with most ideas FREE or CHEAP.
- Titles are unique, imperative and at most 40 characters. Descriptions are 1–2 friendly, concrete sentences.
- More examples to set the tone:

```json
{"title": "Sunrise or sunset walk", "description": "Time a walk to catch the sky changing colour. Bonus points for a new route.", "emoji": "🌅", "category": "Outdoors", "duration": "HOUR", "cost": "FREE", "place": "OUTDOOR", "people": "ANY", "energy": "ACTIVE"},
{"title": "Host a blind taste test", "description": "Buy three versions of the same snack and rank them blindfolded with friends.", "emoji": "🍫", "category": "Food", "duration": "HOUR", "cost": "CHEAP", "place": "INDOOR", "people": "GROUP", "energy": "CHILL"},
{"title": "Learn 10 words of a new language", "description": "Pick a language you've never studied and learn greetings, numbers and 'thank you'.", "emoji": "🗣️", "category": "Learning", "duration": "QUICK", "cost": "FREE", "place": "ANYWHERE", "people": "SOLO", "energy": "CHILL"},
{"title": "Box-breathing break", "description": "Breathe in 4, hold 4, out 4, hold 4. Repeat for five minutes and notice the difference.", "emoji": "🫁", "category": "Mindful", "duration": "QUICK", "cost": "FREE", "place": "ANYWHERE", "people": "SOLO", "energy": "CHILL"}
```

- [ ] **Step 6: Run all the seed tests**

Run: `./gradlew testDebugUnitTest --tests "com.armen.spark.data.SeedParserTest"`
Expected: PASS (3 tests). If a filter-coverage assertion fails, add ideas with the missing tag until it passes.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "feat: add seed parser and 150+ bundled activity ideas"
```

---

### Task 4: Room database and repositories

**Files:**
- Create: `app/src/main/java/com/armen/spark/data/Entities.kt`
- Create: `app/src/main/java/com/armen/spark/data/Daos.kt`
- Create: `app/src/main/java/com/armen/spark/data/SparkDatabase.kt`
- Create: `app/src/main/java/com/armen/spark/data/IdeaRepository.kt`
- Create: `app/src/main/java/com/armen/spark/data/SettingsRepository.kt`

**Interfaces:**
- Consumes: `Idea`, `HistoryItem`, `Filters`, the enums (Task 2); `SeedParser` (Task 3).
- Produces:
  - `interface IdeaRepository`, with members exactly as below
  - `class RoomIdeaRepository(db: SparkDatabase, loadSeed: () -> List<Idea>, now: () -> Long = System::currentTimeMillis) : IdeaRepository`
  - `interface FilterStore { val filters: Flow<Filters>; suspend fun setFilters(filters: Filters) }`
  - `data class Settings(dailyEnabled: Boolean = false, dailyTime: LocalTime = LocalTime.of(9, 0))`
  - `class SettingsRepository(store: DataStore<Preferences>) : FilterStore`, with `val settings: Flow<Settings>`, `suspend fun setDailyEnabled(Boolean)` and `suspend fun setDailyTime(LocalTime)`
  - `SparkDatabase.build(context): SparkDatabase`

- [ ] **Step 1: Write the entities and mappers** (`data/Entities.kt`)

```kotlin
package com.armen.spark.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.armen.spark.domain.Cost
import com.armen.spark.domain.Duration
import com.armen.spark.domain.Energy
import com.armen.spark.domain.Idea
import com.armen.spark.domain.People
import com.armen.spark.domain.Place

@Entity(tableName = "ideas")
data class IdeaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val emoji: String,
    val category: String,
    val duration: Duration,
    val cost: Cost,
    val place: Place,
    val people: People,
    val energy: Energy,
    val isCustom: Boolean,
    val isFavorite: Boolean,
)

@Entity(
    tableName = "done_entries",
    foreignKeys = [
        ForeignKey(
            entity = IdeaEntity::class,
            parentColumns = ["id"],
            childColumns = ["ideaId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("ideaId")],
)
data class DoneEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ideaId: Long,
    val doneAt: Long,
)

data class LastDone(val ideaId: Long, val lastDoneAt: Long)

data class HistoryRow(
    val entryId: Long,
    val doneAt: Long,
    @Embedded val idea: IdeaEntity,
)

fun IdeaEntity.toDomain() = Idea(
    id = id, title = title, description = description, emoji = emoji, category = category,
    duration = duration, cost = cost, place = place, people = people, energy = energy,
    isCustom = isCustom, isFavorite = isFavorite,
)

fun Idea.toEntity() = IdeaEntity(
    id = id, title = title, description = description, emoji = emoji, category = category,
    duration = duration, cost = cost, place = place, people = people, energy = energy,
    isCustom = isCustom, isFavorite = isFavorite,
)
```

- [ ] **Step 2: Write the DAOs** (`data/Daos.kt`)

```kotlin
package com.armen.spark.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface IdeaDao {
    @Query("SELECT * FROM ideas ORDER BY title COLLATE NOCASE")
    fun observeAll(): Flow<List<IdeaEntity>>

    /** Saved tab: favorites plus every custom idea, so your own ideas never get lost. */
    @Query("SELECT * FROM ideas WHERE isFavorite = 1 OR isCustom = 1 ORDER BY title COLLATE NOCASE")
    fun observeSaved(): Flow<List<IdeaEntity>>

    @Query("SELECT * FROM ideas")
    suspend fun getAll(): List<IdeaEntity>

    @Query("SELECT * FROM ideas WHERE id = :id")
    suspend fun getById(id: Long): IdeaEntity?

    @Query("SELECT COUNT(*) FROM ideas")
    suspend fun count(): Int

    @Insert
    suspend fun insertAll(ideas: List<IdeaEntity>)

    @Upsert
    suspend fun upsert(idea: IdeaEntity)

    @Query("UPDATE ideas SET isFavorite = NOT isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: Long)

    @Query("DELETE FROM ideas WHERE id = :id AND isCustom = 1")
    suspend fun deleteCustom(id: Long)
}

@Dao
interface DoneDao {
    @Insert
    suspend fun insert(entry: DoneEntryEntity)

    @Query("SELECT ideaId, MAX(doneAt) AS lastDoneAt FROM done_entries GROUP BY ideaId")
    suspend fun lastDone(): List<LastDone>

    @Query(
        "SELECT d.id AS entryId, d.doneAt AS doneAt, i.* FROM done_entries d " +
            "JOIN ideas i ON i.id = d.ideaId ORDER BY d.doneAt DESC",
    )
    fun observeHistory(): Flow<List<HistoryRow>>

    @Query("DELETE FROM done_entries WHERE ideaId = :ideaId")
    suspend fun deleteForIdea(ideaId: Long)
}
```

- [ ] **Step 3: Write the database** (`data/SparkDatabase.kt`)

```kotlin
package com.armen.spark.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [IdeaEntity::class, DoneEntryEntity::class], version = 1, exportSchema = false)
abstract class SparkDatabase : RoomDatabase() {
    abstract fun ideaDao(): IdeaDao
    abstract fun doneDao(): DoneDao

    companion object {
        fun build(context: Context): SparkDatabase =
            Room.databaseBuilder(context.applicationContext, SparkDatabase::class.java, "spark.db").build()
    }
}
```

- [ ] **Step 4: Write `IdeaRepository`** (`data/IdeaRepository.kt`)

```kotlin
package com.armen.spark.data

import android.util.Log
import androidx.room.withTransaction
import com.armen.spark.domain.HistoryItem
import com.armen.spark.domain.Idea
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

interface IdeaRepository {
    val ideas: Flow<List<Idea>>
    /** Favorites plus all custom ideas. */
    val saved: Flow<List<Idea>>
    val history: Flow<List<HistoryItem>>
    suspend fun allIdeas(): List<Idea>
    suspend fun getIdea(id: Long): Idea?
    suspend fun lastDoneMap(): Map<Long, Long>
    suspend fun toggleFavorite(id: Long)
    suspend fun markDone(id: Long)
    suspend fun saveCustom(idea: Idea)
    suspend fun deleteCustom(id: Long)
    suspend fun seedIfEmpty()
}

class RoomIdeaRepository(
    private val db: SparkDatabase,
    private val loadSeed: () -> List<Idea>,
    private val now: () -> Long = System::currentTimeMillis,
) : IdeaRepository {
    private val ideaDao = db.ideaDao()
    private val doneDao = db.doneDao()
    private val seedMutex = Mutex()
    @Volatile private var seeded = false

    override val ideas: Flow<List<Idea>> = ideaDao.observeAll().map { list -> list.map { it.toDomain() } }
    override val saved: Flow<List<Idea>> = ideaDao.observeSaved().map { list -> list.map { it.toDomain() } }
    override val history: Flow<List<HistoryItem>> = doneDao.observeHistory().map { rows ->
        rows.map { HistoryItem(entryId = it.entryId, idea = it.idea.toDomain(), doneAt = it.doneAt) }
    }

    override suspend fun allIdeas(): List<Idea> = ideaDao.getAll().map { it.toDomain() }

    override suspend fun getIdea(id: Long): Idea? = ideaDao.getById(id)?.toDomain()

    override suspend fun lastDoneMap(): Map<Long, Long> =
        doneDao.lastDone().associate { it.ideaId to it.lastDoneAt }

    override suspend fun toggleFavorite(id: Long) {
        ideaDao.toggleFavorite(id)
    }

    override suspend fun markDone(id: Long) {
        doneDao.insert(DoneEntryEntity(ideaId = id, doneAt = now()))
    }

    override suspend fun saveCustom(idea: Idea) {
        ideaDao.upsert(idea.copy(isCustom = true).toEntity())
    }

    override suspend fun deleteCustom(id: Long) {
        db.withTransaction {
            doneDao.deleteForIdea(id)
            ideaDao.deleteCustom(id)
        }
    }

    override suspend fun seedIfEmpty() {
        seedMutex.withLock {
            if (seeded) return
            if (ideaDao.count() == 0) {
                val seed = withContext(Dispatchers.IO) {
                    runCatching(loadSeed)
                        .onFailure { Log.e(TAG, "Could not load bundled ideas", it) }
                        .getOrDefault(emptyList())
                }
                ideaDao.insertAll(seed.map { it.toEntity() })
            }
            seeded = true
        }
    }

    private companion object {
        const val TAG = "IdeaRepository"
    }
}
```

- [ ] **Step 5: Write `SettingsRepository`** (`data/SettingsRepository.kt`)

```kotlin
package com.armen.spark.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.armen.spark.domain.Cost
import com.armen.spark.domain.Duration
import com.armen.spark.domain.Energy
import com.armen.spark.domain.Filters
import com.armen.spark.domain.People
import com.armen.spark.domain.Place
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.LocalTime

interface FilterStore {
    val filters: Flow<Filters>
    suspend fun setFilters(filters: Filters)
}

data class Settings(
    val dailyEnabled: Boolean = false,
    val dailyTime: LocalTime = LocalTime.of(9, 0),
)

class SettingsRepository(private val store: DataStore<Preferences>) : FilterStore {
    private val prefs: Flow<Preferences> = store.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    val settings: Flow<Settings> = prefs.map { p ->
        Settings(
            dailyEnabled = p[DAILY_ENABLED] ?: false,
            dailyTime = LocalTime.of(p[DAILY_HOUR] ?: 9, p[DAILY_MINUTE] ?: 0),
        )
    }

    override val filters: Flow<Filters> = prefs.map { p ->
        Filters(
            duration = p[FILTER_DURATION].toEnumOrNull<Duration>(),
            maxCost = p[FILTER_COST].toEnumOrNull<Cost>(),
            place = p[FILTER_PLACE].toEnumOrNull<Place>(),
            people = p[FILTER_PEOPLE].toEnumOrNull<People>(),
            energy = p[FILTER_ENERGY].toEnumOrNull<Energy>(),
        )
    }

    suspend fun setDailyEnabled(enabled: Boolean) {
        store.edit { it[DAILY_ENABLED] = enabled }
    }

    suspend fun setDailyTime(time: LocalTime) {
        store.edit {
            it[DAILY_HOUR] = time.hour
            it[DAILY_MINUTE] = time.minute
        }
    }

    override suspend fun setFilters(filters: Filters) {
        store.edit { p ->
            p.putOrRemove(FILTER_DURATION, filters.duration?.name)
            p.putOrRemove(FILTER_COST, filters.maxCost?.name)
            p.putOrRemove(FILTER_PLACE, filters.place?.name)
            p.putOrRemove(FILTER_PEOPLE, filters.people?.name)
            p.putOrRemove(FILTER_ENERGY, filters.energy?.name)
        }
    }

    private fun MutablePreferences.putOrRemove(key: Preferences.Key<String>, value: String?) {
        if (value == null) remove(key) else this[key] = value
    }

    private companion object {
        val DAILY_ENABLED = booleanPreferencesKey("daily_enabled")
        val DAILY_HOUR = intPreferencesKey("daily_hour")
        val DAILY_MINUTE = intPreferencesKey("daily_minute")
        val FILTER_DURATION = stringPreferencesKey("filter_duration")
        val FILTER_COST = stringPreferencesKey("filter_cost")
        val FILTER_PLACE = stringPreferencesKey("filter_place")
        val FILTER_PEOPLE = stringPreferencesKey("filter_people")
        val FILTER_ENERGY = stringPreferencesKey("filter_energy")
    }
}

private inline fun <reified E : Enum<E>> String?.toEnumOrNull(): E? =
    this?.let { value -> enumValues<E>().firstOrNull { it.name == value } }
```

- [ ] **Step 6: Build (KSP validates every Room query at compile time)**

Run: `./gradlew assembleDebug testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`. A typo in a query fails here with a Room error naming the query.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "feat: add Room storage, idea repository and settings store"
```

---

### Task 5: App wiring, navigation shell and the Discover screen

**Files:**
- Create: `app/src/main/java/com/armen/spark/SparkApp.kt`, `app/src/main/java/com/armen/spark/AppContainer.kt`
- Modify: `app/src/main/java/com/armen/spark/MainActivity.kt` (full replacement)
- Modify: `app/src/main/AndroidManifest.xml` (add `android:name=".SparkApp"` to `<application>`)
- Create: `app/src/main/java/com/armen/spark/ui/components/Components.kt`
- Create: `app/src/main/java/com/armen/spark/ui/discover/DiscoverViewModel.kt`, `DiscoverScreen.kt`
- Create: `app/src/main/java/com/armen/spark/ui/nav/SparkNavHost.kt` (only the Discover tab is wired; Tasks 6–8 add theirs)
- Create: `app/src/main/java/com/armen/spark/notify/Notifications.kt` (`EXTRA_IDEA_ID` and the channel; Task 8 adds posting)
- Test: `app/src/test/java/com/armen/spark/MainDispatcherRule.kt`, `app/src/test/java/com/armen/spark/Fakes.kt`, `app/src/test/java/com/armen/spark/ui/discover/DiscoverViewModelTest.kt`

**Interfaces:**
- Consumes: `IdeaRepository`, `FilterStore`, `SettingsRepository`, `RoomIdeaRepository`, `SparkDatabase.build`, `SeedParser`, `IdeaPicker`, `SparkTheme`.
- Produces:
  - `class AppContainer(context)`, with `ideaRepository: IdeaRepository`, `settingsRepository: SettingsRepository` and `picker: IdeaPicker`
  - `(application as SparkApp).container`
  - `data class DiscoverUiState(filters: Filters, current: Idea?, noMatch: Boolean)`
  - `DiscoverViewModel`, with `uiState`, `messages: Flow<String>`, `setFilters`, `resetFilters`, `spark`, `showIdea(id)`, `toggleFavorite`, `markDone`
  - components `FilterRow`, `IdeaListItem`, `EmptyState` and `Idea.tagLine()`
  - `Notifications.EXTRA_IDEA_ID`, `Notifications.createChannel(context)`
  - test fakes `FakeIdeaRepository` and `FakeFilterStore`, and `MainDispatcherRule`

- [ ] **Step 1: Write the test infrastructure**

`test/.../MainDispatcherRule.kt`:
```kotlin
package com.armen.spark

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(val dispatcher: TestDispatcher = UnconfinedTestDispatcher()) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}
```

`test/.../Fakes.kt`:
```kotlin
package com.armen.spark

import com.armen.spark.data.FilterStore
import com.armen.spark.data.IdeaRepository
import com.armen.spark.domain.Filters
import com.armen.spark.domain.HistoryItem
import com.armen.spark.domain.Idea
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.yield

class FakeIdeaRepository(initial: List<Idea> = emptyList()) : IdeaRepository {
    val ideaFlow = MutableStateFlow(initial)
    val historyFlow = MutableStateFlow(emptyList<HistoryItem>())
    val doneIds = mutableListOf<Long>()
    val savedCustom = mutableListOf<Idea>()

    override val ideas: Flow<List<Idea>> = ideaFlow
    override val saved: Flow<List<Idea>> = ideaFlow.map { list -> list.filter { it.isFavorite || it.isCustom } }
    override val history: Flow<List<HistoryItem>> = historyFlow
    override suspend fun allIdeas() = ideaFlow.value
    override suspend fun getIdea(id: Long) = ideaFlow.value.firstOrNull { it.id == id }
    override suspend fun lastDoneMap() = emptyMap<Long, Long>()
    override suspend fun toggleFavorite(id: Long) {
        ideaFlow.update { list -> list.map { if (it.id == id) it.copy(isFavorite = !it.isFavorite) else it } }
    }
    override suspend fun markDone(id: Long) { doneIds += id }
    override suspend fun saveCustom(idea: Idea) {
        yield() // behave like real I/O: give other coroutines a chance to run
        savedCustom += idea
    }
    override suspend fun deleteCustom(id: Long) {
        ideaFlow.update { list -> list.filterNot { it.id == id } }
    }
    override suspend fun seedIfEmpty() = Unit
}

class FakeFilterStore(initial: Filters = Filters()) : FilterStore {
    private val state = MutableStateFlow(initial)
    val current: Filters get() = state.value
    override val filters: Flow<Filters> = state
    override suspend fun setFilters(filters: Filters) { state.value = filters }
}
```

- [ ] **Step 2: Write the failing ViewModel tests**

`test/.../ui/discover/DiscoverViewModelTest.kt`:
```kotlin
package com.armen.spark.ui.discover

import com.armen.spark.FakeFilterStore
import com.armen.spark.FakeIdeaRepository
import com.armen.spark.MainDispatcherRule
import com.armen.spark.domain.Cost
import com.armen.spark.domain.Energy
import com.armen.spark.domain.Filters
import com.armen.spark.domain.IdeaPicker
import com.armen.spark.testIdea
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class DiscoverViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private val chill = testIdea(1, energy = Energy.CHILL)
    private val active = testIdea(2, energy = Energy.ACTIVE)

    private fun TestScope.viewModel(repo: FakeIdeaRepository, store: FakeFilterStore = FakeFilterStore()) =
        DiscoverViewModel(repo, store, IdeaPicker(Random(7))).also { vm ->
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        }

    @Test
    fun sparkShowsAnIdeaMatchingTheFiltersAndSavesThem() = runTest {
        val store = FakeFilterStore()
        val vm = viewModel(FakeIdeaRepository(listOf(chill, active)), store)

        vm.setFilters(Filters(energy = Energy.ACTIVE))
        vm.spark()

        assertEquals(2L, vm.uiState.value.current?.id)
        assertEquals(Filters(energy = Energy.ACTIVE), store.current)
    }

    @Test
    fun showsNoMatchAndResetClearsIt() = runTest {
        val vm = viewModel(FakeIdeaRepository(listOf(chill)))

        vm.setFilters(Filters(energy = Energy.ACTIVE))
        vm.spark()
        assertTrue(vm.uiState.value.noMatch)
        assertNull(vm.uiState.value.current)

        vm.resetFilters()
        assertFalse(vm.uiState.value.noMatch)
        assertEquals(Filters(), vm.uiState.value.filters)
    }

    @Test
    fun restoresSavedFilters() = runTest {
        val vm = viewModel(FakeIdeaRepository(listOf(chill)), FakeFilterStore(Filters(maxCost = Cost.FREE)))
        assertEquals(Filters(maxCost = Cost.FREE), vm.uiState.value.filters)
    }

    @Test
    fun cardClearsWhenTheShownIdeaIsDeleted() = runTest {
        val mine = testIdea(5, isCustom = true)
        val repo = FakeIdeaRepository(listOf(mine))
        val vm = viewModel(repo)

        vm.spark()
        assertEquals(5L, vm.uiState.value.current?.id)

        repo.deleteCustom(5L)
        assertNull(vm.uiState.value.current)
    }

    @Test
    fun favoriteToggleIsReflectedOnTheCard() = runTest {
        val vm = viewModel(FakeIdeaRepository(listOf(chill)))
        vm.spark()
        vm.toggleFavorite()
        assertTrue(vm.uiState.value.current!!.isFavorite)
    }

    @Test
    fun markDoneLogsTheIdeaAndConfirms() = runTest {
        val repo = FakeIdeaRepository(listOf(chill))
        val vm = viewModel(repo)
        val messages = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.messages.toList(messages) }

        vm.spark()
        vm.markDone()

        assertEquals(listOf(1L), repo.doneIds)
        assertEquals(1, messages.size)
    }

    @Test
    fun showIdeaDisplaysTheRequestedIdea() = runTest {
        val vm = viewModel(FakeIdeaRepository(listOf(chill, active)))
        vm.showIdea(2L)
        assertEquals(2L, vm.uiState.value.current?.id)
    }
}
```

- [ ] **Step 3: Run the tests to verify they fail**

Run: `./gradlew testDebugUnitTest --tests "com.armen.spark.ui.discover.DiscoverViewModelTest"`
Expected: compilation FAILS with `Unresolved reference 'DiscoverViewModel'`.

- [ ] **Step 4: Implement `DiscoverViewModel`**

```kotlin
package com.armen.spark.ui.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.armen.spark.data.FilterStore
import com.armen.spark.data.IdeaRepository
import com.armen.spark.domain.Filters
import com.armen.spark.domain.Idea
import com.armen.spark.domain.IdeaPicker
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DiscoverUiState(
    val filters: Filters = Filters(),
    val current: Idea? = null,
    val noMatch: Boolean = false,
)

class DiscoverViewModel(
    private val repository: IdeaRepository,
    private val filterStore: FilterStore,
    private val picker: IdeaPicker,
) : ViewModel() {
    private val filters = MutableStateFlow(Filters())
    private val currentId = MutableStateFlow<Long?>(null)
    private val noMatch = MutableStateFlow(false)
    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = _messages.receiveAsFlow()

    // The card is resolved against the live list, so edits, favorites and deletions show up immediately.
    val uiState: StateFlow<DiscoverUiState> =
        combine(filters, currentId, noMatch, repository.ideas) { f, id, nm, ideas ->
            DiscoverUiState(filters = f, current = id?.let { wanted -> ideas.firstOrNull { it.id == wanted } }, noMatch = nm)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DiscoverUiState())

    init {
        viewModelScope.launch { repository.seedIfEmpty() }
        viewModelScope.launch {
            val saved = filterStore.filters.first()
            filters.compareAndSet(Filters(), saved) // don't clobber a filter the user already tapped
        }
    }

    fun setFilters(newFilters: Filters) {
        filters.value = newFilters
        noMatch.value = false
        viewModelScope.launch { filterStore.setFilters(newFilters) }
    }

    fun resetFilters() = setFilters(Filters())

    fun spark() {
        viewModelScope.launch {
            val idea = picker.pick(
                candidates = repository.allIdeas(),
                filters = filters.value,
                lastDone = repository.lastDoneMap(),
                excludeId = currentId.value,
            )
            currentId.value = idea?.id
            noMatch.value = idea == null
        }
    }

    fun showIdea(id: Long) {
        currentId.value = id
        noMatch.value = false
    }

    fun toggleFavorite() {
        val id = currentId.value ?: return
        viewModelScope.launch { repository.toggleFavorite(id) }
    }

    fun markDone() {
        val id = currentId.value ?: return
        viewModelScope.launch {
            repository.markDone(id)
            _messages.send("Nice! Logged in History ✓")
        }
    }
}
```

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./gradlew testDebugUnitTest --tests "com.armen.spark.ui.discover.DiscoverViewModelTest"`
Expected: PASS (7 tests).

- [ ] **Step 6: Write the shared components** (`ui/components/Components.kt`)

```kotlin
package com.armen.spark.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armen.spark.domain.Idea

fun Idea.tagLine(): String = listOf(
    "⏱ ${duration.label}", "💸 ${cost.label}", "📍 ${place.label}", "👥 ${people.label}", "🔋 ${energy.label}",
).joinToString("  ·  ")

/** One row of chips. Tapping the selected chip again reports null (= "any"). */
@Composable
fun <T> FilterRow(
    title: String,
    options: List<T>,
    selected: T?,
    label: (T) -> String,
    onSelect: (T?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { option ->
                FilterChip(
                    selected = option == selected,
                    onClick = { onSelect(if (option == selected) null else option) },
                    label = { Text(label(option)) },
                )
            }
        }
    }
}

@Composable
fun IdeaListItem(
    idea: Idea,
    supporting: String = idea.category,
    trailing: (@Composable () -> Unit)? = null,
) {
    ListItem(
        leadingContent = { Text(idea.emoji, fontSize = 28.sp) },
        headlineContent = { Text(idea.title) },
        supportingContent = { Text(if (idea.isCustom) "Mine · $supporting" else supporting) },
        trailingContent = trailing,
    )
}

@Composable
fun EmptyState(emoji: String, title: String, body: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(emoji, fontSize = 44.sp)
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
```

- [ ] **Step 7: Write `DiscoverScreen.kt`**

```kotlin
package com.armen.spark.ui.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.armen.spark.domain.Cost
import com.armen.spark.domain.Duration
import com.armen.spark.domain.Energy
import com.armen.spark.domain.Idea
import com.armen.spark.domain.People
import com.armen.spark.domain.Place
import com.armen.spark.ui.components.EmptyState
import com.armen.spark.ui.components.FilterRow
import com.armen.spark.ui.components.tagLine

@Composable
fun DiscoverScreen(viewModel: DiscoverViewModel, snackbarHostState: SnackbarHostState) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }
    val f = state.filters

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("What should I do?", style = MaterialTheme.typography.headlineMedium)
        FilterRow("Time", listOf(Duration.QUICK, Duration.HOUR), f.duration, ::durationFilterLabel) {
            viewModel.setFilters(f.copy(duration = it))
        }
        FilterRow("Budget", listOf(Cost.FREE, Cost.CHEAP), f.maxCost, Cost::label) {
            viewModel.setFilters(f.copy(maxCost = it))
        }
        FilterRow("Place", listOf(Place.INDOOR, Place.OUTDOOR), f.place, Place::label) {
            viewModel.setFilters(f.copy(place = it))
        }
        FilterRow("People", listOf(People.SOLO, People.GROUP), f.people, People::label) {
            viewModel.setFilters(f.copy(people = it))
        }
        FilterRow("Energy", Energy.entries, f.energy, Energy::label) {
            viewModel.setFilters(f.copy(energy = it))
        }

        Button(onClick = viewModel::spark, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text("✨  Spark me", style = MaterialTheme.typography.titleMedium)
        }

        val current = state.current
        when {
            current != null -> IdeaCard(
                idea = current,
                onFavorite = viewModel::toggleFavorite,
                onDone = viewModel::markDone,
                onAnother = viewModel::spark,
            )
            state.noMatch -> Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                EmptyState("🤷", "No ideas fit — loosen a filter", "Fewer filters means more surprises.")
                TextButton(onClick = viewModel::resetFilters) { Text("Reset filters") }
            }
            else -> Text(
                "Pick any filters you like — or none — and tap Spark me.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun durationFilterLabel(duration: Duration) = when (duration) {
    Duration.QUICK -> "15 min"
    Duration.HOUR -> "Up to 1 h"
    Duration.LONG -> "Any"
}

@Composable
private fun IdeaCard(idea: Idea, onFavorite: () -> Unit, onDone: () -> Unit, onAnother: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(idea.emoji, fontSize = 44.sp)
                Spacer(Modifier.weight(1f))
                IconToggleButton(checked = idea.isFavorite, onCheckedChange = { onFavorite() }) {
                    if (idea.isFavorite) {
                        Icon(Icons.Filled.Favorite, "Remove from saved", tint = MaterialTheme.colorScheme.primary)
                    } else {
                        Icon(Icons.Outlined.FavoriteBorder, "Save")
                    }
                }
            }
            Text(idea.title, style = MaterialTheme.typography.titleLarge)
            Text(idea.category, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            if (idea.description.isNotBlank()) {
                Text(idea.description, style = MaterialTheme.typography.bodyLarge)
            }
            Text(idea.tagLine(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                OutlinedButton(onClick = onDone) {
                    Icon(Icons.Filled.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Done")
                }
                FilledTonalButton(onClick = onAnother) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Another")
                }
            }
        }
    }
}
```

- [ ] **Step 8: Write `notify/Notifications.kt` (the channel and constants only, for now)**

```kotlin
package com.armen.spark.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

object Notifications {
    const val CHANNEL_ID = "daily_idea"
    const val EXTRA_IDEA_ID = "com.armen.spark.IDEA_ID"
    const val DAILY_NOTIFICATION_ID = 1

    fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL_ID, "Daily idea", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "One activity suggestion a day"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
```

- [ ] **Step 9: Write `AppContainer.kt` and `SparkApp.kt`, and register the app in the manifest**

`AppContainer.kt`:
```kotlin
package com.armen.spark

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.armen.spark.data.IdeaRepository
import com.armen.spark.data.RoomIdeaRepository
import com.armen.spark.data.SeedParser
import com.armen.spark.data.SettingsRepository
import com.armen.spark.data.SparkDatabase
import com.armen.spark.domain.IdeaPicker

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database = SparkDatabase.build(appContext)

    val ideaRepository: IdeaRepository = RoomIdeaRepository(
        db = database,
        loadSeed = { SeedParser.parse(appContext.assets.open("activities.json").bufferedReader().use { it.readText() }) },
    )
    val settingsRepository = SettingsRepository(appContext.settingsDataStore)
    val picker = IdeaPicker()
}
```

`SparkApp.kt`:
```kotlin
package com.armen.spark

import android.app.Application
import com.armen.spark.notify.Notifications

class SparkApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifications.createChannel(this)
    }
}
```

In `AndroidManifest.xml`, add `android:name=".SparkApp"` as the first attribute of `<application>`.

- [ ] **Step 10: Write `ui/nav/SparkNavHost.kt` with the Discover tab**

```kotlin
package com.armen.spark.ui.nav

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.armen.spark.AppContainer
import com.armen.spark.ui.discover.DiscoverScreen
import com.armen.spark.ui.discover.DiscoverViewModel
import kotlinx.coroutines.flow.StateFlow

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    DISCOVER("discover", "Discover", Icons.Filled.Star),
    SAVED("saved", "Saved", Icons.Filled.Favorite),
    HISTORY("history", "History", Icons.AutoMirrored.Filled.List),
    SETTINGS("settings", "Settings", Icons.Filled.Settings),
}

@Composable
fun SparkNavHost(
    container: AppContainer,
    pendingIdeaId: StateFlow<Long?>,
    onPendingIdeaConsumed: () -> Unit,
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    // Activity-scoped so the current card survives tab switches and can receive notification taps.
    val discoverViewModel: DiscoverViewModel = viewModel {
        DiscoverViewModel(container.ideaRepository, container.settingsRepository, container.picker)
    }

    val pendingId by pendingIdeaId.collectAsStateWithLifecycle()
    LaunchedEffect(pendingId) {
        val id = pendingId ?: return@LaunchedEffect
        discoverViewModel.showIdea(id)
        navController.navigateToTab(Tab.DISCOVER)
        onPendingIdeaConsumed()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (Tab.entries.any { it.route == currentRoute }) {
                NavigationBar {
                    Tab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = { navController.navigateToTab(tab) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Tab.DISCOVER.route,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
        ) {
            composable(Tab.DISCOVER.route) { DiscoverScreen(discoverViewModel, snackbarHostState) }
            composable(Tab.SAVED.route) { Text("Saved") }
            composable(Tab.HISTORY.route) { Text("History") }
            composable(Tab.SETTINGS.route) { Text("Settings") }
        }
    }
}

private fun NavController.navigateToTab(tab: Tab) = navigate(tab.route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}
```

- [ ] **Step 11: Replace `MainActivity.kt`**

```kotlin
package com.armen.spark

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.armen.spark.notify.Notifications
import com.armen.spark.ui.nav.SparkNavHost
import com.armen.spark.ui.theme.SparkTheme
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    private val pendingIdeaId = MutableStateFlow<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) pendingIdeaId.value = intent.ideaId()
        val container = (application as SparkApp).container
        setContent {
            SparkTheme {
                SparkNavHost(container, pendingIdeaId, onPendingIdeaConsumed = { pendingIdeaId.value = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.ideaId()?.let { pendingIdeaId.value = it }
    }

    private fun Intent.ideaId(): Long? = getLongExtra(Notifications.EXTRA_IDEA_ID, -1L).takeIf { it > 0 }
}
```

- [ ] **Step 12: Build and test**

Run: `./gradlew testDebugUnitTest assembleDebug`
Expected: `BUILD SUCCESSFUL`; all tests pass.

- [ ] **Step 13: Commit**

```bash
git add -A
git commit -m "feat: add Discover screen, navigation shell and app wiring"
```

---

### Task 6: Saved tab and the add/edit idea screen

**Files:**
- Create: `app/src/main/java/com/armen/spark/ui/edit/IdeaDraft.kt`, `EditIdeaViewModel.kt`, `EditIdeaScreen.kt`
- Create: `app/src/main/java/com/armen/spark/ui/saved/SavedViewModel.kt`, `SavedScreen.kt`
- Modify: `app/src/main/java/com/armen/spark/ui/nav/SparkNavHost.kt` (Saved and edit routes)
- Test: `app/src/test/java/com/armen/spark/ui/edit/IdeaDraftTest.kt`, `EditIdeaViewModelTest.kt`

**Interfaces:**
- Consumes: `IdeaRepository` (`saved`, `getIdea`, `saveCustom`, `deleteCustom`, `toggleFavorite`); `FilterRow`, `IdeaListItem`, `EmptyState`; `FakeIdeaRepository`, `MainDispatcherRule`.
- Produces:
  - `data class IdeaDraft`, with `canSave`, `toIdeaOrNull()` and `IdeaDraft.from(idea)`
  - `EditIdeaViewModel(repository, ideaId: Long?)`, with `isNew`, `draft: StateFlow<IdeaDraft>`, `update((IdeaDraft) -> IdeaDraft)` and `save(onSaved: () -> Unit)`
  - `SavedViewModel(repository)`, with `ideas: StateFlow<List<Idea>>`, `toggleFavorite(id)` and `delete(id)`

- [ ] **Step 1: Write the failing tests**

`IdeaDraftTest.kt`:
```kotlin
package com.armen.spark.ui.edit

import com.armen.spark.testIdea
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IdeaDraftTest {
    @Test
    fun blankOrWhitespaceTitleCannotBeSaved() {
        assertFalse(IdeaDraft(title = "").canSave)
        assertFalse(IdeaDraft(title = "   \n").canSave)
        assertNull(IdeaDraft(title = "   ").toIdeaOrNull())
    }

    @Test
    fun savingTrimsTextAndMarksCustom() {
        val idea = IdeaDraft(title = "  Bake bread ", description = " Sourdough \n", emoji = "  ").toIdeaOrNull()!!
        assertEquals("Bake bread", idea.title)
        assertEquals("Sourdough", idea.description)
        assertEquals("✨", idea.emoji)
        assertTrue(idea.isCustom)
        assertTrue("new custom ideas are favorited", idea.isFavorite)
    }

    @Test
    fun roundTripsAnExistingIdea() {
        val original = testIdea(9, title = "Kite flying", isCustom = true, isFavorite = false)
        assertEquals(original, IdeaDraft.from(original).toIdeaOrNull())
    }
}
```

`EditIdeaViewModelTest.kt`:
```kotlin
package com.armen.spark.ui.edit

import com.armen.spark.FakeIdeaRepository
import com.armen.spark.MainDispatcherRule
import com.armen.spark.testIdea
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditIdeaViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule(StandardTestDispatcher())

    @Test
    fun doubleTapOnSaveStoresOneIdea() = runTest {
        val repo = FakeIdeaRepository()
        val vm = EditIdeaViewModel(repo, ideaId = null)
        vm.update { it.copy(title = "Paint a mug") }
        var savedCalls = 0

        vm.save { savedCalls++ }
        vm.save { savedCalls++ }
        advanceUntilIdle()

        assertEquals(1, repo.savedCustom.size)
        assertEquals(1, savedCalls)
    }

    @Test
    fun blankTitleIsNotSaved() = runTest {
        val repo = FakeIdeaRepository()
        val vm = EditIdeaViewModel(repo, ideaId = null)
        vm.save { }
        advanceUntilIdle()
        assertTrue(repo.savedCustom.isEmpty())
    }

    @Test
    fun loadsExistingCustomIdea() = runTest {
        val repo = FakeIdeaRepository(listOf(testIdea(3, title = "Juggle", isCustom = true)))
        val vm = EditIdeaViewModel(repo, ideaId = 3L)
        advanceUntilIdle()
        assertFalse(vm.isNew)
        assertEquals("Juggle", vm.draft.value.title)
        assertEquals(3L, vm.draft.value.id)
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew testDebugUnitTest --tests "com.armen.spark.ui.edit.*"`
Expected: compilation FAILS with `Unresolved reference 'IdeaDraft'`.

- [ ] **Step 3: Implement `IdeaDraft` and `EditIdeaViewModel`**

`IdeaDraft.kt`:
```kotlin
package com.armen.spark.ui.edit

import com.armen.spark.domain.CATEGORIES
import com.armen.spark.domain.Cost
import com.armen.spark.domain.Duration
import com.armen.spark.domain.Energy
import com.armen.spark.domain.Idea
import com.armen.spark.domain.People
import com.armen.spark.domain.Place

data class IdeaDraft(
    val id: Long = 0,
    val title: String = "",
    val description: String = "",
    val emoji: String = DEFAULT_EMOJI,
    val category: String = CATEGORIES.first(),
    val duration: Duration = Duration.HOUR,
    val cost: Cost = Cost.FREE,
    val place: Place = Place.ANYWHERE,
    val people: People = People.ANY,
    val energy: Energy = Energy.CHILL,
    val isFavorite: Boolean = true,
) {
    val canSave: Boolean get() = title.isNotBlank()

    fun toIdeaOrNull(): Idea? = if (!canSave) null else Idea(
        id = id,
        title = title.trim(),
        description = description.trim(),
        emoji = emoji.trim().ifEmpty { DEFAULT_EMOJI },
        category = category,
        duration = duration,
        cost = cost,
        place = place,
        people = people,
        energy = energy,
        isCustom = true,
        isFavorite = isFavorite,
    )

    companion object {
        const val DEFAULT_EMOJI = "✨"

        fun from(idea: Idea) = IdeaDraft(
            id = idea.id, title = idea.title, description = idea.description, emoji = idea.emoji,
            category = idea.category, duration = idea.duration, cost = idea.cost, place = idea.place,
            people = idea.people, energy = idea.energy, isFavorite = idea.isFavorite,
        )
    }
}
```

`EditIdeaViewModel.kt`:
```kotlin
package com.armen.spark.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.armen.spark.data.IdeaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EditIdeaViewModel(
    private val repository: IdeaRepository,
    ideaId: Long?,
) : ViewModel() {
    val isNew: Boolean = ideaId == null
    private val _draft = MutableStateFlow(IdeaDraft())
    val draft: StateFlow<IdeaDraft> = _draft.asStateFlow()
    private var saving = false

    init {
        if (ideaId != null) {
            viewModelScope.launch {
                repository.getIdea(ideaId)?.takeIf { it.isCustom }?.let { _draft.value = IdeaDraft.from(it) }
            }
        }
    }

    fun update(transform: (IdeaDraft) -> IdeaDraft) {
        _draft.update(transform)
    }

    fun save(onSaved: () -> Unit) {
        if (saving) return
        val idea = _draft.value.toIdeaOrNull() ?: return
        saving = true
        viewModelScope.launch {
            repository.saveCustom(idea)
            onSaved()
        }
    }
}
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew testDebugUnitTest --tests "com.armen.spark.ui.edit.*"`
Expected: PASS (6 tests).

- [ ] **Step 5: Write `EditIdeaScreen.kt`**

```kotlin
package com.armen.spark.ui.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.armen.spark.domain.CATEGORIES
import com.armen.spark.domain.Cost
import com.armen.spark.domain.Duration
import com.armen.spark.domain.Energy
import com.armen.spark.domain.People
import com.armen.spark.domain.Place
import com.armen.spark.ui.components.FilterRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditIdeaScreen(viewModel: EditIdeaViewModel, onBack: () -> Unit) {
    val draft by viewModel.draft.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().imePadding()) {
        TopAppBar(
            title = { Text(if (viewModel.isNew) "New idea" else "Edit idea") },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            },
            windowInsets = WindowInsets(0, 0, 0, 0),
        )
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = draft.emoji,
                    onValueChange = { v -> viewModel.update { it.copy(emoji = v) } },
                    label = { Text("Emoji") },
                    singleLine = true,
                    modifier = Modifier.width(96.dp),
                )
                OutlinedTextField(
                    value = draft.title,
                    onValueChange = { v -> viewModel.update { it.copy(title = v) } },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            OutlinedTextField(
                value = draft.description,
                onValueChange = { v -> viewModel.update { it.copy(description = v) } },
                label = { Text("Description (optional)") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            // Tag rows are single-choice: tapping the selected chip (null) keeps the current value.
            FilterRow("Category", CATEGORIES, draft.category, { it }) { v ->
                v?.let { c -> viewModel.update { it.copy(category = c) } }
            }
            FilterRow("Time", Duration.entries, draft.duration, Duration::label) { v ->
                v?.let { d -> viewModel.update { it.copy(duration = d) } }
            }
            FilterRow("Cost", Cost.entries, draft.cost, Cost::label) { v ->
                v?.let { c -> viewModel.update { it.copy(cost = c) } }
            }
            FilterRow("Place", Place.entries, draft.place, Place::label) { v ->
                v?.let { p -> viewModel.update { it.copy(place = p) } }
            }
            FilterRow("People", People.entries, draft.people, People::label) { v ->
                v?.let { p -> viewModel.update { it.copy(people = p) } }
            }
            FilterRow("Energy", Energy.entries, draft.energy, Energy::label) { v ->
                v?.let { e -> viewModel.update { it.copy(energy = e) } }
            }
        }
        Button(
            onClick = { viewModel.save(onSaved = onBack) },
            enabled = draft.canSave,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) { Text("Save") }
    }
}
```

- [ ] **Step 6: Write `SavedViewModel.kt` and `SavedScreen.kt`**

`SavedViewModel.kt`:
```kotlin
package com.armen.spark.ui.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.armen.spark.data.IdeaRepository
import com.armen.spark.domain.Idea
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SavedViewModel(private val repository: IdeaRepository) : ViewModel() {
    val ideas: StateFlow<List<Idea>> =
        repository.saved.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggleFavorite(id: Long) {
        viewModelScope.launch { repository.toggleFavorite(id) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repository.deleteCustom(id) }
    }
}
```

`SavedScreen.kt`:
```kotlin
package com.armen.spark.ui.saved

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.armen.spark.domain.Idea
import com.armen.spark.ui.components.EmptyState
import com.armen.spark.ui.components.IdeaListItem

@Composable
fun SavedScreen(viewModel: SavedViewModel, onAdd: () -> Unit, onEdit: (Long) -> Unit) {
    val ideas by viewModel.ideas.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<Idea?>(null) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Text("Saved", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(16.dp))
            if (ideas.isEmpty()) {
                EmptyState("💛", "No saved ideas yet", "Tap ♥ on a suggestion, or + to add your own.")
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
                    items(ideas, key = { it.id }) { idea ->
                        IdeaListItem(idea, trailing = {
                            Row {
                                if (idea.isCustom) {
                                    IconButton(onClick = { onEdit(idea.id) }) { Icon(Icons.Filled.Edit, "Edit") }
                                    IconButton(onClick = { pendingDelete = idea }) { Icon(Icons.Filled.Delete, "Delete") }
                                }
                                IconButton(onClick = { viewModel.toggleFavorite(idea.id) }) {
                                    if (idea.isFavorite) {
                                        Icon(Icons.Filled.Favorite, "Remove from saved", tint = MaterialTheme.colorScheme.primary)
                                    } else {
                                        Icon(Icons.Outlined.FavoriteBorder, "Save")
                                    }
                                }
                            }
                        })
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = onAdd,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) { Icon(Icons.Filled.Add, contentDescription = "Add your own idea") }
    }

    pendingDelete?.let { idea ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete “${idea.title}”?") },
            text = { Text("This also removes it from your history.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(idea.id)
                    pendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } },
        )
    }
}
```

- [ ] **Step 7: Wire the Saved and edit routes into `SparkNavHost.kt`**

Add these imports:
```kotlin
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.armen.spark.ui.edit.EditIdeaScreen
import com.armen.spark.ui.edit.EditIdeaViewModel
import com.armen.spark.ui.saved.SavedScreen
import com.armen.spark.ui.saved.SavedViewModel
```
Add, below the `Tab` enum:
```kotlin
private const val EDIT_ROUTE = "edit?id={id}"
```
Replace `composable(Tab.SAVED.route) { Text("Saved") }` with:
```kotlin
            composable(Tab.SAVED.route) {
                SavedScreen(
                    viewModel = viewModel { SavedViewModel(container.ideaRepository) },
                    onAdd = { navController.navigate("edit") },
                    onEdit = { id -> navController.navigate("edit?id=$id") },
                )
            }
            composable(
                route = EDIT_ROUTE,
                arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L }),
            ) { entry ->
                val id = entry.arguments?.getLong("id")?.takeIf { it > 0 }
                EditIdeaScreen(
                    viewModel = viewModel { EditIdeaViewModel(container.ideaRepository, id) },
                    onBack = { navController.popBackStack() },
                )
            }
```

- [ ] **Step 8: Build and test**

Run: `./gradlew testDebugUnitTest assembleDebug`
Expected: `BUILD SUCCESSFUL`; all tests pass.

- [ ] **Step 9: Commit**

```bash
git add -A
git commit -m "feat: add Saved tab and custom idea editor"
```

---

### Task 7: History tab

**Files:**
- Create: `app/src/main/java/com/armen/spark/ui/history/HistoryGrouping.kt`, `HistoryViewModel.kt`, `HistoryScreen.kt`
- Modify: `app/src/main/java/com/armen/spark/ui/nav/SparkNavHost.kt` (History route)
- Test: `app/src/test/java/com/armen/spark/ui/history/HistoryGroupingTest.kt`

**Interfaces:**
- Consumes: `IdeaRepository.history`, `HistoryItem`, `IdeaListItem`, `EmptyState`.
- Produces:
  - `data class HistorySection(label: String, items: List<HistoryItem>)`
  - `fun groupHistory(items: List<HistoryItem>, now: Long, zone: ZoneId, locale: Locale = Locale.getDefault()): List<HistorySection>`
  - `HistoryViewModel(repository)`, with `sections: StateFlow<List<HistorySection>>`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.armen.spark.ui.history

import com.armen.spark.domain.HistoryItem
import com.armen.spark.testIdea
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale

class HistoryGroupingTest {
    private val zone = ZoneId.of("Asia/Yerevan") // UTC+4: local midnight is 20:00 UTC

    private fun at(y: Int, m: Int, d: Int, h: Int, min: Int) =
        LocalDateTime.of(y, m, d, h, min).atZone(zone).toInstant().toEpochMilli()

    private fun item(id: Long, doneAt: Long) = HistoryItem(entryId = id, idea = testIdea(id), doneAt = doneAt)

    @Test
    fun labelsTodayYesterdayAndOlderDatesInLocalTime() {
        val now = at(2026, 10, 9, 0, 30)
        val items = listOf(
            item(1, at(2026, 10, 9, 0, 10)),   // 20 min ago, after local midnight
            item(2, at(2026, 10, 8, 23, 50)),  // 40 min ago, before local midnight
            item(3, at(2026, 10, 8, 9, 0)),
            item(4, at(2026, 10, 1, 12, 0)),
        )

        val sections = groupHistory(items, now, zone, Locale.ENGLISH)

        assertEquals(listOf("Today", "Yesterday", "Thu, 1 Oct 2026"), sections.map { it.label })
        assertEquals(listOf(1L), sections[0].items.map { it.entryId })
        assertEquals(listOf(2L, 3L), sections[1].items.map { it.entryId })
    }

    @Test
    fun emptyHistoryHasNoSections() {
        assertTrue(groupHistory(emptyList(), at(2026, 10, 9, 12, 0), zone, Locale.ENGLISH).isEmpty())
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.armen.spark.ui.history.HistoryGroupingTest"`
Expected: compilation FAILS with `Unresolved reference 'groupHistory'`.

- [ ] **Step 3: Implement the grouping** (`HistoryGrouping.kt`)

```kotlin
package com.armen.spark.ui.history

import com.armen.spark.domain.HistoryItem
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class HistorySection(val label: String, val items: List<HistoryItem>)

/** Groups newest-first [items] by local calendar day. */
fun groupHistory(
    items: List<HistoryItem>,
    now: Long,
    zone: ZoneId,
    locale: Locale = Locale.getDefault(),
): List<HistorySection> {
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val dateFormat = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", locale)
    return items
        .groupBy { Instant.ofEpochMilli(it.doneAt).atZone(zone).toLocalDate() }
        .map { (date, dayItems) ->
            val label = when (date) {
                today -> "Today"
                today.minusDays(1) -> "Yesterday"
                else -> date.format(dateFormat)
            }
            HistorySection(label, dayItems)
        }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.armen.spark.ui.history.HistoryGroupingTest"`
Expected: PASS (2 tests).

- [ ] **Step 5: Write the ViewModel and screen**

`HistoryViewModel.kt`:
```kotlin
package com.armen.spark.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.armen.spark.data.IdeaRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.ZoneId

class HistoryViewModel(repository: IdeaRepository) : ViewModel() {
    val sections: StateFlow<List<HistorySection>> = repository.history
        .map { groupHistory(it, System.currentTimeMillis(), ZoneId.systemDefault()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
```

`HistoryScreen.kt`:
```kotlin
package com.armen.spark.ui.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.armen.spark.ui.components.EmptyState
import com.armen.spark.ui.components.IdeaListItem
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun HistoryScreen(viewModel: HistoryViewModel) {
    val sections by viewModel.sections.collectAsStateWithLifecycle()
    val timeFormat = remember { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT) }

    Column(Modifier.fillMaxSize()) {
        Text("History", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(16.dp))
        if (sections.isEmpty()) {
            EmptyState("🌱", "Nothing done yet", "Go spark something!")
        } else {
            LazyColumn {
                sections.forEach { section ->
                    item(key = "header-${section.label}") {
                        Text(
                            section.label,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                        )
                    }
                    items(section.items, key = { it.entryId }) { entry ->
                        val time = Instant.ofEpochMilli(entry.doneAt).atZone(ZoneId.systemDefault()).format(timeFormat)
                        IdeaListItem(entry.idea, supporting = "${entry.idea.category} · $time")
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 6: Wire the History route**

In `SparkNavHost.kt`, add the imports `com.armen.spark.ui.history.HistoryScreen` and `com.armen.spark.ui.history.HistoryViewModel`, then replace `composable(Tab.HISTORY.route) { Text("History") }` with:
```kotlin
            composable(Tab.HISTORY.route) {
                HistoryScreen(viewModel { HistoryViewModel(container.ideaRepository) })
            }
```

- [ ] **Step 7: Build and test**

Run: `./gradlew testDebugUnitTest assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "feat: add History tab grouped by day"
```

---

### Task 8: Settings and the daily-idea notification

**Files:**
- Create: `app/src/main/java/com/armen/spark/notify/DailyIdeaScheduler.kt`, `DailyIdeaWorker.kt`
- Modify: `app/src/main/java/com/armen/spark/notify/Notifications.kt` (add `showDailyIdea`)
- Modify: `app/src/main/java/com/armen/spark/AppContainer.kt` (add `scheduler`)
- Create: `app/src/main/java/com/armen/spark/ui/settings/SettingsViewModel.kt`, `SettingsScreen.kt`
- Modify: `app/src/main/java/com/armen/spark/ui/nav/SparkNavHost.kt` (Settings route)
- Test: `app/src/test/java/com/armen/spark/notify/DailyIdeaSchedulerTest.kt`

**Interfaces:**
- Consumes: `SettingsRepository`, `Settings`, `IdeaRepository`, `IdeaPicker`, `Filters`, `Notifications.CHANNEL_ID/EXTRA_IDEA_ID/DAILY_NOTIFICATION_ID`, `MainActivity`, `R.drawable.ic_notification`.
- Produces:
  - `fun dailyInitialDelayMillis(time: LocalTime, now: ZonedDateTime): Long`
  - `class DailyIdeaScheduler(workManager: WorkManager)`, with `schedule(time: LocalTime)` and `cancel()`
  - `AppContainer.scheduler`
  - `SettingsViewModel(settings, scheduler)`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.armen.spark.notify

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class DailyIdeaSchedulerTest {
    private val hour = 60L * 60 * 1000
    private val yerevan = ZoneId.of("Asia/Yerevan")

    @Test
    fun laterTodayWhenTimeHasNotPassed() {
        val now = ZonedDateTime.of(2026, 10, 9, 8, 0, 0, 0, yerevan)
        assertEquals(1 * hour, dailyInitialDelayMillis(LocalTime.of(9, 0), now))
    }

    @Test
    fun tomorrowWhenTimeHasPassed() {
        val now = ZonedDateTime.of(2026, 10, 9, 10, 0, 0, 0, yerevan)
        assertEquals(23 * hour, dailyInitialDelayMillis(LocalTime.of(9, 0), now))
    }

    @Test
    fun tomorrowWhenTimeIsExactlyNow() {
        val now = ZonedDateTime.of(2026, 10, 9, 9, 0, 0, 0, yerevan)
        assertEquals(24 * hour, dailyInitialDelayMillis(LocalTime.of(9, 0), now))
    }

    @Test
    fun accountsForDaylightSavingJump() {
        // Clocks in Berlin spring forward at 02:00 on 29 March 2026, so 22:00 → 09:00 is 10 real hours.
        val now = ZonedDateTime.of(2026, 3, 28, 22, 0, 0, 0, ZoneId.of("Europe/Berlin"))
        assertEquals(10 * hour, dailyInitialDelayMillis(LocalTime.of(9, 0), now))
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.armen.spark.notify.DailyIdeaSchedulerTest"`
Expected: compilation FAILS with `Unresolved reference 'dailyInitialDelayMillis'`.

- [ ] **Step 3: Implement the scheduler** (`DailyIdeaScheduler.kt`)

```kotlin
package com.armen.spark.notify

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/** Milliseconds from [now] until the next occurrence of [time] (strictly in the future). */
fun dailyInitialDelayMillis(time: LocalTime, now: ZonedDateTime): Long {
    var next = now.toLocalDate().atTime(time).atZone(now.zone)
    if (!next.isAfter(now)) next = next.plusDays(1)
    return java.time.Duration.between(now, next).toMillis()
}

class DailyIdeaScheduler(private val workManager: WorkManager) {
    fun schedule(time: LocalTime) {
        val request = PeriodicWorkRequestBuilder<DailyIdeaWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(dailyInitialDelayMillis(time, ZonedDateTime.now()), TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, request)
    }

    fun cancel() {
        workManager.cancelUniqueWork(WORK_NAME)
    }

    private companion object {
        const val WORK_NAME = "daily_idea"
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.armen.spark.notify.DailyIdeaSchedulerTest"`
Expected: compilation FAILS only on the missing `DailyIdeaWorker`. Do Step 5, then rerun: PASS (4 tests).

- [ ] **Step 5: Implement the worker, notification posting and container wiring**

`DailyIdeaWorker.kt`:
```kotlin
package com.armen.spark.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.armen.spark.SparkApp
import com.armen.spark.domain.Filters

class DailyIdeaWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as SparkApp).container
        val repository = container.ideaRepository
        repository.seedIfEmpty()
        val idea = container.picker.pick(repository.allIdeas(), Filters(), repository.lastDoneMap())
            ?: return Result.success()
        Notifications.showDailyIdea(applicationContext, idea)
        return Result.success()
    }
}
```

Add to `Notifications.kt` (imports `android.Manifest`, `android.annotation.SuppressLint`, `android.app.PendingIntent`, `android.content.Intent`, `android.content.pm.PackageManager`, `android.os.Build`, `androidx.core.app.NotificationCompat`, `androidx.core.app.NotificationManagerCompat`, `androidx.core.content.ContextCompat`, `com.armen.spark.MainActivity`, `com.armen.spark.R`, `com.armen.spark.domain.Idea`):
```kotlin
    @SuppressLint("MissingPermission") // checked just below
    fun showDailyIdea(context: Context, idea: Idea) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_IDEA_ID, idea.id)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("${idea.emoji} ${idea.title}")
            .setContentText(idea.description)
            .setStyle(NotificationCompat.BigTextStyle().bigText(idea.description))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(DAILY_NOTIFICATION_ID, notification)
    }
```

In `AppContainer.kt`, add the imports `androidx.work.WorkManager` and `com.armen.spark.notify.DailyIdeaScheduler`, and this property:
```kotlin
    val scheduler = DailyIdeaScheduler(WorkManager.getInstance(appContext))
```

- [ ] **Step 6: Write `SettingsViewModel.kt` and `SettingsScreen.kt`**

`SettingsViewModel.kt`:
```kotlin
package com.armen.spark.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.armen.spark.data.Settings
import com.armen.spark.data.SettingsRepository
import com.armen.spark.notify.DailyIdeaScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime

class SettingsViewModel(
    private val settings: SettingsRepository,
    private val scheduler: DailyIdeaScheduler,
) : ViewModel() {
    val state: StateFlow<Settings> =
        settings.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Settings())

    fun setDailyEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settings.setDailyEnabled(enabled)
            if (enabled) scheduler.schedule(settings.settings.first().dailyTime) else scheduler.cancel()
        }
    }

    fun setDailyTime(time: LocalTime) {
        viewModelScope.launch {
            settings.setDailyTime(time)
            if (settings.settings.first().dailyEnabled) scheduler.schedule(time)
        }
    }
}
```

`SettingsScreen.kt`:
```kotlin
package com.armen.spark.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showTimePicker by remember { mutableStateOf(false) }
    var permissionDenied by remember { mutableStateOf(false) }
    val timeFormat = remember { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionDenied = !granted
        if (granted) viewModel.setDailyEnabled(true)
    }

    fun onToggle(enabled: Boolean) {
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        when {
            !enabled -> viewModel.setDailyEnabled(false)
            needsPermission -> permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            else -> {
                permissionDenied = false
                viewModel.setDailyEnabled(true)
            }
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(16.dp))
        ListItem(
            headlineContent = { Text("Daily idea") },
            supportingContent = { Text("One suggestion as a notification every day") },
            trailingContent = { Switch(checked = state.dailyEnabled, onCheckedChange = ::onToggle) },
        )
        if (permissionDenied) {
            Text(
                "Notifications are blocked. Allow them for Spark in system settings to get a daily idea.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        ListItem(
            headlineContent = { Text("Time") },
            supportingContent = { Text(state.dailyTime.format(timeFormat)) },
            modifier = Modifier.clickable(enabled = state.dailyEnabled) { showTimePicker = true },
        )
        HorizontalDivider()
        ListItem(
            headlineContent = { Text("About") },
            supportingContent = { Text("Spark 1.0 · your offline activity-ideas app") },
        )
    }

    if (showTimePicker) {
        val pickerState = rememberTimePickerState(
            initialHour = state.dailyTime.hour,
            initialMinute = state.dailyTime.minute,
            is24Hour = DateFormat.is24HourFormat(context),
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setDailyTime(LocalTime.of(pickerState.hour, pickerState.minute))
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } },
            text = { TimePicker(state = pickerState) },
        )
    }
}
```

- [ ] **Step 7: Wire the Settings route**

In `SparkNavHost.kt`, add the imports `com.armen.spark.ui.settings.SettingsScreen` and `com.armen.spark.ui.settings.SettingsViewModel`, then replace `composable(Tab.SETTINGS.route) { Text("Settings") }` with:
```kotlin
            composable(Tab.SETTINGS.route) {
                SettingsScreen(viewModel { SettingsViewModel(container.settingsRepository, container.scheduler) })
            }
```

- [ ] **Step 8: Build and test**

Run: `./gradlew testDebugUnitTest assembleDebug`
Expected: `BUILD SUCCESSFUL`; all tests pass.

- [ ] **Step 9: Commit**

```bash
git add -A
git commit -m "feat: add settings and daily idea notification"
```

---

### Task 9: Final verification

- [ ] **Step 1: Clean build and the full test suite**

Run: `./gradlew clean testDebugUnitTest assembleDebug`
Expected: `BUILD SUCCESSFUL`. Test count: IdeaPicker 10, SeedParser 3, Discover 7, IdeaDraft 3, EditIdeaViewModel 3, HistoryGrouping 2, Scheduler 4 = 32.

- [ ] **Step 2: Smoke-test on a device if one is connected**

Run: `"$LOCALAPPDATA/Android/Sdk/platform-tools/adb" devices`. If a device is listed:
`./gradlew installDebug`, then launch Spark and check:
- Spark me shows a card.
- ♥ adds the idea to Saved.
- Done adds it to History.
- + creates an idea that appears in Saved.

If no device is connected, record that the smoke test was skipped.

- [ ] **Step 3: Commit any fixes**

```bash
git add -A
git commit -m "chore: final verification fixes"
```
