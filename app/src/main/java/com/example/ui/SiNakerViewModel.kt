package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.Naker
import com.example.data.local.entity.Absensi
import com.example.data.local.entity.Material
import com.example.data.local.entity.Alker
import com.example.data.repository.SiNakerRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import com.example.data.remote.GeminiService
import com.example.data.remote.OnlineContact
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SiNakerViewModel(private val repository: SiNakerRepository) : ViewModel() {

    // New Online Search States
    private val _onlineContacts = MutableStateFlow<List<OnlineContact>>(emptyList())
    val onlineContacts: StateFlow<List<OnlineContact>> = _onlineContacts.asStateFlow()

    private val _isSearchLoading = MutableStateFlow(false)
    val isSearchLoading: StateFlow<Boolean> = _isSearchLoading.asStateFlow()

    // Locally added cloud registry (allows user to register themselves "online")
    private val _userCloudRegistrations = MutableStateFlow<List<OnlineContact>>(emptyList())
    val userCloudRegistrations: StateFlow<List<OnlineContact>> = _userCloudRegistrations.asStateFlow()

    // Streams from Database
    val nakerList: StateFlow<List<Naker>> = repository.allNaker
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val absensiList: StateFlow<List<Absensi>> = repository.allAbsensi
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val materialList: StateFlow<List<Material>> = repository.allMaterial
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val alkerList: StateFlow<List<Alker>> = repository.allAlker
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Events for feedback (e.g. snackbars)
    private val _eventFlow = MutableSharedFlow<String>()
    val eventFlow: SharedFlow<String> = _eventFlow.asSharedFlow()

    // ----------------------------------------------------
    // Actions
    // ----------------------------------------------------

    fun addNaker(name: String, role: String, phone: String) {
        viewModelScope.launch {
            if (name.isBlank() || role.isBlank()) {
                _eventFlow.emit("Gagal: Nama dan Peran tidak boleh kosong.")
                return@launch
            }
            val naker = Naker(name = name, role = role, phone = phone)
            repository.insertNaker(naker)
            _eventFlow.emit("Sukses menambahkan tenaga kerja: $name")
        }
    }

    fun submitAbsensi(naker: Naker, status: String, type: String, location: String, notes: String) {
        viewModelScope.launch {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val now = Date()

            val absensi = Absensi(
                nakerId = naker.id,
                nakerName = naker.name,
                type = type,
                status = status,
                date = dateFormat.format(now),
                time = timeFormat.format(now),
                location = if (location.isNotBlank()) location else "Lokasi Lapangan Default (Jakarta)",
                notes = notes
            )
            repository.insertAbsensi(absensi)
            _eventFlow.emit("Sukses mencatat absensi $type untuk ${naker.name}")
        }
    }

    fun addMaterial(name: String, stock: Double, unit: String, updatedBy: String, notes: String) {
        viewModelScope.launch {
            if (name.isBlank() || unit.isBlank()) {
                _eventFlow.emit("Gagal: Nama material dan satuan tidak boleh kosong.")
                return@launch
            }
            val mat = Material(
                name = name,
                remainingStock = stock,
                unit = unit,
                updatedBy = updatedBy.ifBlank { "Sistem" },
                notes = notes
            )
            repository.insertMaterial(mat)
            _eventFlow.emit("Sukses menambahkan material baru: $name")
        }
    }

    fun updateMaterialStock(id: Int, stock: Double, updatedBy: String, notes: String) {
        viewModelScope.launch {
            if (stock < 0) {
                _eventFlow.emit("Gagal: Sisa stok tidak boleh negatif.")
                return@launch
            }
            repository.updateStock(id, stock, updatedBy.ifBlank { "Sistem" }, notes)
            _eventFlow.emit("Sukses memperbarui sisa stok material!")
        }
    }

    fun addAlker(name: String, code: String, condition: String, heldBy: String, notes: String) {
        viewModelScope.launch {
            if (name.isBlank() || code.isBlank()) {
                _eventFlow.emit("Gagal: Nama alat kerja dan kode unit tidak boleh kosong.")
                return@launch
            }
            val alker = Alker(
                name = name,
                code = code,
                condition = condition,
                heldBy = heldBy.ifBlank { "Belum diserahkan" },
                notes = notes
            )
            repository.insertAlker(alker)
            _eventFlow.emit("Sukses menambahkan alat kerja baru: $name")
        }
    }

    fun updateAlkerCondition(id: Int, condition: String, heldBy: String, notes: String) {
        viewModelScope.launch {
            repository.updateAlkerCondition(id, condition, heldBy.ifBlank { "Belum diserahkan" }, notes)
            _eventFlow.emit("Sukses memperbarui kondisi alat kerja!")
        }
    }

    fun searchOnlineContacts(query: String) {
        viewModelScope.launch {
            _isSearchLoading.value = true
            try {
                val apiResults = GeminiService.searchOnlineContacts(query)
                val filteredUserCloud = _userCloudRegistrations.value.filter {
                    it.name.contains(query, ignoreCase = true) ||
                    it.role.contains(query, ignoreCase = true) ||
                    it.location.contains(query, ignoreCase = true)
                }
                val combined = (filteredUserCloud + apiResults).distinctBy { it.name + it.phone }
                _onlineContacts.value = combined
            } catch (e: Exception) {
                _eventFlow.emit("Gagal mencari kontak online: ${e.localizedMessage}")
            } finally {
                _isSearchLoading.value = false
            }
        }
    }

    fun registerContactOnline(name: String, role: String, phone: String, location: String) {
        viewModelScope.launch {
            if (name.isBlank() || role.isBlank() || phone.isBlank() || location.isBlank()) {
                _eventFlow.emit("Lengkapi data untuk mendaftar ke Cloud.")
                return@launch
            }
            val newCloudContact = OnlineContact(name, role, phone, location)
            _userCloudRegistrations.value = _userCloudRegistrations.value + newCloudContact
            _onlineContacts.value = (_onlineContacts.value + newCloudContact).distinctBy { it.name + it.phone }
            _eventFlow.emit("Sukses mendaftarkan $name secara online!")
        }
    }
}

class SiNakerViewModelFactory(private val repository: SiNakerRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SiNakerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SiNakerViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
