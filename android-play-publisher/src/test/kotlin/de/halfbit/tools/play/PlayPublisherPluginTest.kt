package de.halfbit.tools.play

import com.google.common.truth.Truth.assertThat
import de.halfbit.tools.play.publisher.ReleaseStatus
import de.halfbit.tools.play.publisher.ReleaseTrack
import org.gradle.kotlin.dsl.withGroovyBuilder
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test
import java.io.File

class PlayPublisherPluginTest {

    @Test
    fun `Test PublishApkTask with proper configuration`() {

        val project = ProjectBuilder.builder()
            .withName("sample")
            .withProjectDir(File("src/test/resources/sample-application/app"))
            .build()

        project.pluginManager.apply("com.android.application")
        project.pluginManager.apply(PlayPublisherPlugin::class.java)

        project.withGroovyBuilder {

            "android" {
                "compileSdkVersion"(28)
            }

            "publisher" {
                "track"("internal")
                "userFraction"(0.5)
                "secretJsonBase64"("c2VjcmV0")
            }
        }

        val tasks = project.getTasksByName("publishApkRelease", false)
        assertThat(tasks).hasSize(1)

        val task = tasks.first()
        assertThat(task).isNotNull()
        assertThat(task).isInstanceOf(PublishApkTask::class.java)

        val publishApkRelease = task as PublishApkTask
        assertThat(publishApkRelease.artifactFiles).hasSize(1)

        val artifact = publishApkRelease.artifactFiles.first()
        assertThat(artifact).isNotNull()
        assertThat(artifact.path).endsWith("sample-release-unsigned.apk")

        assertThat(publishApkRelease.releaseNotes).hasSize(1)

        val releaseNotes = publishApkRelease.releaseNotes.first()
        assertThat(releaseNotes).isNotNull()
        assertThat(releaseNotes.locale).isEqualTo("en_US")
        assertThat(releaseNotes.file.path).endsWith("release-notes/en_US/internal.txt")

        assertThat(publishApkRelease.credentials).isNotNull()
        assertThat(publishApkRelease.credentials.secretJson).isEqualTo("secret")
        assertThat(publishApkRelease.credentials.secretJsonPath).isNull()

        assertThat(publishApkRelease.obfuscationMappingFile).isNull()
        assertThat(publishApkRelease.applicationId).isEqualTo("de.halfbit.play.simple.test")
        assertThat(publishApkRelease.releaseTrack).isEqualTo(ReleaseTrack.Internal)
        assertThat(publishApkRelease.releaseStatus).isEqualTo(ReleaseStatus.Completed)

    }

}