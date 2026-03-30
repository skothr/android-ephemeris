# Settings Screen Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a full-screen tabbed settings screen (Calculation, Display, Visual) with DataStore persistence, wired into the chart calculation and rendering pipeline.

**Architecture:** Settings are persisted via Jetpack Preferences DataStore, exposed as `StateFlow<AppSettings>` from a `SettingsRepository`. `ChartViewModel` observes settings to rebuild SEFLG flags and recalculate. `WheelRenderer` reads visual settings at draw time. Navigation is a simple boolean state toggle — no navigation library.

**Tech Stack:** Jetpack Compose, Preferences DataStore, Material3, SwissEphemeris JNI (C), Kotlin Coroutines/StateFlow

---

## File Map

### New Files

| File | Responsibility |
|------|---------------|
| `settings/model/SettingsEnums.kt` | All new enums: `ZodiacType`, `Ayanamsa`, `NodeType`, `Center`, `AppTheme`, `SymbolStyle`, `LineStyle` |
| `settings/model/AppSettings.kt` | Data classes: `CalculationSettings`, `DisplaySettings`, `VisualSettings`, `AppSettings` |
| `settings/SettingsRepository.kt` | DataStore read/write, exposes `Flow<AppSettings>` |
| `settings/SettingsScreen.kt` | Top-level tabbed composable with back navigation |
| `settings/tabs/CalculationTab.kt` | Zodiac type, node type, center, house system, flag toggles |
| `settings/tabs/DisplayTab.kt` | Body toggles (2-col grid), aspect toggles with orb editing |
| `settings/tabs/VisualTab.kt` | Theme, symbol style, ring sliders, aspect line controls |
| `settings/components/SegmentedToggle.kt` | Reusable segmented button row |
| `settings/components/SettingsDropdown.kt` | Reusable exposed dropdown menu |
| `settings/components/SettingsSlider.kt` | Labeled slider with value readout |
| `settings/components/OrbEditDialog.kt` | AlertDialog for editing orb values |

All new files live under `app/src/main/java/com/skothr/ephemeris/settings/`.

### Modified Files

| File | Changes |
|------|---------|
| `app/build.gradle.kts:56` | Add DataStore dependency |
| `EphemerisApp.kt` | Create DataStore singleton, expose `SettingsRepository` |
| `MainActivity.kt` | Create `SettingsRepository` from app, pass to both ViewModels |
| `ui/MainScreen.kt` | Add gear icon, `showSettings` state, conditional screen switching |
| `ui/ChartViewModel.kt` | Observe `SettingsRepository`, build flags, pass to calculator |
| `ephemeris/EphemerisProvider.kt` | Add `flags: Int` param to `calculateBody` and `calculateHouses`; add `setSiderealMode` and `setTopographicPosition` |
| `ephemeris/SwissEphemeris.kt` | Implement new interface methods, add JNI declarations |
| `chart/ChartCalculator.kt` | Accept flags/settings, pass to ephemeris calls |
| `chart/AspectCalculator.kt` | Accept `DisplaySettings` instead of `AspectConfig` |
| `ui/chart/WheelRenderer.kt` | Read `VisualSettings` for ring radii, line style, opacity, font |
| `ui/chart/ChartWheel.kt` | Pass `VisualSettings` through to renderer |
| `ui/theme/Theme.kt` | Add `LightColorScheme`, make theme switchable |
| `ui/theme/Color.kt` | Add light theme colors |
| `app/src/main/cpp/swisseph_wrapper.c` | Add `nativeCalculateBodyWithFlags`, `nativeSetSiderealMode`, `nativeSetTopographicPosition` JNI functions |

---

## Task 1: Add DataStore Dependency

**Files:**
- Modify: `app/build.gradle.kts:56-84`

- [ ] **Step 1: Add DataStore to dependencies**

In `app/build.gradle.kts`, add after the Coroutines section (line 74):

```kotlin
    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.2")
```

- [ ] **Step 2: Sync and verify**

Run: `cd /home/ai/ai-projects/android-ephemeris && ./gradlew app:dependencies --configuration implementation 2>&1 | grep datastore`

Expected: Line showing `androidx.datastore:datastore-preferences:1.1.2`

- [ ] **Step 3: Commit**

```bash
git add app/build.gradle.kts
git commit -m "build: add DataStore preferences dependency"
```

---

## Task 2: Settings Enums and Data Model

**Files:**
- Create: `app/src/main/java/com/skothr/ephemeris/settings/model/SettingsEnums.kt`
- Create: `app/src/main/java/com/skothr/ephemeris/settings/model/AppSettings.kt`

- [ ] **Step 1: Create SettingsEnums.kt**

```kotlin
package com.skothr.ephemeris.settings.model

enum class ZodiacType(val displayName: String) {
    TROPICAL("Tropical"),
    SIDEREAL("Sidereal"),
}

enum class Ayanamsa(val displayName: String, val swissEphId: Int) {
    LAHIRI("Lahiri", 1),
    RAMAN("Raman", 3),
    KRISHNAMURTI("Krishnamurti", 5),
    FAGAN_BRADLEY("Fagan-Bradley", 0),
}

enum class NodeType(val displayName: String) {
    TRUE_NODE("True Node"),
    MEAN_NODE("Mean Node"),
}

enum class Center(val displayName: String) {
    GEOCENTRIC("Geocentric"),
    HELIOCENTRIC("Heliocentric"),
    TOPOCENTRIC("Topocentric"),
}

enum class AppTheme(val displayName: String) {
    DARK("Dark"),
    LIGHT("Light"),
}

enum class SymbolStyle(val displayName: String) {
    SYSTEM("System"),
    ASTRO("Astro"),
}

enum class LineStyle(val displayName: String) {
    SOLID("Solid"),
    DASHED("Dashed"),
}
```

- [ ] **Step 2: Create AppSettings.kt**

```kotlin
package com.skothr.ephemeris.settings.model

import com.skothr.ephemeris.chart.models.Aspect
import com.skothr.ephemeris.chart.models.CelestialBody
import com.skothr.ephemeris.chart.models.HouseSystem

data class CalculationSettings(
    val zodiacType: ZodiacType = ZodiacType.TROPICAL,
    val ayanamsa: Ayanamsa = Ayanamsa.LAHIRI,
    val nodeType: NodeType = NodeType.TRUE_NODE,
    val center: Center = Center.GEOCENTRIC,
    val houseSystem: HouseSystem = HouseSystem.PLACIDUS,
    val speedInLongitude: Boolean = true,
    val equatorialCoordinates: Boolean = false,
) {
    fun toSwissEphFlags(): Int {
        var flags = SEFLG_SWIEPH
        if (speedInLongitude) flags = flags or SEFLG_SPEED
        if (zodiacType == ZodiacType.SIDEREAL) flags = flags or SEFLG_SIDEREAL
        if (center == Center.HELIOCENTRIC) flags = flags or SEFLG_HELCTR
        if (center == Center.TOPOCENTRIC) flags = flags or SEFLG_TOPOCTR
        if (equatorialCoordinates) flags = flags or SEFLG_EQUATORIAL
        return flags
    }

    // Note: NodeType is NOT a flag — it's handled by switching the body ID
    // for North Node between SE_TRUE_NODE (11) and SE_MEAN_NODE (10).
    // See ChartCalculator for the body ID mapping.

    companion object {
        const val SEFLG_SWIEPH = 2
        const val SEFLG_HELCTR = 8
        const val SEFLG_SPEED = 256
        const val SEFLG_EQUATORIAL = 2 * 1024
        const val SEFLG_TOPOCTR = 32 * 1024
        const val SEFLG_SIDEREAL = 64 * 1024

        const val SE_MEAN_NODE = 10
        const val SE_TRUE_NODE = 11
    }
}

data class DisplaySettings(
    val enabledBodies: Set<CelestialBody> = CelestialBody.entries.toSet(),
    val enabledAspects: Set<Aspect> = Aspect.entries.toSet(),
    val aspectOrbs: Map<Aspect, Double> = Aspect.entries.associateWith { it.defaultOrb },
)

data class VisualSettings(
    val theme: AppTheme = AppTheme.DARK,
    val symbolStyle: SymbolStyle = SymbolStyle.SYSTEM,
    val zodiacOuterRadius: Float = 0.95f,
    val zodiacInnerRadius: Float = 0.82f,
    val houseOuterRadius: Float = 0.82f,
    val houseInnerRadius: Float = 0.68f,
    val bodyRingRadius: Float = 0.58f,
    val aspectLineThickness: Float = 2.0f,
    val aspectLineOpacity: Float = 0.8f,
    val majorAspectStyle: LineStyle = LineStyle.SOLID,
    val minorAspectStyle: LineStyle = LineStyle.DASHED,
)

data class AppSettings(
    val calculation: CalculationSettings = CalculationSettings(),
    val display: DisplaySettings = DisplaySettings(),
    val visual: VisualSettings = VisualSettings(),
)
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/skothr/ephemeris/settings/
git commit -m "feat: add settings data model and enums"
```

---

## Task 3: Settings Repository (DataStore)

**Files:**
- Create: `app/src/main/java/com/skothr/ephemeris/settings/SettingsRepository.kt`
- Modify: `app/src/main/java/com/skothr/ephemeris/EphemerisApp.kt`

- [ ] **Step 1: Create SettingsRepository.kt**

This class wraps DataStore reads/writes and exposes `Flow<AppSettings>`. Each setting is stored as a separate preference key. The repository combines all keys into a single `AppSettings` flow.

```kotlin
package com.skothr.ephemeris.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.skothr.ephemeris.chart.models.Aspect
import com.skothr.ephemeris.chart.models.CelestialBody
import com.skothr.ephemeris.chart.models.HouseSystem
import com.skothr.ephemeris.settings.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    // Calculation keys
    private val zodiacTypeKey = stringPreferencesKey("calc_zodiac_type")
    private val ayanamsaKey = stringPreferencesKey("calc_ayanamsa")
    private val nodeTypeKey = stringPreferencesKey("calc_node_type")
    private val centerKey = stringPreferencesKey("calc_center")
    private val houseSystemKey = stringPreferencesKey("calc_house_system")
    private val speedKey = booleanPreferencesKey("calc_speed")
    private val equatorialKey = booleanPreferencesKey("calc_equatorial")

    // Display keys (bodies and aspects stored by name)
    private fun bodyKey(body: CelestialBody) = booleanPreferencesKey("display_body_${body.name}")
    private fun aspectEnabledKey(aspect: Aspect) = booleanPreferencesKey("display_aspect_${aspect.name}")
    private fun aspectOrbKey(aspect: Aspect) = doublePreferencesKey("display_aspect_orb_${aspect.name}")

    // Visual keys
    private val themeKey = stringPreferencesKey("visual_theme")
    private val symbolStyleKey = stringPreferencesKey("visual_symbol_style")
    private val zodiacOuterKey = floatPreferencesKey("visual_zodiac_outer")
    private val zodiacInnerKey = floatPreferencesKey("visual_zodiac_inner")
    private val houseOuterKey = floatPreferencesKey("visual_house_outer")
    private val houseInnerKey = floatPreferencesKey("visual_house_inner")
    private val bodyRingKey = floatPreferencesKey("visual_body_ring")
    private val aspectThicknessKey = floatPreferencesKey("visual_aspect_thickness")
    private val aspectOpacityKey = floatPreferencesKey("visual_aspect_opacity")
    private val majorStyleKey = stringPreferencesKey("visual_major_style")
    private val minorStyleKey = stringPreferencesKey("visual_minor_style")

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        val defaults = AppSettings()
        AppSettings(
            calculation = CalculationSettings(
                zodiacType = prefs[zodiacTypeKey]?.let { enumValueOf<ZodiacType>(it) } ?: defaults.calculation.zodiacType,
                ayanamsa = prefs[ayanamsaKey]?.let { enumValueOf<Ayanamsa>(it) } ?: defaults.calculation.ayanamsa,
                nodeType = prefs[nodeTypeKey]?.let { enumValueOf<NodeType>(it) } ?: defaults.calculation.nodeType,
                center = prefs[centerKey]?.let { enumValueOf<Center>(it) } ?: defaults.calculation.center,
                houseSystem = prefs[houseSystemKey]?.let { enumValueOf<HouseSystem>(it) } ?: defaults.calculation.houseSystem,
                speedInLongitude = prefs[speedKey] ?: defaults.calculation.speedInLongitude,
                equatorialCoordinates = prefs[equatorialKey] ?: defaults.calculation.equatorialCoordinates,
            ),
            display = DisplaySettings(
                enabledBodies = CelestialBody.entries.filter { body ->
                    prefs[bodyKey(body)] ?: true
                }.toSet(),
                enabledAspects = Aspect.entries.filter { aspect ->
                    prefs[aspectEnabledKey(aspect)] ?: true
                }.toSet(),
                aspectOrbs = Aspect.entries.associateWith { aspect ->
                    prefs[aspectOrbKey(aspect)] ?: aspect.defaultOrb
                },
            ),
            visual = VisualSettings(
                theme = prefs[themeKey]?.let { enumValueOf<AppTheme>(it) } ?: defaults.visual.theme,
                symbolStyle = prefs[symbolStyleKey]?.let { enumValueOf<SymbolStyle>(it) } ?: defaults.visual.symbolStyle,
                zodiacOuterRadius = prefs[zodiacOuterKey] ?: defaults.visual.zodiacOuterRadius,
                zodiacInnerRadius = prefs[zodiacInnerKey] ?: defaults.visual.zodiacInnerRadius,
                houseOuterRadius = prefs[houseOuterKey] ?: defaults.visual.houseOuterRadius,
                houseInnerRadius = prefs[houseInnerKey] ?: defaults.visual.houseInnerRadius,
                bodyRingRadius = prefs[bodyRingKey] ?: defaults.visual.bodyRingRadius,
                aspectLineThickness = prefs[aspectThicknessKey] ?: defaults.visual.aspectLineThickness,
                aspectLineOpacity = prefs[aspectOpacityKey] ?: defaults.visual.aspectLineOpacity,
                majorAspectStyle = prefs[majorStyleKey]?.let { enumValueOf<LineStyle>(it) } ?: defaults.visual.majorAspectStyle,
                minorAspectStyle = prefs[minorStyleKey]?.let { enumValueOf<LineStyle>(it) } ?: defaults.visual.minorAspectStyle,
            ),
        )
    }

    // Calculation setters
    suspend fun setZodiacType(value: ZodiacType) = context.dataStore.edit { it[zodiacTypeKey] = value.name }
    suspend fun setAyanamsa(value: Ayanamsa) = context.dataStore.edit { it[ayanamsaKey] = value.name }
    suspend fun setNodeType(value: NodeType) = context.dataStore.edit { it[nodeTypeKey] = value.name }
    suspend fun setCenter(value: Center) = context.dataStore.edit { it[centerKey] = value.name }
    suspend fun setHouseSystem(value: HouseSystem) = context.dataStore.edit { it[houseSystemKey] = value.name }
    suspend fun setSpeedInLongitude(value: Boolean) = context.dataStore.edit { it[speedKey] = value }
    suspend fun setEquatorialCoordinates(value: Boolean) = context.dataStore.edit { it[equatorialKey] = value }

    // Display setters
    suspend fun setBodyEnabled(body: CelestialBody, enabled: Boolean) =
        context.dataStore.edit { it[bodyKey(body)] = enabled }
    suspend fun setAspectEnabled(aspect: Aspect, enabled: Boolean) =
        context.dataStore.edit { it[aspectEnabledKey(aspect)] = enabled }
    suspend fun setAspectOrb(aspect: Aspect, orb: Double) =
        context.dataStore.edit { it[aspectOrbKey(aspect)] = orb }

    // Visual setters
    suspend fun setTheme(value: AppTheme) = context.dataStore.edit { it[themeKey] = value.name }
    suspend fun setSymbolStyle(value: SymbolStyle) = context.dataStore.edit { it[symbolStyleKey] = value.name }
    suspend fun setZodiacOuterRadius(value: Float) = context.dataStore.edit { it[zodiacOuterKey] = value }
    suspend fun setZodiacInnerRadius(value: Float) = context.dataStore.edit { it[zodiacInnerKey] = value }
    suspend fun setHouseOuterRadius(value: Float) = context.dataStore.edit { it[houseOuterKey] = value }
    suspend fun setHouseInnerRadius(value: Float) = context.dataStore.edit { it[houseInnerKey] = value }
    suspend fun setBodyRingRadius(value: Float) = context.dataStore.edit { it[bodyRingKey] = value }
    suspend fun setAspectLineThickness(value: Float) = context.dataStore.edit { it[aspectThicknessKey] = value }
    suspend fun setAspectLineOpacity(value: Float) = context.dataStore.edit { it[aspectOpacityKey] = value }
    suspend fun setMajorAspectStyle(value: LineStyle) = context.dataStore.edit { it[majorStyleKey] = value.name }
    suspend fun setMinorAspectStyle(value: LineStyle) = context.dataStore.edit { it[minorStyleKey] = value.name }
}
```

- [ ] **Step 2: Expose SettingsRepository from EphemerisApp**

Add to `EphemerisApp.kt` after `val ephemerisReady` (line 15):

```kotlin
    val settingsRepository by lazy { com.skothr.ephemeris.settings.SettingsRepository(this) }
```

Add the import at top:
```kotlin
import com.skothr.ephemeris.settings.SettingsRepository
```

Then simplify the lazy init to:
```kotlin
    val settingsRepository by lazy { SettingsRepository(this) }
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/skothr/ephemeris/settings/SettingsRepository.kt app/src/main/java/com/skothr/ephemeris/EphemerisApp.kt
git commit -m "feat: add SettingsRepository with DataStore persistence"
```

---

## Task 4: JNI — Add Flags Support and Sidereal/Topocentric Functions

**Files:**
- Modify: `app/src/main/cpp/swisseph_wrapper.c`
- Modify: `app/src/main/java/com/skothr/ephemeris/ephemeris/SwissEphemeris.kt`
- Modify: `app/src/main/java/com/skothr/ephemeris/ephemeris/EphemerisProvider.kt`

- [ ] **Step 1: Add new JNI functions to swisseph_wrapper.c**

Add these three functions after the existing `nativeCalculateHouses` (after line 91):

```c
JNIEXPORT void JNICALL
Java_com_skothr_ephemeris_ephemeris_SwissEphemeris_nativeSetSiderealMode(
    JNIEnv *env, jobject thiz, jint sid_mode) {
    swe_set_sid_mode(sid_mode, 0, 0);
    LOGI("Set sidereal mode to: %d", sid_mode);
}

JNIEXPORT void JNICALL
Java_com_skothr_ephemeris_ephemeris_SwissEphemeris_nativeSetTopographicPosition(
    JNIEnv *env, jobject thiz,
    jdouble lon, jdouble lat, jdouble alt) {
    swe_set_topo(lon, lat, alt);
    LOGI("Set topographic position to: lon=%.4f, lat=%.4f, alt=%.1f", lon, lat, alt);
}

JNIEXPORT jdoubleArray JNICALL
Java_com_skothr_ephemeris_ephemeris_SwissEphemeris_nativeCalculateBodyWithFlags(
    JNIEnv *env, jobject thiz,
    jdouble jd, jint body_id, jint flags) {
    double result[6];
    char err[256];

    LOGI("swe_calc_ut: body=%d, jd=%.6f, flags=%d", body_id, jd, flags);
    int rc = swe_calc_ut(jd, body_id, flags, result, err);
    if (rc < 0) {
        LOGE("swe_calc_ut error for body %d (rc=%d): %s", body_id, rc, err);
        return NULL;
    }

    jdoubleArray jresult = (*env)->NewDoubleArray(env, 4);
    if (jresult == NULL) return NULL;
    double out[4] = { result[0], result[1], result[2], result[3] };
    (*env)->SetDoubleArrayRegion(env, jresult, 0, 4, out);
    return jresult;
}
```

- [ ] **Step 2: Update EphemerisProvider interface**

Replace the entire content of `EphemerisProvider.kt`:

```kotlin
package com.skothr.ephemeris.ephemeris

import com.skothr.ephemeris.chart.models.CelestialBody
import com.skothr.ephemeris.chart.models.HouseSystem
import com.skothr.ephemeris.ephemeris.models.CelestialPosition
import com.skothr.ephemeris.ephemeris.models.HouseData
import java.time.LocalDateTime

interface EphemerisProvider {
    suspend fun julianDay(dateTime: LocalDateTime): Double
    suspend fun calculateBody(julianDay: Double, bodyId: Int, flags: Int): CelestialPosition
    suspend fun calculateHouses(
        julianDay: Double, latitude: Double, longitude: Double, system: HouseSystem
    ): HouseData
    suspend fun setSiderealMode(ayanamsa: Int)
    suspend fun setTopographicPosition(longitude: Double, latitude: Double, altitude: Double)
}
```

- [ ] **Step 3: Update SwissEphemeris.kt to implement new interface**

Replace the `calculateBody` method (lines 38-49) with:

```kotlin
    override suspend fun calculateBody(julianDay: Double, bodyId: Int, flags: Int): CelestialPosition =
        mutex.withLock {
            check(initialized) { "SwissEphemeris not initialized" }
            val result = nativeCalculateBodyWithFlags(julianDay, bodyId, flags)
                ?: throw RuntimeException("Failed to calculate position for body $bodyId")
            CelestialPosition(
                longitude = result[0],
                latitude = result[1],
                distance = result[2],
                speed = result[3],
            )
        }
```

Add new methods before the JNI declarations:

```kotlin
    override suspend fun setSiderealMode(ayanamsa: Int) = mutex.withLock {
        check(initialized) { "SwissEphemeris not initialized" }
        nativeSetSiderealMode(ayanamsa)
    }

    override suspend fun setTopographicPosition(longitude: Double, latitude: Double, altitude: Double) = mutex.withLock {
        check(initialized) { "SwissEphemeris not initialized" }
        nativeSetTopographicPosition(longitude, latitude, altitude)
    }
```

Add new JNI declarations alongside the existing ones (after line 75):

```kotlin
    private external fun nativeCalculateBodyWithFlags(julianDay: Double, bodyId: Int, flags: Int): DoubleArray?
    private external fun nativeSetSiderealMode(sidMode: Int)
    private external fun nativeSetTopographicPosition(lon: Double, lat: Double, alt: Double)
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/cpp/swisseph_wrapper.c app/src/main/java/com/skothr/ephemeris/ephemeris/
git commit -m "feat: add JNI flags support, sidereal mode, and topocentric position"
```

---

## Task 5: Wire Settings into ChartCalculator and ChartViewModel

**Files:**
- Modify: `app/src/main/java/com/skothr/ephemeris/chart/ChartCalculator.kt`
- Modify: `app/src/main/java/com/skothr/ephemeris/chart/AspectCalculator.kt`
- Modify: `app/src/main/java/com/skothr/ephemeris/ui/ChartViewModel.kt`
- Modify: `app/src/main/java/com/skothr/ephemeris/MainActivity.kt`

- [ ] **Step 1: Update ChartCalculator to accept settings**

Replace the entire `ChartCalculator.kt`:

```kotlin
package com.skothr.ephemeris.chart

import com.skothr.ephemeris.chart.models.ChartData
import com.skothr.ephemeris.chart.models.Location
import com.skothr.ephemeris.ephemeris.EphemerisProvider
import com.skothr.ephemeris.ephemeris.models.CelestialPosition
import com.skothr.ephemeris.chart.models.CelestialBody
import com.skothr.ephemeris.settings.model.AppSettings
import com.skothr.ephemeris.settings.model.CalculationSettings
import com.skothr.ephemeris.settings.model.Center
import com.skothr.ephemeris.settings.model.NodeType
import com.skothr.ephemeris.settings.model.ZodiacType
import java.time.LocalDateTime

class ChartCalculator(private val ephemeris: EphemerisProvider) {

    suspend fun calculate(
        dateTime: LocalDateTime, location: Location, settings: AppSettings,
    ): ChartData {
        val calc = settings.calculation
        val display = settings.display

        // Configure sidereal mode if needed
        if (calc.zodiacType == ZodiacType.SIDEREAL) {
            ephemeris.setSiderealMode(calc.ayanamsa.swissEphId)
        }

        // Configure topocentric position if needed
        if (calc.center == Center.TOPOCENTRIC) {
            ephemeris.setTopographicPosition(location.longitude, location.latitude, 0.0)
        }

        val flags = calc.toSwissEphFlags()
        val jd = ephemeris.julianDay(dateTime)

        val positions = mutableMapOf<CelestialBody, CelestialPosition>()
        for (body in display.enabledBodies) {
            // For North Node, switch body ID based on NodeType setting
            val bodyId = if (body == CelestialBody.NORTH_NODE && calc.nodeType == NodeType.MEAN_NODE) {
                CalculationSettings.SE_MEAN_NODE
            } else {
                body.swissEphId
            }
            positions[body] = ephemeris.calculateBody(jd, bodyId, flags)
        }

        val houseData = ephemeris.calculateHouses(

            jd, location.latitude, location.longitude, calc.houseSystem
        )

        val aspectCalculator = AspectCalculator(display.enabledAspects, display.aspectOrbs)
        val aspects = aspectCalculator.calculate(positions)

        return ChartData(
            dateTime = dateTime, location = location, positions = positions,
            houseData = houseData, aspects = aspects, houseSystem = calc.houseSystem,
        )
    }
}
```

- [ ] **Step 2: Update AspectCalculator to use display settings directly**

Replace the entire `AspectCalculator.kt`:

```kotlin
package com.skothr.ephemeris.chart

import com.skothr.ephemeris.chart.models.Aspect
import com.skothr.ephemeris.chart.models.AspectResult
import com.skothr.ephemeris.chart.models.CelestialBody
import com.skothr.ephemeris.ephemeris.models.CelestialPosition
import kotlin.math.abs
import kotlin.math.min

class AspectCalculator(
    private val enabledAspects: Set<Aspect>,
    private val aspectOrbs: Map<Aspect, Double>,
) {

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

                for (aspect in enabledAspects) {
                    val orb = aspectOrbs[aspect] ?: aspect.defaultOrb
                    val aspectOrb = abs(angularDiff - aspect.angle)
                    if (aspectOrb <= orb) {
                        val applying = isApplying(pos1, pos2, aspect)
                        results.add(
                            AspectResult(
                                body1 = body1, body2 = body2, aspect = aspect,
                                exactAngle = angularDiff, orb = aspectOrb, isApplying = applying,
                            )
                        )
                        break
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

    private fun isApplying(pos1: CelestialPosition, pos2: CelestialPosition, aspect: Aspect): Boolean {
        val currentDist = angularDistance(pos1.longitude, pos2.longitude)
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

- [ ] **Step 3: Rewrite ChartViewModel to observe settings**

Replace the entire `ChartViewModel.kt`:

```kotlin
package com.skothr.ephemeris.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skothr.ephemeris.chart.ChartCalculator
import com.skothr.ephemeris.chart.models.ChartData
import com.skothr.ephemeris.chart.models.Location
import com.skothr.ephemeris.ephemeris.EphemerisProvider
import com.skothr.ephemeris.settings.SettingsRepository
import com.skothr.ephemeris.settings.model.AppSettings
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class ChartViewModel(
    ephemeris: EphemerisProvider,
    private val ephemerisReady: kotlinx.coroutines.Deferred<Unit>,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val chartCalculator = ChartCalculator(ephemeris)

    private val _dateTime = MutableStateFlow(LocalDateTime.now())
    val dateTime: StateFlow<LocalDateTime> = _dateTime.asStateFlow()

    private val _location = MutableStateFlow(Location(40.7128, -74.0060))
    val location: StateFlow<Location> = _location.asStateFlow()

    private val _timezone = MutableStateFlow("America/New_York")
    val timezone: StateFlow<String> = _timezone.asStateFlow()

    private val _chartData = MutableStateFlow<ChartData?>(null)
    val chartData: StateFlow<ChartData?> = _chartData.asStateFlow()

    private val _isCalculating = MutableStateFlow(false)
    val isCalculating: StateFlow<Boolean> = _isCalculating.asStateFlow()

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    init {
        @OptIn(FlowPreview::class)
        viewModelScope.launch {
            ephemerisReady.await()
            combine(_dateTime, _location, settingsRepository.settings) { dt, loc, s ->
                Triple(dt, loc, s)
            }
                .debounce(33)
                .collectLatest { (dt, loc, s) ->
                    _isCalculating.value = true
                    try {
                        _chartData.value = chartCalculator.calculate(dt, loc, s)
                    } catch (e: Exception) {
                        Log.e("ChartViewModel", "Chart calculation failed", e)
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
}
```

- [ ] **Step 4: Update MainActivity to pass SettingsRepository**

Replace the entire `MainActivity.kt`:

```kotlin
package com.skothr.ephemeris

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
                return ChartViewModel(app.swissEphemeris, app.ephemerisReady, app.settingsRepository) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MainScreen(viewModel)
        }
    }
}
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/skothr/ephemeris/chart/ app/src/main/java/com/skothr/ephemeris/ui/ChartViewModel.kt app/src/main/java/com/skothr/ephemeris/MainActivity.kt
git commit -m "feat: wire settings into chart calculation pipeline"
```

---

## Task 6: Reusable Settings UI Components

**Files:**
- Create: `app/src/main/java/com/skothr/ephemeris/settings/components/SegmentedToggle.kt`
- Create: `app/src/main/java/com/skothr/ephemeris/settings/components/SettingsDropdown.kt`
- Create: `app/src/main/java/com/skothr/ephemeris/settings/components/SettingsSlider.kt`
- Create: `app/src/main/java/com/skothr/ephemeris/settings/components/OrbEditDialog.kt`

- [ ] **Step 1: Create SegmentedToggle.kt**

```kotlin
package com.skothr.ephemeris.settings.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun <T> SegmentedToggle(
    options: List<T>,
    selected: T,
    onSelected: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            val isSelected = option == selected
            OutlinedButton(
                onClick = { onSelected(option) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.surface,
                    contentColor = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                border = BorderStroke(
                    1.dp,
                    if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant,
                ),
            ) {
                Text(label(option))
            }
        }
    }
}
```

- [ ] **Step 2: Create SettingsDropdown.kt**

```kotlin
package com.skothr.ephemeris.settings.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SettingsDropdown(
    options: List<T>,
    selected: T,
    onSelected: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = label(selected),
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(label(option)) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}
```

- [ ] **Step 3: Create SettingsSlider.kt**

```kotlin
package com.skothr.ephemeris.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    formatValue: (Float) -> String = { "%.2f".format(it) },
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                formatValue(value),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
```

- [ ] **Step 4: Create OrbEditDialog.kt**

```kotlin
package com.skothr.ephemeris.settings.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun OrbEditDialog(
    aspectName: String,
    currentOrb: Double,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf("%.1f".format(currentOrb)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$aspectName Orb") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Orb (degrees)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = {
                text.toDoubleOrNull()?.let { value ->
                    if (value in 0.0..15.0) onConfirm(value)
                }
            }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/skothr/ephemeris/settings/components/
git commit -m "feat: add reusable settings UI components"
```

---

## Task 7: Calculation Tab

**Files:**
- Create: `app/src/main/java/com/skothr/ephemeris/settings/tabs/CalculationTab.kt`

- [ ] **Step 1: Create CalculationTab.kt**

```kotlin
package com.skothr.ephemeris.settings.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.skothr.ephemeris.chart.models.HouseSystem
import com.skothr.ephemeris.settings.components.SegmentedToggle
import com.skothr.ephemeris.settings.components.SettingsDropdown
import com.skothr.ephemeris.settings.model.*

@Composable
fun CalculationTab(
    settings: CalculationSettings,
    onZodiacTypeChanged: (ZodiacType) -> Unit,
    onAyanamsaChanged: (Ayanamsa) -> Unit,
    onNodeTypeChanged: (NodeType) -> Unit,
    onCenterChanged: (Center) -> Unit,
    onHouseSystemChanged: (HouseSystem) -> Unit,
    onSpeedChanged: (Boolean) -> Unit,
    onEquatorialChanged: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // Zodiac Type
        SectionLabel("Zodiac Type")
        SegmentedToggle(
            options = ZodiacType.entries,
            selected = settings.zodiacType,
            onSelected = onZodiacTypeChanged,
            label = { it.displayName },
        )

        // Ayanamsa (only visible for Sidereal)
        if (settings.zodiacType == ZodiacType.SIDEREAL) {
            SectionLabel("Ayanamsa")
            SettingsDropdown(
                options = Ayanamsa.entries,
                selected = settings.ayanamsa,
                onSelected = onAyanamsaChanged,
                label = { it.displayName },
            )
        }

        // Node Type
        SectionLabel("Node Type")
        SegmentedToggle(
            options = NodeType.entries,
            selected = settings.nodeType,
            onSelected = onNodeTypeChanged,
            label = { it.displayName },
        )

        // Center
        SectionLabel("Center")
        SegmentedToggle(
            options = Center.entries,
            selected = settings.center,
            onSelected = onCenterChanged,
            label = { it.displayName },
        )

        // House System
        SectionLabel("House System")
        SettingsDropdown(
            options = HouseSystem.entries,
            selected = settings.houseSystem,
            onSelected = onHouseSystemChanged,
            label = { it.displayName },
        )

        // Toggle flags
        SectionLabel("Options")
        SettingsToggleRow(
            label = "Speed in longitude",
            checked = settings.speedInLongitude,
            onCheckedChange = onSpeedChanged,
        )
        SettingsToggleRow(
            label = "Equatorial coordinates",
            checked = settings.equatorialCoordinates,
            onCheckedChange = onEquatorialChanged,
        )
    }
}

@Composable
internal fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 4.dp),
    )
}

@Composable
internal fun SettingsToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/skothr/ephemeris/settings/tabs/CalculationTab.kt
git commit -m "feat: add Calculation settings tab"
```

---

## Task 8: Display Tab

**Files:**
- Create: `app/src/main/java/com/skothr/ephemeris/settings/tabs/DisplayTab.kt`

- [ ] **Step 1: Create DisplayTab.kt**

```kotlin
package com.skothr.ephemeris.settings.tabs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.skothr.ephemeris.chart.models.Aspect
import com.skothr.ephemeris.chart.models.CelestialBody
import com.skothr.ephemeris.settings.components.OrbEditDialog
import com.skothr.ephemeris.settings.model.DisplaySettings

@Composable
fun DisplayTab(
    settings: DisplaySettings,
    onBodyToggled: (CelestialBody, Boolean) -> Unit,
    onAspectToggled: (Aspect, Boolean) -> Unit,
    onAspectOrbChanged: (Aspect, Double) -> Unit,
) {
    var editingAspect by remember { mutableStateOf<Aspect?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Bodies section
        SectionLabel("Bodies")

        // 2-column grid for bodies
        val bodies = CelestialBody.entries
        for (i in bodies.indices step 2) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BodyToggleItem(
                    body = bodies[i],
                    enabled = bodies[i] in settings.enabledBodies,
                    onToggle = { onBodyToggled(bodies[i], it) },
                    modifier = Modifier.weight(1f),
                )
                if (i + 1 < bodies.size) {
                    BodyToggleItem(
                        body = bodies[i + 1],
                        enabled = bodies[i + 1] in settings.enabledBodies,
                        onToggle = { onBodyToggled(bodies[i + 1], it) },
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Aspects section
        SectionLabel("Aspects")
        Text(
            "Tap orb value to edit",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))

        Aspect.entries.forEach { aspect ->
            AspectToggleRow(
                aspect = aspect,
                enabled = aspect in settings.enabledAspects,
                orb = settings.aspectOrbs[aspect] ?: aspect.defaultOrb,
                onToggle = { onAspectToggled(aspect, it) },
                onOrbClick = { editingAspect = aspect },
            )
        }
    }

    // Orb edit dialog
    editingAspect?.let { aspect ->
        OrbEditDialog(
            aspectName = aspect.displayName,
            currentOrb = settings.aspectOrbs[aspect] ?: aspect.defaultOrb,
            onConfirm = { newOrb ->
                onAspectOrbChanged(aspect, newOrb)
                editingAspect = null
            },
            onDismiss = { editingAspect = null },
        )
    }
}

@Composable
private fun BodyToggleItem(
    body: CelestialBody,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "${body.symbol} ${body.displayName}",
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Switch(checked = enabled, onCheckedChange = onToggle)
    }
}

@Composable
private fun AspectToggleRow(
    aspect: Aspect,
    enabled: Boolean,
    orb: Double,
    onToggle: (Boolean) -> Unit,
    onOrbClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Switch(checked = enabled, onCheckedChange = onToggle)
        Text(
            "${aspect.displayName} (${aspect.angle.toInt()}°)",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            "%.1f°".format(orb),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable(onClick = onOrbClick),
        )
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/skothr/ephemeris/settings/tabs/DisplayTab.kt
git commit -m "feat: add Display settings tab"
```

---

## Task 9: Visual Tab

**Files:**
- Create: `app/src/main/java/com/skothr/ephemeris/settings/tabs/VisualTab.kt`

- [ ] **Step 1: Create VisualTab.kt**

```kotlin
package com.skothr.ephemeris.settings.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skothr.ephemeris.chart.models.CelestialBody
import com.skothr.ephemeris.chart.models.ZodiacSign
import com.skothr.ephemeris.settings.components.SegmentedToggle
import com.skothr.ephemeris.settings.components.SettingsSlider
import com.skothr.ephemeris.settings.model.*

@Composable
fun VisualTab(
    settings: VisualSettings,
    onThemeChanged: (AppTheme) -> Unit,
    onSymbolStyleChanged: (SymbolStyle) -> Unit,
    onZodiacOuterChanged: (Float) -> Unit,
    onZodiacInnerChanged: (Float) -> Unit,
    onHouseOuterChanged: (Float) -> Unit,
    onHouseInnerChanged: (Float) -> Unit,
    onBodyRingChanged: (Float) -> Unit,
    onAspectThicknessChanged: (Float) -> Unit,
    onAspectOpacityChanged: (Float) -> Unit,
    onMajorStyleChanged: (LineStyle) -> Unit,
    onMinorStyleChanged: (LineStyle) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // Theme
        SectionLabel("Theme")
        SegmentedToggle(
            options = AppTheme.entries,
            selected = settings.theme,
            onSelected = onThemeChanged,
            label = { it.displayName },
        )

        // Symbol Style
        SectionLabel("Symbol Style")
        SegmentedToggle(
            options = SymbolStyle.entries,
            selected = settings.symbolStyle,
            onSelected = onSymbolStyleChanged,
            label = { it.displayName },
        )

        // Glyph preview
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(8.dp),
                )
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Show a sample of zodiac signs and planet symbols
            val previewSymbols = listOf(
                ZodiacSign.ARIES.symbol, ZodiacSign.TAURUS.symbol,
                ZodiacSign.GEMINI.symbol, ZodiacSign.CANCER.symbol,
                CelestialBody.SUN.symbol, CelestialBody.MOON.symbol,
                CelestialBody.MARS.symbol, CelestialBody.JUPITER.symbol,
            )
            previewSymbols.forEach { symbol ->
                Text(symbol, fontSize = 24.sp)
            }
        }

        // Ring Proportions
        SectionLabel("Ring Proportions")
        SettingsSlider(
            label = "Zodiac Outer",
            value = settings.zodiacOuterRadius,
            onValueChange = onZodiacOuterChanged,
            valueRange = 0.80f..1.00f,
            formatValue = { "${(it * 100).toInt()}%" },
        )
        SettingsSlider(
            label = "Zodiac Inner",
            value = settings.zodiacInnerRadius,
            onValueChange = onZodiacInnerChanged,
            valueRange = 0.70f..0.90f,
            formatValue = { "${(it * 100).toInt()}%" },
        )
        SettingsSlider(
            label = "House Outer",
            value = settings.houseOuterRadius,
            onValueChange = onHouseOuterChanged,
            valueRange = 0.60f..0.90f,
            formatValue = { "${(it * 100).toInt()}%" },
        )
        SettingsSlider(
            label = "House Inner",
            value = settings.houseInnerRadius,
            onValueChange = onHouseInnerChanged,
            valueRange = 0.50f..0.80f,
            formatValue = { "${(it * 100).toInt()}%" },
        )
        SettingsSlider(
            label = "Body Ring",
            value = settings.bodyRingRadius,
            onValueChange = onBodyRingChanged,
            valueRange = 0.40f..0.70f,
            formatValue = { "${(it * 100).toInt()}%" },
        )

        // Aspect Lines
        SectionLabel("Aspect Lines")
        SettingsSlider(
            label = "Line Thickness",
            value = settings.aspectLineThickness,
            onValueChange = onAspectThicknessChanged,
            valueRange = 0.5f..5.0f,
            formatValue = { "%.1f".format(it) },
        )
        SettingsSlider(
            label = "Line Opacity",
            value = settings.aspectLineOpacity,
            onValueChange = onAspectOpacityChanged,
            valueRange = 0.1f..1.0f,
            formatValue = { "${(it * 100).toInt()}%" },
        )

        SectionLabel("Major Aspect Style")
        SegmentedToggle(
            options = LineStyle.entries,
            selected = settings.majorAspectStyle,
            onSelected = onMajorStyleChanged,
            label = { it.displayName },
        )

        SectionLabel("Minor Aspect Style")
        SegmentedToggle(
            options = LineStyle.entries,
            selected = settings.minorAspectStyle,
            onSelected = onMinorStyleChanged,
            label = { it.displayName },
        )
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/skothr/ephemeris/settings/tabs/VisualTab.kt
git commit -m "feat: add Visual settings tab"
```

---

## Task 10: Settings Screen (Tabbed Container)

**Files:**
- Create: `app/src/main/java/com/skothr/ephemeris/settings/SettingsScreen.kt`

- [ ] **Step 1: Create SettingsScreen.kt**

```kotlin
package com.skothr.ephemeris.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.skothr.ephemeris.settings.model.AppSettings
import com.skothr.ephemeris.settings.tabs.CalculationTab
import com.skothr.ephemeris.settings.tabs.DisplayTab
import com.skothr.ephemeris.settings.tabs.VisualTab
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    repository: SettingsRepository,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        var selectedTab by remember { mutableIntStateOf(0) }
        val tabs = listOf("Calculation", "Display", "Visual")

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) },
                    )
                }
            }

            when (selectedTab) {
                0 -> CalculationTab(
                    settings = settings.calculation,
                    onZodiacTypeChanged = { scope.launch { repository.setZodiacType(it) } },
                    onAyanamsaChanged = { scope.launch { repository.setAyanamsa(it) } },
                    onNodeTypeChanged = { scope.launch { repository.setNodeType(it) } },
                    onCenterChanged = { scope.launch { repository.setCenter(it) } },
                    onHouseSystemChanged = { scope.launch { repository.setHouseSystem(it) } },
                    onSpeedChanged = { scope.launch { repository.setSpeedInLongitude(it) } },
                    onEquatorialChanged = { scope.launch { repository.setEquatorialCoordinates(it) } },
                )
                1 -> DisplayTab(
                    settings = settings.display,
                    onBodyToggled = { body, enabled -> scope.launch { repository.setBodyEnabled(body, enabled) } },
                    onAspectToggled = { aspect, enabled -> scope.launch { repository.setAspectEnabled(aspect, enabled) } },
                    onAspectOrbChanged = { aspect, orb -> scope.launch { repository.setAspectOrb(aspect, orb) } },
                )
                2 -> VisualTab(
                    settings = settings.visual,
                    onThemeChanged = { scope.launch { repository.setTheme(it) } },
                    onSymbolStyleChanged = { scope.launch { repository.setSymbolStyle(it) } },
                    onZodiacOuterChanged = { scope.launch { repository.setZodiacOuterRadius(it) } },
                    onZodiacInnerChanged = { scope.launch { repository.setZodiacInnerRadius(it) } },
                    onHouseOuterChanged = { scope.launch { repository.setHouseOuterRadius(it) } },
                    onHouseInnerChanged = { scope.launch { repository.setHouseInnerRadius(it) } },
                    onBodyRingChanged = { scope.launch { repository.setBodyRingRadius(it) } },
                    onAspectThicknessChanged = { scope.launch { repository.setAspectLineThickness(it) } },
                    onAspectOpacityChanged = { scope.launch { repository.setAspectLineOpacity(it) } },
                    onMajorStyleChanged = { scope.launch { repository.setMajorAspectStyle(it) } },
                    onMinorStyleChanged = { scope.launch { repository.setMinorAspectStyle(it) } },
                )
            }
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/skothr/ephemeris/settings/SettingsScreen.kt
git commit -m "feat: add tabbed SettingsScreen composable"
```

---

## Task 11: MainScreen — Gear Icon and Navigation

**Files:**
- Modify: `app/src/main/java/com/skothr/ephemeris/ui/MainScreen.kt`

- [ ] **Step 1: Add gear icon and settings navigation to MainScreen**

Replace the entire `MainScreen.kt`:

```kotlin
package com.skothr.ephemeris.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.skothr.ephemeris.settings.SettingsRepository
import com.skothr.ephemeris.settings.SettingsScreen
import com.skothr.ephemeris.settings.model.AppSettings
import com.skothr.ephemeris.ui.chart.ChartWheel
import com.skothr.ephemeris.ui.controls.DateTimeControls
import com.skothr.ephemeris.ui.controls.LocationControls
import com.skothr.ephemeris.ui.theme.EphemerisTheme

@Composable
fun MainScreen(viewModel: ChartViewModel, settingsRepository: SettingsRepository) {
    val chartData by viewModel.chartData.collectAsState()
    val dateTime by viewModel.dateTime.collectAsState()
    val location by viewModel.location.collectAsState()
    val timezone by viewModel.timezone.collectAsState()
    val isCalculating by viewModel.isCalculating.collectAsState()
    val settings by viewModel.settings.collectAsState()

    var showSettings by remember { mutableStateOf(false) }

    EphemerisTheme(darkTheme = settings.visual.theme == com.skothr.ephemeris.settings.model.AppTheme.DARK) {
        if (showSettings) {
            SettingsScreen(
                settings = settings,
                repository = settingsRepository,
                onBack = { showSettings = false },
            )
        } else {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                val scrollState = rememberScrollState()
                val coroutineScope = rememberCoroutineScope()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding(),
                ) {
                    // Chart area — scrollable, takes remaining space
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(scrollState),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        chartData?.let { data ->
                            ChartWheel(
                                chartData = data,
                                visualSettings = settings.visual,
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

                        // Gear icon overlay
                        IconButton(
                            onClick = { showSettings = true },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp),
                        ) {
                            Icon(
                                Icons.Filled.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    if (isCalculating) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        )
                    }

                    // Controls — fixed at bottom, outside scroll
                    DateTimeControls(
                        dateTime = dateTime,
                        onDateTimeChanged = { viewModel.updateDateTime(it) },
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                    LocationControls(
                        location = location,
                        timezone = timezone,
                        onLocationChanged = { loc, tz -> viewModel.updateLocation(loc, tz) },
                        onResultsVisible = {
                            coroutineScope.launch {
                                delay(100)
                                scrollState.animateScrollTo(scrollState.maxValue)
                            }
                        },
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 2: Update MainActivity to pass settingsRepository**

In `MainActivity.kt`, update the `setContent` block (line 28-30):

```kotlin
        setContent {
            val app = application as EphemerisApp
            MainScreen(viewModel, app.settingsRepository)
        }
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/skothr/ephemeris/ui/MainScreen.kt app/src/main/java/com/skothr/ephemeris/MainActivity.kt
git commit -m "feat: add gear icon and settings navigation to MainScreen"
```

---

## Task 12: Theme — Add Light Theme Support

**Files:**
- Modify: `app/src/main/java/com/skothr/ephemeris/ui/theme/Color.kt`
- Modify: `app/src/main/java/com/skothr/ephemeris/ui/theme/Theme.kt`

- [ ] **Step 1: Add light theme colors to Color.kt**

Add to the end of `Color.kt`:

```kotlin
// Light theme
val LightBackground = Color(0xFFF5F5F5)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFE8E8EC)
val OnLightText = Color(0xFF1A1A2E)
val OnLightTextVariant = Color(0xFF666680)
```

- [ ] **Step 2: Update Theme.kt to support light/dark switching**

Replace the entire `Theme.kt`:

```kotlin
package com.skothr.ephemeris.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    secondary = SecondaryAmber,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    onBackground = OnLightText,
    onSurface = OnLightText,
    onSurfaceVariant = OnLightTextVariant,
)

@Composable
fun EphemerisTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = EphemerisTypography,
        content = content,
    )
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/skothr/ephemeris/ui/theme/
git commit -m "feat: add light theme support"
```

---

## Task 13: Wire VisualSettings into Chart Rendering

**Files:**
- Modify: `app/src/main/java/com/skothr/ephemeris/ui/chart/WheelRenderer.kt`
- Modify: `app/src/main/java/com/skothr/ephemeris/ui/chart/ChartWheel.kt`
- Modify: `app/src/main/java/com/skothr/ephemeris/ui/chart/ChartColors.kt`

- [ ] **Step 1: Update WheelMath.calculateRingRadii to use VisualSettings**

In `WheelRenderer.kt`, replace the `calculateRingRadii` function (lines 41-50):

```kotlin
    fun calculateRingRadii(availableRadius: Float, visual: VisualSettings): RingRadii {
        return RingRadii(
            zodiacOuter = availableRadius * visual.zodiacOuterRadius,
            zodiacInner = availableRadius * visual.zodiacInnerRadius,
            houseOuter = availableRadius * visual.houseOuterRadius,
            houseInner = availableRadius * visual.houseInnerRadius,
            bodyRing = availableRadius * visual.bodyRingRadius,
            aspectInner = availableRadius * 0.10f,
        )
    }
```

Add the import at the top of `WheelRenderer.kt`:

```kotlin
import com.skothr.ephemeris.settings.model.VisualSettings
import com.skothr.ephemeris.settings.model.LineStyle
```

- [ ] **Step 2: Update drawAspects to use VisualSettings**

Replace the `drawAspects` function (lines 193-221):

```kotlin
    fun DrawScope.drawAspects(
        radii: RingRadii, cx: Float, cy: Float,
        aspects: List<AspectResult>,
        positions: Map<CelestialBody, com.skothr.ephemeris.ephemeris.models.CelestialPosition>,
        ascendant: Double,
        visual: VisualSettings,
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
            val orbFraction = (aspect.orb.toFloat() / (aspect.aspect.defaultOrb.toFloat())).coerceIn(0f, 1f)
            val alpha = visual.aspectLineOpacity * (1f - orbFraction * 0.7f)

            val lineStyle = if (aspect.aspect.isMajor) visual.majorAspectStyle else visual.minorAspectStyle
            val pathEffect = if (lineStyle == LineStyle.DASHED)
                PathEffect.dashPathEffect(floatArrayOf(8f, 4f)) else null

            drawLine(
                color = color.copy(alpha = alpha.coerceIn(0.1f, 1f)),
                start = Offset(x1, y1), end = Offset(x2, y2),
                strokeWidth = if (aspect.aspect.isMajor) visual.aspectLineThickness else visual.aspectLineThickness * 0.7f,
                pathEffect = pathEffect,
            )
        }
    }
```

- [ ] **Step 3: Update ChartWheel.kt to pass VisualSettings**

Replace the entire `ChartWheel.kt`:

```kotlin
package com.skothr.ephemeris.ui.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.skothr.ephemeris.chart.models.ChartData
import com.skothr.ephemeris.settings.model.VisualSettings

@Composable
fun ChartWheel(chartData: ChartData, visualSettings: VisualSettings, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier.fillMaxWidth().aspectRatio(1f)
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val availableRadius = minOf(cx, cy)
        val radii = WheelMath.calculateRingRadii(availableRadius, visualSettings)
        val asc = chartData.houseData.ascendant

        drawCircle(ChartColors.wheelBackground, availableRadius)

        with(WheelDrawing) {
            drawZodiacRing(radii, cx, cy, asc)
            drawHouseRing(radii, cx, cy, chartData.houseData, asc)
            drawBodies(radii, cx, cy, chartData.positions, asc)
            drawAspects(radii, cx, cy, chartData.aspects, chartData.positions, asc, visualSettings)
            drawAngles(radii, cx, cy, chartData.houseData, asc)
        }
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/skothr/ephemeris/ui/chart/
git commit -m "feat: wire VisualSettings into chart rendering"
```

---

## Task 14: Remove Obsolete Config Classes

**Files:**
- Delete: `app/src/main/java/com/skothr/ephemeris/chart/config/BodyConfig.kt`
- Delete: `app/src/main/java/com/skothr/ephemeris/chart/config/AspectConfig.kt`

- [ ] **Step 1: Delete the old config files**

These are no longer referenced — `ChartCalculator` now uses `AppSettings` directly, and `AspectCalculator` takes `Set<Aspect>` and `Map<Aspect, Double>` directly.

```bash
rm app/src/main/java/com/skothr/ephemeris/chart/config/BodyConfig.kt
rm app/src/main/java/com/skothr/ephemeris/chart/config/AspectConfig.kt
```

- [ ] **Step 2: Verify no remaining references**

```bash
cd /home/ai/ai-projects/android-ephemeris && grep -r "BodyConfig\|AspectConfig" app/src/main/java/ --include="*.kt"
```

Expected: No matches (all usages removed in Tasks 5).

- [ ] **Step 3: Commit**

```bash
git add -u app/src/main/java/com/skothr/ephemeris/chart/config/
git commit -m "chore: remove obsolete BodyConfig and AspectConfig"
```

---

## Task 15: Build Verification and Smoke Test

- [ ] **Step 1: Build the project**

```bash
cd /home/ai/ai-projects/android-ephemeris && ./gradlew assembleDebug 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 2: Fix any compilation errors**

If there are compilation errors, fix them. Common issues to watch for:
- Missing imports
- `EphemerisTheme` signature change (now takes `darkTheme: Boolean`) — check all call sites
- `ChartWheel` signature change (now takes `visualSettings`) — check all call sites
- `calculateBody` signature change (now takes `flags: Int`) — check all call sites

- [ ] **Step 3: Run existing tests**

```bash
cd /home/ai/ai-projects/android-ephemeris && ./gradlew test 2>&1 | tail -20
```

Expected: All existing tests pass (or no tests exist yet).

- [ ] **Step 4: Commit any fixes**

```bash
git add -A && git commit -m "fix: resolve compilation errors from settings integration"
```

Only run this step if there were fixes needed. Skip if the build was clean.

---

## Follow-Up (Not in this plan)

**Astrological font bundling:** The `SymbolStyle` enum and UI toggle are wired up, but the actual `.ttf` font file needs to be sourced, licensed, placed in `res/font/`, and loaded in `WheelRenderer` via a `Typeface` check on `SymbolStyle`. This is deferred until a specific open-source font is selected and its license verified.

**ChartColors light theme:** The chart wheel canvas uses its own `ChartColors` object (dark navy background, etc.) independent of Material theme. Making the chart wheel itself light-themed requires a separate pass to make `ChartColors` theme-aware.
