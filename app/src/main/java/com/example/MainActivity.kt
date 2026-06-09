package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.data.local.AppDatabase
import com.example.data.repository.SiNakerRepository
import com.example.ui.SiNakerApp
import com.example.ui.SiNakerViewModel
import com.example.ui.SiNakerViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val database by lazy { AppDatabase.getDatabase(applicationContext, lifecycleScope) }
    private val repository by lazy {
        SiNakerRepository(
            database.nakerDao(),
            database.absensiDao(),
            database.materialDao(),
            database.alkerDao()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val viewModel = ViewModelProvider(
            this, 
            SiNakerViewModelFactory(repository)
        )[SiNakerViewModel::class.java]

        setContent {
            MyApplicationTheme {
                SiNakerApp(viewModel = viewModel)
            }
        }
    }
}
