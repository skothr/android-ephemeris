# Expose Swiss Ephemeris Calculation Flags

**Date:** 2026-03-30

## Summary

Expose additional Swiss Ephemeris calculation flags in the Calculation settings tab, grouped into logical sections: an expanded Center enum, a Corrections section, and a Reference Frame section.

## Center Enum

Add `BARYCENTRIC` to the existing `Center` enum:

| Value | Display Name | Flag |
|-------|-------------|------|
| GEOCENTRIC | Geocentric | (none) |
| HELIOCENTRIC | Heliocentric | `SEFLG_HELCTR` |
| TOPOCENTRIC | Topocentric | `SEFLG_TOPOCTR` |
| BARYCENTRIC | Barycentric | `SEFLG_BARYCTR` |

## Corrections Section

Boolean toggles controlling what adjustments are applied to raw positions. All default to `false` (corrections applied).

| Field | Display Label | Flag | Value |
|-------|--------------|------|-------|
| `truePosition` | True position | `SEFLG_TRUEPOS` | `16` |
| `noGravitationalDeflection` | No gravitational deflection | `SEFLG_NOGDEFL` | `512` |
| `noAberration` | No aberration | `SEFLG_NOABERR` | `1024` |

## Reference Frame Section

Boolean toggles controlling the coordinate reference system. All default to `false`.

| Field | Display Label | Flag | Value |
|-------|--------------|------|-------|
| `j2000Equinox` | J2000 equinox | `SEFLG_J2000` | `32` |
| `noNutation` | No nutation | `SEFLG_NONUT` | `64` |
| `icrsFrame` | ICRS frame | `SEFLG_ICRS` | `128 * 1024` |

## Files to Modify

1. `app/src/main/java/com/skothr/ephemeris/settings/model/SettingsEnums.kt` — add `BARYCENTRIC` to `Center`
2. `app/src/main/java/com/skothr/ephemeris/settings/model/AppSettings.kt` — add 6 boolean fields to `CalculationSettings`, add flag constants, update `toSwissEphFlags()`
3. `app/src/main/java/com/skothr/ephemeris/settings/SettingsRepository.kt` — add 6 DataStore keys, read logic, setter functions
4. `app/src/main/java/com/skothr/ephemeris/settings/tabs/CalculationTab.kt` — add Corrections and Reference Frame sections with toggles
5. `app/src/main/java/com/skothr/ephemeris/settings/SettingsScreen.kt` — wire 6 new callbacks
