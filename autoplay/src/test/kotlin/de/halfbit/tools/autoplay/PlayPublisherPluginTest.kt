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

private const val EXPECTED_EXTENSION_NAME = "autoplay"

@Suppress("RemoveSingleExpressionStringTemplate")
internal class PlayPublisherPluginTest {

    @Rule
    @JvmField
    var thrown: ExpectedException = ExpectedException.none()

    private lateinit var project: Project

    @Before
    fun before() {
        project = ProjectBuilder.builder()
            .withName("sample-app")
            .withProjectDir(File("src/test/resources/sample-app/app"))
            .build()

        project.pluginManager.apply("com.android.application")
        project.pluginManager.apply(PlayPublisherPlugin::class.java)
    }

    @Test
    fun `PlayPublisherPlugin, valid Apk configuration`() {

        project.withGroovyBuilder {

            "android" {
                "compileSdkVersion"(27)
            }

            "$EXPECTED_EXTENSION_NAME" {
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
        assertThat(task).isInstanceOf(PublishTask::class.java)

        val publishApkRelease = task as PublishTask
        assertThat(publishApkRelease.artifacts).hasSize(1)

        val artifact = publishApkRelease.artifacts.first()
        assertThat(artifact).isNotNull()
        assertThat(artifact.path).endsWith("sample-app-release-unsigned.apk")
        assertThat(File(artifact.path).exists()).isTrue()

        assertThat(publishApkRelease.releaseNotes).hasSize(1)

        val releaseNotes = publishApkRelease.releaseNotes.first()
        assertThat(releaseNotes).isNotNull()
        assertThat(releaseNotes.locale).isEqualTo("en-US")
        assertThat(releaseNotes.file.path).endsWith("release-notes/internal/en-US.txt")

        assertThat(publishApkRelease.credentials).isNotNull()
        assertThat(publishApkRelease.credentials.secretJson).isEqualTo("secret")
        assertThat(publishApkRelease.credentials.secretJsonPath).isNull()

        assertThat(publishApkRelease.obfuscationMappingFile).isNull()
        assertThat(publishApkRelease.applicationId).isEqualTo("de.halfbit.tools.autoplay.sample")
        assertThat(publishApkRelease.releaseTrack).isEqualTo(ReleaseTrack.Internal)
        assertThat(publishApkRelease.releaseStatus).isEqualTo(ReleaseStatus.InProgress)

    }

    @Test
    fun `PlayPublisherPlugin, valid Bundle configuration`() {

        project.withGroovyBuilder {

            "android" {
                "compileSdkVersion"(27)
            }

            "$EXPECTED_EXTENSION_NAME" {
                "track"("internal")
                "status"("inProgress")
                "userFraction"(0.5)
                "artifactType"("bundle")
                "secretJsonBase64"("c2VjcmV0")
            }

            "evaluate"()
        }

        val tasks = project.getTasksByName("publishBundleRelease", false)
        assertThat(tasks).hasSize(1)

        val task = tasks.first()
        assertThat(task).isNotNull()
        assertThat(task).isInstanceOf(PublishTask::class.java)

        val publishApkRelease = task as PublishTask
        assertThat(publishApkRelease.artifacts).hasSize(1)

        val artifact = publishApkRelease.artifacts.first()
        assertThat(artifact).isNotNull()
        assertThat(artifact.path).endsWith("sample-app.aab")
        assertThat(File(artifact.path).exists()).isTrue()

        assertThat(publishApkRelease.releaseNotes).hasSize(1)

        val releaseNotes = publishApkRelease.releaseNotes.first()
        assertThat(releaseNotes).isNotNull()
        assertThat(releaseNotes.locale).isEqualTo("en-US")
        assertThat(releaseNotes.file.path).endsWith("release-notes/internal/en-US.txt")

        assertThat(publishApkRelease.credentials).isNotNull()
        assertThat(publishApkRelease.credentials.secretJson).isEqualTo("secret")
        assertThat(publishApkRelease.credentials.secretJsonPath).isNull()

        assertThat(publishApkRelease.obfuscationMappingFile).isNull()
        assertThat(publishApkRelease.applicationId).isEqualTo("de.halfbit.tools.autoplay.sample")
        assertThat(publishApkRelease.releaseTrack).isEqualTo(ReleaseTrack.Internal)
        assertThat(publishApkRelease.releaseStatus).isEqualTo(ReleaseStatus.InProgress)

    }

    @Test
    fun `PublishTask, missing 'track'`() {

        thrown.expect(ProjectConfigurationException::class.java)
        thrown.expectCause(hasMessage(equalTo("$EXPECTED_EXTENSION_NAME { track } property is required.")))

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