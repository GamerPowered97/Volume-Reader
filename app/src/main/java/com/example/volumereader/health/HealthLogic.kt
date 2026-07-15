package com.example.volumereader.health

import androidx.compose.ui.graphics.Color
import com.example.volumereader.theme.GaugeGreen
import com.example.volumereader.theme.GaugeOrange
import com.example.volumereader.theme.GaugeRed
import com.example.volumereader.theme.GaugeYellow
import com.example.volumereader.theme.StatusCaution
import com.example.volumereader.theme.StatusDanger
import com.example.volumereader.theme.StatusSafe

data class HealthAdvice(
    val effect: String,
    val advice: String,
    val nioshLimit: String,
    val oshaLimit: String,
    val color: Color,
    val statusColor: Color,
    val severity: Int  // 0=safe, 1=caution, 2=warning, 3=danger, 4=extreme
)

object HealthLogic {

    private val safeDefault = HealthAdvice(
        effect = "Safe",
        advice = "Sound levels are perfectly safe. No hearing protection is needed.",
        nioshLimit = "Unlimited",
        oshaLimit = "Unlimited",
        color = GaugeGreen,
        statusColor = StatusSafe,
        severity = 0
    )

    fun getAdviceForDb(db: Float): HealthAdvice {
        // Guard against NaN, negative, or invalid values
        if (db.isNaN() || db.isInfinite() || db < 0f) return safeDefault

        return when {
            db < 70 -> safeDefault
            db < 85 -> HealthAdvice(
                effect = "Moderately Loud",
                advice = "Generally safe for normal durations. WHO recommends keeping 24-hour averages below 70 dBA.",
                nioshLimit = "> 8 hours",
                oshaLimit = "Unlimited",
                color = GaugeGreen,
                statusColor = StatusSafe,
                severity = 0
            )
            db < 90 -> HealthAdvice(
                effect = "Loud — Action Level",
                advice = "Hearing protection is recommended. Prolonged exposure beyond 8 hours can cause permanent damage.",
                nioshLimit = "8 hours",
                oshaLimit = "16 hours",
                color = GaugeYellow,
                statusColor = StatusCaution,
                severity = 1
            )
            db < 95 -> HealthAdvice(
                effect = "Very Loud — PEL",
                advice = "Protection strongly advised. OSHA Permissible Exposure Limit reached. NIOSH recommends under 2.5 hours.",
                nioshLimit = "~2.5 hours",
                oshaLimit = "8 hours",
                color = GaugeOrange,
                statusColor = StatusCaution,
                severity = 2
            )
            db < 100 -> HealthAdvice(
                effect = "Extremely Loud",
                advice = "Dangerous levels. Wear hearing protection immediately. Damage occurs in under an hour per NIOSH.",
                nioshLimit = "47 minutes",
                oshaLimit = "4 hours",
                color = GaugeOrange,
                statusColor = StatusDanger,
                severity = 3
            )
            db < 105 -> HealthAdvice(
                effect = "Hazardous",
                advice = "Hazardous levels. Protection strictly required. NIOSH limits exposure to 15 minutes.",
                nioshLimit = "15 minutes",
                oshaLimit = "2 hours",
                color = GaugeRed,
                statusColor = StatusDanger,
                severity = 3
            )
            db < 115 -> HealthAdvice(
                effect = "Severe Hazard",
                advice = "Immediate danger to hearing. High-grade hearing protection required. Evacuate if unprotected.",
                nioshLimit = "< 5 minutes",
                oshaLimit = "30–60 min",
                color = GaugeRed,
                statusColor = StatusDanger,
                severity = 4
            )
            else -> HealthAdvice(
                effect = "Extreme Hazard",
                advice = "Permanent hearing loss can occur almost immediately. Double protection (earplugs + earmuffs) essential.",
                nioshLimit = "< 30 seconds",
                oshaLimit = "< 15 min",
                color = GaugeRed,
                statusColor = StatusDanger,
                severity = 4
            )
        }
    }
}
