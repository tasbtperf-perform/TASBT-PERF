package com.example.data.repository

import com.example.data.local.dao.NakerDao
import com.example.data.local.dao.AbsensiDao
import com.example.data.local.dao.MaterialDao
import com.example.data.local.dao.AlkerDao
import com.example.data.local.entity.Naker
import com.example.data.local.entity.Absensi
import com.example.data.local.entity.Material
import com.example.data.local.entity.Alker
import kotlinx.coroutines.flow.Flow

class SiNakerRepository(
    private val nakerDao: NakerDao,
    private val absensiDao: AbsensiDao,
    private val materialDao: MaterialDao,
    private val alkerDao: AlkerDao
) {
    val allNaker: Flow<List<Naker>> = nakerDao.getAllNaker()
    val allAbsensi: Flow<List<Absensi>> = absensiDao.getAllAbsensi()
    val allMaterial: Flow<List<Material>> = materialDao.getAllMaterial()
    val allAlker: Flow<List<Alker>> = alkerDao.getAllAlker()

    suspend fun insertNaker(naker: Naker) {
        nakerDao.insertNaker(naker)
    }

    suspend fun deleteNaker(naker: Naker) {
        nakerDao.deleteNaker(naker)
    }

    suspend fun insertAbsensi(absensi: Absensi) {
        absensiDao.insertAbsensi(absensi)
    }

    fun getAbsensiByDate(date: String): Flow<List<Absensi>> {
        return absensiDao.getAbsensiByDate(date)
    }

    fun getAbsensiByNaker(nakerId: Int): Flow<List<Absensi>> {
        return absensiDao.getAbsensiByNaker(nakerId)
    }

    suspend fun insertMaterial(material: Material) {
        materialDao.insertMaterial(material)
    }

    suspend fun updateStock(id: Int, stock: Double, updatedBy: String, notes: String) {
        materialDao.updateStock(id, stock, updatedBy, System.currentTimeMillis(), notes)
    }

    suspend fun insertAlker(alker: Alker) {
        alkerDao.insertAlker(alker)
    }

    suspend fun updateAlkerCondition(id: Int, condition: String, heldBy: String, notes: String) {
        alkerDao.updateAlkerCondition(id, condition, heldBy, System.currentTimeMillis(), notes)
    }
}
