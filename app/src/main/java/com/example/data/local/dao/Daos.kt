package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.Naker
import com.example.data.local.entity.Absensi
import com.example.data.local.entity.Material
import com.example.data.local.entity.Alker
import kotlinx.coroutines.flow.Flow

@Dao
interface NakerDao {
    @Query("SELECT * FROM naker ORDER BY name ASC")
    fun getAllNaker(): Flow<List<Naker>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNaker(naker: Naker)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNakers(nakers: List<Naker>)

    @Delete
    suspend fun deleteNaker(naker: Naker)
}

@Dao
interface AbsensiDao {
    @Query("SELECT * FROM absensi ORDER BY date DESC, time DESC")
    fun getAllAbsensi(): Flow<List<Absensi>>

    @Query("SELECT * FROM absensi WHERE date = :date ORDER BY time DESC")
    fun getAbsensiByDate(date: String): Flow<List<Absensi>>

    @Query("SELECT * FROM absensi WHERE nakerId = :nakerId ORDER BY date DESC, time DESC")
    fun getAbsensiByNaker(nakerId: Int): Flow<List<Absensi>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAbsensi(absensi: Absensi)
}

@Dao
interface MaterialDao {
    @Query("SELECT * FROM material ORDER BY name ASC")
    fun getAllMaterial(): Flow<List<Material>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaterial(material: Material)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaterials(materials: List<Material>)

    @Query("UPDATE material SET remainingStock = :stock, updatedBy = :updatedBy, updateTime = :time, notes = :notes WHERE id = :id")
    suspend fun updateStock(id: Int, stock: Double, updatedBy: String, time: Long, notes: String)
}

@Dao
interface AlkerDao {
    @Query("SELECT * FROM alker ORDER BY name ASC")
    fun getAllAlker(): Flow<List<Alker>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlker(alker: Alker)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlkers(alkers: List<Alker>)

    @Query("UPDATE alker SET condition = :condition, heldBy = :heldBy, updateTime = :time, notes = :notes WHERE id = :id")
    suspend fun updateAlkerCondition(id: Int, condition: String, heldBy: String, time: Long, notes: String)
}
