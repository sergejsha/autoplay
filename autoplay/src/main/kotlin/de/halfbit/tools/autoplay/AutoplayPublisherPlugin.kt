package de.halfbit.tools.autoplay

import com.android.build.gradle.AppExtension
import com.android.build.gradle.api.ApkVariantOutput
import com.android.build.gradle.api.ApplicationVariant
import com.android.builder.model.Version
import de.halfbit.tools.autoplay.publisher.*
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.createTask
import java.io.File
import java.util.*

internal const val TASK_GROUP = "Publishing"
internal const val PLUGIN_ID = "android-autoplay"
internal const val EXTENSION_NAME = "autoplay"

internal class PlayPublisherPlugin : Plugin<Project> {

    override fun apply(project: Project) {

        val extension = project.extensions
            .create(EXTENSION_NAME, AutoplayPublisherExtension::class.java)

        val androidAppExtension = project.requireAndroidAppExtension()
        androidAppExtension.applicationVariants.whenObjectAdded {
            if (buildType.isDebuggable) return@whenObjectAdded

            val applicationVariant = this@whenObjectAdded
            val variantName = name.capitalize()

            project.createTask("publishApk$variantName", PublishApkTask::class) {
                description = "Publish $variantName apk, mapping and release-notes to Google Play."
                group = TASK_GROUP

                applicationId = applicationVariant.applicationId
                artifactFiles = getArtifactFiles()
                obfuscationMappingFile = getObfuscationMappingFile()
                releaseTrack = extension.getReleaseTrack()
                releaseStatus = extension.getReleaseStatus()
                releaseNotes = extension.getReleaseNotes(project.rootDir)
                credentials = extension.getCredentials()
            }
        }

    }

    companion object {

        private const val MINIMAL_ANDROID_PLUGIN_VERSION = "3.0.1"

        fun ApplicationVariant.getArtifactFiles(): List<File> {
            return this.outputs
                .filterIsInstance<ApkVariantOutput>()
                .map { it.outputFile }
        }

        fun ApplicationVariant.getObfuscationMappingFile(): File? {
            val mapping = mappingFile
            if (mapping == null || mapping.length() == 0L) {
                return null
            }
            return mapping
        }

        fun AutoplayPublisherExtension.getReleaseTrack(): ReleaseTrack {
            return when (track) {
                null -> error("autoplay { track } property is required.")
                ReleaseTrack.Internal.name -> ReleaseTrack.Internal
                ReleaseTrack.Alpha.name -> ReleaseTrack.Alpha
                ReleaseTrack.Beta.name -> ReleaseTrack.Beta
                TRACK_ROLLOUT -> ReleaseTrack.Rollout(userFraction ?: 1.0)
                ReleaseTrack.Production.name -> ReleaseTrack.Production
                else -> error("Unsupported track: $track")
            }
        }

        fun AutoplayPublisherExtension.getReleaseStatus(): ReleaseStatus {
            return when (status) {
                null -> error("autoplay { status } property is required.")
                ReleaseStatus.Completed.name -> ReleaseStatus.Completed
                ReleaseStatus.Draft.name -> ReleaseStatus.Draft
                ReleaseStatus.Halted.name -> ReleaseStatus.Halted
                ReleaseStatus.InProgress.name -> ReleaseStatus.InProgress
                else -> error("Unsupported status: $status")
            }
        }

        fun AutoplayPublisherExtension.getReleaseNotes(rootDir: File): List<ReleaseNotes> {
            val releaseNotePath = this.releaseNotesPath ?: error("autoplay { releaseNotesPath } is required.")
            val root = File(rootDir, releaseNotePath)
            return if (root.exists()) {
                root.listFiles()
                    .filter { it.isDirectory }
                    .mapNotNull { localizedDirectory ->
                        val file = File(localizedDirectory, "$track.txt")
                        if (file.exists()) file else null
                    }
                    .map { releaseNoteFile ->
                        ReleaseNotes(
                            releaseNoteFile.parentFile.name,
                            releaseNoteFile
                        )
                    }
            } else {
                emptyList()
            }
        }

        fun AutoplayPublisherExtension.getCredentials(): Credentials {
            val secretJson = if (secretJsonBase64 != null) {
                Base64.getDecoder().decode(secretJsonBase64).toString(Charsets.UTF_8)
            } else {
                null
            }
            return Credentials(secretJson, secretJsonPath)
        }

        fun Project.requireAndroidAppExtension(): AppExtension {
            val current = Version.ANDROID_GRADLE_PLUGIN_VERSION
            val expected = MINIMAL_ANDROID_PLUGIN_VERSION
            if (current < expected) {
                error("Plugin '$PLUGIN_ID' requires 'com.android.application' plugin version $expected or higher," +
                    " while yours is $current. Update android gradle plugin and try again.")
            }
            return project.extensions.findByType(AppExtension::class.java)
                ?: error("Required 'com.android.application' plugin must be added prior '$PLUGIN_ID' plugin.")
        }

    }

}