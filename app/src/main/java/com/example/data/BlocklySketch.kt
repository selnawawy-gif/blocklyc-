package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blockly_sketches")
data class BlocklySketch(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val boardType: String = "UNO",
    val xmlContent: String,
    val cppContent: String,
    val blockCount: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)
