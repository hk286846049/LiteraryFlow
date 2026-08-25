package com.monster.literaryflow.room

import androidx.room.*
import com.monster.literaryflow.bean.AutoInfo

@Dao
interface AutoInfoDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(autoInfo: AutoInfo)

    @Query("SELECT * FROM auto_info")
    fun getAll(): List<AutoInfo>

    @Update
    fun update(autoInfo: AutoInfo)
    @Query("SELECT * FROM auto_info WHERE id = :id LIMIT 1")
    fun findById(id: Int): AutoInfo?
    @Query("SELECT * FROM auto_info WHERE title = :title LIMIT 1")
    fun findByTitle(title: String): AutoInfo?

    @Delete
    fun delete(autoInfo: AutoInfo)

}
