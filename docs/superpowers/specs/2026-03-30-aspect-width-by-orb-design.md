# Scale Aspect Line Width by Orb Strength

**Date:** 2026-03-30

## Summary

Add an option to scale aspect line width proportionally to how exact the aspect is (how close the orb is to 0). Tighter aspects render with thicker lines, wide-orb aspects render thinner. This layers on top of the existing major/minor thickness distinction.

## Data Model

Two new fields in `VisualSettings` (`AppSettings.kt`):

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `scaleWidthByOrb` | `Boolean` | `false` | Toggle for orb-based width scaling |
| `widthScaleOrb` | `Double` | `6.0` | Orb in degrees at which minimum width is reached |

## Rendering

In `WheelRenderer.kt` `drawAspects()`, after computing the base width from the major/minor classification:

```
baseWidth = if (aspect.isMajor) visual.aspectLineThickness else visual.aspectLineThickness * 0.7f
```

When `scaleWidthByOrb` is enabled, apply an additional multiplier:

```
orbFraction = (orb / visual.widthScaleOrb).coerceIn(0.0, 1.0)
widthScale = 1f - orbFraction * 0.7f
finalWidth = baseWidth * widthScale
```

- At 0° orb (exact): `widthScale = 1.0` (full base width)
- At `widthScaleOrb` degrees or beyond: `widthScale = 0.3` (30% of base width)
- Linear interpolation between these extremes

When disabled, rendering is unchanged from current behavior.

## Settings UI

In `VisualTab.kt`, within the aspect settings section:

1. **Toggle row** — "Scale Width by Orb" with a switch
2. **Conditional slider** — "Width Scale Orb" visible only when toggle is on
   - Range: 1.0° – 15.0°
   - Default: 6.0°
   - Displays value with 1 decimal place and degree symbol

## Persistence

Two new keys in `SettingsRepository.kt`:

| DataStore Key | Type | Maps To |
|--------------|------|---------|
| `visual_aspect_width_scale` | `Boolean` | `VisualSettings.scaleWidthByOrb` |
| `visual_aspect_width_scale_orb` | `Float` | `VisualSettings.widthScaleOrb` |

## Files to Modify

1. `app/src/main/java/com/skothr/ephemeris/settings/model/AppSettings.kt` — add fields to `VisualSettings`
2. `app/src/main/java/com/skothr/ephemeris/ui/chart/WheelRenderer.kt` — apply width scaling in `drawAspects()`
3. `app/src/main/java/com/skothr/ephemeris/settings/tabs/VisualTab.kt` — add toggle and slider UI
4. `app/src/main/java/com/skothr/ephemeris/settings/SettingsRepository.kt` — add persistence keys and read/write logic
