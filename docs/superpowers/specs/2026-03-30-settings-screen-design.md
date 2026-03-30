# Settings Screen Design

## Overview

Add a full-screen tabbed settings screen to the Android Ephemeris app, accessible via a gear icon on the main screen. Three tabs: Calculation, Display, Visual. Settings persist across app restarts via Jetpack DataStore.

## Navigation

- Gear icon (⚙) in top-right corner of MainScreen, overlaying the chart area
- Tapping opens a full-screen SettingsScreen with a back arrow to return
- No navigation library needed — simple conditional composable switching in MainActivity/MainScreen with a `showSettings` state boolean

## Settings Screen Structure

### Tab 1: Calculation

| Setting | Control | Values | Default |
|---------|---------|--------|---------|
| Zodiac Type | Segmented toggle | Tropical, Sidereal | Tropical |
| Ayanamsa | Dropdown (visible only when Sidereal) | Lahiri, Raman, Krishnamurti, Fagan-Bradley | Lahiri |
| Node Type | Segmented toggle | True Node, Mean Node | True Node |
| Center | Segmented toggle | Geocentric, Heliocentric, Topocentric | Geocentric |
| House System | Dropdown/combobox | Placidus, Whole Sign, Equal, Koch | Placidus |
| Speed in Longitude | Toggle switch | on/off | on |
| Equatorial Coordinates | Toggle switch | on/off | off |

### Tab 2: Display

**Bodies** — Toggle switch for each, listed in a 2-column grid:

| Body | Symbol | Default |
|------|--------|---------|
| Sun | ☉ | on |
| Moon | ☽ | on |
| Mercury | ☿ | on |
| Venus | ♀ | on |
| Mars | ♂ | on |
| Jupiter | ♃ | on |
| Saturn | ♄ | on |
| Uranus | ♅ | on |
| Neptune | ♆ | on |
| Pluto | ♇ | on |
| Chiron | ⚷ | on |
| North Node | ☊ | on |
| Lilith | ⚸ | on |

**Aspects** — Toggle switch + tappable orb value for each:

| Aspect | Angle | Default Orb | Default |
|--------|-------|-------------|---------|
| Conjunction | 0° | 8.0° | on |
| Opposition | 180° | 8.0° | on |
| Trine | 120° | 8.0° | on |
| Square | 90° | 8.0° | on |
| Sextile | 60° | 6.0° | on |
| Quincunx | 150° | 3.0° | on |
| Semi-sextile | 30° | 2.0° | on |
| Semi-square | 45° | 2.0° | on |
| Sesquiquadrate | 135° | 2.0° | on |

Tapping an orb value opens an inline edit field or small dialog to type a new value.

### Tab 3: Visual

| Setting | Control | Values | Default |
|---------|---------|--------|---------|
| Theme | Segmented toggle | Dark, Light | Dark |
| Symbol Style | Segmented toggle | System (Unicode), Astro (bundled font) | System |
| Zodiac Ring | Range slider | outer 80-100%, inner 70-90% of radius | 82-95% |
| House Ring | Range slider | outer 60-90%, inner 50-80% of radius | 68-82% |
| Body Ring | Slider | 40-70% of radius | 58% |
| Aspect Line Thickness | Slider | 0.5 - 5.0 | 2.0 |
| Aspect Line Opacity | Slider | 10% - 100% | 80% |
| Major Aspect Style | Segmented toggle | Solid, Dashed | Solid |
| Minor Aspect Style | Segmented toggle | Solid, Dashed | Dashed |

A glyph preview row below Symbol Style shows current symbols in the selected style.

## Architecture

### Persistence: Jetpack Preferences DataStore

Add `androidx.datastore:datastore-preferences` dependency. Single DataStore instance created in `EphemerisApp`, injected into ViewModels.

Keys organized by prefix:
- `calc_zodiac_type`, `calc_ayanamsa`, `calc_node_type`, `calc_center`, `calc_house_system`, `calc_speed`, `calc_equatorial`
- `display_body_{name}`, `display_aspect_{name}`, `display_aspect_orb_{name}`
- `visual_theme`, `visual_symbol_style`, `visual_zodiac_outer`, `visual_zodiac_inner`, `visual_house_outer`, `visual_house_inner`, `visual_body_ring`, `visual_aspect_thickness`, `visual_aspect_opacity`, `visual_major_style`, `visual_minor_style`

### Data Model

```
data class CalculationSettings(
    val zodiacType: ZodiacType,          // TROPICAL, SIDEREAL
    val ayanamsa: Ayanamsa,              // LAHIRI, RAMAN, KRISHNAMURTI, FAGAN_BRADLEY
    val nodeType: NodeType,              // TRUE_NODE, MEAN_NODE
    val center: Center,                  // GEOCENTRIC, HELIOCENTRIC, TOPOCENTRIC
    val houseSystem: HouseSystem,        // existing enum
    val speedInLongitude: Boolean,
    val equatorialCoordinates: Boolean
)

data class DisplaySettings(
    val enabledBodies: Set<CelestialBody>,
    val enabledAspects: Set<Aspect>,
    val aspectOrbs: Map<Aspect, Double>
)

data class VisualSettings(
    val theme: AppTheme,                 // DARK, LIGHT
    val symbolStyle: SymbolStyle,        // SYSTEM, ASTRO
    val zodiacOuterRadius: Float,        // 0.80-1.00
    val zodiacInnerRadius: Float,        // 0.70-0.90
    val houseOuterRadius: Float,         // 0.60-0.90
    val houseInnerRadius: Float,         // 0.50-0.80
    val bodyRingRadius: Float,           // 0.40-0.70
    val aspectLineThickness: Float,      // 0.5-5.0
    val aspectLineOpacity: Float,        // 0.1-1.0
    val majorAspectStyle: LineStyle,     // SOLID, DASHED
    val minorAspectStyle: LineStyle      // SOLID, DASHED
)

data class AppSettings(
    val calculation: CalculationSettings,
    val display: DisplaySettings,
    val visual: VisualSettings
)
```

### New Enums

```
enum class ZodiacType { TROPICAL, SIDEREAL }
enum class Ayanamsa { LAHIRI, RAMAN, KRISHNAMURTI, FAGAN_BRADLEY }
enum class NodeType { TRUE_NODE, MEAN_NODE }
enum class Center { GEOCENTRIC, HELIOCENTRIC, TOPOCENTRIC }
enum class AppTheme { DARK, LIGHT }
enum class SymbolStyle { SYSTEM, ASTRO }
enum class LineStyle { SOLID, DASHED }
```

### SettingsViewModel

- Reads/writes DataStore
- Exposes `StateFlow<AppSettings>` for the settings screen UI
- Each setting change writes to DataStore immediately (no save button)

### Integration with ChartViewModel

- ChartViewModel observes the same DataStore flows
- `CalculationSettings` changes trigger chart recalculation (build SEFLG flags from settings, pass to SwissEphemeris)
- `DisplaySettings` changes filter which bodies/aspects are shown in ChartData
- `VisualSettings` changes are read by WheelRenderer at draw time

### Swiss Ephemeris Flag Mapping

```
var flags = 0
if (speedInLongitude) flags |= SEFLG_SPEED
if (zodiacType == SIDEREAL) flags |= SEFLG_SIDEREAL
if (nodeType == TRUE_NODE) flags |= SEFLG_TRUENODE
if (center == HELIOCENTRIC) flags |= SEFLG_HELCTR
if (center == TOPOCENTRIC) flags |= SEFLG_TOPOCTR
if (equatorialCoordinates) flags |= SEFLG_EQUATORIAL
```

Ayanamsa set via `swe_set_sid_mode()` JNI call (new native function needed).

### Astrological Font

Bundle an open-source astrological TrueType font in `res/font/`. Candidates:
- AstroDotBasic
- Hamburg Symbole
- Astronomicon

The font provides proper astrological glyphs for zodiac signs and planets. WheelRenderer checks `SymbolStyle` and loads either system Typeface or the bundled font.

Need to verify licensing for whichever font is chosen at implementation time.

### New Files

```
settings/
├── SettingsScreen.kt          -- Top-level tabbed settings composable
├── SettingsViewModel.kt       -- DataStore read/write, StateFlow<AppSettings>
├── SettingsRepository.kt      -- DataStore access layer
├── model/
│   ├── AppSettings.kt         -- Data classes above
│   ├── ZodiacType.kt          -- New enum
│   ├── Ayanamsa.kt            -- New enum
│   ├── NodeType.kt            -- New enum
│   ├── Center.kt              -- New enum
│   ├── AppTheme.kt            -- New enum
│   ├── SymbolStyle.kt         -- New enum
│   └── LineStyle.kt           -- New enum
├── tabs/
│   ├── CalculationTab.kt      -- Calculation settings composables
│   ├── DisplayTab.kt          -- Body/aspect toggle composables
│   └── VisualTab.kt           -- Theme/style/slider composables
└── components/
    ├── SegmentedToggle.kt     -- Reusable segmented button row
    ├── SettingsDropdown.kt    -- Reusable dropdown/combobox
    ├── SettingsSlider.kt      -- Labeled slider with value display
    └── OrbEditDialog.kt       -- Small dialog for editing orb values
```

### Modified Files

- `build.gradle.kts` — add DataStore dependency
- `EphemerisApp.kt` — create DataStore singleton
- `MainScreen.kt` — add gear icon, settings navigation state
- `ChartViewModel.kt` — observe settings, build SEFLG flags, pass to calculations
- `SwissEphemeris.kt` — add `nativeSetSiderealMode()` JNI function, accept flags parameter in calculate methods
- `ChartCalculator.kt` — pass flags through to ephemeris calls
- `WheelRenderer.kt` — read VisualSettings for ring proportions, line styles, font selection
- `ChartColors.kt` — support light theme variant
- `Theme.kt` — add light theme
- `BodyConfig.kt` / `AspectConfig.kt` — replace hardcoded defaults with DataStore-backed values (may be folded into DisplaySettings)

### JNI Changes

New native functions needed:
- `nativeSetSiderealMode(ayanamsa: Int)` — wraps `swe_set_sid_mode()`
- Modify `nativeCalculateBody` to accept `flags: Int` parameter
- Modify `nativeCalculateHouses` to accept `flags: Int` parameter

## Testing

- Unit tests for SEFLG flag building from CalculationSettings
- Unit tests for settings data class defaults and serialization
- UI tests for each tab rendering and interaction
- Integration test: change a setting → verify chart recalculates with new parameters
