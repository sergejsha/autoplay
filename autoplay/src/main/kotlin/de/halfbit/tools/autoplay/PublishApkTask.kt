package de.halfbit.tools.autoplay

import de.halfbit.tools.autoplay.publisher.*
import de.halfbit.tools.autoplay.publisher.v3.V3GooglePlayPublisher
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import java.io.File

internal open class PublishApkTask : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    lateinit var artifactFiles: List<File>

    @get:Optional
    @get:InputFile
    var obfuscationMappingFile: File? = null

    @get:Input
    lateinit var releaseNotes: List<ReleaseNotes>

    @get:Input
    lateinit var applicationId: String

    @get:Input
    lateinit var credentials: Credentials

    @get:Input
    lateinit var releaseTrack: ReleaseTrack

    @get:Input
    var releaseStatus: ReleaseStatus = ReleaseStatus.Completed

    @TaskAction
    @Suppress("UNUSED_PARAMETER", "unused")
    fun execute(inputs: IncrementalTaskInputs) {
        credentials.validate()

        V3GooglePlayPublisher
            .getGooglePlayPublisher(credentials, applicationId)
            .publish(
                ReleaseData(
                    applicationId,
                    artifactFiles,
                    obfuscationMappingFile,
                    releaseNotes,
                    releaseStatus,
                    releaseTrack
                )
            )
    }

    companion object {

        fun Credentials.validate() {
            if (!secretJson.isNullOrEmpty() && !secretJsonPath.isNullOrEmpty()) {
                error("Either $EXTENSION_NAME { secretJsonBase64 } or" +
                    " $EXTENSION_NAME { secretJsonPath } must be specified, never both.")
            }
        }

    }

}