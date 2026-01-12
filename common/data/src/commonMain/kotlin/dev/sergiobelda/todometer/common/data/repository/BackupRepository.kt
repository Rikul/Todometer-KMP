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

package dev.sergiobelda.todometer.common.data.repository

import dev.sergiobelda.todometer.common.database.TodometerDatabase
import dev.sergiobelda.todometer.common.database.dao.ITaskChecklistItemDao
import dev.sergiobelda.todometer.common.database.dao.ITaskDao
import dev.sergiobelda.todometer.common.database.dao.ITaskListDao
import dev.sergiobelda.todometer.common.database.mapper.asTaskChecklistItem
import dev.sergiobelda.todometer.common.database.mapper.asTaskChecklistItemEntity
import dev.sergiobelda.todometer.common.database.mapper.asTaskEntity
import dev.sergiobelda.todometer.common.database.mapper.asTaskList
import dev.sergiobelda.todometer.common.database.mapper.asTaskListEntity
import dev.sergiobelda.todometer.common.database.mapper.asTask
import dev.sergiobelda.todometer.common.domain.model.BackupData
import dev.sergiobelda.todometer.common.domain.repository.IBackupRepository
import kotlinx.coroutines.flow.first

class BackupRepository(
    private val todometerDatabase: TodometerDatabase,
    private val taskListDao: ITaskListDao,
    private val taskDao: ITaskDao,
    private val taskChecklistItemDao: ITaskChecklistItemDao,
) : IBackupRepository {

    override suspend fun getBackupData(): BackupData {
        val taskLists = taskListDao.getTaskLists().first().map { it.asTaskList() }
        val tasks = taskDao.getAllTasks().map { it.asTask() }
        val taskChecklistItems =
            taskChecklistItemDao.getAllTaskChecklistItems().map { it.asTaskChecklistItem() }
        return BackupData(
            taskLists = taskLists,
            tasks = tasks,
            taskChecklistItems = taskChecklistItems,
        )
    }

    override suspend fun restoreBackupData(backupData: BackupData) {
        try {
            // Backup current data in case of rollback
            val currentBackup = try {
                getBackupData()
            } catch (e: Exception) {
                null
            }

            try {
                // Delete existing data
                taskChecklistItemDao.deleteAllTaskChecklistItems()
                taskDao.deleteAllTasks()
                taskListDao.deleteAllTaskLists()

                // Insert Data
                // Order: TaskLists -> Tasks -> ChecklistItems
                taskListDao.insertTaskLists(backupData.taskLists.map { it.asTaskListEntity() })
                taskDao.insertTasks(backupData.tasks.map { it.asTaskEntity() })
                // insertTaskChecklistItems takes vararg
                val checklistItemEntities = backupData.taskChecklistItems.map { it.asTaskChecklistItemEntity() }
                if (checklistItemEntities.isNotEmpty()) {
                    taskChecklistItemDao.insertTaskChecklistItems(*checklistItemEntities.toTypedArray())
                }
            } catch (e: Exception) {
                // If restore fails, attempt to restore from backup
                if (currentBackup != null) {
                    try {
                        restoreBackupData(currentBackup)
                    } catch (rollbackError: Exception) {
                        // If rollback also fails, throw the original error with context
                        throw RestoreException(
                            "Failed to restore backup and rollback also failed",
                            e
                        )
                    }
                }
                throw RestoreException("Failed to restore backup data", e)
            }
        } catch (e: RestoreException) {
            throw e
        } catch (e: Exception) {
            throw RestoreException("Unexpected error during restore", e)
        }
    }
}
