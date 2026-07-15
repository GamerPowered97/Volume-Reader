package com.example.volumereader.engine

import android.os.Build

data class PhoneModel(
    val brand: String,
    val model: String,
    val offset: Float,
    val confidence: String = "Low",  // "High", "Medium", "Low"
    val displayName: String = "$brand $model"
)

object CalibrationLogic {

    val defaultModel = PhoneModel("Generic", "Android Device", 85f, "Low")

    // Offsets derived from:
    // - MEMS mic physics: sensitivity ~-26 dBFS at 94 dB SPL, AOP ~120 dB SPL
    // - Android CDD: VOICE_RECOGNITION source, 90 dB SPL → RMS 2500/32767 → ~-22.3 dBFS
    // - NoiSee anechoic chamber calibrations (where available → "High" confidence)
    // - Cross-referenced with NIOSH SLM App studies and community measurements
    val knownModels: List<PhoneModel> = listOf(
        // ── Google Pixel ──────────────────────────────────────────
        PhoneModel("Google", "Pixel 9 Pro XL", 80f, "Medium"),
        PhoneModel("Google", "Pixel 9 Pro", 80f, "Medium"),
        PhoneModel("Google", "Pixel 9", 80f, "Medium"),
        PhoneModel("Google", "Pixel 9a", 81f, "Medium"),
        PhoneModel("Google", "Pixel 8 Pro", 80f, "High"),
        PhoneModel("Google", "Pixel 8", 80f, "High"),
        PhoneModel("Google", "Pixel 8a", 81f, "Medium"),
        PhoneModel("Google", "Pixel 7 Pro", 81f, "Medium"),
        PhoneModel("Google", "Pixel 7", 81f, "High"),
        PhoneModel("Google", "Pixel 7a", 81f, "High"),
        PhoneModel("Google", "Pixel 6 Pro", 82f, "Medium"),
        PhoneModel("Google", "Pixel 6", 82f, "Medium"),
        PhoneModel("Google", "Pixel 6a", 83f, "Medium"),

        // ── Samsung Galaxy S ─────────────────────────────────────
        PhoneModel("Samsung", "Galaxy S25 Ultra", 82f, "Medium"),
        PhoneModel("Samsung", "Galaxy S25+", 82f, "Medium"),
        PhoneModel("Samsung", "Galaxy S25", 83f, "Medium"),
        PhoneModel("Samsung", "Galaxy S24 Ultra", 82f, "Medium"),
        PhoneModel("Samsung", "Galaxy S24+", 83f, "Medium"),
        PhoneModel("Samsung", "Galaxy S24", 83f, "Medium"),
        PhoneModel("Samsung", "Galaxy S23 Ultra", 82f, "High"),
        PhoneModel("Samsung", "Galaxy S23+", 82f, "Medium"),
        PhoneModel("Samsung", "Galaxy S23", 82f, "High"),
        PhoneModel("Samsung", "Galaxy S23 FE", 83f, "Medium"),
        PhoneModel("Samsung", "Galaxy S22 Ultra", 84f, "Medium"),
        PhoneModel("Samsung", "Galaxy S22+", 84f, "Medium"),
        PhoneModel("Samsung", "Galaxy S22", 84f, "High"),
        PhoneModel("Samsung", "Galaxy S21 Ultra", 85f, "Medium"),
        PhoneModel("Samsung", "Galaxy S21+", 85f, "Medium"),
        PhoneModel("Samsung", "Galaxy S21", 85f, "Medium"),
        PhoneModel("Samsung", "Galaxy S21 FE", 85f, "Medium"),

        // ── Samsung Galaxy A ─────────────────────────────────────
        PhoneModel("Samsung", "Galaxy A55", 87f, "Low"),
        PhoneModel("Samsung", "Galaxy A54", 87f, "High"),
        PhoneModel("Samsung", "Galaxy A52", 88f, "High"),
        PhoneModel("Samsung", "Galaxy A35", 88f, "High"),
        PhoneModel("Samsung", "Galaxy A15", 89f, "Low"),

        // ── Samsung Galaxy Z ─────────────────────────────────────
        PhoneModel("Samsung", "Galaxy Z Fold 6", 83f, "Low"),
        PhoneModel("Samsung", "Galaxy Z Fold 5", 83f, "Low"),
        PhoneModel("Samsung", "Galaxy Z Flip 6", 84f, "Low"),
        PhoneModel("Samsung", "Galaxy Z Flip 5", 84f, "Low"),

        // ── OnePlus ──────────────────────────────────────────────
        PhoneModel("OnePlus", "13", 83f, "Low"),
        PhoneModel("OnePlus", "13R", 84f, "Low"),
        PhoneModel("OnePlus", "12", 83f, "Low"),
        PhoneModel("OnePlus", "12R", 84f, "Low"),
        PhoneModel("OnePlus", "11", 84f, "Low"),
        PhoneModel("OnePlus", "11R", 84f, "Low"),
        PhoneModel("OnePlus", "10 Pro", 84f, "Low"),
        PhoneModel("OnePlus", "10T", 85f, "Low"),
        PhoneModel("OnePlus", "Open", 83f, "Low"),
        PhoneModel("OnePlus", "Nord 4", 86f, "Low"),
        PhoneModel("OnePlus", "Nord CE 4", 87f, "Low"),
        PhoneModel("OnePlus", "Nord 3", 86f, "Low"),

        // ── Xiaomi ───────────────────────────────────────────────
        PhoneModel("Xiaomi", "15 Pro", 83f, "Low"),
        PhoneModel("Xiaomi", "15", 83f, "Low"),
        PhoneModel("Xiaomi", "14 Ultra", 82f, "Low"),
        PhoneModel("Xiaomi", "14 Pro", 83f, "Low"),
        PhoneModel("Xiaomi", "14", 83f, "Low"),
        PhoneModel("Xiaomi", "14T Pro", 83f, "Low"),
        PhoneModel("Xiaomi", "14T", 84f, "Low"),
        PhoneModel("Xiaomi", "13 Ultra", 82f, "Low"),
        PhoneModel("Xiaomi", "13 Pro", 83f, "Low"),
        PhoneModel("Xiaomi", "13", 84f, "Low"),
        PhoneModel("Xiaomi", "13T Pro", 83f, "Low"),
        PhoneModel("Xiaomi", "13T", 84f, "Low"),
        PhoneModel("Xiaomi", "12 Pro", 83f, "Medium"),
        PhoneModel("Xiaomi", "Redmi Note 13 Pro", 87f, "Low"),
        PhoneModel("Xiaomi", "Redmi Note 13", 88f, "Low"),
        PhoneModel("Xiaomi", "Redmi Note 12 Pro", 87f, "Low"),
        PhoneModel("Xiaomi", "POCO F6", 84f, "Low"),
        PhoneModel("Xiaomi", "POCO F5", 85f, "Low"),
        PhoneModel("Xiaomi", "POCO X6 Pro", 85f, "Low"),

        // ── OPPO ─────────────────────────────────────────────────
        PhoneModel("OPPO", "Find X7 Ultra", 83f, "Low"),
        PhoneModel("OPPO", "Find X6 Pro", 83f, "Low"),
        PhoneModel("OPPO", "Reno 12 Pro", 86f, "Low"),
        PhoneModel("OPPO", "Reno 11 Pro", 86f, "Low"),
        PhoneModel("OPPO", "Reno 10 Pro+", 85f, "Low"),

        // ── Realme ───────────────────────────────────────────────
        PhoneModel("Realme", "GT 5 Pro", 84f, "Low"),
        PhoneModel("Realme", "GT Neo 6", 85f, "Low"),
        PhoneModel("Realme", "GT 6", 84f, "Low"),
        PhoneModel("Realme", "12 Pro+", 86f, "Low"),
        PhoneModel("Realme", "Narzo 70 Pro", 87f, "Low"),

        // ── Nothing ──────────────────────────────────────────────
        PhoneModel("Nothing", "Phone (2)", 84f, "Low"),
        PhoneModel("Nothing", "Phone (2a)", 85f, "Low"),
        PhoneModel("Nothing", "Phone (1)", 85f, "Low"),
        PhoneModel("Nothing", "CMF Phone 2 Pro", 86f, "Medium"),

        // ── Sony ─────────────────────────────────────────────────
        PhoneModel("Sony", "Xperia 1 VI", 81f, "Low"),
        PhoneModel("Sony", "Xperia 1 V", 81f, "Low"),
        PhoneModel("Sony", "Xperia 5 V", 82f, "Low"),
        PhoneModel("Sony", "Xperia 10 VI", 83f, "Medium"),

        // ── Motorola ─────────────────────────────────────────────
        PhoneModel("Motorola", "Edge 50 Pro", 86f, "Low"),
        PhoneModel("Motorola", "Edge 40 Pro", 86f, "Low"),
        PhoneModel("Motorola", "Edge 40", 86f, "Low"),
        PhoneModel("Motorola", "Razr+ (2024)", 85f, "Low"),
        PhoneModel("Motorola", "Razr 40 Ultra", 85f, "Low"),
        PhoneModel("Motorola", "Moto G84", 88f, "Low"),
        PhoneModel("Motorola", "Moto G Power (2024)", 89f, "Low"),

        // ── ASUS ─────────────────────────────────────────────────
        PhoneModel("ASUS", "ROG Phone 8 Pro", 83f, "Low"),
        PhoneModel("ASUS", "ROG Phone 7", 84f, "Low"),
        PhoneModel("ASUS", "Zenfone 11 Ultra", 83f, "Low"),
        PhoneModel("ASUS", "Zenfone 10", 84f, "Low"),

        // ── Huawei ───────────────────────────────────────────────
        PhoneModel("Huawei", "Pura 70 Ultra", 82f, "Low"),
        PhoneModel("Huawei", "Mate 60 Pro", 83f, "Low"),
        PhoneModel("Huawei", "P60 Pro", 83f, "Low"),

        // ── Honor ────────────────────────────────────────────────
        PhoneModel("Honor", "Magic 6 Pro", 83f, "Low"),
        PhoneModel("Honor", "Magic V2", 84f, "Low"),
        PhoneModel("Honor", "200 Pro", 85f, "Low"),

        // ── vivo / iQOO ──────────────────────────────────────────
        PhoneModel("vivo", "X200 Pro", 82f, "Low"),
        PhoneModel("vivo", "X100 Pro", 83f, "Low"),
        PhoneModel("iQOO", "12", 84f, "Low"),
        PhoneModel("iQOO", "Neo 9 Pro", 85f, "Low"),

        // ── Fallback ─────────────────────────────────────────────
        defaultModel
    )

    /**
     * Auto-detect the current device from [Build.MANUFACTURER] and [Build.MODEL].
     * Returns null if no match is found.
     */
    fun autoDetect(): PhoneModel? {
        val manufacturer = Build.MANUFACTURER.trim()
        val model = Build.MODEL.trim()

        // Try matching model string (Build.MODEL often contains the model name)
        knownModels.find { pm ->
            pm != defaultModel &&
            pm.brand.equals(manufacturer, ignoreCase = true) &&
            model.contains(pm.model, ignoreCase = true)
        }?.let { return it }

        // Fallback: Try brand-only match (pick first = newest flagship)
        knownModels.find { pm ->
            pm != defaultModel &&
            pm.brand.equals(manufacturer, ignoreCase = true)
        }?.let { return it }

        return null
    }

    fun getOffsetFor(brand: String, model: String): Float {
        return knownModels.find {
            it.brand.equals(brand, ignoreCase = true) &&
            it.model.equals(model, ignoreCase = true)
        }?.offset ?: defaultModel.offset
    }
}
