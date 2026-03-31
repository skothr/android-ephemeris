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
    private val lockAscendantKey = booleanPreferencesKey("visual_lock_ascendant")
    private val coloredZodiacBandsKey = booleanPreferencesKey("visual_colored_zodiac_bands")
    private val zodiacOuterKey = floatPreferencesKey("visual_zodiac_outer")
    private val zodiacInnerKey = floatPreferencesKey("visual_zodiac_inner")
    private val houseOuterKey = floatPreferencesKey("visual_house_outer")
    private val houseInnerKey = floatPreferencesKey("visual_house_inner")
    private val bodyRingKey = floatPreferencesKey("visual_body_ring")
    private val aspectInnerKey = floatPreferencesKey("visual_aspect_inner")
    private val aspectThicknessKey = floatPreferencesKey("visual_aspect_thickness")
    private val aspectOpacityKey = floatPreferencesKey("visual_aspect_opacity")
    private val majorStyleKey = stringPreferencesKey("visual_major_style")
    private val minorStyleKey = stringPreferencesKey("visual_minor_style")
    private val scaleWidthByOrbKey = booleanPreferencesKey("visual_aspect_width_scale")
    private val widthScaleOrbKey = floatPreferencesKey("visual_aspect_width_scale_orb")

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
                lockAscendant = prefs[lockAscendantKey] ?: defaults.visual.lockAscendant,
                coloredZodiacBands = prefs[coloredZodiacBandsKey] ?: defaults.visual.coloredZodiacBands,
                zodiacOuterRadius = prefs[zodiacOuterKey] ?: defaults.visual.zodiacOuterRadius,
                zodiacInnerRadius = prefs[zodiacInnerKey] ?: defaults.visual.zodiacInnerRadius,
                houseOuterRadius = prefs[houseOuterKey] ?: defaults.visual.houseOuterRadius,
                houseInnerRadius = prefs[houseInnerKey] ?: defaults.visual.houseInnerRadius,
                bodyRingRadius = prefs[bodyRingKey] ?: defaults.visual.bodyRingRadius,
                aspectInnerRadius = prefs[aspectInnerKey] ?: defaults.visual.aspectInnerRadius,
                aspectLineThickness = prefs[aspectThicknessKey] ?: defaults.visual.aspectLineThickness,
                aspectLineOpacity = prefs[aspectOpacityKey] ?: defaults.visual.aspectLineOpacity,
                majorAspectStyle = prefs[majorStyleKey]?.let { enumValueOf<LineStyle>(it) } ?: defaults.visual.majorAspectStyle,
                minorAspectStyle = prefs[minorStyleKey]?.let { enumValueOf<LineStyle>(it) } ?: defaults.visual.minorAspectStyle,
                scaleWidthByOrb = prefs[scaleWidthByOrbKey] ?: defaults.visual.scaleWidthByOrb,
                widthScaleOrb = prefs[widthScaleOrbKey] ?: defaults.visual.widthScaleOrb,
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
    suspend fun setLockAscendant(value: Boolean) = context.dataStore.edit { it[lockAscendantKey] = value }
    suspend fun setColoredZodiacBands(value: Boolean) = context.dataStore.edit { it[coloredZodiacBandsKey] = value }
    suspend fun setZodiacOuterRadius(value: Float) = context.dataStore.edit { it[zodiacOuterKey] = value }
    suspend fun setZodiacInnerRadius(value: Float) = context.dataStore.edit { it[zodiacInnerKey] = value }
    suspend fun setHouseOuterRadius(value: Float) = context.dataStore.edit { it[houseOuterKey] = value }
    suspend fun setHouseInnerRadius(value: Float) = context.dataStore.edit { it[houseInnerKey] = value }
    suspend fun setBodyRingRadius(value: Float) = context.dataStore.edit { it[bodyRingKey] = value }
    suspend fun setAspectInnerRadius(value: Float) = context.dataStore.edit { it[aspectInnerKey] = value }
    suspend fun setAspectLineThickness(value: Float) = context.dataStore.edit { it[aspectThicknessKey] = value }
    suspend fun setAspectLineOpacity(value: Float) = context.dataStore.edit { it[aspectOpacityKey] = value }
    suspend fun setMajorAspectStyle(value: LineStyle) = context.dataStore.edit { it[majorStyleKey] = value.name }
    suspend fun setMinorAspectStyle(value: LineStyle) = context.dataStore.edit { it[minorStyleKey] = value.name }
    suspend fun setScaleWidthByOrb(value: Boolean) = context.dataStore.edit { it[scaleWidthByOrbKey] = value }
    suspend fun setWidthScaleOrb(value: Float) = context.dataStore.edit { it[widthScaleOrbKey] = value }
}
