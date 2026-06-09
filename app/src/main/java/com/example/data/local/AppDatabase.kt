package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.NakerDao
import com.example.data.local.dao.AbsensiDao
import com.example.data.local.dao.MaterialDao
import com.example.data.local.dao.AlkerDao
import com.example.data.local.entity.Naker
import com.example.data.local.entity.Absensi
import com.example.data.local.entity.Material
import com.example.data.local.entity.Alker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [Naker::class, Absensi::class, Material::class, Alker::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun nakerDao(): NakerDao
    abstract fun absensiDao(): AbsensiDao
    abstract fun materialDao(): MaterialDao
    abstract fun alkerDao(): AlkerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sinaker_database"
                )
                .addCallback(AppDatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database)
                }
            }
        }

        suspend fun populateDatabase(db: AppDatabase) {
            // Initial Naker
            val nakers = listOf(
                Naker(name = "Budi Santoso", role = "Teknisi Splicing", phone = "081234567890"),
                Naker(name = "Ahmad Hidayat", role = "Team Leader FO", phone = "081345678901"),
                Naker(name = "Rian Hidayat", role = "Helper Pasang Baru", phone = "081456789012"),
                Naker(name = "Siti Aminah", role = "Admin Logistik", phone = "081567890123")
            )
            db.nakerDao().insertNakers(nakers)

            // Initial Materials
            val materials = listOf(
                Material(
                    name = "Kabel Drop Core FO 1 Core",
                    remainingStock = 1250.0,
                    unit = "meter",
                    updatedBy = "Siti Aminah",
                    notes = "Stok awal gudang utama"
                ),
                Material(
                    name = "Konektor Fast Connector UPC",
                    remainingStock = 120.0,
                    unit = "buah",
                    updatedBy = "Siti Aminah",
                    notes = "Baru datang dari disti"
                ),
                Material(
                    name = "Joint Closure Dome 24 Core",
                    remainingStock = 5.0,
                    unit = "buah",
                    updatedBy = "Ahmad Hidayat",
                    notes = "Sisa proyek tol rampas"
                ),
                Material(
                    name = "Optical Distribution Point (ODP) Solid",
                    remainingStock = 12.0,
                    unit = "buah",
                    updatedBy = "Siti Aminah",
                    notes = "Stok siap pasang"
                ),
                Material(
                    name = "Splitter Pasif 1:8 PLC",
                    remainingStock = 15.0,
                    unit = "buah",
                    updatedBy = "Budi Santoso",
                    notes = "Sisa pasang baru perumahan"
                )
            )
            db.materialDao().insertMaterials(materials)

            // Initial Alker
            val alkers = listOf(
                Alker(
                    name = "Fusion Splicer Fujikura 90S",
                    code = "SPL-001",
                    condition = "Sangat Baik",
                    heldBy = "Budi Santoso",
                    notes = "Kalibrasi laser harian ok"
                ),
                Alker(
                    name = "OTDR Yokogawa AQ1210",
                    code = "OTD-002",
                    condition = "Baik",
                    heldBy = "Ahmad Hidayat",
                    notes = "Bodi tergores sedikit, fungsi normal"
                ),
                Alker(
                    name = "Tangga Aluminium Slide 6M",
                    code = "TNG-003",
                    condition = "Rusak Ringan",
                    heldBy = "Rian Hidayat",
                    notes = "Karet kaki bawah sebelah kanan hilang"
                ),
                Alker(
                    name = "Optical Power Meter & VFL",
                    code = "OPM-004",
                    condition = "Sangat Baik",
                    heldBy = "Budi Santoso",
                    notes = "Baterai baru diisi ulang"
                )
            )
            db.alkerDao().insertAlkers(alkers)

            // Add a sample attendance record to have history
            val sampleAttendance = listOf(
                Absensi(
                    nakerId = 1,
                    nakerName = "Budi Santoso",
                    type = "Check In",
                    status = "Hadir",
                    date = "2026-06-09",
                    time = "08:02",
                    location = "-6.2088, 106.8456 (Gudang Jakarta East)",
                    notes = "Mulai shift pagi dan ambil Splicer"
                ),
                Absensi(
                    nakerId = 2,
                    nakerName = "Ahmad Hidayat",
                    type = "Check In",
                    status = "Hadir",
                    date = "2026-06-09",
                    time = "07:55",
                    location = "-6.2088, 106.8456 (Gudang Jakarta East)",
                    notes = "Briefing tim sebelum ke lapangan"
                )
            )
            db.absensiDao().insertAbsensi(sampleAttendance[0])
            db.absensiDao().insertAbsensi(sampleAttendance[1])
        }
    }
}
