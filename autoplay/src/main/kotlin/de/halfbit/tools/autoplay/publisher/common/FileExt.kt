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

import java.io.File

internal fun File.readTextLines(maxLength: Long): String {
    if (!exists()) error("File must exist: $this")
    if (length() == 0L) error("File must not be empty: $this")

    var complete = false
    val text = StringBuilder()
    forEachLine { line ->
        if (complete) return@forEachLine

        if (text.length + line.length < maxLength) {
            text.append(line.trim()).append("\n")
        } else {
            complete = true
        }
    }
    return text.toString()
}