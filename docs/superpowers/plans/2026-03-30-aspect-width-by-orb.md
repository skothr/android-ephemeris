# Aspect Width by Orb Strength — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an option to scale aspect line width proportionally to orb exactness, with a configurable orb range.

**Architecture:** Two new fields in `VisualSettings` (`scaleWidthByOrb`, `widthScaleOrb`) flow through DataStore persistence, settings UI, and into the renderer. The renderer multiplies the existing base width (major/minor) by an orb-derived scale factor when the toggle is on.

**Tech Stack:** Kotlin, Jetpack Compose, DataStore Preferences

---

### Task 1: Add fields to VisualSettings

**Files:**
- Modify: `app/src/main/java/com/skothr/ephemeris/settings/model/AppSettings.kt:49-64`

- [ ] **Step 1: Add scaleWidthByOrb and widthScaleOrb to VisualSettings**

In `AppSettings.kt`, add two new fields to the `VisualSettings` data class, after `minorAspectStyle`:

```kotlin
data class VisualSettings(
    val theme: AppTheme = AppTheme.DARK,
    val symbolStyle: SymbolStyle = SymbolStyle.SYSTEM,
    val lockAscendant: Boolean = false,
    val coloredZodiacBands: Boolean = false,
    val zodiacOuterRadius: Float = 0.95f,
    val zodiacInnerRadius: Float = 0.82f,
    val houseOuterRadius: Float = 0.82f,
    val houseInnerRadius: Float = 0.68f,
    val bodyRingRadius: Float = 0.58f,
    val aspectInnerRadius: Float = 0.10f,
    val aspectLineThickness: Float = 2.0f,
    val aspectLineOpacity: Float = 0.8f,
    val majorAspectStyle: LineStyle = LineStyle.SOLID,
    val minorAspectStyle: LineStyle = LineStyle.DASHED,
    val scaleWidthByOrb: Boolean = false,
    val widthScaleOrb: Float = 6.0f,
)
```

- [ ] **Step 2: Verify the project compiles**

Run: `./gradlew compileDebugKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/skothr/ephemeris/settings/model/AppSettings.kt
git commit -m "feat: add scaleWidthByOrb and widthScaleOrb fields to VisualSettings"
```

---

### Task 2: Add persistence in SettingsRepository

**Files:**
- Modify: `app/src/main/java/com/skothr/ephemeris/settings/SettingsRepository.kt`

- [ ] **Step 1: Add DataStore keys**

After the existing `minorStyleKey` declaration (line 46), add:

```kotlin
    private val scaleWidthByOrbKey = booleanPreferencesKey("visual_aspect_width_scale")
    private val widthScaleOrbKey = floatPreferencesKey("visual_aspect_width_scale_orb")
```

- [ ] **Step 2: Read new keys in the settings Flow**

In the `settings` Flow, inside the `VisualSettings(...)` constructor (after the `minorAspectStyle` line), add:

```kotlin
                scaleWidthByOrb = prefs[scaleWidthByOrbKey] ?: defaults.visual.scaleWidthByOrb,
                widthScaleOrb = prefs[widthScaleOrbKey] ?: defaults.visual.widthScaleOrb,
```

- [ ] **Step 3: Add setter functions**

After the existing `setMinorAspectStyle` function (line 121), add:

```kotlin
    suspend fun setScaleWidthByOrb(value: Boolean) = context.dataStore.edit { it[scaleWidthByOrbKey] = value }
    suspend fun setWidthScaleOrb(value: Float) = context.dataStore.edit { it[widthScaleOrbKey] = value }
```

- [ ] **Step 4: Verify the project compiles**

Run: `./gradlew compileDebugKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/skothr/ephemeris/settings/SettingsRepository.kt
git commit -m "feat: persist scaleWidthByOrb and widthScaleOrb settings"
```

---

### Task 3: Apply width scaling in WheelRenderer

**Files:**
- Modify: `app/src/main/java/com/skothr/ephemeris/ui/chart/WheelRenderer.kt:258-263`

- [ ] **Step 1: Update the strokeWidth calculation in drawAspects**

Replace the current `drawLine` call (lines 258-263) with width scaling logic:

```kotlin
            val baseWidth = if (aspect.aspect.isMajor) visual.aspectLineThickness else visual.aspectLineThickness * 0.7f
            val strokeWidth = if (visual.scaleWidthByOrb) {
                val widthOrbFraction = (aspect.orb.toFloat() / visual.widthScaleOrb).coerceIn(0f, 1f)
                baseWidth * (1f - widthOrbFraction * 0.7f)
            } else {
                baseWidth
            }

            drawLine(
                color = color.copy(alpha = alpha.coerceIn(0.1f, 1f)),
                start = Offset(x1, y1), end = Offset(x2, y2),
                strokeWidth = strokeWidth,
                pathEffect = pathEffect,
            )
```

- [ ] **Step 2: Verify the project compiles**

Run: `./gradlew compileDebugKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/skothr/ephemeris/ui/chart/WheelRenderer.kt
git commit -m "feat: scale aspect line width by orb when enabled"
```

---

### Task 4: Add settings UI in VisualTab

**Files:**
- Modify: `app/src/main/java/com/skothr/ephemeris/settings/tabs/VisualTab.kt`
- Modify: `app/src/main/java/com/skothr/ephemeris/settings/SettingsScreen.kt`

- [ ] **Step 1: Add callback parameters to VisualTab**

Add two new callback parameters to the `VisualTab` composable function signature, after `onMinorStyleChanged`:

```kotlin
    onScaleWidthByOrbChanged: (Boolean) -> Unit,
    onWidthScaleOrbChanged: (Float) -> Unit,
```

- [ ] **Step 2: Add the toggle and conditional slider in the UI**

After the "Minor Aspect Style" `SegmentedToggle` block (line 187, before the closing `}`), add:

```kotlin
        SettingsToggleRow(
            label = "Scale width by orb",
            checked = settings.scaleWidthByOrb,
            onCheckedChange = onScaleWidthByOrbChanged,
        )
        if (settings.scaleWidthByOrb) {
            SettingsSlider(
                label = "Width Scale Orb",
                value = settings.widthScaleOrb,
                onValueChange = onWidthScaleOrbChanged,
                valueRange = 1.0f..15.0f,
                formatValue = { "%.1f°".format(it) },
            )
        }
```

- [ ] **Step 3: Wire up callbacks in SettingsScreen**

In `SettingsScreen.kt`, in the `VisualTab(...)` call (after `onMinorStyleChanged`), add:

```kotlin
                    onScaleWidthByOrbChanged = { scope.launch { repository.setScaleWidthByOrb(it) } },
                    onWidthScaleOrbChanged = { scope.launch { repository.setWidthScaleOrb(it) } },
```

- [ ] **Step 4: Verify the project compiles**

Run: `./gradlew compileDebugKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/skothr/ephemeris/settings/tabs/VisualTab.kt \
       app/src/main/java/com/skothr/ephemeris/settings/SettingsScreen.kt
git commit -m "feat: add width-by-orb toggle and slider to Visual settings tab"
```

---

### Task 5: Verify existing tests still pass

**Files:**
- Test: `app/src/test/java/com/skothr/ephemeris/ui/chart/WheelRendererTest.kt`

- [ ] **Step 1: Run existing tests**

Run: `./gradlew test 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL, all tests pass. The new `VisualSettings` fields have defaults, so `VisualSettings()` calls in existing tests remain valid.
