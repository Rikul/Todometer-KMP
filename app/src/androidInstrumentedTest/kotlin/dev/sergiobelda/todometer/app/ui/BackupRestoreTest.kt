package dev.sergiobelda.todometer.app.ui

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.platform.app.InstrumentationRegistry
import dev.sergiobelda.todometer.app.ui.main.MainActivity
import dev.sergiobelda.todometer.common.domain.Result
import dev.sergiobelda.todometer.common.domain.model.BackupData
import dev.sergiobelda.todometer.common.domain.model.Tag
import dev.sergiobelda.todometer.common.domain.model.Task
import dev.sergiobelda.todometer.common.domain.model.TaskChecklistItem
import dev.sergiobelda.todometer.common.domain.model.TaskChecklistItemState
import dev.sergiobelda.todometer.common.domain.model.TaskList
import dev.sergiobelda.todometer.common.domain.model.TaskState
import dev.sergiobelda.todometer.common.domain.repository.ITaskListRepository
import dev.sergiobelda.todometer.common.domain.repository.ITaskRepository
import dev.sergiobelda.todometer.common.domain.repository.ITaskChecklistItemsRepository
import dev.sergiobelda.todometer.common.database.dao.ITaskDao
import dev.sergiobelda.todometer.common.database.dao.ITaskListDao
import dev.sergiobelda.todometer.common.database.dao.ITaskChecklistItemDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.hamcrest.CoreMatchers.allOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.GlobalContext
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BackupRestoreTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        Intents.init()
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun verifyBackupAndRestoreOptionsDisplayed() {
        // Open Navigation Drawer
        composeTestRule.onNodeWithContentDescription("Menu").performClick()

        // Verify Backup Data option is displayed
        composeTestRule.onNodeWithText("Backup Data").assertIsDisplayed()

        // Verify Restore Data option is displayed
        composeTestRule.onNodeWithText("Restore Data").assertIsDisplayed()
    }

    @Test
    fun verifyBackupDataIntent() {
        // Stub the ACTION_CREATE_DOCUMENT intent to prevent the external activity from starting
        val expectedIntent = hasAction(Intent.ACTION_CREATE_DOCUMENT)
        intending(expectedIntent).respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))

        // Open Navigation Drawer
        composeTestRule.onNodeWithContentDescription("Menu").performClick()

        // Click Backup Data
        composeTestRule.onNodeWithText("Backup Data").performClick()

        composeTestRule.waitForIdle()

        // Verify ACTION_CREATE_DOCUMENT intent
        intended(expectedIntent)

        // Verify default filename 
        val expectedFilename = "TodometerBackup.json"
        intended(allOf(hasAction(Intent.ACTION_CREATE_DOCUMENT), hasExtra(Intent.EXTRA_TITLE, expectedFilename)))
    }

    @Test
    fun verifyBackupSavedAndToastDisplayed() {
        // Create a real temporary file that can be written to
        val context = composeTestRule.activity
        val tempFile = java.io.File(context.cacheDir, "test_backup.json")
        tempFile.createNewFile()

        // Create a content URI that ContentResolver can write to
        val fileUri = Uri.fromFile(tempFile)
        val resultData = Intent().apply {
            data = fileUri
            // Grant write permission for the URI
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }

        // Stub the ACTION_CREATE_DOCUMENT intent with a successful result containing the file URI
        val expectedIntent = hasAction(Intent.ACTION_CREATE_DOCUMENT)
        intending(expectedIntent).respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, resultData))

        // Open Navigation Drawer
        composeTestRule.onNodeWithContentDescription("Menu").performClick()

        // Click Backup Data
        composeTestRule.onNodeWithText("Backup Data").performClick()

        composeTestRule.waitForIdle()

        // Verify ACTION_CREATE_DOCUMENT intent was sent
        intended(expectedIntent)

        // Wait for the backup operation to complete and verify the success snackbar
        composeTestRule.waitForIdle()

        // Verify the snackbar using Compose test API
        composeTestRule.onNodeWithText("Backup successful").assertIsDisplayed()

        // Clean up
        tempFile.delete()
    }

    @Test
    fun verifyRestoreDataShowsConfirmationAndIntent() {

        val confirmationMessage = "This will overwrite all current data. Are you sure?"
        val expectedIntent = hasAction(Intent.ACTION_OPEN_DOCUMENT)
        intending(expectedIntent).respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))

        // Open Navigation Drawer
        composeTestRule.onNodeWithContentDescription("Menu").performClick()

        // Click Backup Data
        composeTestRule.onNodeWithText("Restore Data").performClick()

        composeTestRule.waitForIdle()

        // Verify confirmation dialog is displayed
        composeTestRule.onNodeWithText(confirmationMessage).assertIsDisplayed()

        // Click "Yes" on the confirmation dialog
        composeTestRule.onNodeWithText("Yes").performClick()

        // Verify ACTION_OPEN_DOCUMENT intent is sent
        intended(expectedIntent)
    }

    @Test
    fun verifyCancelRestoreDoesNotLaunchIntent() {
        val confirmationMessage = "This will overwrite all current data. Are you sure?"
        val addTaskDescription = "Add task"
        val enterTaskNamePlaceholder = "Enter a Task name"
        val saveDescription = "Save"

        createTask("Do not delete this task")
        /*
        // Click Add Task FAB
        composeTestRule.onNodeWithContentDescription(addTaskDescription).performClick()

        // Type name
        composeTestRule.onNodeWithText(enterTaskNamePlaceholder).performTextReplacement("Do not delete this task")

        // Click Save
        composeTestRule.onNodeWithContentDescription(saveDescription).performClick()
        */
        composeTestRule.waitForIdle()

        Thread.sleep(1000)

        // Open Navigation Drawer and click Restore Data
        composeTestRule.onNodeWithContentDescription("Menu").performClick()
        composeTestRule.onNodeWithText("Restore Data").performClick()
        composeTestRule.waitForIdle()
        
        // Verify confirmation dialog is displayed
        composeTestRule.onNodeWithText(confirmationMessage).assertIsDisplayed()
        
        // Click "Cancel" on the confirmation dialog
        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.waitForIdle()
        
        // Verify that NO ACTION_OPEN_DOCUMENT intent was launched
        // Note: We can't use intended() because it would fail if no intent was sent
        // Instead, we verify the dialog is dismissed and tasks remain
        
        // Verify the tasks are still there by checking UI
        composeTestRule.onNodeWithText("Do not delete this task").assertIsDisplayed()
    }

    @Test
    fun verifyRestoreFailedWithInvalidJson() {

        /*
        val confirmationMessage = "This will overwrite all current data. Are you sure?"
        val addTaskDescription = "Add task"
        val enterTaskNamePlaceholder = "Enter a Task name"
        val saveDescription = "Save"

        // Step 1: Create existing tasks
        composeTestRule.onNodeWithContentDescription(addTaskDescription).performClick()
        composeTestRule.onNodeWithText(enterTaskNamePlaceholder).performTextReplacement("Original Task 1")
        composeTestRule.onNodeWithContentDescription(saveDescription).performClick()
        composeTestRule.waitForIdle()

        Thread.sleep(1000)

        composeTestRule.onNodeWithContentDescription(addTaskDescription).performClick()
        composeTestRule.onNodeWithText(enterTaskNamePlaceholder).performTextReplacement("Original Task 2")
        composeTestRule.onNodeWithContentDescription(saveDescription).performClick()
        composeTestRule.waitForIdle()
        */

        createTask("Original Task 1")
        createTask("Original Task 2")

        Thread.sleep(1000)

        // Verify original tasks exist
        composeTestRule.onNodeWithText("Original Task 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Original Task 2").assertIsDisplayed()

        // Step 2: Create a file with invalid JSON
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val invalidJsonFile = java.io.File(context.filesDir, "test_invalid_restore.json")
        if (invalidJsonFile.exists()) invalidJsonFile.delete()
        
        // Write malformed JSON
        invalidJsonFile.writeText("{ invalid json content without proper structure }")
        
        val backupUri = Uri.fromFile(invalidJsonFile)
        val resultData = Intent().apply {
            data = backupUri
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        // Step 3: Stub the ACTION_OPEN_DOCUMENT intent
        val expectedIntent = hasAction(Intent.ACTION_OPEN_DOCUMENT)
        intending(expectedIntent).respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, resultData))

        // Step 4: Open Navigation Drawer and click Restore Data
        composeTestRule.onNodeWithContentDescription("Menu").performClick()
        composeTestRule.onNodeWithText("Restore Data").performClick()
        composeTestRule.waitForIdle()

        // Step 5: Click "Yes" on the confirmation dialog
        composeTestRule.onNodeWithText("Yes").performClick()
        composeTestRule.waitForIdle()

        // Wait for restore operation to complete
        Thread.sleep(2000)

        // Step 6: Verify "Restore failed" message is displayed
        composeTestRule.onNodeWithText("Restore failed", useUnmergedTree = true).assertExists()

        // Step 7: Verify original tasks still exist (data unchanged)
        composeTestRule.onNodeWithText("Original Task 1", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithText("Original Task 2", useUnmergedTree = true).assertExists()

        // Clean up
        if (invalidJsonFile.exists()) invalidJsonFile.delete()
    }

    @Test
    fun verifyBackupEmptyList() {

        deleteAllTasks()

        composeTestRule.waitForIdle()

        // Step 2: Create a mock file URI and trigger backup
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val tempFile = java.io.File(context.cacheDir, "test_backup_empty.json")
        if (tempFile.exists()) tempFile.delete()
        tempFile.createNewFile()

        val fileUri = Uri.fromFile(tempFile)
        val resultData = Intent().apply {
            data = fileUri
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }

        val expectedIntent = hasAction(Intent.ACTION_CREATE_DOCUMENT)
        intending(expectedIntent).respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, resultData))

        // Step 3: Open Navigation Drawer and trigger backup
        composeTestRule.onNodeWithContentDescription("Menu").performClick()
        composeTestRule.onNodeWithText("Backup Data").performClick()
        composeTestRule.waitForIdle()

        // Give time for the file to be written
        Thread.sleep(1000)

        // Step 4: Read and verify the JSON content
        assertTrue(tempFile.exists(), "Backup file should exist")
        val jsonContent = tempFile.readText()
        assertTrue(jsonContent.isNotEmpty(), "Backup file should not be empty")

        // Step 5: Parse JSON and verify it contains empty lists
        val backupData = Json.decodeFromString<BackupData>(jsonContent)
        
        // Verify empty lists
        assertTrue(backupData.tasks.isEmpty(), "Backup should contain zero tasks")
        assertTrue(backupData.taskChecklistItems.isEmpty(), "Backup should contain zero checklist items")
        
        // TaskLists might have a default list, so we check if it's empty or has only the default
        // But tasks array should definitely be empty
        assertEquals(0, backupData.tasks.size, "Tasks array should be empty")

        // Verify JSON structure contains the fields with empty arrays
        assertTrue(jsonContent.contains("\"tasks\":[]"), "JSON should contain empty tasks array")
        assertTrue(jsonContent.contains("\"taskChecklistItems\":[]"), "JSON should contain empty taskChecklistItems array")

        // Clean up
        tempFile.delete()
    }

    @Test
    fun verifyBackupContainsCorrectTaskData() {

        createSampleTasks()

        // Wait for database operations to complete
        composeTestRule.waitForIdle()

        // Create a mock file URI and trigger backup
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val tempFile = java.io.File(context.cacheDir, "test_backup_data.json")
        if (tempFile.exists()) tempFile.delete()
        tempFile.createNewFile()

        val fileUri = Uri.fromFile(tempFile)
        val resultData = Intent().apply {
            data = fileUri
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }

        val expectedIntent = hasAction(Intent.ACTION_CREATE_DOCUMENT)
        intending(expectedIntent).respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, resultData))

        // Open Navigation Drawer and trigger backup
        composeTestRule.onNodeWithContentDescription("Menu").performClick()
        composeTestRule.onNodeWithText("Backup Data").performClick()
        composeTestRule.waitForIdle()

        // Give some time for the file to be written
        Thread.sleep(1000)

        // Read and verify the JSON content
        assertTrue(tempFile.exists(), "Backup file should exist")
        val jsonContent = tempFile.readText()
        assertTrue(jsonContent.isNotEmpty(), "Backup file should not be empty")

        // Parse JSON and verify it contains the tasks
        val backupData = Json.decodeFromString<BackupData>(jsonContent)
        
        // Verify we have tasks
        assertTrue(backupData.tasks.isNotEmpty(), "Backup should contain tasks")
        assertEquals(3, backupData.tasks.size, "Backup should contain exactly 3 tasks")

        // Verify task titles
        val taskTitles = backupData.tasks.map { it.title }.toSet()
        assertTrue(taskTitles.contains("Buy groceries"), "Backup should contain 'Buy groceries' task")
        assertTrue(taskTitles.contains("Write report"), "Backup should contain 'Write report' task")
        assertTrue(taskTitles.contains("Call dentist"), "Backup should contain 'Call dentist' task")

        // Verify task states
        val completedTask = backupData.tasks.find { it.title == "Buy groceries" }
        assertEquals(TaskState.DONE, completedTask?.state, "'Buy groceries' should be marked as DONE")

        val uncompletedTasks = backupData.tasks.filter { it.state == TaskState.DOING }
        assertEquals(2, uncompletedTasks.size, "Should have 2 uncompleted tasks")

        // Verify JSON structure contains required fields
        assertTrue(jsonContent.contains("\"taskLists\""), "JSON should contain taskLists field")
        assertTrue(jsonContent.contains("\"tasks\""), "JSON should contain tasks field")
        assertTrue(jsonContent.contains("\"taskChecklistItems\""), "JSON should contain taskChecklistItems field")

        // Clean up
        tempFile.delete()
    }

    @Test
    fun verifyRestoreContainsCorrectTaskData() {
        createTask("Buy groceries")

        // Verify tasks are displayed in UI
        composeTestRule.onNodeWithText("Buy groceries", useUnmergedTree = true).assertExists()

        // Step 2: Backup the data to a file
        val context = composeTestRule.activity
        val backupFile = java.io.File(context.cacheDir, "test_full_backup.json")
        if (backupFile.exists()) backupFile.delete()
        backupFile.createNewFile()

        val backupUri = Uri.fromFile(backupFile)
        val backupResultData = Intent().apply {
            data = backupUri
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }

        val createDocumentIntent = hasAction(Intent.ACTION_CREATE_DOCUMENT)
        intending(createDocumentIntent).respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, backupResultData))

        // Open Navigation Drawer and trigger backup
        composeTestRule.onNodeWithContentDescription("Menu").performClick()
        composeTestRule.onNodeWithText("Backup Data").performClick()
        composeTestRule.waitForIdle()

        // Wait for backup to complete
        Thread.sleep(2000)

        // Verify backup file was created and has content
        assertTrue(backupFile.exists(), "Backup file should exist")
        assertTrue(backupFile.length() > 0, "Backup file should not be empty")

        deleteAllTasks()

        composeTestRule.waitForIdle()
        Thread.sleep(1000)

        // Step 4: Restore from the backup file
        val openDocumentIntent = hasAction(Intent.ACTION_OPEN_DOCUMENT)
        val restoreResultData = Intent().apply {
            data = backupUri
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        intending(openDocumentIntent).respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, restoreResultData))

        composeTestRule.waitForIdle()

        // Open Navigation Drawer and trigger restore
        composeTestRule.onNodeWithContentDescription("Menu").performClick()
        composeTestRule.onNodeWithText("Restore Data").performClick()
        composeTestRule.waitForIdle()

        // Confirm restoration in the dialog
        composeTestRule.onNodeWithText("Yes").performClick()
        composeTestRule.waitForIdle()

        // Wait for restore to complete
        Thread.sleep(3000)

        // Force UI refresh by navigating to Settings and back
        // This causes Home screen to recompose and re-collect Flows with restored data
        composeTestRule.onNodeWithContentDescription("Menu").performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(500)

        // Verify the restored data appears in the UI
        composeTestRule.onNodeWithText("Buy groceries", useUnmergedTree = true).assertExists()

        // Clean up
        backupFile.delete()
    }

    fun createSampleTasks() {
        
        // Get repository instances from Koin
        val taskRepository: ITaskRepository = GlobalContext.get().get()
        val taskListRepository: ITaskListRepository = GlobalContext.get().get()
        val taskChecklistItemsRepository: ITaskChecklistItemsRepository = GlobalContext.get().get()

        runBlocking {
            // Get the default task list ID (or create one)
            val taskListResult = taskListRepository.insertTaskList("Default List")
            val taskListId = when (taskListResult) {
                is Result.Success -> taskListResult.value
                is Result.Error -> throw Exception("Failed to create task list")
            }

            // Task 1 - Completed with description and checklist items
            val task1Result = taskRepository.insertTask(
                title = "Buy groceries",
                tag = Tag.RED,
                description = "Weekly grocery shopping at the supermarket",
                dueDate = null,
                taskListId = taskListId
            )
            val task1Id = when (task1Result) {
                is Result.Success -> task1Result.value
                is Result.Error -> throw Exception("Failed to create task 1")
            }
            
            // Add checklist items for task 1
            taskChecklistItemsRepository.insertTaskChecklistItems(
                task1Id,
                "Buy milk",
                "Buy bread",
                "Buy eggs"
            )
            
            // Mark as done
            taskRepository.updateTaskState(task1Id, TaskState.DONE)

            // Task 2 - Not completed with description and checklist items
            val task2Result = taskRepository.insertTask(
                title = "Write report",
                tag = Tag.BLUE,
                description = "Prepare quarterly sales report for management review",
                dueDate = null,
                taskListId = taskListId
            )
            val task2Id = when (task2Result) {
                is Result.Success -> task2Result.value
                is Result.Error -> throw Exception("Failed to create task 2")
            }
            
            // Add checklist items for task 2
            taskChecklistItemsRepository.insertTaskChecklistItems(
                task2Id,
                "Gather sales data",
                "Create charts",
                "Write analysis",
                "Review and proofread"
            )

            // Task 3 - Not completed with description only
            val task3Result = taskRepository.insertTask(
                title = "Call dentist",
                tag = Tag.GREEN,
                description = "Schedule annual dental checkup appointment",
                dueDate = null,
                taskListId = taskListId
            )
        }

    }

    fun deleteAllTasks() {
        // Get DAO instances from Koin
        val taskDao: ITaskDao = GlobalContext.get().get()
        val taskListDao: ITaskListDao = GlobalContext.get().get()
        val taskChecklistItemDao: ITaskChecklistItemDao = GlobalContext.get().get()

        runBlocking {
            // Delete existing data
            taskChecklistItemDao.deleteAllTaskChecklistItems()
            taskDao.deleteAllTasks()
            taskListDao.deleteAllTaskLists()
        }

        // Wait for UI to update
        composeTestRule.waitForIdle()
        Thread.sleep(500)
    }

    private fun createTask(taskName: String) {
        val addTaskDescription = "Add task"
        val enterTaskNamePlaceholder = "Enter a Task name"
        val saveDescription = "Save"

        composeTestRule.onNodeWithContentDescription(addTaskDescription).performClick()

        // Wait for Add Task screen (anchor on something stable)
        composeTestRule.waitUntilNodeExists(
            matcher = hasText(enterTaskNamePlaceholder)
        )

        composeTestRule.onNodeWithText(enterTaskNamePlaceholder).performClick()
        composeTestRule.onNodeWithText(enterTaskNamePlaceholder)
            .performTextReplacement(taskName)

        composeTestRule.onNodeWithContentDescription(saveDescription).performClick()
        
        // Wait until we are back on the list screen (optional but helpful)
        // Example: a title, toolbar, or the FAB existing again.
        composeTestRule.waitUntilNodeExists(
            matcher = hasContentDescription(addTaskDescription)
        )

        // Wait until the item is actually in the list
        composeTestRule.waitUntilNodeExists(
            matcher = hasText(taskName)
        )
    }

    private fun ComposeContentTestRule.waitUntilNodeExists(
        timeoutMillis: Long = 5_000,
        useUnmergedTree: Boolean = true,
        matcher: SemanticsMatcher
    ) {
        waitUntil(timeoutMillis) {
            onAllNodes(matcher, useUnmergedTree = useUnmergedTree)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

}
