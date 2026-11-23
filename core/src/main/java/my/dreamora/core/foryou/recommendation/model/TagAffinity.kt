package my.dreamora.core.foryou.recommendation.model

/*
 * Derived from Novery (https://github.com/1Finn2me/Novery), GPL-3.0.
 * Modified for Dreamora. See NOTICE.md.
 */

import my.dreamora.core.foryou.recommendation.TagNormalizer.TagCategory

/**
 * Represents user's affinity for a specific tag
 */
data class TagAffinity(
    val tag: TagCategory,
    val score: Float,           // 0.0 - 1.0, higher = user likes more
    val confidence: Float,      // 0.0 - 1.0, higher = more data points
    val novelCount: Int,
    val completionRate: Float,
    val dropRate: Float
) {
    val isStrong: Boolean get() = score >= 0.7f && confidence >= 0.5f
    val isWeak: Boolean get() = score < 0.3f || confidence < 0.2f
}