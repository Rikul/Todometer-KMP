/*
 * Copyright 2024 Sergio Belda
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.sergiobelda.todometer.app.common.ui.files

import androidx.compose.runtime.Composable
import java.io.File
import java.awt.FileDialog
import java.awt.Frame

@Composable
actual fun rememberToDometerFileSaver(
    fileName: String,
    title: String,
    initialDirectory: String?,
    onFileSaved: () -> Unit,
    onError: (Throwable?) -> Unit
): (String) -> Unit {
    return { content ->
        try {
            val fileDialog = FileDialog(null as Frame?, title, FileDialog.SAVE)
            fileDialog.file = fileName
            initialDirectory?.let { fileDialog.directory = it }
            fileDialog.isVisible = true
            
            if (fileDialog.file != null) {
                val file = File(fileDialog.directory, fileDialog.file)
                file.writeText(content)
                onFileSaved()
            }
        } catch (e: Exception) {
            onError(e)
        }
    }
}

@Composable
actual fun rememberToDometerFilePicker(
    title: String,
    initialDirectory: String?,
    onFilePicked: (String) -> Unit,
    onError: (Throwable?) -> Unit
): () -> Unit {
    return {
        try {
            val fileDialog = FileDialog(null as Frame?, title, FileDialog.LOAD)
            initialDirectory?.let { fileDialog.directory = it }
            fileDialog.setFilenameFilter { _, name -> name.endsWith(".json", ignoreCase = true) }
            fileDialog.isVisible = true
            
            if (fileDialog.file != null) {
                val file = File(fileDialog.directory, fileDialog.file)
                val content = file.readText()
                onFilePicked(content)
            }
        } catch (e: Exception) {
            onError(e)
        }
    }
}
