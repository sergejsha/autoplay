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
import de.halfbit.tools.autoplay.publisher.Credentials
import de.halfbit.tools.autoplay.publisher.GooglePlayPublisher
import de.halfbit.tools.autoplay.publisher.ReleaseData
import de.halfbit.tools.autoplay.publisher.ReleaseTrack
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
        val edit = edits.insert(data.applicationId, null).execute()

        val apkVersionCodes = data.artifacts.map {

            val apkVersionCode = edits.apks()
                .upload(
                    data.applicationId,
                    edit.id,
                    FileContent(MIME_TYPE_APK, it)
                )
                .execute()
                .versionCode

            data.obfuscationMappingFile?.let {
                edits.deobfuscationfiles()
                    .upload(
                        data.applicationId,
                        edit.id,
                        apkVersionCode,
                        TYPE_PROGUARD,
                        FileContent(MIME_TYPE_STREAM, it)
                    )
                    .execute()
            }

            apkVersionCode.toLong()
        }

        val trackUpdate = Track().apply {
            track = data.releaseTrack.name
            releases = listOf(data.createTrackRelease(apkVersionCodes))
        }

        edits.tracks()
            .update(
                data.applicationId,
                edit.id,
                data.releaseTrack.name,
                trackUpdate
            )
            .execute()

        edits.commit(data.applicationId, edit.id).execute()

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

        private fun ReleaseData.createTrackRelease(apkVersionCodes: List<Long>): TrackRelease {
            return TrackRelease().apply {
                releaseNotes = getLocalizedReleaseNotes()
                versionCodes = apkVersionCodes
                status = releaseStatus.name
                if (releaseTrack is ReleaseTrack.Rollout) {
                    userFraction = releaseTrack.userFraction
                }
            }
        }

        private fun ReleaseData.getLocalizedReleaseNotes(): List<LocalizedText> {
            return releaseNotes.map {
                LocalizedText().apply {
                    language = it.locale
                    text = it.file.readText().replace("\n\r", "\n")
                }
            }
        }

        private fun ReleaseData.validate() {
            if (artifacts.isEmpty()) {
                error("No artifacts to publish.")
            }
            artifacts.forEach {
                if (!it.exists()) error("Artifact does not exist: $it")
                if (it.length() == 0L) error("Artifact is empty: $it")
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
                    error("autoplay { secretJsonBase64 } must not be empty.")
                }
                return secretJson
            }
            if (secretJsonPath == null) {
                error("Either autoplay { secretJsonBase64 } or autoplay { secretJsonPath } must be specified.")
            }
            val file = File(secretJsonPath)
            if (!file.exists()) {
                error("SecretJson file cannot be found: $file")
            }
            if (file.length() == 0L) {
                error("SecretJson file is empty: $file")
            }
            return file.readText()
        }

    }

}
