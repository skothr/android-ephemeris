# Android Ephemeris — Design Spec

## Overview

An Android app that displays astrological charts calculated using the Swiss Ephemeris, with scrubbable date/time/location controls for smooth real-time visualization of planetary positions, house cusps, and aspects.

Inspired by [astrolograph](https://github.com/skothr/astrolograph) (a C/C++/OpenGL node-graph-based astrological data viewer), distilled to a single-screen chart view without the node graph interface.

## Platform & Stack

- **Language:** Kotlin
- **UI framework:** Jetpack Compose
- **Min SDK:** 26 (Android 8.0, ~95% device coverage)
- **Ephemeris:** Swiss Ephemeris C library via JNI/NDK
- **Chart rendering:** Compose Canvas (custom drawing)
- **Build:** Gradle with CMake for native code

## Architecture

Single Gradle module with layered package separation. No Android dependencies in the domain/ephemeris layers — pure Kotlin for testability and future KMP extractability.

```
com.skothr.ephemeris/
├── ephemeris/              # Swiss Ephemeris JNI wrapper
│   ├── SwissEphemeris.kt          # Kotlin API (singleton, mutex for thread safety)
│   ├── jni/                       # C JNI bridge code + Swiss Ephemeris source
│   │   └── swisseph_wrapper.c
│   └── models/
│       ├── CelestialPosition.kt   # longitude, latitude, distance, speed, retrograde
│       ├── HouseData.kt           # 12 cusps + ASC/MC/DSC/IC angles
│       └── EphemerisResult.kt
│
├── chart/                  # Domain logic (no Android dependencies)
│   ├── ChartCalculator.kt        # Orchestrates ephemeris calls -> ChartData
│   ├── AspectCalculator.kt       # Aspect detection between body pairs
│   ├── models/
│   │   ├── CelestialBody.kt      # Enum: Sun..Pluto, Chiron, NorthNode, Lilith
│   │   ├── ZodiacSign.kt         # Enum with degree ranges, element, modality
│   │   ├── HouseSystem.kt        # Enum: Placidus, WholeSign, Equal, Koch
│   │   ├── Aspect.kt             # Enum: Conjunction..Sesquiquadrate with default orbs
│   │   ├── AspectResult.kt       # body1, body2, type, exact angle, orb, applying/separating
│   │   └── ChartData.kt          # Complete chart snapshot
│   └── config/
│       ├── AspectConfig.kt        # Which aspects enabled, custom orbs
│       └── BodyConfig.kt          # Which bodies enabled
│
└── ui/                     # Compose UI layer
    ├── MainScreen.kt              # Single screen composable
    ├── ChartViewModel.kt          # StateFlow<DateTime>, StateFlow<Location>, StateFlow<ChartData>
    ├── chart/
    │   ├── ChartWheel.kt          # Main Canvas composable
    │   ├── WheelRenderer.kt       # Stateless drawing functions
    │   └── ChartColors.kt         # Element/aspect color definitions
    ├── controls/
    │   ├── DateTimeControls.kt    # Vertical-drag scrub fields
    │   └── LocationControls.kt    # City search + raw coordinate input
    └── theme/                     # Material theme, typography
```

### Data Flow

```
User scrubs controls
    -> ViewModel updates DateTime/Location
    -> ChartCalculator calls SwissEphemeris via JNI
    -> New ChartData emitted via StateFlow
    -> ChartWheel recomposes with new positions
```

## Swiss Ephemeris JNI Layer

### Native Side

- `swisseph_wrapper.c` — JNI functions calling Swiss Ephemeris C API (`swe_calc_ut`, `swe_houses`, `swe_set_ephe_path`, etc.)
- Swiss Ephemeris source files compiled via CMake in the NDK build
- Ephemeris data files (`.se1`) bundled in `assets/` and extracted to app-internal storage on first launch. Include files covering 1800–2400 CE (covers historical charts and far-future transits; ~10MB)

### Kotlin API

`SwissEphemeris.kt` — singleton managing native lifecycle:

- `init(ephePath: String)` / `close()` — lifecycle management
- `calculateBody(julianDay: Double, body: CelestialBody): CelestialPosition`
- `calculateHouses(julianDay: Double, lat: Double, lon: Double, system: HouseSystem): HouseData`
- `julianDay(dateTime: LocalDateTime): Double`
- Thread safety via `Mutex` — Swiss Ephemeris C library is not thread-safe

### Tests

- Julian Day conversion against known reference values
- Planetary positions verified against published ephemeris tables (e.g., Sun longitude at J2000.0 epoch)
- Each supported house system returns valid cusp values
- Edge cases: extreme latitudes (polar regions where some house systems fail), date boundaries

## Chart Domain Models & Calculation

### Models

| Model | Fields |
|-------|--------|
| `CelestialBody` | Enum: Sun, Moon, Mercury, Venus, Mars, Jupiter, Saturn, Uranus, Neptune, Pluto, Chiron, NorthNode, Lilith |
| `ZodiacSign` | Enum with degree ranges, element (Fire/Earth/Air/Water), modality (Cardinal/Fixed/Mutable) |
| `HouseSystem` | Enum: Placidus, WholeSign, Equal, Koch |
| `Aspect` | Enum: Conjunction(0, orb 8°), Opposition(180, 8°), Trine(120, 8°), Square(90, 7°), Sextile(60, 6°), Quincunx(150, 3°), SemiSextile(30, 2°), SemiSquare(45, 2°), Sesquiquadrate(135, 2°) |
| `CelestialPosition` | longitude, latitude, distance, speed, retrograde flag |
| `AspectResult` | body1, body2, aspect type, exact angle, current orb, applying/separating |
| `HouseData` | 12 cusps + ASC/MC/DSC/IC angles |
| `ChartData` | dateTime, location, Map<CelestialBody, CelestialPosition>, HouseData, List<AspectResult> |

### ChartCalculator

- Accepts `SwissEphemeris`, `AspectConfig`, `BodyConfig`
- `calculate(dateTime, location, houseSystem): ChartData` — single call producing everything the UI needs
- Computes Julian Day, fetches all body positions, calculates houses, runs aspect detection

### AspectCalculator

- Checks each unique body pair for angular difference within configured orbs
- Determines applying vs. separating by comparing body speeds
- Returns sorted list of `AspectResult`

### Tests

- AspectCalculator: known positions -> expected aspects found/not found, correct applying/separating
- ChartCalculator: full integration test with known date -> verified against reference chart data
- ZodiacSign.fromLongitude: e.g., 185.0 == Libra
- Retrograde flag derived from negative speed
- Aspect orb boundaries: exactly at orb limit, just inside, just outside

## Chart Wheel Rendering

### Visual Structure (concentric rings, outside to inside)

1. **Zodiac ring** — 12 colored segments (by element), sign glyphs centered in each
2. **House ring** — 12 house divisions with house numbers, radial lines from center
3. **Body ring** — planet/body glyphs at zodiac longitude, with collision avoidance for clustered bodies
4. **Aspect web** — lines across center connecting aspected bodies. Solid = major, dashed = minor. Color encodes quality (red = square/opposition, blue = trine/sextile, green = conjunction)
5. **Angle markers** — ASC/MC/DSC/IC as emphasized lines extending through rings

### Implementation

- `ChartWheel.kt` — Compose Canvas composable taking `ChartData`
- `WheelRenderer.kt` — stateless drawing functions: `drawZodiacRing()`, `drawHouseRing()`, `drawBodies()`, `drawAspects()`, `drawAngles()`
- Each function independent — rendering math testable in isolation from Canvas

### Glyph Rendering

Unicode astrological symbols drawn as text on Canvas. Bundled custom font as fallback if device doesn't support the astro Unicode block.

### Smooth Scrubbing

No explicit animation needed. When date/time changes, ChartData recalculates and Canvas recomposes. All positions are angular (0-360), so intermediate frames show planets smoothly sliding around the wheel.

### Tests

- Body collision avoidance: N bodies at known longitudes -> no output positions overlap within glyph radius
- Longitude-to-canvas-angle conversion correctness
- Point-on-ring calculation for various ring radii
- Aspect line endpoints at correct positions
- Ring dimension calculations across various screen sizes

## Scrub Controls

### DateTimeControls

- Row of 6 labeled fields: Year, Month, Day, Hour, Minute, Second
- **Vertical drag** to increment/decrement (up = increase, down = decrease)
- Drag velocity maps to scrub speed: slow = 1 unit/gesture, fast = accelerating
- **Tap** a field to type a value directly via keyboard
- Natural rollover (e.g., 59 minutes -> next hour)
- **"Now" button** resets to current date/time

### LocationControls

- **City search** — text field with autocomplete dropdown. Bundled GeoNames cities15000 database (~25k cities, ~2MB) for offline use.
- **Raw coordinates** — lat/lon fields with same drag-to-scrub behavior as time fields. Tap to type exact values.
- **Timezone** — auto-resolved from coordinates using Android's `TimeZone` API with `java.time.ZoneId` for modern tz support. Displayed but not directly editable (derived from location).

### ViewModel Integration

- `ChartViewModel` holds `StateFlow<DateTime>` and `StateFlow<Location>`
- Scrub gestures emit throttled updates (~30 recalculations/sec during active scrubbing)
- Final full-precision calculation fires when scrubbing stops
- City search debounced at 300ms

### Tests

- DateTime rollover: Dec 31 23:59:59 -> Jan 1 00:00:00 next year
- Drag velocity to increment mapping: verify acceleration curve
- City search: "New" returns New York, New Delhi, etc. in ranked order
- Timezone resolution: known coordinates -> expected timezone
- Throttle behavior: rapid input changes produce <= 30 calculations/sec

## Future Directions

> **Note:** Everything in this section is out of scope for the initial prototype. Listed here to inform architectural decisions and document potential evolution paths.

### Near-term (architecture already supports)

- **Configurable bodies** — UI for toggling individual celestial bodies on/off (BodyConfig already exists)
- **Configurable aspects & orbs** — UI for toggling aspects and adjusting orb values (AspectConfig already exists)
- **House system selector** — already calculated, just needs a UI dropdown
- **Chart save/load** — persist ChartData snapshots with Room DB, name and recall charts

### Medium-term (moderate additions)

- **Transit overlay** — second chart ring showing current transits against a natal chart (two ChartData instances on one wheel)
- **Synastry / chart comparison** — two charts overlaid with inter-chart aspects
- **Secondary progressions** — ProgressCalculator derives progressed DateTime, feeds same ChartCalculator
- **Time-lapse playback** — auto-scrub at configurable speed, animation loop driving DateTime state

### Longer-term (bigger lifts)

- **KMP shared core** — `chart/` and `ephemeris/` packages have no Android dependencies, extractable to Kotlin Multiplatform for iOS
- **Node graph interface** — bring back the astrolograph concept with touch-friendly node editor on mobile
- **Aspect pattern detection** — Grand Trine, T-Square, Yod, etc. algorithmic detection from aspect list
- **Notifications** — alert on significant exact transits (e.g., "Saturn conjunct natal Sun today")
