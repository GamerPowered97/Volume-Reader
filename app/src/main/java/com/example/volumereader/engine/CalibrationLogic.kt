package com.example.volumereader.engine

data class PhoneModel(val brand: String, val model: String, val offset: Float)

object CalibrationLogic {

    val knownModels = listOf(
        PhoneModel("Samsung", "Galaxy S23", 82f),
        PhoneModel("Samsung", "Galaxy S22", 84f),
        PhoneModel("Google", "Pixel 8", 80f),
        PhoneModel("Google", "Pixel 7", 81f),
        PhoneModel("Generic", "Average Android", 85f)
    )

    fun getOffsetFor(brand: String, model: String): Float {
        return knownModels.find { it.brand.equals(brand, ignoreCase = true) && it.model.equals(model, ignoreCase = true) }?.offset ?: 85f
    }
}
