package com.monster.literaryflow.bean

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "test_data")
class TestData(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val appName: String, val appTitle: String
)