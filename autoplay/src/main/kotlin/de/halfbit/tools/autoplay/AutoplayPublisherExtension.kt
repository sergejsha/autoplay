package de.halfbit.tools.autoplay

import de.halfbit.tools.autoplay.publisher.ReleaseStatus

internal const val RELEASE_NOTES_PATH = "src/main/play/release-notes"

internal open class AutoplayPublisherExtension {
    var track: String? = null
    var userFraction: Double? = null
    var status: String? = ReleaseStatus.Completed.name
    var secretJsonBase64: String? = null
    var secretJsonPath: String? = null
    var releaseNotesPath: String? = RELEASE_NOTES_PATH
}
