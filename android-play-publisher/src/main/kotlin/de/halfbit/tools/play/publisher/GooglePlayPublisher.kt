package de.halfbit.tools.play.publisher

import java.io.File
import java.io.Serializable

internal const val TRACK_ROLLOUT = "rollout"

internal interface GooglePlayPublisher {
    fun publish(data: ReleaseData)
}

internal class ReleaseData(
    val applicationId: String,
    val artifacts: List<File>,
    val obfuscationMappingFile: File?,
    val releaseNotes: List<ReleaseNotes>,
    val releaseStatus: ReleaseStatus,
    val releaseTrack: ReleaseTrack
)

internal class ReleaseNotes(
    val locale: String,
    val file: File
) : Serializable

internal sealed class ReleaseTrack(val name: String) : Serializable {
    object Internal : ReleaseTrack("internal")
    object Alpha : ReleaseTrack("alpha")
    object Beta : ReleaseTrack("beta")
    class Rollout(val userFraction: Double) : ReleaseTrack(TRACK_ROLLOUT)
    object Production : ReleaseTrack("production")
}

internal sealed class ReleaseStatus(val name: String) : Serializable {
    object Completed : ReleaseStatus("completed")
    object Draft : ReleaseStatus("draft")
    object Halted : ReleaseStatus("halted")
    object InProgress : ReleaseStatus("inProgress")
}

internal class Credentials(
    val secretJson: String?,
    val secretJsonPath: String?
) : Serializable
