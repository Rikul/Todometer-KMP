package dev.sergiobelda.todometer.common.domain.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TaskSerializationTest {

    @Test
    fun testTaskSerialization() {
        // 1. Create a test list of Task model objects (including completed and uncompleted tasks)
        val task1 = Task(
            id = "1",
            title = "Task 1",
            tag = Tag.RED,
            description = "Description 1",
            dueDate = 1630000000000L,
            state = TaskState.DOING, // Uncompleted
            taskListId = "list1",
            isPinned = false,
            sync = true
        )
        val task2 = Task(
            id = "2",
            title = "Task 2",
            tag = Tag.BLUE,
            description = null,
            dueDate = null,
            state = TaskState.DONE, // Completed
            taskListId = "list1",
            isPinned = true,
            sync = false
        )

        val taskList = listOf(task1, task2)

        // 2. Pass this list to the serialization logic
        val jsonString = Json.encodeToString(taskList)

        assertTrue(jsonString.startsWith("[") && jsonString.endsWith("]"), "Output should be a JSON array")
        
        assertTrue(jsonString.contains("\"id\":\"1\""), "Should contain task 1 id")
        assertTrue(jsonString.contains("\"title\":\"Task 1\""), "Should contain task 1 title")
        assertTrue(jsonString.contains("\"state\":\"DOING\""), "Should contain task 1 state")
        
        assertTrue(jsonString.contains("\"id\":\"2\""), "Should contain task 2 id")
        assertTrue(jsonString.contains("\"state\":\"DONE\""), "Should contain task 2 state")
        
        val decodedList = Json.decodeFromString<List<Task>>(jsonString)
        assertEquals(taskList, decodedList, "Deserialized list should match original list")
    }
}
