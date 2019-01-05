/*
 * Copyright (C) 2018-2019 Sergej Shafarenka, www.halfbit.de
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

import de.halfbit.tools.autoplay.publisher.ArtifactType
import de.halfbit.tools.autoplay.publisher.ReleaseStatus

internal const val RELEASE_NOTES_PATH = "src/main/autoplay/release-notes"
internal const val UNINITIALIZED = ""

open class AutoplayPublisherExtension {

    var track: String = UNINITIALIZED
    var userFraction: Double = 1.0
    var status: String = ReleaseStatus.Completed.name
    var secretJsonBase64: String? = null
    var secretJsonPath: String? = null
    var releaseNotesPath: String = RELEASE_NOTES_PATH
    var artifactType = ArtifactType.Apk.name

}
