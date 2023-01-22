package org.snd.mediaserver

import org.snd.config.MetadataPostProcessingConfig
import org.snd.mediaserver.model.MediaServerBook
import org.snd.mediaserver.model.SeriesAndBookMetadata
import org.snd.metadata.BookNameParser
import org.snd.metadata.model.BookMetadata
import org.snd.metadata.model.SeriesMetadata
import org.snd.metadata.model.SeriesTitle

class MetadataPostProcessor(
    private val config: MetadataPostProcessingConfig
) {

    fun process(metadata: SeriesAndBookMetadata): SeriesAndBookMetadata {
        val seriesMetadata = postProcessSeries(metadata.seriesMetadata)
        val bookMetadata = postProcessBooks(metadata.bookMetadata)
        return SeriesAndBookMetadata(seriesMetadata, bookMetadata)
    }

    private fun postProcessSeries(series: SeriesMetadata?): SeriesMetadata? {
        val metadata = if (series == null && shouldCreateMetadata()) SeriesMetadata()
        else series ?: return null

        return metadata.copy(
            title = if (config.seriesTitle) seriesTitle(metadata.titles) ?: metadata.title else null,
            titles = if (config.alternativeSeriesTitles) metadata.titles else emptyList(),
            readingDirection = config.readingDirectionValue ?: metadata.readingDirection,
            language = config.languageValue ?: metadata.language,
        )
    }

    private fun postProcessBooks(books: Map<MediaServerBook, BookMetadata?>): Map<MediaServerBook, BookMetadata?> {
        return if (config.orderBooks) {
            books.map { (book, metadata) ->
                book to orderBook(book, (metadata ?: BookMetadata()))
            }.toMap()
        } else books
    }

    private fun orderBook(book: MediaServerBook, metadata: BookMetadata): BookMetadata {
        val range = BookNameParser.getVolumes(book.name) ?: BookNameParser.getChapters(book.name)

        return metadata.copy(
            number = range ?: metadata.number,
            numberSort = range?.start ?: metadata.numberSort
        )
    }

    private fun shouldCreateMetadata() = config.languageValue != null || config.readingDirectionValue != null

    private fun seriesTitle(titles: Collection<SeriesTitle>): SeriesTitle? {
        val knownTitles = titles.filter { it.type != null }
        return (knownTitles.find { it.type == config.titleType } ?: knownTitles.firstOrNull())
    }
}