/*
 * Copyright (C) 2018-2021 Sergej Shafarenka, www.halfbit.de
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.halfbit.tools.autoplay

import com.android.Version
import com.android.build.gradle.AppExtension
import com.android.build.gradle.api.ApkVariantOutput
import com.android.build.gradle.api.ApplicationVariant
import de.halfbit.tools.autoplay.publisher.ArtifactType
import de.halfbit.tools.autoplay.publisher.Credentials
import de.halfbit.tools.autoplay.publisher.ReleaseNotes
import de.halfbit.tools.autoplay.publisher.ReleaseStatus
import de.halfbit.tools.autoplay.publisher.ReleaseTrack
import de.halfbit.tools.autoplay.publisher.TRACK_ROLLOUT
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.register
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

            val appVariant = this
            val variantName = appVariant.name.capitalize()

            when (extension.artifactType) {
                ArtifactType.Apk.name -> {
                    project.tasks {
                        register<PublishTask>("publishApk$variantName") {
                            description =
                                "Publish $variantName apk, mapping and release-notes to Google Play."
                            group = TASK_GROUP

                            applicationId = appVariant.applicationId
                            artifactType = ArtifactType.Apk
                            artifacts = appVariant.collectArtifacts(ArtifactType.Apk, project)
                            obfuscationMappingFile = appVariant.getObfuscationMappingFile()
                            releaseTrack = extension.getReleaseTrack()
                            releaseStatus = extension.getReleaseStatus()
                            releaseNotes = extension.getReleaseNotes(project.projectDir)
                            credentials = extension.getCredentials()
                        }
                    }
                }

                ArtifactType.Bundle.name -> {
                    project.tasks {
                        register<PublishTask>("publishBundle$variantName") {
                            description =
                                "Publish $variantName bundle and release-notes to Google Play."
                            group = TASK_GROUP

                            applicationId = appVariant.applicationId
                            artifactType = ArtifactType.Bundle
                            artifacts = appVariant.collectArtifacts(ArtifactType.Bundle, project)
                            releaseTrack = extension.getReleaseTrack()
                            releaseStatus = extension.getReleaseStatus()
                            releaseNotes = extension.getReleaseNotes(project.projectDir)
                            credentials = extension.getCredentials()
                        }
                    }
                }
            }
        }
    }

    companion object {

        private const val MINIMAL_ANDROID_PLUGIN_VERSION = "3.0.1"

        private fun ApplicationVariant.collectArtifacts(
            artifactType: ArtifactType, project: Project
        ): List<File> {

            return when (artifactType) {
                ArtifactType.Apk -> this.outputs
                    .filterIsInstance<ApkVariantOutput>()
                    .map { it.outputFile }

                ArtifactType.Bundle -> {
                    val archivesBaseName = project.properties["archivesBaseName"] as String
                    listOf(File(project.buildDir, "outputs/bundle/$name/$archivesBaseName.aab"))
                }
            }
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
                UNINITIALIZED -> error("$EXTENSION_NAME { track } property is required.")
                ReleaseTrack.Internal.name -> ReleaseTrack.Internal
                ReleaseTrack.Alpha.name -> ReleaseTrack.Alpha
                ReleaseTrack.Beta.name -> ReleaseTrack.Beta
                TRACK_ROLLOUT -> ReleaseTrack.Rollout(userFraction)
                ReleaseTrack.Production.name -> ReleaseTrack.Production
                else -> error("Unsupported track: $track")
            }
        }

        private fun AutoplayPublisherExtension.getReleaseStatus(): ReleaseStatus {
            return when (status) {
                ReleaseStatus.Completed.name -> ReleaseStatus.Completed
                ReleaseStatus.Draft.name -> ReleaseStatus.Draft
                ReleaseStatus.Halted.name -> ReleaseStatus.Halted
                ReleaseStatus.InProgress.name -> ReleaseStatus.InProgress
                else -> error("Unsupported status: $status")
            }
        }

        private fun AutoplayPublisherExtension.getReleaseNotes(rootDir: File): List<ReleaseNotes> {
            val trackDirectory = File(rootDir, "$releaseNotesPath/$track")
            if (!trackDirectory.exists()) return emptyList()
            val directoryFiles = trackDirectory.listFiles() ?: return emptyList()
            return directoryFiles
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
                error(
                    "Plugin '$PLUGIN_ID' requires 'com.android.application' plugin version $expected or higher," +
                        " while yours is $current. Update android gradle plugin and try again."
                )
            }
            return project.extensions.findByType(AppExtension::class.java)
                ?: error("Required 'com.android.application' plugin must be added prior '$PLUGIN_ID' plugin.")
        }

        private fun File.getLocale(): String {
            if (name.length < 5 || name.substring(2, 3) != "-") {
                error(
                    "Release notes must be named using the following format:" +
                        " <language>-<COUNTRY>.txt, e.g. en-US.txt. Found name: $this"
                )
            }
            return name.substring(0, 5)
        }

    }

}