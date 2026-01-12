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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import java.io.FileOutputStream
import java.io.FileInputStream
import java.io.InputStreamReader
import java.io.BufferedReader

@Composable
actual fun rememberToDometerFileSaver(
    fileName: String,
    title: String,
    initialDirectory: String?,
    onFileSaved: () -> Unit,
    onError: (Throwable?) -> Unit
): (String) -> Unit {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var contentToSave by remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    contentToSave?.let { content ->
                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            outputStream.write(content.toByteArray())
                        }
                        onFileSaved()
                    }
                } catch (e: Exception) {
                    onError(e)
                }
            }
        }
    }
    
    return { content ->
        contentToSave = content
        launcher.launch(fileName)
    }
}

@Composable
actual fun rememberToDometerFilePicker(
    title: String,
    initialDirectory: String?,
    onFilePicked: (String) -> Unit,
    onError: (Throwable?) -> Unit
): () -> Unit {
     val context = LocalContext.current
     val coroutineScope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        val text = BufferedReader(InputStreamReader(inputStream)).readText()
                        onFilePicked(text)
                    }
                } catch (e: Exception) {
                    onError(e)
                }
            }
        }
    }

    return {
        launcher.launch(arrayOf("application/json"))
    }
}
