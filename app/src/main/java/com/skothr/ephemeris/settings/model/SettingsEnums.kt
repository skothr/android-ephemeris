package com.skothr.ephemeris.settings.model

enum class ZodiacType(val displayName: String) {
    TROPICAL("Tropical"),
    SIDEREAL("Sidereal"),
}

enum class Ayanamsa(val displayName: String, val swissEphId: Int) {
    FAGAN_BRADLEY("Fagan-Bradley", 0),
    LAHIRI("Lahiri", 1),
    DELUCE("De Luce", 2),
    RAMAN("Raman", 3),
    USHASHASHI("Usha-Shashi", 4),
    KRISHNAMURTI("Krishnamurti", 5),
    DJWHAL_KHUL("Djwhal Khul", 6),
    YUKTESHWAR("Yukteshwar", 7),
    JN_BHASIN("JN Bhasin", 8),
    BABYL_KUGLER1("Babylonian (Kugler 1)", 9),
    BABYL_KUGLER2("Babylonian (Kugler 2)", 10),
    BABYL_KUGLER3("Babylonian (Kugler 3)", 11),
    BABYL_HUBER("Babylonian (Huber)", 12),
    BABYL_ETPSC("Babylonian (ETPSC)", 13),
    ALDEBARAN_15TAU("Aldebaran 15\u00b0 Tau", 14),
    HIPPARCHOS("Hipparchos", 15),
    SASSANIAN("Sassanian", 16),
    GALCENT_0SAG("Gal. Center 0\u00b0 Sag", 17),
    J2000("J2000", 18),
    J1900("J1900", 19),
    B1950("B1950", 20),
    SURYASIDDHANTA("Surya Siddhanta", 21),
    SURYASIDDHANTA_MSUN("Surya Sidd. (mean Sun)", 22),
    ARYABHATA("Aryabhata", 23),
    ARYABHATA_MSUN("Aryabhata (mean Sun)", 24),
    SS_REVATI("SS Revati", 25),
    SS_CITRA("SS Citra", 26),
    TRUE_CITRA("True Citra", 27),
    TRUE_REVATI("True Revati", 28),
    TRUE_PUSHYA("True Pushya", 29),
    GALCENT_RGILBRAND("Gal. Center (Rgilbrand)", 30),
    GALEQU_IAU1958("Gal. Eq. (IAU 1958)", 31),
    GALEQU_TRUE("Gal. Eq. (true)", 32),
    GALEQU_MULA("Gal. Eq. (Mula)", 33),
    GALALIGN_MARDYKS("Gal. Align. (Mardyks)", 34),
    TRUE_MULA("True Mula", 35),
    GALCENT_MULA_WILHELM("Gal. Center (Mula/Wilhelm)", 36),
    ARYABHATA_522("Aryabhata 522", 37),
    BABYL_BRITTON("Babylonian (Britton)", 38),
    TRUE_SHEORAN("True Sheoran", 39),
    GALCENT_COCHRANE("Gal. Center (Cochrane)", 40),
    GALEQU_FIORENZA("Gal. Eq. (Fiorenza)", 41),
    VALENS_MOON("Valens Moon", 42),
    LAHIRI_1940("Lahiri 1940", 43),
    LAHIRI_VP285("Lahiri VP285", 44),
    KRISHNAMURTI_VP291("Krishnamurti VP291", 45),
    LAHIRI_ICRC("Lahiri ICRC", 46),
}

enum class NodeType(val displayName: String) {
    TRUE_NODE("True Node"),
    MEAN_NODE("Mean Node"),
}

enum class Center(val displayName: String) {
    GEOCENTRIC("Geocentric"),
    HELIOCENTRIC("Heliocentric"),
    TOPOCENTRIC("Topocentric"),
    BARYCENTRIC("Barycentric"),
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
