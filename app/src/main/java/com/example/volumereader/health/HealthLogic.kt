package com.example.volumereader.health

import androidx.compose.ui.graphics.Color
import com.example.volumereader.theme.HoloGreen
import com.example.volumereader.theme.HoloOrange
import com.example.volumereader.theme.HoloRed

data class HealthAdvice(
    val effect: String,
    val advice: String,
    val color: Color
)

object HealthLogic {
    fun getAdviceForDb(db: Float): HealthAdvice {
        return when {
            db < 70 -> HealthAdvice(
                effect = "Safe",
                advice = "Sound levels are perfectly safe. No hearing protection is needed.",
                color = HoloGreen
            )
            db < 85 -> HealthAdvice(
                effect = "Moderately Loud",
                advice = "Generally safe. WHO recommends keeping 24-hour averages below 70 dBA.",
                color = HoloGreen
            )
            db < 90 -> HealthAdvice(
                effect = "Loud (Action Level)",
                advice = "Hearing protection recommended. Damage possible after 8 hours.",
                color = HoloOrange
            )
            db < 95 -> HealthAdvice(
                effect = "Very Loud (PEL)",
                advice = "Protection strongly advised. OSHA Limit is 8 hours.",
                color = HoloOrange
            )
            db < 100 -> HealthAdvice(
                effect = "Extremely Loud",
                advice = "Dangerous. Wear protection. Damage occurs in under an hour.",
                color = HoloRed
            )
            db < 105 -> HealthAdvice(
                effect = "Hazardous",
                advice = "Hazardous. Protection required. Damage occurs in 15 minutes.",
                color = HoloRed
            )
            db < 115 -> HealthAdvice(
                effect = "Severe Hazard",
                advice = "Immediate danger. High-grade protection needed. Damage in minutes.",
                color = HoloRed
            )
            else -> HealthAdvice(
                effect = "Extreme Hazard",
                advice = "Permanent loss likely almost immediately. Avoid entirely.",
                color = HoloRed
            )
        }
    }
}
