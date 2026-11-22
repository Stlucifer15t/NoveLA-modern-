package my.noveldokusha.extension_compat.core

import kotlinx.serialization.Serializable

/**
 * Подписка на репозиторий расширений, добавленная пользователем.
 *
 * Пользователь вводит ОДНУ строку URL — [url]. Это может быть:
 *  - ссылка на конкретный индекс-файл (`.../index.min.json`, `.../bookSource.json`);
 *  - ссылка на репозиторий целиком (`https://github.com/owner/repo`);
 *  - ссылка на файл в репозитории (`.../blob/main/sources.json`);
 *  - ссылка на релиз (`.../releases/latest`).
 *
 * Что именно скачиваем решает [RepoUrlResolver]: он подбирает [indexUrl]
 * (или [RepoKind.TREE], когда индекса нет, а источники лежат отдельными файлами).
 * Формат индекса определяется по содержимому ([CompatRepoParsers.sniff]) и
 * запоминается в [ecosystem], чтобы повторные refresh не угадывали заново.
 */
@Serializable
data class CompatRepository(
    /** Устойчивый id подписки: слаг из URL + короткий хэш (CompatRepositoryManager.repositoryId).
     * По нему кэшируется индекс (`indexes/<id>.json`) и привязываются установленные записи. */
    val id: String,
    /** URL, как его ввёл пользователь. */
    val url: String,
    /** Реально скачанный индекс-файл (пусто при [RepoKind.TREE]). */
    val indexUrl: String = "",
    val kind: RepoKind = RepoKind.INDEX,
    /** Экосистема, определённая по содержимому индекса. */
    val ecosystem: CompatEcosystem? = null,
    /** Отображаемое имя (из манифеста репозитория, иначе host/path). */
    val name: String = "",
    val enabled: Boolean = true,
    val addedAt: Long = 0,
    val lastRefreshAt: Long = 0,
    /** Сколько источников найден в индексе (для «Install all»). */
    val entryCount: Int = 0,
    /** Последняя ошибка refresh — не роняем UI, показываем в списке репозиториев. */
    val lastError: String? = null,
    /** Имя ветки/тега, если вывели из URL (нужно для TREE и относительных путей). */
    val ref: String = "",
    /** Печать подписи репозитория (Mihon v2 `signingKey`) — пиннинг APK. */
    val signerFingerprint: String = "",
)

/**
 * Как устроен репозиторий:
 *  - [INDEX] — один индекс-файл со списком источников (keiyoushi `index.json`,
 *    LNReader `plugins.min.json`, Legado `bookSource.json`, Mangayomi `novel_index.json`).
 *  - [TREE] — индекса нет, каждый источник лежит отдельным файлом
 *    (типично для Legado-репозиториев вида `site-name.json`). Список файлов
 *    даёт git-дерево репозитория либо листинг каталога.
 */
enum class RepoKind {
    INDEX,
    TREE,
}

/**
 * Один доступный для установки источник внутри репозитория — нормализованная
 * запись любого из поддерживаемых индексов. [CompatRepositoryManager] умеет
 * по ней установить ([install]) расширение.
 *
 * Payload берётся одним из двух путей:
 *  - [payloadUrl] непустой → скачать файл (APK/JS/одиночный bookSource);
 *  - [inlineKey] непустой → объект уже есть в кэшированном индексе
 *    (Legado-подборки: bookSource лежит прямо в `bookSource.json`).
 */
@Serializable
data class CompatRepoEntry(
    /** Стабильный ключ внутри репозитория (pkg / id / bookSourceUrl / путь файла). */
    val key: String,
    val repoId: String = "",
    val name: String,
    val ecosystem: CompatEcosystem,
    val version: String = "",
    val versionCode: Long = 0,
    /** ISO 639-1 или "" (репозиторий не указал язык). */
    val language: String = "",
    /** Домашний URL источника — по нему NoveLA сопоставляет URL книги. */
    val baseUrl: String = "",
    val payloadUrl: String = "",
    /** Ключ объекта в кэшированном индексе (Legado), если payload инлайновый. */
    val inlineKey: String = "",
    val iconUrl: String? = null,
    /** "" = novel, "manga", "video", "audio". */
    val contentType: String = "",
    val nsfw: Boolean = false,
    val description: String = "",
    /** Расширение скачанного payload'а — влияет на выбор загрузчика. */
    val payloadKind: CompatPayloadKind = CompatPayloadKind.JSON,
    /** Печать подписи из репозитория (если есть) — проверка при установке APK. */
    val signerFingerprint: String = "",
    /** Размер файла из индекса, байты (0 = неизвестно). */
    val size: Long = 0,
    /** Доп. поля индекса как есть — для диагностики и будущих адаптеров. */
    val extra: Map<String, String> = emptyMap(),
)

/** Тип payload'а, который скачиваем и складываем в [CompatStorage]. */
enum class CompatPayloadKind(val fileExtension: String) {
    JSON("json"),
    JS("js"),
    DART("dart"),
    JAR("jar"),
    APK("apk"),
    CS3("cs3"),
    ;

    companion object {
        fun fromUrlOrName(url: String): CompatPayloadKind = when {
            url.endsWith(".apk", true) -> APK
            url.endsWith(".jar", true) -> JAR
            url.endsWith(".cs3", true) -> CS3
            url.endsWith(".dart", true) -> DART
            url.endsWith(".js", true) -> JS
            else -> JSON
        }
    }
}

/**
 * Результат добавления/обновления репозитория — UI показывает его прямо в
 * диалоге («Определено: Legado, 412 источников»), поэтому пользователю видно,
 * ПОНИМАЕТ ли приложение его URL, а не просто «что-то загрузилось».
 */
data class CompatRepoResult(
    val repository: CompatRepository? = null,
    val entries: List<CompatRepoEntry> = emptyList(),
    val ok: Boolean = false,
    /** Человекочитаемое объяснение провала (какие URL пробовали, почему не подошли). */
    val message: String = "",
)
