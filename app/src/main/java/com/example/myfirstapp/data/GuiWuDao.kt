package com.example.myfirstapp.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GuiWuDao {
    @Query("SELECT * FROM guiwu_items ORDER BY createdAt DESC")
    fun getAllItems(): Flow<List<GuiWuItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: GuiWuItem)

    @Delete
    suspend fun deleteItem(item: GuiWuItem)
}
