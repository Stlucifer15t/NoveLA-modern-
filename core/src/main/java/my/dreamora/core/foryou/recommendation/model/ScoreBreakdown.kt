package my.dreamora.core.foryou.recommendation.model

/*
 * Derived from Novery (https://github.com/1Finn2me/Novery), GPL-3.0.
 * Modified for Dreamora. See NOTICE.md.
 */

/**
 * Detailed breakdown of how the score was calculated
 */
data class ScoreBreakdown(
    val tagSimilarity: Float = 0f,
    val userPreferenceMatch: Float = 0f,
    val authorMatch: Float = 0f,
    val ratingScore: Float = 0f,
    val providerBoost: Float = 0f,
    val synopsisMatch: Float = 0f,
    val popularityScore: Float = 0f
) {
    val total: Float
        get() = (tagSimilarity * 0.30f +
                userPreferenceMatch * 0.25f +
                authorMatch * 0.15f +
                ratingScore * 0.10f +
                providerBoost * 0.10f +
                synopsisMatch * 0.05f +
                popularityScore * 0.05f).coerceIn(0f, 1f)
}
