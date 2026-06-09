package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "naker")
data class Naker(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val role: String,
    val phone: String
)

@Entity(tableName = "absensi")
data class Absensi(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nakerId: Int,
    val nakerName: String,
    val type: String, // "Check In" atau "Check Out"
    val status: String, // "Hadir", "Izin", "Sakit", "Cuti"
    val date: String, // yyyy-MM-dd
    val time: String, // HH:mm
    val location: String, // Lokasi check-in/out
    val notes: String // Catatan tambahan (misalnya: "Mulai pekerjaan FO", "Mengajukan izin sakit", dll)
)

@Entity(tableName = "material")
data class Material(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val remainingStock: Double,
    val unit: String, // "meter", "buah", "saset", "set", dll
    val updatedBy: String, // Nama Naker yang memperbarui
    val updateTime: Long = System.currentTimeMillis(),
    val notes: String
)

@Entity(tableName = "alker")
data class Alker(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String, // Nama Alat Kerja
    val code: String, // Kode barang / SN (Serial Number)
    val condition: String, // "Sangat Baik", "Baik", "Rusak Ringan", "Rusak Berat"
    val heldBy: String, // Dipegang oleh siapa (Nama Naker)
    val updateTime: Long = System.currentTimeMillis(),
    val notes: String // Keterangan perbaikan / keluhan
)
