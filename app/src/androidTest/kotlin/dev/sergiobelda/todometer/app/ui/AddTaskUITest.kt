/*
 * Copyright 2025 Sergio Belda
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

package dev.sergiobelda.todometer.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextReplacement
import dev.sergiobelda.todometer.app.ui.main.MainActivity
import org.junit.Rule
import org.junit.Test

class AddTaskUITest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun addTaskWithNameOnly() {
        val addTaskDescription = "Add task"
        val enterTaskNamePlaceholder = "Enter a Task name"
        val saveDescription = "Save"

        // Click Add Task FAB
        composeTestRule.onNodeWithContentDescription(addTaskDescription).performClick()

        // Type name
        composeTestRule.onNodeWithText(enterTaskNamePlaceholder).performTextReplacement("New Task Name Only")

        // Click Save
        composeTestRule.onNodeWithContentDescription(saveDescription).performClick()

        composeTestRule.waitForIdle()

        // Verify task exists on home
        composeTestRule.onNodeWithText("New Task Name Only").assertIsDisplayed()
    }

    @Test
    fun addTaskWithDateChecklistItemsAndDescription() {
        val addTaskDescription = "Add task"
        val enterTaskNamePlaceholder = "Enter a Task name"
        val enterDescriptionPlaceholder = "Enter a Description"
        val addElementPlaceholder = "Add element"
        val saveDescription = "Save"

        // Click Add Task FAB
        composeTestRule.onNodeWithContentDescription(addTaskDescription).performClick()

        // Type name
        composeTestRule.onNodeWithText(enterTaskNamePlaceholder).performTextReplacement("Task with Details")

        // Type description
        composeTestRule.onNodeWithText(enterDescriptionPlaceholder).performTextReplacement("Description text")

        // Add checklist item
        composeTestRule.onNodeWithText(addElementPlaceholder).performTextReplacement("Checklist Item 1")
        composeTestRule.onNodeWithContentDescription("Add").performClick()

        // Click Save
        composeTestRule.onNodeWithContentDescription(saveDescription).performClick()

        composeTestRule.waitForIdle()

        // Verify
        composeTestRule.onNodeWithText("Task with Details").assertIsDisplayed()
    }

    @Test
    fun editTask() {
        // Create a task to edit
        val addTaskDescription = "Add task"
        val enterTaskNamePlaceholder = "Enter a Task name"
        val saveDescription = "Save"
        val editTaskDescription = "Edit task"

        composeTestRule.onNodeWithContentDescription(addTaskDescription).performClick()
        composeTestRule.onNodeWithText(enterTaskNamePlaceholder).performTextReplacement("Task to Edit")
        composeTestRule.onNodeWithContentDescription(saveDescription).performClick()

        composeTestRule.waitForIdle()

        // Click on the task to open details
        composeTestRule.onNodeWithText("Task to Edit").performClick()

        // Click Edit button
        composeTestRule.onNodeWithContentDescription(editTaskDescription).performClick()

        composeTestRule.waitForIdle()

        // Change name
        composeTestRule.onNodeWithText("Task to Edit").performTextReplacement("Task Edited")

        // Click Save
        composeTestRule.onNodeWithContentDescription(saveDescription).performClick()

        composeTestRule.waitForIdle()

        // Verify on Home (we should be back on home after save)
        composeTestRule.onNodeWithText("Task Edited").assertIsDisplayed()
    }
}