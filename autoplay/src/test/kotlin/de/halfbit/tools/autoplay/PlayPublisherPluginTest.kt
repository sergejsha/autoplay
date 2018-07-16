package de.halfbit.tools.autoplay

import com.google.common.truth.Truth.assertThat
import de.halfbit.tools.autoplay.publisher.ReleaseStatus
import de.halfbit.tools.autoplay.publisher.ReleaseTrack
import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException
import org.gradle.kotlin.dsl.withGroovyBuilder
import org.gradle.testfixtures.ProjectBuilder
import org.hamcrest.core.IsEqual.equalTo
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.internal.matchers.ThrowableMessageMatcher.hasMessage
import org.junit.rules.ExpectedException
import java.io.File

internal class PlayPublisherPluginTest {

    @Rule
    @JvmField
    var thrown: ExpectedException = ExpectedException.none()

    private lateinit var project: Project

    @Before
    fun before() {
        project = ProjectBuilder.builder()
            .withName("sample-application")
            .withProjectDir(File("src/test/resources/sample-application/app"))
            .build()

        project.pluginManager.apply("com.android.application")
        project.pluginManager.apply(PlayPublisherPlugin::class.java)
    }

    @Test
    fun `PlayPublisherPlugin, valid configuration`() {

        project.withGroovyBuilder {

            "android" {
                "compileSdkVersion"(27)
            }

            "autoplay" {
                "track"("internal")
                "status"("inProgress")
                "userFraction"(0.5)
                "secretJsonBase64"("c2VjcmV0")
            }

            "evaluate"()
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
        assertThat(artifact.path).endsWith("sample-application-release-unsigned.apk")
        assertThat(File(artifact.path).exists()).isTrue()

        assertThat(publishApkRelease.releaseNotes).hasSize(1)

        val releaseNotes = publishApkRelease.releaseNotes.first()
        assertThat(releaseNotes).isNotNull()
        assertThat(releaseNotes.locale).isEqualTo("en_US")
        assertThat(releaseNotes.file.path).endsWith("release-notes/internal/en_US.txt")

        assertThat(publishApkRelease.credentials).isNotNull()
        assertThat(publishApkRelease.credentials.secretJson).isEqualTo("secret")
        assertThat(publishApkRelease.credentials.secretJsonPath).isNull()

        assertThat(publishApkRelease.obfuscationMappingFile).isNull()
        assertThat(publishApkRelease.applicationId).isEqualTo("de.halfbit.tools.autoplay.sample")
        assertThat(publishApkRelease.releaseTrack).isEqualTo(ReleaseTrack.Internal)
        assertThat(publishApkRelease.releaseStatus).isEqualTo(ReleaseStatus.InProgress)

    }

    @Test
    fun `PublishApkTask, missing 'track'`() {

        thrown.expect(ProjectConfigurationException::class.java)
        thrown.expectCause(hasMessage(equalTo("autoplay { track } property is required.")))

        project.withGroovyBuilder {
            "android" {
                "compileSdkVersion"(27)
            }
            "autoplay" {
            }
            "evaluate"()
        }
    }

}