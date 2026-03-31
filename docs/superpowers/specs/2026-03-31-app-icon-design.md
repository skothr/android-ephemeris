# Ephemeris App Icon Design

## Overview

Design and implement a custom launcher icon for the Ephemeris Android app, replacing the current placeholder (white background / black foreground). The icon should communicate "technical astrological chart tool" through geometric precision and subtle color, consistent with the Swiss Ephemeris foundation of the app.

## Design Direction

**Concept:** Geometric zodiac wheel with muted element-colored markers and a central sun glyph on a dark background. Combines abstract geometry (style A) with symbolic elements (style B) -- technical and clean rather than illustrative or "horoscope-y."

## Visual Specification

### Structure (concentric, inside-out)

1. **Central sun glyph** -- Gold (#FFB300) circle-with-dot. The focal point and only saturated element.
   - Outer circle radius: 16 (viewBox 200x200)
   - Inner dot radius: 3.5
   - Stroke width: 2.2

2. **Inner ring** -- Desaturated blue-gray (#4A5578) at radius 68.
   - Stroke width: 1.3, opacity 0.7

3. **Outer ring** -- Same blue-gray (#4A5578) at radius 88.
   - Stroke width: 2.2

4. **12 radial tick marks** -- Between inner and outer rings at 30-degree intervals (sign boundaries: 0, 30, 60, ... 330 degrees).
   - Stroke: #4A5578, width 1.2, opacity 0.55

5. **12 element marker dots** -- On the outer ring, offset +15 degrees from tick marks (centered in each sign segment). Muted element colors:
   - Fire (Aries/Leo/Sag): #A8656A (muted rose)
   - Earth (Taurus/Virgo/Cap): #5E8A68 (muted sage)
   - Air (Gemini/Libra/Aqua): #9E8A5C (muted gold)
   - Water (Cancer/Scorpio/Pisces): #5880A0 (muted steel blue)
   - Dot radius: 3.5 (full size)

### Dot positions (r=88, center=100,100, offset +15 degrees)

| Angle | Sign       | Element | x     | y     |
|-------|------------|---------|-------|-------|
| 15    | Aries      | Fire    | 122.8 | 15.0  |
| 45    | Taurus     | Earth   | 162.2 | 37.8  |
| 75    | Gemini     | Air     | 185.0 | 77.2  |
| 105   | Cancer     | Water   | 185.0 | 122.8 |
| 135   | Leo        | Fire    | 162.2 | 162.2 |
| 165   | Virgo      | Earth   | 122.8 | 185.0 |
| 195   | Libra      | Air     | 77.2  | 185.0 |
| 225   | Scorpio    | Water   | 37.8  | 162.2 |
| 255   | Sagittarius| Fire    | 15.0  | 122.8 |
| 285   | Capricorn  | Earth   | 15.0  | 77.2  |
| 315   | Aquarius   | Air     | 37.8  | 37.8  |
| 345   | Pisces     | Water   | 77.2  | 15.0  |

### Background

- Solid dark circle: #121212
- No gradient or embellishments

### Color Palette Summary

| Role            | Color   | Notes                        |
|-----------------|---------|------------------------------|
| Sun glyph       | #FFB300 | Gold -- only saturated color |
| Rings/ticks     | #4A5578 | Desaturated blue-gray        |
| Fire dots       | #A8656A | Muted rose                   |
| Earth dots      | #5E8A68 | Muted sage                   |
| Air dots        | #9E8A5C | Muted gold                   |
| Water dots      | #5880A0 | Muted steel blue             |
| Background      | #121212 | Near-black                   |

## Android Implementation

### Adaptive Icon (API 26+)

- **Background layer:** Solid #121212 color resource
- **Foreground layer:** Vector drawable (XML) containing the wheel design, sized within the 66% safe zone per Material Design adaptive icon guidelines
- Both layers sized at 108dp x 108dp (the adaptive icon canvas)

### Size-Responsive Scaling

The design is pure geometry with no fine detail, so a single vector drawable scales well. For the smallest sizes (mdpi 48x48), the element dots and sun glyph should be proportionally larger to remain visible:

- **Full/xxxhdpi:** Dot r=3.5, sun r=16, strokes as specified
- **Medium (xxxhdpi 108dp):** Dot r=5.5, sun r=18, slightly thicker strokes
- **Small (mdpi 48dp):** Dot r=8, sun r=22, thicker strokes (3.5/2.0)

### File Structure

```
app/src/main/res/
  mipmap-anydpi-v26/
    ic_launcher.xml          (adaptive icon pointing to bg + fg)
  drawable/
    ic_launcher_background.xml  (solid #121212)
    ic_launcher_foreground.xml  (wheel vector drawable)
  values/
    ic_launcher_background.xml  (color resource #121212)
```

### Legacy Fallback

For pre-API-26 devices, generate rasterized PNGs at each density from the vector source:
- mipmap-mdpi: 48x48
- mipmap-hdpi: 72x72
- mipmap-xhdpi: 96x96
- mipmap-xxhdpi: 144x144
- mipmap-xxxhdpi: 192x192

## Preview

See `docs/icon-preview.html` for a live browser preview at three sizes.

## Design Rationale

- **Geometric wheel** communicates "chart tool" immediately to astrology users
- **Muted element colors** add recognizable detail without being loud or "horoscope-y"
- **Gold sun as focal point** draws the eye and provides brand warmth
- **Desaturated structure** keeps the technical, Swiss Ephemeris precision feel
- **Dots offset 15 degrees** from tick marks prevent visual clumping at cardinal axes
- **Dark background** matches the app's default dark theme
