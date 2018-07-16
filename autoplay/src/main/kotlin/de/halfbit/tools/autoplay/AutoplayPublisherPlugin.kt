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
                releaseNotes = extension.getReleaseNotes(project.projectDir)
                credentials = extension.getCredentials()
            }
        }

    }

    companion object {

        private const val MINIMAL_ANDROID_PLUGIN_VERSION = "3.0.1"

        private fun ApplicationVariant.getArtifactFiles(): List<File> {
            return this.outputs
                .filterIsInstance<ApkVariantOutput>()
                .map { it.outputFile }
        }

        private fun ApplicationVariant.getObfuscationMappingFile(): File? {
            val mapping = mappingFile
            if (mapping == null || mapping.length() == 0L) {
                return null
            }
            return mapping
        }

        private fun AutoplayPublisherExtension.getReleaseTrack(): ReleaseTrack {
            return when (track) {
                null -> error("$EXTENSION_NAME { track } property is required.")
                ReleaseTrack.Internal.name -> ReleaseTrack.Internal
                ReleaseTrack.Alpha.name -> ReleaseTrack.Alpha
                ReleaseTrack.Beta.name -> ReleaseTrack.Beta
                TRACK_ROLLOUT -> ReleaseTrack.Rollout(userFraction ?: 1.0)
                ReleaseTrack.Production.name -> ReleaseTrack.Production
                else -> error("Unsupported track: $track")
            }
        }

        private fun AutoplayPublisherExtension.getReleaseStatus(): ReleaseStatus {
            return when (status) {
                null -> error("$EXTENSION_NAME { status } property is required.")
                ReleaseStatus.Completed.name -> ReleaseStatus.Completed
                ReleaseStatus.Draft.name -> ReleaseStatus.Draft
                ReleaseStatus.Halted.name -> ReleaseStatus.Halted
                ReleaseStatus.InProgress.name -> ReleaseStatus.InProgress
                else -> error("Unsupported status: $status")
            }
        }

        private fun AutoplayPublisherExtension.getReleaseNotes(rootDir: File): List<ReleaseNotes> {
            val releaseNotePath = this.releaseNotesPath ?: error("$EXTENSION_NAME { releaseNotesPath } is required.")
            val trackDirectory = File(rootDir, "$releaseNotePath/$track")
            return if (trackDirectory.exists()) {
                trackDirectory.listFiles()
                    .filter { it.isFile }
                    .mapNotNull { localizedFile ->
                        if (localizedFile.exists()) localizedFile else null
                    }
                    .map { releaseNoteFile ->
                        ReleaseNotes(
                            releaseNoteFile.getLocale(),
                            releaseNoteFile
                        )
                    }
            } else {
                emptyList()
            }
        }

        private fun AutoplayPublisherExtension.getCredentials(): Credentials {
            val secretJson = if (secretJsonBase64 != null) {
                Base64.getDecoder().decode(secretJsonBase64).toString(Charsets.UTF_8)
            } else {
                null
            }
            return Credentials(secretJson, secretJsonPath)
        }

        private fun Project.requireAndroidAppExtension(): AppExtension {
            val current = Version.ANDROID_GRADLE_PLUGIN_VERSION
            val expected = MINIMAL_ANDROID_PLUGIN_VERSION
            if (current < expected) {
                error("Plugin '$PLUGIN_ID' requires 'com.android.application' plugin version $expected or higher," +
                    " while yours is $current. Update android gradle plugin and try again.")
            }
            return project.extensions.findByType(AppExtension::class.java)
                ?: error("Required 'com.android.application' plugin must be added prior '$PLUGIN_ID' plugin.")
        }

        private fun File.getLocale(): String {
            if (name.length < 5 || name.substring(2, 3) != "-") {
                error("Release notes must be named using the following format:" +
                    " <language>-<COUNTRY>.txt, e.g. en-US.txt. Found name: $this")
            }
            return name.substring(0, 5)
        }

    }

}