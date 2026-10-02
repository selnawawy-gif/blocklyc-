package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SketchDao {
    @Query("SELECT * FROM blockly_sketches ORDER BY updatedAt DESC")
    fun getAllSketches(): Flow<List<BlocklySketch>>

    @Query("SELECT * FROM blockly_sketches WHERE id = :id LIMIT 1")
    suspend fun getSketchById(id: Long): BlocklySketch?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSketch(sketch: BlocklySketch): Long

    @Update
    suspend fun updateSketch(sketch: BlocklySketch)

    @Delete
    suspend fun deleteSketch(sketch: BlocklySketch)

    @Query("DELETE FROM blockly_sketches WHERE id = :id")
    suspend fun deleteSketchById(id: Long)
}
