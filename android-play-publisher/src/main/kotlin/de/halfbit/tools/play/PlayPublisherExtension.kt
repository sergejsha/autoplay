package de.halfbit.tools.play

import de.halfbit.tools.play.publisher.ReleaseStatus

internal const val RELEASE_NOTES_PATH = "src/main/play/release-notes"

internal open class PlayPublisherExtension {
    var track: String? = null
    var userFraction: Double? = null
    var status: String? = ReleaseStatus.Completed.name
    var secretJsonBase64: String? = null
    var secretJsonPath: String? = null
    var releaseNotesPath: String? = RELEASE_NOTES_PATH
}
