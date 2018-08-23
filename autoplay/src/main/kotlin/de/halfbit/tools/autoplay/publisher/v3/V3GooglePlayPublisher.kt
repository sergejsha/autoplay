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

package de.halfbit.tools.autoplay.publisher.v3

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.FileContent
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.androidpublisher.AndroidPublisher
import com.google.api.services.androidpublisher.AndroidPublisherScopes
import com.google.api.services.androidpublisher.model.LocalizedText
import com.google.api.services.androidpublisher.model.Track
import com.google.api.services.androidpublisher.model.TrackRelease
import de.halfbit.tools.autoplay.EXTENSION_NAME
import de.halfbit.tools.autoplay.publisher.Credentials
import de.halfbit.tools.autoplay.publisher.GooglePlayPublisher
import de.halfbit.tools.autoplay.publisher.ReleaseData
import de.halfbit.tools.autoplay.publisher.ReleaseTrack
import de.halfbit.tools.autoplay.publisher.common.readTextLines
import java.io.File

internal const val MIME_TYPE_APK = "application/vnd.android.package-archive"
internal const val MIME_TYPE_STREAM = "application/octet-stream"
internal const val TYPE_PROGUARD = "proguard"

internal class V3GooglePlayPublisher(
    private val androidPublisher: AndroidPublisher
) : GooglePlayPublisher {

    override fun publish(data: ReleaseData) {

        data.validate()

        val edits = androidPublisher.edits()
        val appEdit = edits.insert(data.applicationId, null).execute()

        val apkVersionCodes = data.artifacts.map {

            val apkVersionCode = edits.apks()
                .upload(
                    data.applicationId,
                    appEdit.id,
                    FileContent(MIME_TYPE_APK, it)
                )
                .execute()
                .versionCode

            data.obfuscationMappingFile?.let { obfuscationMappingFile ->
                edits.deobfuscationfiles()
                    .upload(
                        data.applicationId,
                        appEdit.id,
                        apkVersionCode,
                        TYPE_PROGUARD,
                        FileContent(MIME_TYPE_STREAM, obfuscationMappingFile)
                    )
                    .execute()
            }

            apkVersionCode.toLong()
        }

        edits.tracks()
            .update(
                data.applicationId,
                appEdit.id,
                data.releaseTrack.name,
                data.createTrackUpdate(apkVersionCodes)
            )
            .execute()

        edits.commit(data.applicationId, appEdit.id).execute()

    }

    companion object {

        private val jsonFactory = JacksonFactory.getDefaultInstance()
        private val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
        private var publisher: GooglePlayPublisher? = null

        private fun createAndroidPublisher(secretJson: String, applicationName: String): AndroidPublisher {
            val credentials = GoogleCredential
                .fromStream(secretJson.byteInputStream(), httpTransport, jsonFactory)
                .createScoped(listOf(AndroidPublisherScopes.ANDROIDPUBLISHER))

            return AndroidPublisher
                .Builder(httpTransport, jsonFactory, credentials)
                .setApplicationName(applicationName)
                .build()
        }

        private fun ReleaseData.createTrackUpdate(apkVersionCodes: List<Long>): Track {
            return Track().apply {
                releases = listOf(
                    TrackRelease().apply {
                        versionCodes = apkVersionCodes
                        status = releaseStatus.name
                        if (releaseTrack is ReleaseTrack.Rollout) {
                            userFraction = releaseTrack.userFraction
                        }
                        releaseNotes = this@createTrackUpdate.releaseNotes.map { releaseNotes ->
                            LocalizedText().apply {
                                language = releaseNotes.locale
                                text = releaseNotes.file.readTextLines(maxLength = 500)
                            }
                        }
                    }
                )
            }
        }

        private fun ReleaseData.validate() {
            if (artifacts.isEmpty()) {
                error("No artifacts found for publishing.")
            }
            artifacts.forEach {
                if (!it.exists()) error("Artifact does not exist: $it")
                if (it.length() == 0L) error("Artifact must not be empty: $it")
            }
        }

        fun getGooglePlayPublisher(credentials: Credentials, applicationName: String): GooglePlayPublisher {
            var instance = publisher
            if (instance == null) {
                val androidPublisher = createAndroidPublisher(credentials.getSecretJson(), applicationName)
                instance = V3GooglePlayPublisher(androidPublisher)
                publisher = instance
            }
            return instance
        }

        private fun Credentials.getSecretJson(): String {
            if (secretJson != null) {
                if (secretJson.isEmpty()) {
                    error("$EXTENSION_NAME { secretJsonBase64 } must not be empty.")
                }
                return secretJson
            }
            if (secretJsonPath == null) {
                error("Either $EXTENSION_NAME { secretJsonBase64 } or $EXTENSION_NAME { secretJsonPath } must be specified.")
            }
            val file = File(secretJsonPath)
            if (!file.exists()) {
                error("SecretJson file cannot be found: $file")
            }
            if (file.length() == 0L) {
                error("SecretJson file must not be empty: $file")
            }
            return file.readText()
        }

    }

}
