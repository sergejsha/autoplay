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

package de.halfbit.tools.autoplay.publisher

import java.io.File
import java.io.Serializable

internal const val TRACK_ROLLOUT = "rollout"

internal interface GooglePlayPublisher {
    fun publish(data: ReleaseData)
}

internal class ReleaseData(
    val applicationId: String,
    val artifacts: List<ReleaseArtifact>,
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

internal sealed class ReleaseArtifact(val file: File) {
    class Apk(file: File) : ReleaseArtifact(file)
    class Bundle(file: File) : ReleaseArtifact(file)
}

internal sealed class ArtifactType(val name: String) : Serializable {
    object Apk : ArtifactType("apk")
    object Bundle : ArtifactType("bundle")
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

internal class Configuration(
        val readTimeout: Int,
        val connectTimeout: Int
)