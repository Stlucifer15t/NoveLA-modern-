package my.dreamora.core.foryou.recommendation.model

/**
 * One author's preference, as the scorer needs it. The database maps its rows to this
 * type, so the scorer does not depend on storage.
 *
 * @param authorNormalized normalised author key used by [NovelVector.authorNormalized]
 * @param affinityScore 0..1000 scale, as stored
 * @param isFavorite the user marked this author as a favourite
 */
data class AuthorAffinity(
    val authorNormalized: String,
    val affinityScore: Int,
    val isFavorite: Boolean
)
