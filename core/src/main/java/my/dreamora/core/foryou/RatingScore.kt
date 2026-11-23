package my.dreamora.core.foryou

/**
 * A source-declared rating as a number on a ten-point scale, or null when the text is not a
 * score. Sources write this field heterogeneously — "4.5", "4.8/5", "9.2/10", "#12",
 * "RANK 216" — and an ordering is not a rating, so anything that is not a plain leading
 * decimal is dropped rather than guessed at.
 */
fun parseRatingScore(text: String): Double? {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return null
    val value = Regex("^\\d{1,2}(?:[.,]\\d{1,2})?")
        .find(trimmed)
        ?.value
        ?.replace(',', '.')
        ?.toDoubleOrNull()
        ?: return null
    val tenPoint = "/10" in trimmed.replace(" ", "") || "из 10" in trimmed
    return when {
        tenPoint -> value.takeIf { it in 0.0..10.0 }
        else -> value.takeIf { it in 0.0..5.0 }?.times(2.0)
    }
}
