package com.adriantache.greatimagedownloader.domain.utils

import com.adriantache.greatimagedownloader.domain.data.model.PhotoFile

/**
 * Computes the updated per-directory "latest downloaded photo" markers without advancing past gaps.
 *
 * For each directory present in [photosToDownload], files are ordered by name ascending and only
 * the contiguous prefix of files present in [completedPhotos] is considered. That way, if any
 * file fails or is skipped before a later file completes, the marker stops before the gap so
 * missing files will be retried on the next sync.
 */
fun computeUpdatedLatestDownloadedPhotos(
    existingLatest: List<PhotoFile>,
    photosToDownload: List<PhotoFile>,
    completedPhotos: Set<PhotoFile>,
): List<PhotoFile> {
    if (completedPhotos.isEmpty()) return existingLatest

    val existingByDir = existingLatest.associateBy { it.directory }.toMutableMap()

    for ((dir, dirPhotos) in photosToDownload.groupBy { it.directory }) {
        val lastContiguous = dirPhotos
            .sortedBy { it.name }
            .takeWhile { it in completedPhotos }
            .lastOrNull()
            ?: continue

        val existing = existingByDir[dir]
        if (existing == null || lastContiguous.name >= existing.name) {
            existingByDir[dir] = lastContiguous
        }
    }

    return existingByDir.values.toList()
}
