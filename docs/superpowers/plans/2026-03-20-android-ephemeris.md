# Android Ephemeris Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build an Android app that displays astrological charts calculated via Swiss Ephemeris with scrubbable date/time/location controls.

**Architecture:** Single Gradle module with layered packages — `ephemeris/` (JNI wrapper), `chart/` (pure Kotlin domain), `ui/` (Jetpack Compose). Data flows from user controls through ViewModel to ChartCalculator to Canvas rendering.

**Tech Stack:** Kotlin, Jetpack Compose, Swiss Ephemeris C via JNI/NDK, CMake, JUnit 4

**Spec:** `docs/superpowers/specs/2026-03-20-android-ephemeris-design.md`

---

## File Structure

```
android-ephemeris/
├── build.gradle.kts                          # Project-level Gradle config
├── settings.gradle.kts                       # Project settings
├── gradle.properties                         # Gradle properties (compose compiler, etc.)
├── app/
│   ├── build.gradle.kts                      # App module: Compose, NDK/CMake, dependencies
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml
│   │   │   ├── assets/ephe/                  # Swiss Ephemeris data files (.se1)
│   │   │   ├── cpp/
│   │   │   │   ├── CMakeLists.txt            # CMake build for Swiss Ephemeris + JNI wrapper
│   │   │   │   ├── swisseph/                 # Swiss Ephemeris C source (downloaded)
│   │   │   │   └── swisseph_wrapper.c        # JNI bridge functions
│   │   │   ├── java/com/skothr/ephemeris/
│   │   │   │   ├── EphemerisApp.kt           # Application class (ephemeris init)
│   │   │   │   ├── MainActivity.kt           # Single activity
│   │   │   │   ├── ephemeris/
│   │   │   │   │   ├── SwissEphemeris.kt     # Kotlin JNI API (singleton, mutex)
│   │   │   │   │   └── models/
│   │   │   │   │       ├── CelestialPosition.kt
│   │   │   │   │       └── HouseData.kt
│   │   │   │   ├── chart/
│   │   │   │   │   ├── ChartCalculator.kt
│   │   │   │   │   ├── AspectCalculator.kt
│   │   │   │   │   ├── models/
│   │   │   │   │   │   ├── CelestialBody.kt
│   │   │   │   │   │   ├── ZodiacSign.kt
│   │   │   │   │   │   ├── HouseSystem.kt
│   │   │   │   │   │   ├── Aspect.kt
│   │   │   │   │   │   ├── AspectResult.kt
│   │   │   │   │   │   └── ChartData.kt
│   │   │   │   │   └── config/
│   │   │   │   │       ├── AspectConfig.kt
│   │   │   │   │       └── BodyConfig.kt
│   │   │   │   └── ui/
│   │   │   │       ├── MainScreen.kt
│   │   │   │       ├── ChartViewModel.kt
│   │   │   │       ├── chart/
│   │   │   │       │   ├── ChartWheel.kt
│   │   │   │       │   ├── WheelRenderer.kt
│   │   │   │       │   └── ChartColors.kt
│   │   │   │       ├── controls/
│   │   │   │       │   ├── DateTimeControls.kt
│   │   │   │       │   └── LocationControls.kt
│   │   │   │       └── theme/
│   │   │   │           ├── Theme.kt
│   │   │   │           ├── Color.kt
│   │   │   │           └── Type.kt
│   │   │   └── res/
│   │   │       ├── font/
│   │   │       │   └── astro_glyphs.ttf      # Astrological symbol font
│   │   │       ├── raw/
│   │   │       │   └── cities15000.tsv        # GeoNames city database
│   │   │       ├── values/
│   │   │       │   ├── strings.xml
│   │   │       │   └── themes.xml
│   │   │       └── drawable/                  # Launcher icons etc.
│   │   ├── test/java/com/skothr/ephemeris/    # JVM unit tests
│   │   │   ├── chart/
│   │   │   │   ├── models/
│   │   │   │   │   ├── ZodiacSignTest.kt
│   │   │   │   │   ├── AspectTest.kt
│   │   │   │   │   └── CelestialBodyTest.kt
│   │   │   │   ├── AspectCalculatorTest.kt
│   │   │   │   └── ChartCalculatorTest.kt
│   │   │   └── ui/chart/
│   │   │       └── WheelRendererTest.kt
│   │   └── androidTest/java/com/skothr/ephemeris/  # Instrumented tests
│   │       └── ephemeris/
│   │           └── SwissEphemerisTest.kt
```

---

### Task 1: Android Project Scaffold

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/skothr/ephemeris/MainActivity.kt`
- Create: `app/src/main/java/com/skothr/ephemeris/EphemerisApp.kt`
- Create: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/values/themes.xml`

- [ ] **Step 1: Create Gradle wrapper**

Download Gradle wrapper (8.7+) so the project can build:

```bash
cd /home/ai/ai-projects/android-ephemeris
gradle wrapper --gradle-version 8.7
```

If `gradle` is not installed, manually create the wrapper files. The key files are `gradlew`, `gradlew.bat`, and `gradle/wrapper/gradle-wrapper.properties`.

- [ ] **Step 2: Create settings.gradle.kts**

```kotlin
// settings.gradle.kts
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

rootProject.name = "android-ephemeris"
include(":app")
```

- [ ] **Step 3: Create project-level build.gradle.kts**

```kotlin
// build.gradle.kts
plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.1.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0" apply false
}
```

- [ ] **Step 4: Create gradle.properties**

```properties
# gradle.properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
android.nonTransitiveRClass=true
```

- [ ] **Step 5: Create app/build.gradle.kts**

```kotlin
// app/build.gradle.kts
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.skothr.ephemeris"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.skothr.ephemeris"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // AndroidX
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
```

- [ ] **Step 6: Create AndroidManifest.xml**

```xml
<!-- app/src/main/AndroidManifest.xml -->
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <application
        android:name=".EphemerisApp"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.Ephemeris">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.Ephemeris">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

- [ ] **Step 7: Create minimal Application and Activity**

`EphemerisApp.kt`:
```kotlin
package com.skothr.ephemeris

import android.app.Application

class EphemerisApp : Application()
```

`MainActivity.kt`:
```kotlin
package com.skothr.ephemeris

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Text("Android Ephemeris")
        }
    }
}
```

- [ ] **Step 8: Create resource files**

`strings.xml`:
```xml
<resources>
    <string name="app_name">Ephemeris</string>
</resources>
```

`themes.xml`:
```xml
<resources>
    <style name="Theme.Ephemeris" parent="android:Theme.Material.Light.NoActionBar" />
</resources>
```

- [ ] **Step 9: Create placeholder CMakeLists.txt**

This is needed so the build doesn't fail (referenced in `build.gradle.kts`). Swiss Ephemeris source will be added in Task 3.

```cmake
# app/src/main/cpp/CMakeLists.txt
cmake_minimum_required(VERSION 3.22.1)
project("ephemeris")

# Placeholder — Swiss Ephemeris will be added in Task 3
```

- [ ] **Step 10: Verify build compiles**

```bash
cd /home/ai/ai-projects/android-ephemeris
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 11: Commit**

```bash
git add -A
git commit -m "feat: scaffold Android project with Compose, NDK/CMake config"
```

---

### Task 2: Chart Domain Models

**Files:**
- Create: `app/src/main/java/com/skothr/ephemeris/chart/models/CelestialBody.kt`
- Create: `app/src/main/java/com/skothr/ephemeris/chart/models/ZodiacSign.kt`
- Create: `app/src/main/java/com/skothr/ephemeris/chart/models/HouseSystem.kt`
- Create: `app/src/main/java/com/skothr/ephemeris/chart/models/Aspect.kt`
- Create: `app/src/main/java/com/skothr/ephemeris/chart/models/AspectResult.kt`
- Create: `app/src/main/java/com/skothr/ephemeris/chart/models/ChartData.kt`
- Create: `app/src/main/java/com/skothr/ephemeris/chart/config/AspectConfig.kt`
- Create: `app/src/main/java/com/skothr/ephemeris/chart/config/BodyConfig.kt`
- Create: `app/src/main/java/com/skothr/ephemeris/ephemeris/models/CelestialPosition.kt`
- Create: `app/src/main/java/com/skothr/ephemeris/ephemeris/models/HouseData.kt`
- Test: `app/src/test/java/com/skothr/ephemeris/chart/models/ZodiacSignTest.kt`
- Test: `app/src/test/java/com/skothr/ephemeris/chart/models/AspectTest.kt`
- Test: `app/src/test/java/com/skothr/ephemeris/chart/models/CelestialBodyTest.kt`

- [ ] **Step 1: Write ZodiacSign tests**

```kotlin
// app/src/test/java/com/skothr/ephemeris/chart/models/ZodiacSignTest.kt
package com.skothr.ephemeris.chart.models

import org.junit.Assert.*
import org.junit.Test

class ZodiacSignTest {

    @Test
    fun `fromLongitude returns Aries for 0 degrees`() {
        assertEquals(ZodiacSign.ARIES, ZodiacSign.fromLongitude(0.0))
    }

    @Test
    fun `fromLongitude returns Aries for 29 degrees`() {
        assertEquals(ZodiacSign.ARIES, ZodiacSign.fromLongitude(29.99))
    }

    @Test
    fun `fromLongitude returns Taurus for 30 degrees`() {
        assertEquals(ZodiacSign.TAURUS, ZodiacSign.fromLongitude(30.0))
    }

    @Test
    fun `fromLongitude returns Libra for 185 degrees`() {
        assertEquals(ZodiacSign.LIBRA, ZodiacSign.fromLongitude(185.0))
    }

    @Test
    fun `fromLongitude returns Pisces for 359 degrees`() {
        assertEquals(ZodiacSign.PISCES, ZodiacSign.fromLongitude(359.0))
    }

    @Test
    fun `fromLongitude normalizes values above 360`() {
        assertEquals(ZodiacSign.ARIES, ZodiacSign.fromLongitude(360.0))
        assertEquals(ZodiacSign.TAURUS, ZodiacSign.fromLongitude(390.0))
    }

    @Test
    fun `fromLongitude normalizes negative values`() {
        assertEquals(ZodiacSign.PISCES, ZodiacSign.fromLongitude(-10.0))
    }

    @Test
    fun `sign degree returns position within sign`() {
        assertEquals(5.0, ZodiacSign.signDegree(185.0), 0.001)
        assertEquals(0.0, ZodiacSign.signDegree(0.0), 0.001)
        assertEquals(15.0, ZodiacSign.signDegree(45.0), 0.001)
    }

    @Test
    fun `fire signs have correct element`() {
        assertEquals(Element.FIRE, ZodiacSign.ARIES.element)
        assertEquals(Element.FIRE, ZodiacSign.LEO.element)
        assertEquals(Element.FIRE, ZodiacSign.SAGITTARIUS.element)
    }

    @Test
    fun `cardinal signs have correct modality`() {
        assertEquals(Modality.CARDINAL, ZodiacSign.ARIES.modality)
        assertEquals(Modality.CARDINAL, ZodiacSign.CANCER.modality)
        assertEquals(Modality.CARDINAL, ZodiacSign.LIBRA.modality)
        assertEquals(Modality.CARDINAL, ZodiacSign.CAPRICORN.modality)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew :app:testDebugUnitTest --tests "com.skothr.ephemeris.chart.models.ZodiacSignTest" -q
```

Expected: Compilation error — ZodiacSign not found.

- [ ] **Step 3: Implement ZodiacSign, Element, Modality**

```kotlin
// app/src/main/java/com/skothr/ephemeris/chart/models/ZodiacSign.kt
package com.skothr.ephemeris.chart.models

enum class Element { FIRE, EARTH, AIR, WATER }
enum class Modality { CARDINAL, FIXED, MUTABLE }

enum class ZodiacSign(
    val displayName: String,
    val symbol: String,
    val startDegree: Double,
    val element: Element,
    val modality: Modality,
) {
    ARIES("Aries", "\u2648", 0.0, Element.FIRE, Modality.CARDINAL),
    TAURUS("Taurus", "\u2649", 30.0, Element.EARTH, Modality.FIXED),
    GEMINI("Gemini", "\u264A", 60.0, Element.AIR, Modality.MUTABLE),
    CANCER("Cancer", "\u264B", 90.0, Element.WATER, Modality.CARDINAL),
    LEO("Leo", "\u264C", 120.0, Element.FIRE, Modality.FIXED),
    VIRGO("Virgo", "\u264D", 150.0, Element.EARTH, Modality.MUTABLE),
    LIBRA("Libra", "\u264E", 180.0, Element.AIR, Modality.CARDINAL),
    SCORPIO("Scorpio", "\u264F", 210.0, Element.WATER, Modality.FIXED),
    SAGITTARIUS("Sagittarius", "\u2650", 240.0, Element.FIRE, Modality.MUTABLE),
    CAPRICORN("Capricorn", "\u2651", 270.0, Element.EARTH, Modality.CARDINAL),
    AQUARIUS("Aquarius", "\u2652", 300.0, Element.AIR, Modality.FIXED),
    PISCES("Pisces", "\u2653", 330.0, Element.WATER, Modality.MUTABLE);

    companion object {
        fun fromLongitude(longitude: Double): ZodiacSign {
            val normalized = ((longitude % 360.0) + 360.0) % 360.0
            val index = (normalized / 30.0).toInt().coerceIn(0, 11)
            return entries[index]
        }

        fun signDegree(longitude: Double): Double {
            val normalized = ((longitude % 360.0) + 360.0) % 360.0
            return normalized % 30.0
        }
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
./gradlew :app:testDebugUnitTest --tests "com.skothr.ephemeris.chart.models.ZodiacSignTest" -q
```

Expected: All tests PASS.

- [ ] **Step 5: Write CelestialBody tests**

```kotlin
// app/src/test/java/com/skothr/ephemeris/chart/models/CelestialBodyTest.kt
package com.skothr.ephemeris.chart.models

import org.junit.Assert.*
import org.junit.Test

class CelestialBodyTest {

    @Test
    fun `all 13 bodies defined`() {
        assertEquals(13, CelestialBody.entries.size)
    }

    @Test
    fun `each body has unique swissEphId`() {
        val ids = CelestialBody.entries.map { it.swissEphId }
        assertEquals(ids.size, ids.distinct().size)
    }

    @Test
    fun `sun has correct swiss ephemeris id 0`() {
        assertEquals(0, CelestialBody.SUN.swissEphId)
    }

    @Test
    fun `pluto has correct swiss ephemeris id 9`() {
        assertEquals(9, CelestialBody.PLUTO.swissEphId)
    }

    @Test
    fun `each body has non-empty symbol`() {
        CelestialBody.entries.forEach {
            assertTrue("${it.name} should have a symbol", it.symbol.isNotEmpty())
        }
    }
}
```

- [ ] **Step 6: Run test to verify it fails, then implement CelestialBody**

```kotlin
// app/src/main/java/com/skothr/ephemeris/chart/models/CelestialBody.kt
package com.skothr.ephemeris.chart.models

enum class CelestialBody(
    val displayName: String,
    val symbol: String,
    val swissEphId: Int,
) {
    SUN("Sun", "\u2609", 0),
    MOON("Moon", "\u263D", 1),
    MERCURY("Mercury", "\u263F", 2),
    VENUS("Venus", "\u2640", 3),
    MARS("Mars", "\u2642", 4),
    JUPITER("Jupiter", "\u2643", 5),
    SATURN("Saturn", "\u2644", 6),
    URANUS("Uranus", "\u2645", 7),
    NEPTUNE("Neptune", "\u2646", 8),
    PLUTO("Pluto", "\u2647", 9),
    CHIRON("Chiron", "\u26B7", 15),       // SE_CHIRON
    NORTH_NODE("North Node", "\u260A", 11), // SE_TRUE_NODE
    LILITH("Lilith", "\u26B8", 12);        // SE_MEAN_APOG
}
```

- [ ] **Step 7: Run CelestialBody tests**

```bash
./gradlew :app:testDebugUnitTest --tests "com.skothr.ephemeris.chart.models.CelestialBodyTest" -q
```

Expected: All tests PASS.

- [ ] **Step 8: Write Aspect tests**

```kotlin
// app/src/test/java/com/skothr/ephemeris/chart/models/AspectTest.kt
package com.skothr.ephemeris.chart.models

import org.junit.Assert.*
import org.junit.Test

class AspectTest {

    @Test
    fun `conjunction has angle 0 and orb 8`() {
        assertEquals(0.0, Aspect.CONJUNCTION.angle, 0.001)
        assertEquals(8.0, Aspect.CONJUNCTION.defaultOrb, 0.001)
    }

    @Test
    fun `opposition has angle 180 and orb 8`() {
        assertEquals(180.0, Aspect.OPPOSITION.angle, 0.001)
        assertEquals(8.0, Aspect.OPPOSITION.defaultOrb, 0.001)
    }

    @Test
    fun `minor aspects have smaller default orbs`() {
        assertTrue(Aspect.SEMI_SEXTILE.defaultOrb <= 3.0)
        assertTrue(Aspect.SEMI_SQUARE.defaultOrb <= 3.0)
        assertTrue(Aspect.SESQUIQUADRATE.defaultOrb <= 3.0)
        assertTrue(Aspect.QUINCUNX.defaultOrb <= 3.0)
    }

    @Test
    fun `major aspects identified correctly`() {
        assertTrue(Aspect.CONJUNCTION.isMajor)
        assertTrue(Aspect.OPPOSITION.isMajor)
        assertTrue(Aspect.TRINE.isMajor)
        assertTrue(Aspect.SQUARE.isMajor)
        assertTrue(Aspect.SEXTILE.isMajor)
        assertFalse(Aspect.QUINCUNX.isMajor)
        assertFalse(Aspect.SEMI_SEXTILE.isMajor)
    }

    @Test
    fun `all 9 aspects defined`() {
        assertEquals(9, Aspect.entries.size)
    }
}
```

- [ ] **Step 9: Run test to verify it fails, then implement Aspect**

```kotlin
// app/src/main/java/com/skothr/ephemeris/chart/models/Aspect.kt
package com.skothr.ephemeris.chart.models

enum class Aspect(
    val displayName: String,
    val symbol: String,
    val angle: Double,
    val defaultOrb: Double,
    val isMajor: Boolean,
) {
    CONJUNCTION("Conjunction", "\u260C", 0.0, 8.0, true),
    OPPOSITION("Opposition", "\u260D", 180.0, 8.0, true),
    TRINE("Trine", "\u25B3", 120.0, 8.0, true),
    SQUARE("Square", "\u25A1", 90.0, 7.0, true),
    SEXTILE("Sextile", "\u26B9", 60.0, 6.0, true),
    QUINCUNX("Quincunx", "Qx", 150.0, 3.0, false),
    SEMI_SEXTILE("Semi-Sextile", "SSx", 30.0, 2.0, false),
    SEMI_SQUARE("Semi-Square", "SSq", 45.0, 2.0, false),
    SESQUIQUADRATE("Sesquiquadrate", "Sq", 135.0, 2.0, false);
}
```

- [ ] **Step 10: Run Aspect tests**

```bash
./gradlew :app:testDebugUnitTest --tests "com.skothr.ephemeris.chart.models.AspectTest" -q
```

Expected: All tests PASS.

- [ ] **Step 11: Implement remaining models (no tests needed — pure data classes)**

`CelestialPosition.kt`:
```kotlin
package com.skothr.ephemeris.ephemeris.models

data class CelestialPosition(
    val longitude: Double,
    val latitude: Double,
    val distance: Double,
    val speed: Double,
) {
    val isRetrograde: Boolean get() = speed < 0.0
}
```

`HouseData.kt`:
```kotlin
package com.skothr.ephemeris.ephemeris.models

data class HouseData(
    val cusps: List<Double>,   // 12 house cusps (indices 0-11)
    val ascendant: Double,
    val midheaven: Double,
    val descendant: Double,
    val imumCoeli: Double,
)
```

`HouseSystem.kt`:
```kotlin
package com.skothr.ephemeris.chart.models

enum class HouseSystem(val displayName: String, val swissEphCode: Char) {
    PLACIDUS("Placidus", 'P'),
    WHOLE_SIGN("Whole Sign", 'W'),
    EQUAL("Equal", 'E'),
    KOCH("Koch", 'K');
}
```

`AspectResult.kt`:
```kotlin
package com.skothr.ephemeris.chart.models

data class AspectResult(
    val body1: CelestialBody,
    val body2: CelestialBody,
    val aspect: Aspect,
    val exactAngle: Double,
    val orb: Double,
    val isApplying: Boolean,
)
```

`ChartData.kt`:
```kotlin
package com.skothr.ephemeris.chart.models

import com.skothr.ephemeris.ephemeris.models.CelestialPosition
import com.skothr.ephemeris.ephemeris.models.HouseData
import java.time.LocalDateTime

data class Location(val latitude: Double, val longitude: Double)

data class ChartData(
    val dateTime: LocalDateTime,
    val location: Location,
    val positions: Map<CelestialBody, CelestialPosition>,
    val houseData: HouseData,
    val aspects: List<AspectResult>,
    val houseSystem: HouseSystem,
)
```

`AspectConfig.kt`:
```kotlin
package com.skothr.ephemeris.chart.config

import com.skothr.ephemeris.chart.models.Aspect

data class AspectConfig(
    val enabledAspects: Map<Aspect, Double> = Aspect.entries.associateWith { it.defaultOrb },
)
```

`BodyConfig.kt`:
```kotlin
package com.skothr.ephemeris.chart.config

import com.skothr.ephemeris.chart.models.CelestialBody

data class BodyConfig(
    val enabledBodies: Set<CelestialBody> = CelestialBody.entries.toSet(),
)
```

- [ ] **Step 12: Run all model tests**

```bash
./gradlew :app:testDebugUnitTest -q
```

Expected: All tests PASS.

- [ ] **Step 13: Commit**

```bash
git add -A
git commit -m "feat: add chart domain models with tests"
```

---

### Task 3: Swiss Ephemeris JNI Integration

**Files:**
- Create: `app/src/main/cpp/swisseph/` (Swiss Ephemeris C source)
- Create: `app/src/main/cpp/swisseph_wrapper.c`
- Modify: `app/src/main/cpp/CMakeLists.txt`
- Create: `app/src/main/java/com/skothr/ephemeris/ephemeris/SwissEphemeris.kt`
- Create: `app/src/main/assets/ephe/` (ephemeris data files)
- Modify: `app/src/main/java/com/skothr/ephemeris/EphemerisApp.kt`
- Test: `app/src/androidTest/java/com/skothr/ephemeris/ephemeris/SwissEphemerisTest.kt`

**Prerequisites:** Download Swiss Ephemeris source from https://www.astro.com/ftp/swisseph/src/ and data files from https://www.astro.com/ftp/swisseph/ephe/. Required files:
- Source: All `.c` and `.h` files from the src archive
- Data: `sepl18.se1`, `semo18.se1`, `seas18.se1` (covers 1800-2400 CE)

- [ ] **Step 1: Download and place Swiss Ephemeris C source**

```bash
cd /home/ai/ai-projects/android-ephemeris/app/src/main/cpp
mkdir -p swisseph
cd swisseph
# Download and extract Swiss Ephemeris source
# The key files needed: swephexp.h, sweph.h, swejpl.h, swedate.h, sweodef.h,
# swehouse.h, swephlib.h, swecl.c, swedate.c, swehouse.c, swejpl.c,
# swemmoon.c, swemplan.c, sweph.c, swephlib.c, swepcalc.c (if present)
```

Verify the key header `swephexp.h` exists after extraction.

- [ ] **Step 2: Download and place ephemeris data files**

```bash
mkdir -p /home/ai/ai-projects/android-ephemeris/app/src/main/assets/ephe
cd /home/ai/ai-projects/android-ephemeris/app/src/main/assets/ephe
# Place: sepl18.se1, semo18.se1, seas18.se1
```

- [ ] **Step 3: Write CMakeLists.txt**

```cmake
# app/src/main/cpp/CMakeLists.txt
cmake_minimum_required(VERSION 3.22.1)
project("ephemeris")

# Collect Swiss Ephemeris C source files
file(GLOB SWISSEPH_SOURCES "swisseph/*.c")

add_library(swisseph_jni SHARED
    swisseph_wrapper.c
    ${SWISSEPH_SOURCES}
)

target_include_directories(swisseph_jni PRIVATE
    swisseph
)

target_link_libraries(swisseph_jni
    android
    log
)
```

- [ ] **Step 4: Write JNI wrapper**

```c
// app/src/main/cpp/swisseph_wrapper.c
#include <jni.h>
#include <string.h>
#include <android/log.h>
#include "swisseph/swephexp.h"

#define LOG_TAG "SwissEphemeris"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

JNIEXPORT void JNICALL
Java_com_skothr_ephemeris_ephemeris_SwissEphemeris_nativeInit(
    JNIEnv *env, jobject thiz, jstring ephe_path) {
    const char *path = (*env)->GetStringUTFChars(env, ephe_path, NULL);
    swe_set_ephe_path((char *)path);
    (*env)->ReleaseStringUTFChars(env, ephe_path, path);
}

JNIEXPORT void JNICALL
Java_com_skothr_ephemeris_ephemeris_SwissEphemeris_nativeClose(
    JNIEnv *env, jobject thiz) {
    swe_close();
}

JNIEXPORT jdouble JNICALL
Java_com_skothr_ephemeris_ephemeris_SwissEphemeris_nativeJulianDay(
    JNIEnv *env, jobject thiz,
    jint year, jint month, jint day,
    jdouble hour) {
    return swe_julday(year, month, day, hour, SE_GREG_CAL);
}

JNIEXPORT jdoubleArray JNICALL
Java_com_skothr_ephemeris_ephemeris_SwissEphemeris_nativeCalculateBody(
    JNIEnv *env, jobject thiz,
    jdouble jd, jint body_id) {
    double result[6];
    char err[256];
    int flags = SEFLG_SPEED | SEFLG_SWIEPH;

    int rc = swe_calc_ut(jd, body_id, flags, result, err);
    if (rc < 0) {
        LOGE("swe_calc_ut error for body %d: %s", body_id, err);
        return NULL;
    }

    jdoubleArray jresult = (*env)->NewDoubleArray(env, 4);
    // result: [longitude, latitude, distance, speed_in_longitude]
    double out[4] = { result[0], result[1], result[2], result[3] };
    (*env)->SetDoubleArrayRegion(env, jresult, 0, 4, out);
    return jresult;
}

JNIEXPORT jdoubleArray JNICALL
Java_com_skothr_ephemeris_ephemeris_SwissEphemeris_nativeCalculateHouses(
    JNIEnv *env, jobject thiz,
    jdouble jd, jdouble lat, jdouble lon, jchar house_system) {
    double cusps[13];  // 1-12 (index 0 unused by swe_houses)
    double ascmc[10];
    char sys = (char)house_system;

    int rc = swe_houses(jd, lat, lon, sys, cusps, ascmc);
    if (rc < 0) {
        LOGE("swe_houses error for system %c", sys);
        return NULL;
    }

    // Pack: 12 cusps + ASC + MC + DSC(=ASC+180) + IC(=MC+180)
    double out[16];
    for (int i = 0; i < 12; i++) {
        out[i] = cusps[i + 1];  // swe_houses cusps are 1-indexed
    }
    out[12] = ascmc[0]; // ASC
    out[13] = ascmc[1]; // MC
    double dsc = ascmc[0] + 180.0;
    if (dsc >= 360.0) dsc -= 360.0;
    out[14] = dsc;      // DSC
    double ic = ascmc[1] + 180.0;
    if (ic >= 360.0) ic -= 360.0;
    out[15] = ic;       // IC

    jdoubleArray jresult = (*env)->NewDoubleArray(env, 16);
    (*env)->SetDoubleArrayRegion(env, jresult, 0, 16, out);
    return jresult;
}
```

- [ ] **Step 5: Write SwissEphemeris.kt**

```kotlin
// app/src/main/java/com/skothr/ephemeris/ephemeris/SwissEphemeris.kt
package com.skothr.ephemeris.ephemeris

import com.skothr.ephemeris.chart.models.CelestialBody
import com.skothr.ephemeris.chart.models.HouseSystem
import com.skothr.ephemeris.ephemeris.models.CelestialPosition
import com.skothr.ephemeris.ephemeris.models.HouseData
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDateTime

class SwissEphemeris {

    companion object {
        init {
            System.loadLibrary("swisseph_jni")
        }
    }

    private val mutex = Mutex()
    private var initialized = false

    suspend fun init(ephePath: String) = mutex.withLock {
        nativeInit(ephePath)
        initialized = true
    }

    suspend fun close() = mutex.withLock {
        nativeClose()
        initialized = false
    }

    suspend fun julianDay(dateTime: LocalDateTime): Double = mutex.withLock {
        check(initialized) { "SwissEphemeris not initialized" }
        val hour = dateTime.hour + dateTime.minute / 60.0 + dateTime.second / 3600.0
        nativeJulianDay(dateTime.year, dateTime.monthValue, dateTime.dayOfMonth, hour)
    }

    suspend fun calculateBody(julianDay: Double, body: CelestialBody): CelestialPosition =
        mutex.withLock {
            check(initialized) { "SwissEphemeris not initialized" }
            val result = nativeCalculateBody(julianDay, body.swissEphId)
                ?: throw RuntimeException("Failed to calculate position for ${body.name}")
            CelestialPosition(
                longitude = result[0],
                latitude = result[1],
                distance = result[2],
                speed = result[3],
            )
        }

    suspend fun calculateHouses(
        julianDay: Double,
        latitude: Double,
        longitude: Double,
        system: HouseSystem,
    ): HouseData = mutex.withLock {
        check(initialized) { "SwissEphemeris not initialized" }
        val result = nativeCalculateHouses(julianDay, latitude, longitude, system.swissEphCode)
            ?: throw RuntimeException("Failed to calculate houses for ${system.name}")
        HouseData(
            cusps = result.take(12),
            ascendant = result[12],
            midheaven = result[13],
            descendant = result[14],
            imumCoeli = result[15],
        )
    }

    // JNI native methods
    private external fun nativeInit(ephePath: String)
    private external fun nativeClose()
    private external fun nativeJulianDay(year: Int, month: Int, day: Int, hour: Double): Double
    private external fun nativeCalculateBody(julianDay: Double, bodyId: Int): DoubleArray?
    private external fun nativeCalculateHouses(
        julianDay: Double, lat: Double, lon: Double, houseSystem: Char
    ): DoubleArray?
}
```

- [ ] **Step 6: Update EphemerisApp to extract assets and init**

```kotlin
// app/src/main/java/com/skothr/ephemeris/EphemerisApp.kt
package com.skothr.ephemeris

import android.app.Application
import com.skothr.ephemeris.ephemeris.SwissEphemeris
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class EphemerisApp : Application() {

    val swissEphemeris = SwissEphemeris()
    val ephemerisReady = CompletableDeferred<Unit>()

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.IO).launch {
            val ephePath = extractEphemerisData()
            swissEphemeris.init(ephePath)
            ephemerisReady.complete(Unit)
        }
    }

    private fun extractEphemerisData(): String {
        val epheDir = File(filesDir, "ephe")
        if (!epheDir.exists()) {
            epheDir.mkdirs()
            assets.list("ephe")?.forEach { filename ->
                assets.open("ephe/$filename").use { input ->
                    File(epheDir, filename).outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }
        return epheDir.absolutePath
    }
}
```

- [ ] **Step 7: Build and verify native compilation**

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL (native library compiles).

- [ ] **Step 8: Write instrumented tests for SwissEphemeris**

```kotlin
// app/src/androidTest/java/com/skothr/ephemeris/ephemeris/SwissEphemerisTest.kt
package com.skothr.ephemeris.ephemeris

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.skothr.ephemeris.chart.models.CelestialBody
import com.skothr.ephemeris.chart.models.HouseSystem
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class SwissEphemerisTest {

    private lateinit var swe: SwissEphemeris
    private lateinit var context: Context

    @Before
    fun setup() = runTest {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        val epheDir = File(context.filesDir, "ephe")
        if (!epheDir.exists()) {
            epheDir.mkdirs()
            context.assets.list("ephe")?.forEach { filename ->
                context.assets.open("ephe/$filename").use { input ->
                    File(epheDir, filename).outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }
        swe = SwissEphemeris()
        swe.init(epheDir.absolutePath)
    }

    @After
    fun teardown() = runTest {
        swe.close()
    }

    @Test
    fun julianDay_j2000Epoch_returnsCorrectValue() = runTest {
        // J2000.0 = 2000-01-01 12:00:00 TT -> JD 2451545.0
        val dt = LocalDateTime.of(2000, 1, 1, 12, 0, 0)
        val jd = swe.julianDay(dt)
        assertEquals(2451545.0, jd, 0.001)
    }

    @Test
    fun julianDay_unixEpoch_returnsCorrectValue() = runTest {
        // 1970-01-01 00:00:00 -> JD 2440587.5
        val dt = LocalDateTime.of(1970, 1, 1, 0, 0, 0)
        val jd = swe.julianDay(dt)
        assertEquals(2440587.5, jd, 0.001)
    }

    @Test
    fun calculateBody_sunAtJ2000_longitudeNear280() = runTest {
        // Sun at J2000.0 is approximately at 280.5° ecliptic longitude (Capricorn)
        val jd = swe.julianDay(LocalDateTime.of(2000, 1, 1, 12, 0, 0))
        val pos = swe.calculateBody(jd, CelestialBody.SUN)
        assertTrue("Sun longitude should be near 280°, got ${pos.longitude}",
            pos.longitude in 279.0..282.0)
    }

    @Test
    fun calculateBody_allBodiesReturnValidLongitudes() = runTest {
        val jd = swe.julianDay(LocalDateTime.of(2024, 6, 15, 12, 0, 0))
        CelestialBody.entries.forEach { body ->
            val pos = swe.calculateBody(jd, body)
            assertTrue("${body.name} longitude should be 0-360, got ${pos.longitude}",
                pos.longitude in 0.0..360.0)
        }
    }

    @Test
    fun calculateBody_sunHasPositiveSpeed() = runTest {
        val jd = swe.julianDay(LocalDateTime.of(2024, 6, 15, 12, 0, 0))
        val pos = swe.calculateBody(jd, CelestialBody.SUN)
        assertTrue("Sun should always move direct", pos.speed > 0.0)
        assertFalse(pos.isRetrograde)
    }

    @Test
    fun calculateHouses_placidus_returns12Cusps() = runTest {
        val jd = swe.julianDay(LocalDateTime.of(2024, 6, 15, 12, 0, 0))
        // New York City
        val houses = swe.calculateHouses(jd, 40.7128, -74.0060, HouseSystem.PLACIDUS)
        assertEquals(12, houses.cusps.size)
        houses.cusps.forEach { cusp ->
            assertTrue("Cusp should be 0-360, got $cusp", cusp in 0.0..360.0)
        }
    }

    @Test
    fun calculateHouses_allSystems_returnValidData() = runTest {
        val jd = swe.julianDay(LocalDateTime.of(2024, 6, 15, 12, 0, 0))
        HouseSystem.entries.forEach { system ->
            val houses = swe.calculateHouses(jd, 40.7128, -74.0060, system)
            assertEquals("${system.name} should have 12 cusps", 12, houses.cusps.size)
            assertTrue("${system.name} ASC should be 0-360", houses.ascendant in 0.0..360.0)
            assertTrue("${system.name} MC should be 0-360", houses.midheaven in 0.0..360.0)
        }
    }

    @Test
    fun calculateHouses_dscIsOppositeAsc() = runTest {
        val jd = swe.julianDay(LocalDateTime.of(2024, 6, 15, 12, 0, 0))
        val houses = swe.calculateHouses(jd, 40.7128, -74.0060, HouseSystem.PLACIDUS)
        val expectedDsc = (houses.ascendant + 180.0) % 360.0
        assertEquals(expectedDsc, houses.descendant, 0.01)
    }
}
```

- [ ] **Step 9: Run instrumented tests on connected device/emulator**

```bash
./gradlew connectedAndroidTest --tests "com.skothr.ephemeris.ephemeris.SwissEphemerisTest"
```

Expected: All tests PASS. (Requires a connected device or running emulator.)

- [ ] **Step 10: Commit**

```bash
git add -A
git commit -m "feat: integrate Swiss Ephemeris via JNI with instrumented tests"
```

---

### Task 4: AspectCalculator (TDD)

**Files:**
- Create: `app/src/main/java/com/skothr/ephemeris/chart/AspectCalculator.kt`
- Test: `app/src/test/java/com/skothr/ephemeris/chart/AspectCalculatorTest.kt`

- [ ] **Step 1: Write AspectCalculator tests**

```kotlin
// app/src/test/java/com/skothr/ephemeris/chart/AspectCalculatorTest.kt
package com.skothr.ephemeris.chart

import com.skothr.ephemeris.chart.config.AspectConfig
import com.skothr.ephemeris.chart.models.*
import com.skothr.ephemeris.ephemeris.models.CelestialPosition
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AspectCalculatorTest {

    private lateinit var calculator: AspectCalculator
    private val defaultConfig = AspectConfig()

    @Before
    fun setup() {
        calculator = AspectCalculator(defaultConfig)
    }

    private fun pos(longitude: Double, speed: Double = 1.0) =
        CelestialPosition(longitude, 0.0, 1.0, speed)

    @Test
    fun `exact conjunction detected`() {
        val positions = mapOf(
            CelestialBody.SUN to pos(100.0),
            CelestialBody.MOON to pos(100.0),
        )
        val aspects = calculator.calculate(positions)
        assertEquals(1, aspects.size)
        assertEquals(Aspect.CONJUNCTION, aspects[0].aspect)
        assertEquals(0.0, aspects[0].orb, 0.001)
    }

    @Test
    fun `conjunction within orb detected`() {
        val positions = mapOf(
            CelestialBody.SUN to pos(100.0),
            CelestialBody.MOON to pos(107.5),
        )
        val aspects = calculator.calculate(positions)
        assertEquals(1, aspects.size)
        assertEquals(Aspect.CONJUNCTION, aspects[0].aspect)
        assertEquals(7.5, aspects[0].orb, 0.001)
    }

    @Test
    fun `conjunction outside orb not detected`() {
        val positions = mapOf(
            CelestialBody.SUN to pos(100.0),
            CelestialBody.MOON to pos(109.0),
        )
        val aspects = calculator.calculate(positions)
        assertTrue(aspects.none { it.aspect == Aspect.CONJUNCTION })
    }

    @Test
    fun `opposition detected across 0-360 boundary`() {
        val positions = mapOf(
            CelestialBody.SUN to pos(5.0),
            CelestialBody.MOON to pos(185.0),
        )
        val aspects = calculator.calculate(positions)
        assertTrue(aspects.any { it.aspect == Aspect.OPPOSITION })
    }

    @Test
    fun `trine detected`() {
        val positions = mapOf(
            CelestialBody.SUN to pos(10.0),
            CelestialBody.MOON to pos(130.0),
        )
        val aspects = calculator.calculate(positions)
        assertTrue(aspects.any { it.aspect == Aspect.TRINE })
    }

    @Test
    fun `square detected`() {
        val positions = mapOf(
            CelestialBody.SUN to pos(10.0),
            CelestialBody.MOON to pos(100.0),
        )
        val aspects = calculator.calculate(positions)
        assertTrue(aspects.any { it.aspect == Aspect.SQUARE })
    }

    @Test
    fun `sextile detected`() {
        val positions = mapOf(
            CelestialBody.SUN to pos(10.0),
            CelestialBody.MOON to pos(70.0),
        )
        val aspects = calculator.calculate(positions)
        assertTrue(aspects.any { it.aspect == Aspect.SEXTILE })
    }

    @Test
    fun `applying aspect when faster body approaches`() {
        // Moon at 95° moving faster than Sun at 100° — Moon is approaching conjunction
        val positions = mapOf(
            CelestialBody.SUN to pos(100.0, speed = 1.0),
            CelestialBody.MOON to pos(95.0, speed = 13.0),
        )
        val aspects = calculator.calculate(positions)
        val conjunction = aspects.first { it.aspect == Aspect.CONJUNCTION }
        assertTrue("Should be applying", conjunction.isApplying)
    }

    @Test
    fun `separating aspect when faster body moves away`() {
        // Moon at 105° moving faster than Sun at 100° — Moon is separating from conjunction
        val positions = mapOf(
            CelestialBody.SUN to pos(100.0, speed = 1.0),
            CelestialBody.MOON to pos(105.0, speed = 13.0),
        )
        val aspects = calculator.calculate(positions)
        val conjunction = aspects.first { it.aspect == Aspect.CONJUNCTION }
        assertFalse("Should be separating", conjunction.isApplying)
    }

    @Test
    fun `orb exactly at limit is included`() {
        // Conjunction orb = 8.0, so exactly 8.0 apart should be included
        val positions = mapOf(
            CelestialBody.SUN to pos(100.0),
            CelestialBody.MOON to pos(108.0),
        )
        val aspects = calculator.calculate(positions)
        assertTrue(aspects.any { it.aspect == Aspect.CONJUNCTION })
    }

    @Test
    fun `multiple aspects between multiple bodies`() {
        val positions = mapOf(
            CelestialBody.SUN to pos(0.0),
            CelestialBody.MOON to pos(90.0),
            CelestialBody.MARS to pos(120.0),
        )
        val aspects = calculator.calculate(positions)
        // Sun-Moon: square (90°)
        // Sun-Mars: trine (120°)
        // Moon-Mars: semi-sextile (30°)
        assertTrue(aspects.any { it.body1 == CelestialBody.SUN && it.body2 == CelestialBody.MOON && it.aspect == Aspect.SQUARE })
        assertTrue(aspects.any { it.body1 == CelestialBody.SUN && it.body2 == CelestialBody.MARS && it.aspect == Aspect.TRINE })
        assertTrue(aspects.any { it.body1 == CelestialBody.MOON && it.body2 == CelestialBody.MARS && it.aspect == Aspect.SEMI_SEXTILE })
    }

    @Test
    fun `disabled aspect not detected`() {
        val config = AspectConfig(
            enabledAspects = mapOf(Aspect.CONJUNCTION to 8.0)  // Only conjunction enabled
        )
        val calc = AspectCalculator(config)
        val positions = mapOf(
            CelestialBody.SUN to pos(0.0),
            CelestialBody.MOON to pos(90.0),  // Would be square
        )
        val aspects = calc.calculate(positions)
        assertTrue(aspects.isEmpty())
    }

    @Test
    fun `aspects sorted by orb ascending`() {
        val positions = mapOf(
            CelestialBody.SUN to pos(0.0),
            CelestialBody.MOON to pos(93.0),    // square, orb 3
            CelestialBody.MARS to pos(125.0),    // trine, orb 5
        )
        val aspects = calculator.calculate(positions)
        for (i in 0 until aspects.size - 1) {
            assertTrue(aspects[i].orb <= aspects[i + 1].orb)
        }
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
./gradlew :app:testDebugUnitTest --tests "com.skothr.ephemeris.chart.AspectCalculatorTest" -q
```

Expected: Compilation error — AspectCalculator not found.

- [ ] **Step 3: Implement AspectCalculator**

```kotlin
// app/src/main/java/com/skothr/ephemeris/chart/AspectCalculator.kt
package com.skothr.ephemeris.chart

import com.skothr.ephemeris.chart.config.AspectConfig
import com.skothr.ephemeris.chart.models.Aspect
import com.skothr.ephemeris.chart.models.AspectResult
import com.skothr.ephemeris.chart.models.CelestialBody
import com.skothr.ephemeris.ephemeris.models.CelestialPosition
import kotlin.math.abs
import kotlin.math.min

class AspectCalculator(private val config: AspectConfig) {

    fun calculate(positions: Map<CelestialBody, CelestialPosition>): List<AspectResult> {
        val bodies = positions.keys.toList()
        val results = mutableListOf<AspectResult>()

        for (i in bodies.indices) {
            for (j in i + 1 until bodies.size) {
                val body1 = bodies[i]
                val body2 = bodies[j]
                val pos1 = positions[body1]!!
                val pos2 = positions[body2]!!

                val angularDiff = angularDistance(pos1.longitude, pos2.longitude)

                for ((aspect, orb) in config.enabledAspects) {
                    val aspectOrb = abs(angularDiff - aspect.angle)
                    if (aspectOrb <= orb) {
                        val applying = isApplying(pos1, pos2, aspect)
                        results.add(
                            AspectResult(
                                body1 = body1,
                                body2 = body2,
                                aspect = aspect,
                                exactAngle = angularDiff,
                                orb = aspectOrb,
                                isApplying = applying,
                            )
                        )
                        break // One aspect per body pair (closest match)
                    }
                }
            }
        }

        return results.sortedBy { it.orb }
    }

    private fun angularDistance(lon1: Double, lon2: Double): Double {
        val diff = abs(lon1 - lon2) % 360.0
        return min(diff, 360.0 - diff)
    }

    private fun isApplying(
        pos1: CelestialPosition,
        pos2: CelestialPosition,
        aspect: Aspect,
    ): Boolean {
        // The faster body is "applying" if moving toward the exact aspect angle
        val currentDist = angularDistance(pos1.longitude, pos2.longitude)
        // Simulate a tiny step forward
        val futureDist = angularDistance(
            pos1.longitude + pos1.speed * 0.01,
            pos2.longitude + pos2.speed * 0.01,
        )
        val futureOrb = abs(futureDist - aspect.angle)
        val currentOrb = abs(currentDist - aspect.angle)
        return futureOrb < currentOrb
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
./gradlew :app:testDebugUnitTest --tests "com.skothr.ephemeris.chart.AspectCalculatorTest" -q
```

Expected: All tests PASS.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: add AspectCalculator with full TDD test suite"
```

---

### Task 5: ChartCalculator

**Files:**
- Create: `app/src/main/java/com/skothr/ephemeris/chart/ChartCalculator.kt`
- Test: `app/src/test/java/com/skothr/ephemeris/chart/ChartCalculatorTest.kt`

- [ ] **Step 1: Write ChartCalculator tests (using a fake SwissEphemeris)**

Since `SwissEphemeris` requires JNI, we define an interface and test against a fake.

First, extract an interface from SwissEphemeris:

```kotlin
// app/src/main/java/com/skothr/ephemeris/ephemeris/EphemerisProvider.kt
package com.skothr.ephemeris.ephemeris

import com.skothr.ephemeris.chart.models.CelestialBody
import com.skothr.ephemeris.chart.models.HouseSystem
import com.skothr.ephemeris.ephemeris.models.CelestialPosition
import com.skothr.ephemeris.ephemeris.models.HouseData
import java.time.LocalDateTime

interface EphemerisProvider {
    suspend fun julianDay(dateTime: LocalDateTime): Double
    suspend fun calculateBody(julianDay: Double, body: CelestialBody): CelestialPosition
    suspend fun calculateHouses(
        julianDay: Double, latitude: Double, longitude: Double, system: HouseSystem
    ): HouseData
}
```

Update `SwissEphemeris.kt` to implement `EphemerisProvider`.

Then write tests:

```kotlin
// app/src/test/java/com/skothr/ephemeris/chart/ChartCalculatorTest.kt
package com.skothr.ephemeris.chart

import com.skothr.ephemeris.chart.config.AspectConfig
import com.skothr.ephemeris.chart.config.BodyConfig
import com.skothr.ephemeris.chart.models.*
import com.skothr.ephemeris.ephemeris.EphemerisProvider
import com.skothr.ephemeris.ephemeris.models.CelestialPosition
import com.skothr.ephemeris.ephemeris.models.HouseData
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime

class ChartCalculatorTest {

    private val fakeHouseData = HouseData(
        cusps = List(12) { it * 30.0 },
        ascendant = 0.0,
        midheaven = 270.0,
        descendant = 180.0,
        imumCoeli = 90.0,
    )

    private val fakePositions = mapOf(
        CelestialBody.SUN to CelestialPosition(280.0, 0.0, 1.0, 1.0),
        CelestialBody.MOON to CelestialPosition(45.0, 5.0, 0.002, 13.0),
        CelestialBody.MERCURY to CelestialPosition(275.0, -1.0, 0.8, 1.5),
    )

    private val fakeEphemeris = object : EphemerisProvider {
        override suspend fun julianDay(dateTime: LocalDateTime): Double = 2451545.0
        override suspend fun calculateBody(julianDay: Double, body: CelestialBody): CelestialPosition =
            fakePositions[body] ?: CelestialPosition(0.0, 0.0, 1.0, 1.0)
        override suspend fun calculateHouses(
            julianDay: Double, latitude: Double, longitude: Double, system: HouseSystem
        ): HouseData = fakeHouseData
    }

    private val bodyConfig = BodyConfig(enabledBodies = setOf(
        CelestialBody.SUN, CelestialBody.MOON, CelestialBody.MERCURY
    ))

    @Test
    fun `calculate returns ChartData with all enabled bodies`() = runTest {
        val calc = ChartCalculator(fakeEphemeris, AspectConfig(), bodyConfig)
        val result = calc.calculate(
            LocalDateTime.of(2024, 6, 15, 12, 0, 0),
            Location(40.7128, -74.006),
            HouseSystem.PLACIDUS,
        )
        assertEquals(3, result.positions.size)
        assertTrue(result.positions.containsKey(CelestialBody.SUN))
        assertTrue(result.positions.containsKey(CelestialBody.MOON))
        assertTrue(result.positions.containsKey(CelestialBody.MERCURY))
    }

    @Test
    fun `calculate includes house data`() = runTest {
        val calc = ChartCalculator(fakeEphemeris, AspectConfig(), bodyConfig)
        val result = calc.calculate(
            LocalDateTime.of(2024, 6, 15, 12, 0, 0),
            Location(40.7128, -74.006),
            HouseSystem.PLACIDUS,
        )
        assertEquals(12, result.houseData.cusps.size)
        assertEquals(0.0, result.houseData.ascendant, 0.001)
    }

    @Test
    fun `calculate includes aspects`() = runTest {
        val calc = ChartCalculator(fakeEphemeris, AspectConfig(), bodyConfig)
        val result = calc.calculate(
            LocalDateTime.of(2024, 6, 15, 12, 0, 0),
            Location(40.7128, -74.006),
            HouseSystem.PLACIDUS,
        )
        // Sun at 280, Mercury at 275 -> 5° apart -> conjunction (orb 8)
        assertTrue(result.aspects.any {
            it.aspect == Aspect.CONJUNCTION &&
            it.body1 == CelestialBody.SUN && it.body2 == CelestialBody.MERCURY
        })
    }

    @Test
    fun `calculate stores correct metadata`() = runTest {
        val calc = ChartCalculator(fakeEphemeris, AspectConfig(), bodyConfig)
        val dt = LocalDateTime.of(2024, 6, 15, 12, 0, 0)
        val loc = Location(40.7128, -74.006)
        val result = calc.calculate(dt, loc, HouseSystem.PLACIDUS)
        assertEquals(dt, result.dateTime)
        assertEquals(loc, result.location)
        assertEquals(HouseSystem.PLACIDUS, result.houseSystem)
    }

    @Test
    fun `disabled bodies are excluded`() = runTest {
        val smallConfig = BodyConfig(enabledBodies = setOf(CelestialBody.SUN))
        val calc = ChartCalculator(fakeEphemeris, AspectConfig(), smallConfig)
        val result = calc.calculate(
            LocalDateTime.of(2024, 6, 15, 12, 0, 0),
            Location(40.7128, -74.006),
            HouseSystem.PLACIDUS,
        )
        assertEquals(1, result.positions.size)
        assertTrue(result.positions.containsKey(CelestialBody.SUN))
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
./gradlew :app:testDebugUnitTest --tests "com.skothr.ephemeris.chart.ChartCalculatorTest" -q
```

Expected: Compilation error — ChartCalculator not found.

- [ ] **Step 3: Create EphemerisProvider interface and update SwissEphemeris**

Create `EphemerisProvider.kt` as shown in Step 1.

Add `implements EphemerisProvider` to `SwissEphemeris` class declaration:

```kotlin
class SwissEphemeris : EphemerisProvider {
```

- [ ] **Step 4: Implement ChartCalculator**

```kotlin
// app/src/main/java/com/skothr/ephemeris/chart/ChartCalculator.kt
package com.skothr.ephemeris.chart

import com.skothr.ephemeris.chart.config.AspectConfig
import com.skothr.ephemeris.chart.config.BodyConfig
import com.skothr.ephemeris.chart.models.ChartData
import com.skothr.ephemeris.chart.models.HouseSystem
import com.skothr.ephemeris.chart.models.Location
import com.skothr.ephemeris.ephemeris.EphemerisProvider
import com.skothr.ephemeris.ephemeris.models.CelestialPosition
import com.skothr.ephemeris.chart.models.CelestialBody
import java.time.LocalDateTime

class ChartCalculator(
    private val ephemeris: EphemerisProvider,
    private val aspectConfig: AspectConfig,
    private val bodyConfig: BodyConfig,
) {
    private val aspectCalculator = AspectCalculator(aspectConfig)

    suspend fun calculate(
        dateTime: LocalDateTime,
        location: Location,
        houseSystem: HouseSystem,
    ): ChartData {
        val jd = ephemeris.julianDay(dateTime)

        val positions = mutableMapOf<CelestialBody, CelestialPosition>()
        for (body in bodyConfig.enabledBodies) {
            positions[body] = ephemeris.calculateBody(jd, body)
        }

        val houseData = ephemeris.calculateHouses(
            jd, location.latitude, location.longitude, houseSystem
        )

        val aspects = aspectCalculator.calculate(positions)

        return ChartData(
            dateTime = dateTime,
            location = location,
            positions = positions,
            houseData = houseData,
            aspects = aspects,
            houseSystem = houseSystem,
        )
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

```bash
./gradlew :app:testDebugUnitTest --tests "com.skothr.ephemeris.chart.ChartCalculatorTest" -q
```

Expected: All tests PASS.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "feat: add ChartCalculator with EphemerisProvider interface and tests"
```

---

### Task 6: WheelRenderer Math (TDD)

**Files:**
- Create: `app/src/main/java/com/skothr/ephemeris/ui/chart/WheelRenderer.kt`
- Test: `app/src/test/java/com/skothr/ephemeris/ui/chart/WheelRendererTest.kt`

- [ ] **Step 1: Write WheelRenderer math tests**

```kotlin
// app/src/test/java/com/skothr/ephemeris/ui/chart/WheelRendererTest.kt
package com.skothr.ephemeris.ui.chart

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sqrt

class WheelRendererTest {

    @Test
    fun `longitudeToAngle converts 0 degrees longitude to top of chart (ASC left)`() {
        // With ASC at 0°, the 0° longitude point should be at the left (9 o'clock = PI)
        val angle = WheelMath.longitudeToAngle(0.0, ascendant = 0.0)
        assertEquals(PI, angle, 0.001)
    }

    @Test
    fun `longitudeToAngle rotates based on ascendant`() {
        // If ASC is at 90° (Cancer rising), then 90° longitude should be at the left
        val angle = WheelMath.longitudeToAngle(90.0, ascendant = 90.0)
        assertEquals(PI, angle, 0.001)
    }

    @Test
    fun `longitudeToAngle 180 degrees from ASC is at right`() {
        val angle = WheelMath.longitudeToAngle(180.0, ascendant = 0.0)
        assertEquals(0.0, angle, 0.001)
    }

    @Test
    fun `pointOnCircle at angle 0 is at right`() {
        val (x, y) = WheelMath.pointOnCircle(100f, 100f, 50f, 0.0)
        assertEquals(150f, x, 0.1f)
        assertEquals(100f, y, 0.1f)
    }

    @Test
    fun `pointOnCircle at angle PI_2 is at bottom`() {
        val (x, y) = WheelMath.pointOnCircle(100f, 100f, 50f, PI / 2.0)
        assertEquals(100f, x, 0.1f)
        assertEquals(150f, y, 0.1f)
    }

    @Test
    fun `pointOnCircle at angle PI is at left`() {
        val (x, y) = WheelMath.pointOnCircle(100f, 100f, 50f, PI)
        assertEquals(50f, x, 0.1f)
        assertEquals(100f, y, 0.1f)
    }

    @Test
    fun `ringRadii are properly nested`() {
        val radii = WheelMath.calculateRingRadii(500f)
        assertTrue("Zodiac outer > zodiac inner", radii.zodiacOuter > radii.zodiacInner)
        assertTrue("Zodiac inner > house outer", radii.zodiacInner > radii.houseOuter)
        assertTrue("House outer > house inner", radii.houseOuter > radii.houseInner)
        assertTrue("House inner > body ring", radii.houseInner > radii.bodyRing)
        assertTrue("Body ring > aspect inner", radii.bodyRing > radii.aspectInner)
        assertTrue("All radii positive", radii.aspectInner > 0f)
    }

    @Test
    fun `resolveCollisions separates overlapping bodies`() {
        // Two bodies at exactly the same angle
        val inputAngles = listOf(1.0, 1.0, 3.0)
        val minSeparation = 0.2
        val resolved = WheelMath.resolveCollisions(inputAngles, minSeparation)
        for (i in resolved.indices) {
            for (j in i + 1 until resolved.size) {
                val diff = kotlin.math.abs(resolved[i] - resolved[j])
                assertTrue(
                    "Bodies at $i and $j should be at least $minSeparation apart, got $diff",
                    diff >= minSeparation - 0.001
                )
            }
        }
    }

    @Test
    fun `resolveCollisions preserves order`() {
        val inputAngles = listOf(1.0, 1.05, 3.0)
        val resolved = WheelMath.resolveCollisions(inputAngles, 0.2)
        // Order should be preserved
        for (i in 0 until resolved.size - 1) {
            assertTrue(resolved[i] <= resolved[i + 1])
        }
    }

    @Test
    fun `resolveCollisions no change when already separated`() {
        val inputAngles = listOf(1.0, 2.0, 3.0)
        val resolved = WheelMath.resolveCollisions(inputAngles, 0.2)
        assertEquals(1.0, resolved[0], 0.001)
        assertEquals(2.0, resolved[1], 0.001)
        assertEquals(3.0, resolved[2], 0.001)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
./gradlew :app:testDebugUnitTest --tests "com.skothr.ephemeris.ui.chart.WheelRendererTest" -q
```

Expected: Compilation error — WheelMath not found.

- [ ] **Step 3: Implement WheelMath and RingRadii**

```kotlin
// app/src/main/java/com/skothr/ephemeris/ui/chart/WheelRenderer.kt
package com.skothr.ephemeris.ui.chart

import kotlin.math.*

data class RingRadii(
    val zodiacOuter: Float,
    val zodiacInner: Float,
    val houseOuter: Float,
    val houseInner: Float,
    val bodyRing: Float,
    val aspectInner: Float,
)

object WheelMath {

    /**
     * Convert ecliptic longitude to canvas angle in radians.
     * ASC is placed at the left (PI radians). Zodiac increases counter-clockwise.
     */
    fun longitudeToAngle(longitude: Double, ascendant: Double): Double {
        val adjusted = ascendant - longitude
        val radians = Math.toRadians(adjusted)
        return ((radians % (2 * PI)) + 2 * PI) % (2 * PI)
    }

    /**
     * Get (x, y) coordinates for a point on a circle.
     */
    fun pointOnCircle(cx: Float, cy: Float, radius: Float, angleRadians: Double): Pair<Float, Float> {
        val x = cx + radius * cos(angleRadians).toFloat()
        val y = cy + radius * sin(angleRadians).toFloat()
        return Pair(x, y)
    }

    /**
     * Calculate ring radii based on available size.
     * Returns nested radii from outermost (zodiac) to innermost (aspect area).
     */
    fun calculateRingRadii(availableRadius: Float): RingRadii {
        return RingRadii(
            zodiacOuter = availableRadius * 0.95f,
            zodiacInner = availableRadius * 0.82f,
            houseOuter = availableRadius * 0.82f,
            houseInner = availableRadius * 0.68f,
            bodyRing = availableRadius * 0.58f,
            aspectInner = availableRadius * 0.10f,
        )
    }

    /**
     * Resolve visual collisions between body glyphs by spreading overlapping angles.
     * Input: sorted list of angles. Output: adjusted angles with minimum separation.
     */
    fun resolveCollisions(sortedAngles: List<Double>, minSeparation: Double): List<Double> {
        if (sortedAngles.size <= 1) return sortedAngles
        val result = sortedAngles.toMutableList()

        // Iterative relaxation
        repeat(10) {
            for (i in 0 until result.size - 1) {
                val diff = result[i + 1] - result[i]
                if (diff < minSeparation) {
                    val shift = (minSeparation - diff) / 2.0
                    result[i] -= shift
                    result[i + 1] += shift
                }
            }
        }
        return result
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
./gradlew :app:testDebugUnitTest --tests "com.skothr.ephemeris.ui.chart.WheelRendererTest" -q
```

Expected: All tests PASS.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: add WheelMath rendering calculations with TDD tests"
```

---

### Task 7: Chart Wheel Compose Canvas

**Files:**
- Create: `app/src/main/java/com/skothr/ephemeris/ui/chart/ChartColors.kt`
- Create: `app/src/main/java/com/skothr/ephemeris/ui/chart/ChartWheel.kt`
- Modify: `app/src/main/java/com/skothr/ephemeris/ui/chart/WheelRenderer.kt` (add drawing functions)

- [ ] **Step 1: Create ChartColors**

```kotlin
// app/src/main/java/com/skothr/ephemeris/ui/chart/ChartColors.kt
package com.skothr.ephemeris.ui.chart

import androidx.compose.ui.graphics.Color
import com.skothr.ephemeris.chart.models.Aspect
import com.skothr.ephemeris.chart.models.Element

object ChartColors {
    // Element colors for zodiac ring segments
    val elementColor = mapOf(
        Element.FIRE to Color(0xFFE53935),    // Red
        Element.EARTH to Color(0xFF43A047),   // Green
        Element.AIR to Color(0xFFFFB300),     // Amber
        Element.WATER to Color(0xFF1E88E5),   // Blue
    )

    // Aspect line colors
    val aspectColor = mapOf(
        Aspect.CONJUNCTION to Color(0xFF66BB6A),      // Green
        Aspect.OPPOSITION to Color(0xFFEF5350),       // Red
        Aspect.TRINE to Color(0xFF42A5F5),            // Blue
        Aspect.SQUARE to Color(0xFFEF5350),           // Red
        Aspect.SEXTILE to Color(0xFF42A5F5),          // Blue
        Aspect.QUINCUNX to Color(0xFFAB47BC),         // Purple
        Aspect.SEMI_SEXTILE to Color(0xFF78909C),     // Grey
        Aspect.SEMI_SQUARE to Color(0xFFFF7043),      // Orange
        Aspect.SESQUIQUADRATE to Color(0xFFFF7043),   // Orange
    )

    val wheelBackground = Color(0xFF1A1A2E)
    val ringStroke = Color(0xFF4A4A6A)
    val bodyGlyph = Color(0xFFE0E0E0)
    val angleMarker = Color(0xFFFFD54F)
    val houseNumber = Color(0xFF9E9E9E)
    val signGlyph = Color(0xFFFFFFFF)
}
```

- [ ] **Step 2: Add drawing functions to WheelRenderer.kt**

Add these functions to the existing `WheelRenderer.kt` file (below `WheelMath`):

```kotlin
// Add to WheelRenderer.kt — these are the Canvas drawing functions.
// They use Android Compose Canvas APIs (DrawScope).

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.nativeCanvas
import android.graphics.Paint
import android.graphics.Typeface
import com.skothr.ephemeris.chart.models.*
import com.skothr.ephemeris.ephemeris.models.HouseData

object WheelDrawing {

    fun DrawScope.drawZodiacRing(radii: RingRadii, cx: Float, cy: Float, ascendant: Double) {
        val signs = ZodiacSign.entries
        for (sign in signs) {
            val startAngle = WheelMath.longitudeToAngle(sign.startDegree, ascendant)
            val sweepAngle = -Math.toRadians(30.0)  // 30° per sign, counter-clockwise
            val color = ChartColors.elementColor[sign.element] ?: Color.Gray

            // Draw arc segment
            drawArc(
                color = color.copy(alpha = 0.3f),
                startAngle = Math.toDegrees(startAngle).toFloat(),
                sweepAngle = Math.toDegrees(sweepAngle).toFloat(),
                useCenter = true,
                topLeft = Offset(cx - radii.zodiacOuter, cy - radii.zodiacOuter),
                size = Size(radii.zodiacOuter * 2, radii.zodiacOuter * 2),
            )

            // Draw sign glyph at midpoint
            val midLon = sign.startDegree + 15.0
            val midAngle = WheelMath.longitudeToAngle(midLon, ascendant)
            val midRadius = (radii.zodiacOuter + radii.zodiacInner) / 2f
            val (gx, gy) = WheelMath.pointOnCircle(cx, cy, midRadius, midAngle)

            drawContext.canvas.nativeCanvas.drawText(
                sign.symbol,
                gx, gy + 8f,
                Paint().apply {
                    textSize = (radii.zodiacOuter - radii.zodiacInner) * 0.5f
                    textAlign = Paint.Align.CENTER
                    this.color = android.graphics.Color.WHITE
                    isAntiAlias = true
                }
            )
        }

        // Ring border circles
        drawCircle(ChartColors.ringStroke, radii.zodiacOuter, Offset(cx, cy), style = Stroke(1.5f))
        drawCircle(ChartColors.ringStroke, radii.zodiacInner, Offset(cx, cy), style = Stroke(1.5f))
    }

    fun DrawScope.drawHouseRing(
        radii: RingRadii, cx: Float, cy: Float,
        houseData: HouseData, ascendant: Double,
    ) {
        // Draw house cusp lines
        for (i in houseData.cusps.indices) {
            val angle = WheelMath.longitudeToAngle(houseData.cusps[i], ascendant)
            val (ox, oy) = WheelMath.pointOnCircle(cx, cy, radii.zodiacInner, angle)
            val (ix, iy) = WheelMath.pointOnCircle(cx, cy, radii.houseInner, angle)
            drawLine(ChartColors.ringStroke, Offset(ox, oy), Offset(ix, iy), strokeWidth = 1f)

            // House number at midpoint of house
            val nextCusp = houseData.cusps[(i + 1) % 12]
            var midLon = (houseData.cusps[i] + nextCusp) / 2.0
            if (nextCusp < houseData.cusps[i]) midLon = ((houseData.cusps[i] + nextCusp + 360.0) / 2.0) % 360.0
            val midAngle = WheelMath.longitudeToAngle(midLon, ascendant)
            val numRadius = (radii.houseOuter + radii.houseInner) / 2f
            val (nx, ny) = WheelMath.pointOnCircle(cx, cy, numRadius, midAngle)

            drawContext.canvas.nativeCanvas.drawText(
                "${i + 1}",
                nx, ny + 6f,
                Paint().apply {
                    textSize = (radii.houseOuter - radii.houseInner) * 0.3f
                    textAlign = Paint.Align.CENTER
                    color = android.graphics.Color.GRAY
                    isAntiAlias = true
                }
            )
        }

        drawCircle(ChartColors.ringStroke, radii.houseInner, Offset(cx, cy), style = Stroke(1.5f))
    }

    fun DrawScope.drawBodies(
        radii: RingRadii, cx: Float, cy: Float,
        positions: Map<CelestialBody, com.skothr.ephemeris.ephemeris.models.CelestialPosition>,
        ascendant: Double,
    ) {
        // Convert to sorted angles for collision avoidance
        val bodiesByAngle = positions.entries
            .map { (body, pos) ->
                val angle = WheelMath.longitudeToAngle(pos.longitude, ascendant)
                Triple(body, pos, angle)
            }
            .sortedBy { it.third }

        val angles = bodiesByAngle.map { it.third }
        val minSep = Math.toRadians(8.0) // ~8° minimum visual separation
        val resolvedAngles = WheelMath.resolveCollisions(angles, minSep)

        for (i in bodiesByAngle.indices) {
            val (body, pos, originalAngle) = bodiesByAngle[i]
            val displayAngle = resolvedAngles[i]

            // Glyph on body ring
            val (bx, by) = WheelMath.pointOnCircle(cx, cy, radii.bodyRing, displayAngle)
            drawContext.canvas.nativeCanvas.drawText(
                body.symbol,
                bx, by + 8f,
                Paint().apply {
                    textSize = (radii.houseInner - radii.bodyRing) * 0.6f
                    textAlign = Paint.Align.CENTER
                    color = android.graphics.Color.WHITE
                    isAntiAlias = true
                }
            )

            // Tick mark from body ring to actual zodiac position
            val (tx, ty) = WheelMath.pointOnCircle(cx, cy, radii.houseInner, originalAngle)
            val (tx2, ty2) = WheelMath.pointOnCircle(cx, cy, radii.houseInner - 8f, originalAngle)
            drawLine(ChartColors.bodyGlyph.copy(alpha = 0.5f), Offset(tx, ty), Offset(tx2, ty2), strokeWidth = 1f)
        }
    }

    fun DrawScope.drawAspects(
        radii: RingRadii, cx: Float, cy: Float,
        aspects: List<AspectResult>,
        positions: Map<CelestialBody, com.skothr.ephemeris.ephemeris.models.CelestialPosition>,
        ascendant: Double,
    ) {
        for (aspect in aspects) {
            val pos1 = positions[aspect.body1] ?: continue
            val pos2 = positions[aspect.body2] ?: continue
            val angle1 = WheelMath.longitudeToAngle(pos1.longitude, ascendant)
            val angle2 = WheelMath.longitudeToAngle(pos2.longitude, ascendant)

            val innerRadius = radii.aspectInner + (radii.bodyRing - radii.aspectInner) * 0.3f
            val (x1, y1) = WheelMath.pointOnCircle(cx, cy, innerRadius, angle1)
            val (x2, y2) = WheelMath.pointOnCircle(cx, cy, innerRadius, angle2)

            val color = ChartColors.aspectColor[aspect.aspect] ?: Color.Gray
            val alpha = 1f - (aspect.orb.toFloat() / aspect.aspect.defaultOrb.toFloat()) * 0.7f
            val pathEffect = if (aspect.aspect.isMajor) null
                else PathEffect.dashPathEffect(floatArrayOf(8f, 4f))

            drawLine(
                color = color.copy(alpha = alpha.coerceIn(0.2f, 1f)),
                start = Offset(x1, y1),
                end = Offset(x2, y2),
                strokeWidth = if (aspect.aspect.isMajor) 1.5f else 1f,
                pathEffect = pathEffect,
            )
        }
    }

    fun DrawScope.drawAngles(
        radii: RingRadii, cx: Float, cy: Float,
        houseData: HouseData, ascendant: Double,
    ) {
        val angles = listOf(
            houseData.ascendant to "ASC",
            houseData.midheaven to "MC",
            houseData.descendant to "DSC",
            houseData.imumCoeli to "IC",
        )
        for ((longitude, label) in angles) {
            val angle = WheelMath.longitudeToAngle(longitude, ascendant)
            val (ox, oy) = WheelMath.pointOnCircle(cx, cy, radii.zodiacOuter, angle)
            val (ix, iy) = WheelMath.pointOnCircle(cx, cy, radii.aspectInner, angle)
            drawLine(ChartColors.angleMarker, Offset(ox, oy), Offset(ix, iy), strokeWidth = 2f)

            // Label just outside zodiac ring
            val (lx, ly) = WheelMath.pointOnCircle(cx, cy, radii.zodiacOuter + 14f, angle)
            drawContext.canvas.nativeCanvas.drawText(
                label, lx, ly + 5f,
                Paint().apply {
                    textSize = 24f
                    textAlign = Paint.Align.CENTER
                    color = android.graphics.Color.parseColor("#FFD54F")
                    isAntiAlias = true
                    isFakeBoldText = true
                }
            )
        }
    }
}
```

- [ ] **Step 3: Create ChartWheel composable**

```kotlin
// app/src/main/java/com/skothr/ephemeris/ui/chart/ChartWheel.kt
package com.skothr.ephemeris.ui.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.skothr.ephemeris.chart.models.ChartData

@Composable
fun ChartWheel(
    chartData: ChartData,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val availableRadius = minOf(cx, cy)
        val radii = WheelMath.calculateRingRadii(availableRadius)
        val asc = chartData.houseData.ascendant

        // Background
        drawCircle(ChartColors.wheelBackground, availableRadius)

        // Draw layers from outside in
        with(WheelDrawing) {
            drawZodiacRing(radii, cx, cy, asc)
            drawHouseRing(radii, cx, cy, chartData.houseData, asc)
            drawBodies(radii, cx, cy, chartData.positions, asc)
            drawAspects(radii, cx, cy, chartData.aspects, chartData.positions, asc)
            drawAngles(radii, cx, cy, chartData.houseData, asc)
        }
    }
}
```

- [ ] **Step 4: Verify build compiles**

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: add ChartWheel Canvas rendering with zodiac, houses, bodies, aspects"
```

---

### Task 8: DateTime Scrub Controls

**Files:**
- Create: `app/src/main/java/com/skothr/ephemeris/ui/controls/DateTimeControls.kt`

- [ ] **Step 1: Implement DateTimeControls**

```kotlin
// app/src/main/java/com/skothr/ephemeris/ui/controls/DateTimeControls.kt
package com.skothr.ephemeris.ui.controls

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDateTime
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

@Composable
fun DateTimeControls(
    dateTime: LocalDateTime,
    onDateTimeChanged: (LocalDateTime) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ScrubField("Year", dateTime.year.toString(), Modifier.weight(1.2f)) { delta ->
                onDateTimeChanged(safeAdjust { dateTime.plusYears(delta.toLong()) })
            }
            ScrubField("Mon", dateTime.monthValue.toString().padStart(2, '0'), Modifier.weight(1f)) { delta ->
                onDateTimeChanged(safeAdjust { dateTime.plusMonths(delta.toLong()) })
            }
            ScrubField("Day", dateTime.dayOfMonth.toString().padStart(2, '0'), Modifier.weight(1f)) { delta ->
                onDateTimeChanged(safeAdjust { dateTime.plusDays(delta.toLong()) })
            }
            ScrubField("Hr", dateTime.hour.toString().padStart(2, '0'), Modifier.weight(1f)) { delta ->
                onDateTimeChanged(safeAdjust { dateTime.plusHours(delta.toLong()) })
            }
            ScrubField("Min", dateTime.minute.toString().padStart(2, '0'), Modifier.weight(1f)) { delta ->
                onDateTimeChanged(safeAdjust { dateTime.plusMinutes(delta.toLong()) })
            }
            ScrubField("Sec", dateTime.second.toString().padStart(2, '0'), Modifier.weight(1f)) { delta ->
                onDateTimeChanged(safeAdjust { dateTime.plusSeconds(delta.toLong()) })
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        TextButton(
            onClick = { onDateTimeChanged(LocalDateTime.now()) },
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text("Now")
        }
    }
}

@Composable
private fun ScrubField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onDelta: (Int) -> Unit,
) {
    var accumulatedDrag by remember { mutableFloatStateOf(0f) }
    val dragThreshold = 20f // pixels per unit

    Column(
        modifier = modifier
            .padding(horizontal = 2.dp)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { accumulatedDrag = 0f },
                    onVerticalDrag = { _, dragAmount ->
                        accumulatedDrag += dragAmount
                        // Acceleration: larger accumulated drag = bigger steps
                        val speed = 1 + (abs(accumulatedDrag) / 200f).toInt()
                        val units = (accumulatedDrag / dragThreshold).roundToInt()
                        if (units != 0) {
                            // Negative drag (up) = increase, positive (down) = decrease
                            onDelta(-units * speed)
                            accumulatedDrag -= units * dragThreshold
                        }
                    },
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private inline fun safeAdjust(block: () -> LocalDateTime): LocalDateTime {
    return try {
        block()
    } catch (e: Exception) {
        LocalDateTime.now()
    }
}
```

- [ ] **Step 2: Verify build compiles**

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "feat: add DateTimeControls with vertical drag scrubbing"
```

---

### Task 9: Location Controls & City Database

**Files:**
- Create: `app/src/main/java/com/skothr/ephemeris/ui/controls/LocationControls.kt`
- Create: `app/src/main/res/raw/cities15000.tsv` (GeoNames data, downloaded separately)

**Prerequisite:** Download `cities15000.zip` from http://download.geonames.org/export/dump/ and extract the TSV file into `app/src/main/res/raw/cities15000.tsv`. The file is tab-separated with columns: geonameid, name, asciiname, alternatenames, latitude, longitude, ... timezone (col 17).

- [ ] **Step 1: Create a CityData model and parser**

Add to LocationControls.kt (or a separate file if preferred):

```kotlin
// app/src/main/java/com/skothr/ephemeris/ui/controls/LocationControls.kt
package com.skothr.ephemeris.ui.controls

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skothr.ephemeris.R
import com.skothr.ephemeris.chart.models.Location
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.roundToInt

data class CityData(
    val name: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    val population: Long,
)

class CityDatabase(context: Context) {
    private val cities: List<CityData> by lazy {
        val result = mutableListOf<CityData>()
        context.resources.openRawResource(R.raw.cities15000).use { stream ->
            BufferedReader(InputStreamReader(stream)).useLines { lines ->
                lines.forEach { line ->
                    val cols = line.split("\t")
                    if (cols.size >= 18) {
                        try {
                            result.add(CityData(
                                name = cols[1],
                                country = cols[8],
                                latitude = cols[4].toDouble(),
                                longitude = cols[5].toDouble(),
                                timezone = cols[17],
                                population = cols[14].toLongOrNull() ?: 0L,
                            ))
                        } catch (_: Exception) {}
                    }
                }
            }
        }
        result.sortedByDescending { it.population }
    }

    fun search(query: String, limit: Int = 20): List<CityData> {
        if (query.length < 2) return emptyList()
        val lower = query.lowercase()
        return cities
            .filter { it.name.lowercase().startsWith(lower) }
            .take(limit)
    }
}

@Composable
fun LocationControls(
    location: Location,
    timezone: String,
    onLocationChanged: (Location, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val cityDb = remember { CityDatabase(context) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<CityData>>(emptyList()) }
    var showSearch by remember { mutableStateOf(true) }

    // Debounce city search at 300ms
    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2) {
            kotlinx.coroutines.delay(300)
            searchResults = cityDb.search(searchQuery)
        } else {
            searchResults = emptyList()
        }
    }

    Column(modifier = modifier.padding(8.dp)) {
        // City search
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { query ->
                searchQuery = query
                showSearch = true
            },
            label = { Text("City") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        if (showSearch && searchResults.isNotEmpty()) {
            LazyColumn(modifier = Modifier.heightIn(max = 150.dp)) {
                items(searchResults) { city ->
                    Text(
                        text = "${city.name}, ${city.country}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onLocationChanged(
                                    Location(city.latitude, city.longitude),
                                    city.timezone,
                                )
                                searchQuery = "${city.name}, ${city.country}"
                                showSearch = false
                            }
                            .padding(8.dp),
                        fontSize = 14.sp,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Raw coordinate controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            CoordField("Lat", "%.4f".format(location.latitude), Modifier.weight(1f)) { delta ->
                val newLat = (location.latitude + delta * 0.1).coerceIn(-90.0, 90.0)
                onLocationChanged(Location(newLat, location.longitude), timezone)
            }
            CoordField("Lon", "%.4f".format(location.longitude), Modifier.weight(1f)) { delta ->
                val newLon = (location.longitude + delta * 0.1).let { l ->
                    ((l + 180.0) % 360.0 + 360.0) % 360.0 - 180.0
                }
                onLocationChanged(Location(location.latitude, newLon), timezone)
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("TZ", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(timezone, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun CoordField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onDelta: (Int) -> Unit,
) {
    var accumulatedDrag by remember { mutableFloatStateOf(0f) }
    val dragThreshold = 20f

    Column(
        modifier = modifier
            .padding(horizontal = 2.dp)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { accumulatedDrag = 0f },
                    onVerticalDrag = { _, dragAmount ->
                        accumulatedDrag += dragAmount
                        val units = (accumulatedDrag / dragThreshold).roundToInt()
                        if (units != 0) {
                            onDelta(-units)
                            accumulatedDrag -= units * dragThreshold
                        }
                    },
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 14.sp, textAlign = TextAlign.Center)
    }
}
```

- [ ] **Step 2: Verify build compiles**

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL (will fail if cities15000.tsv is missing — that's expected, it must be downloaded manually as a prerequisite).

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "feat: add LocationControls with city search and coordinate scrubbing"
```

---

### Task 10: Theme

**Files:**
- Create: `app/src/main/java/com/skothr/ephemeris/ui/theme/Color.kt`
- Create: `app/src/main/java/com/skothr/ephemeris/ui/theme/Theme.kt`
- Create: `app/src/main/java/com/skothr/ephemeris/ui/theme/Type.kt`

- [ ] **Step 1: Create theme files**

```kotlin
// app/src/main/java/com/skothr/ephemeris/ui/theme/Color.kt
package com.skothr.ephemeris.ui.theme

import androidx.compose.ui.graphics.Color

val DarkBackground = Color(0xFF0D0D1A)
val DarkSurface = Color(0xFF1A1A2E)
val DarkSurfaceVariant = Color(0xFF252542)
val PrimaryBlue = Color(0xFF5C6BC0)
val SecondaryAmber = Color(0xFFFFB300)
val OnDarkText = Color(0xFFE0E0E0)
val OnDarkTextVariant = Color(0xFF9E9E9E)
```

```kotlin
// app/src/main/java/com/skothr/ephemeris/ui/theme/Type.kt
package com.skothr.ephemeris.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val EphemerisTypography = Typography(
    bodyLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal),
    labelSmall = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Normal),
    titleMedium = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium),
)
```

```kotlin
// app/src/main/java/com/skothr/ephemeris/ui/theme/Theme.kt
package com.skothr.ephemeris.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    secondary = SecondaryAmber,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onBackground = OnDarkText,
    onSurface = OnDarkText,
    onSurfaceVariant = OnDarkTextVariant,
)

@Composable
fun EphemerisTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = EphemerisTypography,
        content = content,
    )
}
```

- [ ] **Step 2: Verify build compiles**

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "feat: add dark theme for astrological chart UI"
```

---

### Task 11: ViewModel & MainScreen Integration

**Files:**
- Create: `app/src/main/java/com/skothr/ephemeris/ui/ChartViewModel.kt`
- Create: `app/src/main/java/com/skothr/ephemeris/ui/MainScreen.kt`
- Modify: `app/src/main/java/com/skothr/ephemeris/MainActivity.kt`
- Modify: `app/src/main/java/com/skothr/ephemeris/EphemerisApp.kt`

- [ ] **Step 1: Create ChartViewModel**

```kotlin
// app/src/main/java/com/skothr/ephemeris/ui/ChartViewModel.kt
package com.skothr.ephemeris.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skothr.ephemeris.chart.ChartCalculator
import com.skothr.ephemeris.chart.config.AspectConfig
import com.skothr.ephemeris.chart.config.BodyConfig
import com.skothr.ephemeris.chart.models.ChartData
import com.skothr.ephemeris.chart.models.HouseSystem
import com.skothr.ephemeris.chart.models.Location
import com.skothr.ephemeris.ephemeris.EphemerisProvider
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class ChartViewModel(
    ephemeris: EphemerisProvider,
    private val ephemerisReady: kotlinx.coroutines.Deferred<Unit>,
) : ViewModel() {

    private val chartCalculator = ChartCalculator(ephemeris, AspectConfig(), BodyConfig())

    private val _dateTime = MutableStateFlow(LocalDateTime.now())
    val dateTime: StateFlow<LocalDateTime> = _dateTime.asStateFlow()

    private val _location = MutableStateFlow(Location(40.7128, -74.0060)) // NYC default
    val location: StateFlow<Location> = _location.asStateFlow()

    private val _timezone = MutableStateFlow("America/New_York")
    val timezone: StateFlow<String> = _timezone.asStateFlow()

    private val _houseSystem = MutableStateFlow(HouseSystem.PLACIDUS)
    val houseSystem: StateFlow<HouseSystem> = _houseSystem.asStateFlow()

    private val _chartData = MutableStateFlow<ChartData?>(null)
    val chartData: StateFlow<ChartData?> = _chartData.asStateFlow()

    private val _isCalculating = MutableStateFlow(false)
    val isCalculating: StateFlow<Boolean> = _isCalculating.asStateFlow()

    init {
        // Wait for ephemeris init, then recalculate when any input changes, throttled to ~30fps
        @OptIn(FlowPreview::class)
        viewModelScope.launch {
            ephemerisReady.await()
            combine(_dateTime, _location, _houseSystem) { dt, loc, hs ->
                Triple(dt, loc, hs)
            }
                .debounce(33) // ~30fps
                .collectLatest { (dt, loc, hs) ->
                    _isCalculating.value = true
                    try {
                        _chartData.value = chartCalculator.calculate(dt, loc, hs)
                    } catch (e: Exception) {
                        // Log error, keep last valid chart
                    } finally {
                        _isCalculating.value = false
                    }
                }
        }
    }

    fun updateDateTime(dateTime: LocalDateTime) {
        _dateTime.value = dateTime
    }

    fun updateLocation(location: Location, timezone: String) {
        _location.value = location
        _timezone.value = timezone
    }

    fun updateHouseSystem(system: HouseSystem) {
        _houseSystem.value = system
    }
}
```

- [ ] **Step 2: Create MainScreen**

```kotlin
// app/src/main/java/com/skothr/ephemeris/ui/MainScreen.kt
package com.skothr.ephemeris.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.skothr.ephemeris.ui.chart.ChartWheel
import com.skothr.ephemeris.ui.controls.DateTimeControls
import com.skothr.ephemeris.ui.controls.LocationControls
import com.skothr.ephemeris.ui.theme.EphemerisTheme

@Composable
fun MainScreen(viewModel: ChartViewModel) {
    val chartData by viewModel.chartData.collectAsState()
    val dateTime by viewModel.dateTime.collectAsState()
    val location by viewModel.location.collectAsState()
    val timezone by viewModel.timezone.collectAsState()
    val isCalculating by viewModel.isCalculating.collectAsState()

    EphemerisTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Chart wheel
                chartData?.let { data ->
                    ChartWheel(
                        chartData = data,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                    )
                } ?: Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }

                if (isCalculating) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Date/Time controls
                DateTimeControls(
                    dateTime = dateTime,
                    onDateTimeChanged = { viewModel.updateDateTime(it) },
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Location controls
                LocationControls(
                    location = location,
                    timezone = timezone,
                    onLocationChanged = { loc, tz -> viewModel.updateLocation(loc, tz) },
                )
            }
        }
    }
}
```

- [ ] **Step 3: Update MainActivity to use MainScreen**

```kotlin
// app/src/main/java/com/skothr/ephemeris/MainActivity.kt
package com.skothr.ephemeris

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.skothr.ephemeris.ui.ChartViewModel
import com.skothr.ephemeris.ui.MainScreen

class MainActivity : ComponentActivity() {

    private val viewModel: ChartViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = application as EphemerisApp
                return ChartViewModel(app.swissEphemeris, app.ephemerisReady) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainScreen(viewModel)
        }
    }
}
```

- [ ] **Step 4: Verify full build compiles**

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: wire up ChartViewModel and MainScreen, complete initial app integration"
```

---

### Task 12: End-to-End Verification

- [ ] **Step 1: Run all unit tests**

```bash
./gradlew :app:testDebugUnitTest -q
```

Expected: All tests PASS.

- [ ] **Step 2: Run instrumented tests (requires device/emulator)**

```bash
./gradlew connectedAndroidTest
```

Expected: All tests PASS.

- [ ] **Step 3: Install and launch on device**

```bash
./gradlew installDebug
adb shell am start -n com.skothr.ephemeris/.MainActivity
```

Verify:
- Chart wheel renders with zodiac signs, house cusps, planet glyphs, aspect lines
- ASC/MC/DSC/IC angles are visible
- Dragging date/time fields smoothly updates the chart
- City search finds cities and updates the chart
- Raw coordinate dragging works
- "Now" button resets to current time

- [ ] **Step 4: Final commit if any fixes needed**

```bash
git add -A
git commit -m "fix: end-to-end verification fixes"
```
