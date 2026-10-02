package com.example.data

import kotlinx.coroutines.flow.Flow

class SketchRepository(private val sketchDao: SketchDao) {
    val allSketches: Flow<List<BlocklySketch>> = sketchDao.getAllSketches()

    suspend fun getSketchById(id: Long): BlocklySketch? = sketchDao.getSketchById(id)

    suspend fun saveSketch(sketch: BlocklySketch): Long = sketchDao.insertSketch(sketch)

    suspend fun updateSketch(sketch: BlocklySketch) = sketchDao.updateSketch(sketch)

    suspend fun deleteSketch(sketch: BlocklySketch) = sketchDao.deleteSketch(sketch)

    suspend fun deleteSketchById(id: Long) = sketchDao.deleteSketchById(id)
}
