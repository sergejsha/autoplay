/*
 * Copyright (C) 2018 Sergej Shafarenka, www.halfbit.de
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

import de.halfbit.tools.autoplay.publisher.*
import de.halfbit.tools.autoplay.publisher.v3.V3GooglePlayPublisher
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.*
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import java.io.File

internal open class PublishTask : DefaultTask() {

    @get:Input
    lateinit var artifactType: ArtifactType

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    lateinit var artifacts: List<File>

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

        val configuration = Configuration(
            readTimeout = project.getIntProperty("readTimeout", 120_000),
            connectTimeout = project.getIntProperty("connectTimeout", 120_000)
        )

        V3GooglePlayPublisher
            .getGooglePlayPublisher(credentials, applicationId, configuration)
            .publish(
                ReleaseData(
                    applicationId,
                    artifacts.map { it.toReleaseArtifact(artifactType) },
                    obfuscationMappingFile,
                    releaseNotes,
                    releaseStatus,
                    releaseTrack
                )
            )
    }

}

private fun Project.getIntProperty(propertyName: String, default: Int): Int =
    (project.properties["de.halfbit.autoplay.$propertyName"] as? String)?.toInt() ?: default

private fun File.toReleaseArtifact(artifactType: ArtifactType): ReleaseArtifact = when (artifactType) {
    ArtifactType.Apk -> ReleaseArtifact.Apk(this)
    ArtifactType.Bundle -> ReleaseArtifact.Bundle(this)
}

private fun Credentials.validate() {
    if (!secretJson.isNullOrEmpty() && !secretJsonPath.isNullOrEmpty()) {
        error("Either $EXTENSION_NAME { secretJsonBase64 } or" +
            " $EXTENSION_NAME { secretJsonPath } must be specified, never both.")
    }
}

