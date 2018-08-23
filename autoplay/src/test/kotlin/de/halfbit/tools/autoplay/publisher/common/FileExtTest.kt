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

package de.halfbit.tools.autoplay.publisher.common

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

class FileExtTest {

    @Test
    fun testReadingShortFile() {
        val file = getResourceFile("release-notes-short.txt")
        val text = file.readTextLines(maxLength = 500)

        assertThat(text).startsWith("v 2.15.0")
        assertThat(text).hasLength(206)
        assertThat(text).endsWith("* New: feature three\n")
    }

    @Test
    fun testReadingLongFile() {
        val file = getResourceFile("release-notes-long.txt")
        val text = file.readTextLines(maxLength = 500)

        assertThat(text).startsWith("v 2.15.0")
        assertThat(text.length).isAtMost(500)
        assertThat(text).endsWith("v 2.7.0\n")
    }

    companion object {
        fun getResourceFile(name: String): File {
            val path = FileExtTest::class.java.getResource(
                File.separator + "LinesFileReader" + File.separator + name).file
            return File(path)
        }
    }

}