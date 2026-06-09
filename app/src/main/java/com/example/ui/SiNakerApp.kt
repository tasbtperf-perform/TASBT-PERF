package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.Absensi
import com.example.data.local.entity.Alker
import com.example.data.local.entity.Material
import com.example.data.local.entity.Naker
import com.example.data.remote.OnlineContact
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

enum class Screen {
    Home,
    Absensi,
    Material,
    Alker,
    Naker
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiNakerApp(viewModel: SiNakerViewModel) {
    val context = LocalContext.current
    var currentScreen by rememberSaveable { mutableStateOf(Screen.Home) }

    // SharedPreferences for Device Naker Binding
    val sharedPref = remember { context.getSharedPreferences("sinaker_prefs", Context.MODE_PRIVATE) }
    var boundNakerId by remember { mutableStateOf(sharedPref.getInt("bound_naker_id", -1)) }
    var boundNakerName by remember { mutableStateOf(sharedPref.getString("bound_naker_name", "") ?: "") }

    // Collect data state
    val nakers by viewModel.nakerList.collectAsStateWithLifecycle()
    val absensiList by viewModel.absensiList.collectAsStateWithLifecycle()
    val materials by viewModel.materialList.collectAsStateWithLifecycle()
    val alkers by viewModel.alkerList.collectAsStateWithLifecycle()
    val onlineContacts by viewModel.onlineContacts.collectAsStateWithLifecycle()
    val isSearchLoading by viewModel.isSearchLoading.collectAsStateWithLifecycle()

    // Match bound identity asynchronously on load / change
    val activeNaker = nakers.find { it.id == boundNakerId } ?: nakers.find { it.name.trim().lowercase() == boundNakerName.trim().lowercase() }

    LaunchedEffect(activeNaker) {
        if (activeNaker != null && activeNaker.id != boundNakerId) {
            sharedPref.edit().putInt("bound_naker_id", activeNaker.id).apply()
            boundNakerId = activeNaker.id
        }
    }

    // Dialog trigger states
    var showAddNakerDialog by remember { mutableStateOf(false) }
    var showAddAbsensiDialog by remember { mutableStateOf(false) }
    var showAddMaterialDialog by remember { mutableStateOf(false) }
    var showUpdateStockDialog by remember { mutableStateOf(false) }
    var showAddAlkerDialog by remember { mutableStateOf(false) }
    var showUpdateAlkerDialog by remember { mutableStateOf(false) }
    var showChangeUserDialog by remember { mutableStateOf(false) }

    // Selection helper for updates
    var selectedMaterial by remember { mutableStateOf<Material?>(null) }
    var selectedAlker by remember { mutableStateOf<Alker?>(null) }

    // Scaffold Snackbar
    val snackbarHostState = remember { SnackbarHostState() }

    // Listen to ViewModel events
    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (activeNaker != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showChangeUserDialog = true }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEADDFF)),
                                contentAlignment = Alignment.Center
                            ) {
                                val initials = if (activeNaker.name.trim().contains(" ")) {
                                    val parts = activeNaker.name.trim().split("\\s+".toRegex())
                                    (parts[0].take(1) + parts[1].take(1)).uppercase()
                                } else {
                                    activeNaker.name.take(2).uppercase()
                                }
                                Text(
                                    text = initials,
                                    color = Color(0xFF21005D),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = activeNaker.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${activeNaker.role} • Terikat HP ini",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = "Ganti Profil",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Construction,
                                contentDescription = "Logo",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Sistem Integrasi SiNaker",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            Toast.makeText(context, "Tidak ada notifikasi baru", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "Notifikasi",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        },
        bottomBar = {
            if (activeNaker != null) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    NavigationBarItem(
                        selected = currentScreen == Screen.Home,
                        onClick = { currentScreen = Screen.Home },
                        icon = {
                            Icon(
                                imageVector = if (currentScreen == Screen.Home) Icons.Filled.Dashboard else Icons.Outlined.Dashboard,
                                contentDescription = "Beranda"
                            )
                        },
                        label = { Text("Beranda", fontSize = 11.sp, fontWeight = FontWeight.Medium) }
                    )
                    NavigationBarItem(
                        selected = currentScreen == Screen.Absensi,
                        onClick = { currentScreen = Screen.Absensi },
                        icon = {
                            Icon(
                                imageVector = if (currentScreen == Screen.Absensi) Icons.Filled.AssignmentInd else Icons.Outlined.AssignmentInd,
                                contentDescription = "Absen"
                            )
                        },
                        label = { Text("Absen", fontSize = 11.sp, fontWeight = FontWeight.Medium) }
                    )
                    NavigationBarItem(
                        selected = currentScreen == Screen.Material,
                        onClick = { currentScreen = Screen.Material },
                        icon = {
                            Icon(
                                imageVector = if (currentScreen == Screen.Material) Icons.Filled.Inventory else Icons.Outlined.Inventory,
                                contentDescription = "Material"
                            )
                        },
                        label = { Text("Material", fontSize = 11.sp, fontWeight = FontWeight.Medium) }
                    )
                    NavigationBarItem(
                        selected = currentScreen == Screen.Alker,
                        onClick = { currentScreen = Screen.Alker },
                        icon = {
                            Icon(
                                imageVector = if (currentScreen == Screen.Alker) Icons.Filled.Build else Icons.Outlined.Build,
                                contentDescription = "Alat Kerja"
                            )
                        },
                        label = { Text("Alker", fontSize = 11.sp, fontWeight = FontWeight.Medium) }
                    )
                    NavigationBarItem(
                        selected = currentScreen == Screen.Naker,
                        onClick = { currentScreen = Screen.Naker },
                        icon = {
                            Icon(
                                imageVector = if (currentScreen == Screen.Naker) Icons.Filled.Badge else Icons.Outlined.Badge,
                                contentDescription = "Data Naker"
                            )
                        },
                        label = { Text("Naker", fontSize = 11.sp, fontWeight = FontWeight.Medium) }
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (activeNaker == null) {
                DeviceBindingScreen(
                    nakers = nakers,
                    onBindExisting = { n ->
                        sharedPref.edit()
                            .putInt("bound_naker_id", n.id)
                            .putString("bound_naker_name", n.name)
                            .apply()
                        boundNakerId = n.id
                        boundNakerName = n.name
                        Toast.makeText(context, "Perangkat diikat ke: ${n.name}", Toast.LENGTH_SHORT).show()
                    },
                    onRegisterAndBind = { name, role, phone ->
                        viewModel.addNaker(name, role, phone)
                        sharedPref.edit().putString("bound_naker_name", name).apply()
                        boundNakerName = name
                        Toast.makeText(context, "Profil didaftarkan. Menghubungkan perangkat...", Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        fadeIn(animationSpec = spring()) togetherWith fadeOut(animationSpec = spring())
                    },
                    label = "ScreenTransition"
                ) { targetScreen ->
                    when (targetScreen) {
                        Screen.Home -> DashboardScreen(
                            nakers = nakers,
                            absensiList = absensiList,
                            materials = materials,
                            alkers = alkers,
                            onNavigateTo = { currentScreen = it },
                            onUpdateMaterialStockClick = { mat ->
                                selectedMaterial = mat
                                showUpdateStockDialog = true
                            },
                            onAddMaterialClick = { showAddMaterialDialog = true },
                            onUpdateAlkerClick = { alk ->
                                selectedAlker = alk
                                showUpdateAlkerDialog = true
                            },
                            onAddAlkerClick = { showAddAlkerDialog = true },
                            onSubmitAbsensi = { status, type, location, notes ->
                                if (activeNaker != null) {
                                    viewModel.submitAbsensi(activeNaker, status, type, location, notes)
                                }
                            },
                            activeNaker = activeNaker,
                            onChooseOtherNaker = {
                                sharedPref.edit().remove("bound_naker_id").remove("bound_naker_name").apply()
                                boundNakerId = -1
                                boundNakerName = ""
                            }
                        )
                        Screen.Absensi -> AbsensiScreen(
                            nakers = nakers,
                            absensiList = absensiList,
                            onAddAbsensiClick = { showAddAbsensiDialog = true }
                        )
                        Screen.Material -> MaterialScreen(
                            materials = materials,
                            nakers = nakers,
                            onAddMaterialClick = { showAddMaterialDialog = true },
                            onUpdateStockClick = { mat ->
                                selectedMaterial = mat
                                showUpdateStockDialog = true
                            }
                        )
                    Screen.Alker -> AlkerScreen(
                        alkers = alkers,
                        nakers = nakers,
                        onAddAlkerClick = { showAddAlkerDialog = true },
                        onUpdateAlkerClick = { al ->
                            selectedAlker = al
                            showUpdateAlkerDialog = true
                        }
                    )
                    Screen.Naker -> NakerScreen(
                        nakers = nakers,
                        onlineContacts = onlineContacts,
                        isSearchLoading = isSearchLoading,
                        onSearchOnline = { query -> viewModel.searchOnlineContacts(query) },
                        onRegisterOnline = { name, role, phone, location ->
                            viewModel.registerContactOnline(name, role, phone, location)
                        },
                        onAddOnlineToLocal = { contact ->
                            viewModel.addNaker(contact.name, contact.role, contact.phone)
                        },
                        onAddNakerClick = { showAddNakerDialog = true }
                    )
                }
            }
        }
    }
}

    // ----------------------------------------------------
    // DIALOG DIALOG OVERLAYS
    // ----------------------------------------------------

    if (showAddNakerDialog) {
        AddNakerDialog(
            onDismiss = { showAddNakerDialog = false },
            onConfirmWithCloud = { name, role, phone, location, registerOnline ->
                // Save locally first
                viewModel.addNaker(name, role, phone)
                // Register online directly
                if (registerOnline) {
                    viewModel.registerContactOnline(name, role, phone, location)
                }
                showAddNakerDialog = false
            }
        )
    }

    if (showAddAbsensiDialog) {
        if (nakers.isEmpty()) {
            AlertDialog(
                onDismissRequest = { showAddAbsensiDialog = false },
                title = { Text("Tidak Ada Tenaga Kerja") },
                text = { Text("Daftarkan tenaga kerja (naker) terlebih dahulu di tab Naker sebelum melaporkan absensi.") },
                confirmButton = {
                    Button(onClick = {
                        showAddAbsensiDialog = false
                        currentScreen = Screen.Naker
                    }) {
                        Text("Daftar Naker")
                    }
                }
            )
        } else {
            AddAbsensiDialog(
                nakers = nakers,
                onDismiss = { showAddAbsensiDialog = false },
                onConfirm = { naker, status, type, location, notes ->
                    viewModel.submitAbsensi(naker, status, type, location, notes)
                    showAddAbsensiDialog = false
                }
            )
        }
    }

    if (showAddMaterialDialog) {
        AddMaterialDialog(
            nakers = nakers,
            onDismiss = { showAddMaterialDialog = false },
            onConfirm = { name, initialStock, unit, updatedBy, notes ->
                viewModel.addMaterial(name, initialStock, unit, updatedBy, notes)
                showAddMaterialDialog = false
            }
        )
    }

    if (showUpdateStockDialog && selectedMaterial != null) {
        UpdateStockDialog(
            material = selectedMaterial!!,
            nakers = nakers,
            onDismiss = {
                showUpdateStockDialog = false
                selectedMaterial = null
            },
            onConfirm = { id, newStock, updatedBy, notes ->
                viewModel.updateMaterialStock(id, newStock, updatedBy, notes)
                showUpdateStockDialog = false
                selectedMaterial = null
            }
        )
    }

    if (showAddAlkerDialog) {
        AddAlkerDialog(
            nakers = nakers,
            onDismiss = { showAddAlkerDialog = false },
            onConfirm = { name, code, condition, heldBy, notes ->
                viewModel.addAlker(name, code, condition, heldBy, notes)
                showAddAlkerDialog = false
            }
        )
    }

    if (showUpdateAlkerDialog && selectedAlker != null) {
        UpdateAlkerConditionDialog(
            alker = selectedAlker!!,
            nakers = nakers,
            onDismiss = {
                showUpdateAlkerDialog = false
                selectedAlker = null
            },
            onConfirm = { id, newCondition, heldBy, notes ->
                viewModel.updateAlkerCondition(id, newCondition, heldBy, notes)
                showUpdateAlkerDialog = false
                selectedAlker = null
            }
        )
    }

    if (showChangeUserDialog && activeNaker != null) {
        AlertDialog(
            onDismissRequest = { showChangeUserDialog = false },
            title = { Text("Ganti Profil Perangkat") },
            text = { Text("Satu perangkat hanya diperbolehkan untuk satu tenaga kerja.\n\nApakah anda yakin ingin melepas ikatan perangkat dari ${activeNaker.name}?") },
            confirmButton = {
                Button(
                    onClick = {
                        sharedPref.edit().remove("bound_naker_id").remove("bound_naker_name").apply()
                        boundNakerId = -1
                        boundNakerName = ""
                        showChangeUserDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Lepas Ikatan", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangeUserDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

// --------------------------------------------------------------------------------------------------
// ONBOARDING DEVICE BINDING SCREEN (1 DEVICE = 1 WORKER ONLY)
// --------------------------------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceBindingScreen(
    nakers: List<Naker>,
    onBindExisting: (Naker) -> Unit,
    onRegisterAndBind: (String, String, String) -> Unit
) {
    var isRegistering by remember { mutableStateOf(false) }
    var selectedNaker by remember { mutableStateOf<Naker?>(null) }
    
    // Registration form fields
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    
    // Filtering/Search query
    var searchQuery by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.PhonelinkLock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(38.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Ikat Profil Perangkat",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Keamanan Integrasi: 1 Perangkat handphone hanya diijinkan terikat pada 1 profil tenaga kerja SiNaker.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (nakers.isEmpty() || isRegistering) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Daftarkan Profil Anda",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nama Lengkap") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                    )
                    
                    OutlinedTextField(
                        value = role,
                        onValueChange = { role = it },
                        label = { Text("Peran / Jabatan (e.g. Field Engineer)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Work, contentDescription = null) }
                    )
                    
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Nomor HP Aktif") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) }
                    )
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Button(
                        onClick = {
                            if (name.isNotBlank() && role.isNotBlank()) {
                                onRegisterAndBind(name, role, phone)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Daftar & Ikat Perangkat Ini", fontWeight = FontWeight.Bold)
                    }
                    
                    if (nakers.isNotEmpty()) {
                        TextButton(
                            onClick = { isRegistering = false },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text("Batal, pilih dari daftar profil naker", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Gunakan Profil Terdaftar",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Ketik nama untuk mencari...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                    )
                    
                    val filteredNakers = nakers.filter {
                        it.name.contains(searchQuery, ignoreCase = true) ||
                        it.role.contains(searchQuery, ignoreCase = true)
                    }
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        filteredNakers.forEach { naker ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (selectedNaker?.id == naker.id) MaterialTheme.colorScheme.primaryContainer 
                                        else MaterialTheme.colorScheme.surface
                                    )
                                    .clickable { selectedNaker = naker }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (naker.name.length >= 2) naker.name.take(2).uppercase() else "NK",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = naker.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = naker.role,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (selectedNaker?.id == naker.id) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Button(
                        onClick = {
                            selectedNaker?.let { onBindExisting(it) }
                        },
                        enabled = selectedNaker != null,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Hubungkan ke Profil Terpilih", fontWeight = FontWeight.Bold)
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    
                    TextButton(
                        onClick = { isRegistering = true }
                    ) {
                        Text("Tidak ada nama saya? Daftarkan Baru", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

// --------------------------------------------------------------------------------------------------
// 1. DASHBOARD SCREEN
// --------------------------------------------------------------------------------------------------
@Composable
fun DashboardScreen(
    nakers: List<Naker>,
    absensiList: List<Absensi>,
    materials: List<Material>,
    alkers: List<Alker>,
    onNavigateTo: (Screen) -> Unit,
    onUpdateMaterialStockClick: (Material) -> Unit,
    onAddMaterialClick: () -> Unit,
    onUpdateAlkerClick: (Alker) -> Unit,
    onAddAlkerClick: () -> Unit,
    onSubmitAbsensi: (status: String, type: String, location: String, notes: String) -> Unit,
    activeNaker: Naker?,
    onChooseOtherNaker: () -> Unit
) {
    val context = LocalContext.current
    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val myTodayAbsensi = absensiList.filter { it.nakerId == activeNaker?.id && it.date == todayStr }
    val checkInAbsen = myTodayAbsensi.find { it.type == "Check In" }
    val checkOutAbsen = myTodayAbsensi.find { it.type == "Check Out" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Presensi Hari Ini (Attendance Card) - DESIGN-THEME STYLED
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFEADDFF)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Presensi Presisi Anda",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF21005D)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id", "ID")).format(Date()),
                            fontSize = 12.sp,
                            color = Color(0xFF49454F)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(Color(0xFFFFD8E4))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (checkOutAbsen != null) "SELESAI HARI INI" else if (checkInAbsen != null) "SEDANG BEKERJA" else "SHIFT PAGI",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF31111D)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Show core metrics (Check-in Check-out Display)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.5f))
                            .border(1.dp, Color(0xFFD0BCFF), RoundedCornerShape(16.dp))
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "MASUK",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF49454F)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = checkInAbsen?.time ?: "--:--",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF21005D)
                            )
                            if (checkInAbsen != null) {
                                Text(
                                    text = "(${checkInAbsen.status})",
                                    fontSize = 11.sp,
                                    color = Color(0xFF2E7D32),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.5f))
                            .border(1.dp, Color(0xFFD0BCFF), RoundedCornerShape(16.dp))
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "PULANG / KELUAR",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF49454F)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = checkOutAbsen?.time ?: "--:--",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF21005D)
                            )
                            if (checkOutAbsen != null) {
                                Text(
                                    text = "(Selesai)",
                                    fontSize = 11.sp,
                                    color = Color(0xFF1565C0),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Interactive Quick Presensi Form
                if (checkInAbsen == null) {
                    HorizontalDivider(color = Color(0xFFD0BCFF).copy(alpha = 0.4f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Laporkan Masuk Kerja:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF21005D)
                    )
                    
                    var quickStatus by remember { mutableStateOf("Hadir") }
                    var quickNotes by remember { mutableStateOf("") }
                    var quickLoc by remember { mutableStateOf("Lokasi Lapangan Default (A1)") }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Hadir", "Izin", "Sakit").forEach { label ->
                            val selected = quickStatus == label
                            ElevatedFilterChip(
                                selected = selected,
                                onClick = { quickStatus = label },
                                label = { Text(label, fontSize = 12.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = quickNotes,
                        onValueChange = { quickNotes = it },
                        placeholder = { Text("Ketik rencana penugasan / catatan sakit") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color.White.copy(alpha = 0.6f),
                            focusedContainerColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            onSubmitAbsensi(quickStatus, "Check In", quickLoc, quickNotes)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4))
                    ) {
                        Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Kirim Presensi Masuk (Clock In)", fontWeight = FontWeight.Bold)
                    }

                } else if (checkOutAbsen == null) {
                    HorizontalDivider(color = Color(0xFFD0BCFF).copy(alpha = 0.4f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Laporkan Selesai Pekerjaan:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF21005D)
                    )

                    var quickNotes by remember { mutableStateOf("") }
                    var quickLoc by remember { mutableStateOf("Lokasi Lapangan Default (A1)") }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = quickNotes,
                        onValueChange = { quickNotes = it },
                        placeholder = { Text("Tulis ringkasan hasil pekerjaan hari ini...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color.White.copy(alpha = 0.6f),
                            focusedContainerColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            onSubmitAbsensi("Hadir", "Check Out", quickLoc, quickNotes)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4))
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Kirim Presensi Pulang (Clock Out)", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE8F5E9))
                            .border(1.dp, Color(0xFFC8E6C9), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Laporan harian berhasil terkirim lengkap!",
                                color = Color(0xFF1B5E20),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Direct Sisa Material (Inventory Card) - DESIGN-THEME STYLED
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF3EDF7)
            ),
            border = BorderStroke(1.dp, Color(0xFFCAC4D0))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEADDFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = Color(0xFF21005D),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Sisa Material",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    TextButton(onClick = onAddMaterialClick) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tambah", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(10.dp))
                
                if (materials.isEmpty()) {
                    Text(
                        text = "Belum ada jenis material. Klik tombol Tambah di atas.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        materials.forEach { mat ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White)
                                    .border(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .padding(vertical = 10.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = mat.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Oleh: ${mat.updatedBy}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "${mat.remainingStock} ${mat.unit}",
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF6750A4),
                                        fontSize = 14.sp
                                    )
                                    IconButton(
                                        onClick = { onUpdateMaterialStockClick(mat) },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(Color(0xFFEADDFF), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Update material stock",
                                            tint = Color(0xFF21005D),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Direct Kondisi Alat Kerja (Alker Card) - DESIGN-THEME STYLED
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF3EDF7)
            ),
            border = BorderStroke(1.dp, Color(0xFFCAC4D0))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEADDFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = null,
                                tint = Color(0xFF21005D),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Kondisi Alat Kerja",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    TextButton(onClick = onAddAlkerClick) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tambah", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(10.dp))
                
                if (alkers.isEmpty()) {
                    Text(
                        text = "Belum ada alker. Klik tombol Tambah di atas.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        alkers.forEach { alk ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White)
                                    .border(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .padding(vertical = 10.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = alk.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    when (alk.condition) {
                                                        "Sangat Baik" -> Color(0xFF2E7D32)
                                                        "Baik" -> Color(0xFF4CAF50)
                                                        "Rusak Ringan" -> Color(0xFFEF6C00)
                                                        else -> Color(0xFFB3261E)
                                                    }
                                                )
                                        )
                                        Text(
                                            text = "${alk.condition} • PJ: ${alk.heldBy}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { onUpdateAlkerClick(alk) },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color(0xFFEADDFF), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Update alker condition",
                                        tint = Color(0xFF21005D),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Quick Statistics Summary Grid
        Text(
            text = "Ringkasan Lapangan Hari Ini",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val totalHadir = absensiList.filter { it.status == "Hadir" }.distinctBy { it.nakerId }.size
            StatMiniCard(
                modifier = Modifier.weight(1f),
                title = "Naker Hadir",
                value = "$totalHadir / ${nakers.size}",
                icon = Icons.Outlined.People,
                tint = Color(0xFF2E7D32),
                backgroundColor = Color(0xFFE8F5E9),
                onClick = { onNavigateTo(Screen.Absensi) }
            )

            val lowStockCount = materials.filter { it.remainingStock < 20 }.size
            StatMiniCard(
                modifier = Modifier.weight(1f),
                title = "Material Kritis",
                value = "$lowStockCount Low",
                icon = Icons.Outlined.Inventory,
                tint = Color(0xFFC62828),
                backgroundColor = Color(0xFFFFEBEE),
                onClick = { onNavigateTo(Screen.Material) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val damagedAlker = alkers.filter { it.condition == "Rusak Ringan" || it.condition == "Rusak Berat" }.size
            StatMiniCard(
                modifier = Modifier.weight(1f),
                title = "Alker Bermasalah",
                value = "$damagedAlker Unit",
                icon = Icons.Outlined.Build,
                tint = Color(0xFFEF6C00),
                backgroundColor = Color(0xFFFFF3E0),
                onClick = { onNavigateTo(Screen.Alker) }
            )

            StatMiniCard(
                modifier = Modifier.weight(1f),
                title = "Total Alat",
                value = "${alkers.size} Unit",
                icon = Icons.Outlined.Devices,
                tint = Color(0xFF1565C0),
                backgroundColor = Color(0xFFE3F2FD),
                onClick = { onNavigateTo(Screen.Alker) }
            )
        }

        // Timeline of recent activities
        Text(
            text = "Log Aktivitas Terkini Lapangan",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (absensiList.isEmpty() && materials.isEmpty() && alkers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Belum ada rekaman log/aktivitas kerja.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    val logs = mutableListOf<TimelineItem>()
                    
                    // Put attendance into timeline log
                    absensiList.take(3).forEach { abs ->
                        logs.add(
                            TimelineItem(
                                title = "${abs.nakerName} melapor ${abs.type} (${abs.status})",
                                subtitle = "Lokasi: ${abs.location}\nCatatan: ${abs.notes}",
                                date = abs.date,
                                time = abs.time,
                                icon = Icons.Default.AssignmentInd,
                                tint = when(abs.status) {
                                    "Hadir" -> Color(0xFF2E7D32)
                                    "Sakit" -> Color(0xFFED6C02)
                                    "Izin" -> Color(0xFF0288D1)
                                    else -> Color(0xFF7B1FA2)
                                }
                            )
                        )
                    }

                    // Put material log (simulated derived from update time)
                    materials.sortedByDescending { it.updateTime }.take(2).forEach { mat ->
                        logs.add(
                            TimelineItem(
                                title = "Update Stok: ${mat.name}",
                                subtitle = "Sisa: ${mat.remainingStock} ${mat.unit} - Diupdate oleh: ${mat.updatedBy}\nNotes: ${mat.notes}",
                                date = "Today",
                                time = "Stok Aktif",
                                icon = Icons.Default.Inventory,
                                tint = if (mat.remainingStock < 20) Color(0xFFC62828) else Color(0xFF00796B)
                            )
                        )
                    }

                    // Put tools tracker
                    alkers.sortedByDescending { it.updateTime }.take(2).forEach { alk ->
                        logs.add(
                            TimelineItem(
                                title = "Update Alker: ${alk.name}",
                                subtitle = "Kondisi: ${alk.condition} - Dipegang: ${alk.heldBy}\nKeluhan: ${alk.notes}",
                                date = "Today",
                                time = "Alker Stat",
                                icon = Icons.Default.Build,
                                tint = when (alk.condition) {
                                    "Sangat Baik" -> Color(0xFF2E7D32)
                                    "Baik" -> Color(0xFF4CAF50)
                                    "Rusak Ringan" -> Color(0xFFEF6C00)
                                    else -> Color(0xFFC62828)
                                }
                            )
                        )
                    }

                    // Display sorted timeline
                    logs.take(5).forEachIndexed { index, item ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(end = 12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(item.tint.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = null,
                                        tint = item.tint,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                if (index < logs.take(5).size - 1) {
                                    Spacer(
                                        modifier = Modifier
                                            .width(2.dp)
                                            .height(35.dp)
                                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1.0f)) {
                                Text(
                                    text = item.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = item.subtitle,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 16.sp,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                                Text(
                                    text = "${item.date} • ${item.time}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Light,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class TimelineItem(
    val title: String,
    val subtitle: String,
    val date: String,
    val time: String,
    val icon: ImageVector,
    val tint: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatMiniCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    tint: Color,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(backgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}


// --------------------------------------------------------------------------------------------------
// 2. ABSENSI SCREEN (ATTENDANCE)
// --------------------------------------------------------------------------------------------------
@Composable
fun AbsensiScreen(
    nakers: List<Naker>,
    absensiList: List<Absensi>,
    onAddAbsensiClick: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredAbsensi = absensiList.filter {
        it.nakerName.contains(searchQuery, ignoreCase = true) ||
        it.status.contains(searchQuery, ignoreCase = true) ||
        it.type.contains(searchQuery, ignoreCase = true)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Kehadiran Tenaga Kerja",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Daftar Absen Check In & Check Out Lapangan",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                shape = RoundedCornerShape(12.dp),
                placeholder = { Text("Cari nama naker, tipe, status...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors()
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredAbsensi.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.0f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.AssignmentLate,
                            contentDescription = "Empty",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Belum ada data absensi tercatat.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1.0f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredAbsensi) { abs ->
                        AbsensiCard(abs)
                    }
                }
            }
        }

        // FAB to record attendance
        FloatingActionButton(
            onClick = onAddAbsensiClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Icon(Icons.Default.Add, contentDescription = "Catat Absen")
        }
    }
}

@Composable
fun AbsensiCard(abs: Absensi) {
    val statusColor = when(abs.status) {
        "Hadir" -> Color(0xFF2E7D32)
        "Sakit" -> Color(0xFFEF6C00)
        "Izin" -> Color(0xFF0288D1)
        "Cuti" -> Color(0xFF7B1FA2)
        else -> MaterialTheme.colorScheme.outline
    }

    val statusBgColor = when(abs.status) {
        "Hadir" -> Color(0xFFE8F5E9)
        "Sakit" -> Color(0xFFFFF3E0)
        "Izin" -> Color(0xFFE3F2FD)
        "Cuti" -> Color(0xFFF3E5F5)
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    }

    val typeColor = if (abs.type == "Check In") {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }

    val typeBg = if (abs.type == "Check In") {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = abs.nakerName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Type Badge (Check In / Check Out)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(typeBg)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = abs.type,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = typeColor
                        )
                    }

                    // Status Badge (Hadir, Sakit, Izin, Cuti)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(statusBgColor)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = abs.status,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${abs.date} • ${abs.time} WIB",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp).padding(top = 1.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = abs.location,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (abs.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Catatan: ${abs.notes}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}


// --------------------------------------------------------------------------------------------------
// 3. MATERIAL SCREEN (STOCK REPORTING)
// --------------------------------------------------------------------------------------------------
@Composable
fun MaterialScreen(
    materials: List<Material>,
    nakers: List<Naker>,
    onAddMaterialClick: () -> Unit,
    onUpdateStockClick: (Material) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredMaterials = materials.filter {
        it.name.contains(searchQuery, ignoreCase = true)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                text = "Laporan Sisa Material",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Gunakan tombol update stok sisa pada kartu komit pekerjaan.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                shape = RoundedCornerShape(12.dp),
                placeholder = { Text("Cari material...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredMaterials.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Inventory,
                            contentDescription = "Empty stock",
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Belum ada produk/material terdaftar.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredMaterials) { mat ->
                        MaterialItemCard(
                            material = mat,
                            onUpdateClick = { onUpdateStockClick(mat) }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onAddMaterialClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Icon(Icons.Default.Add, contentDescription = "Tambah Material")
        }
    }
}

@Composable
fun MaterialItemCard(
    material: Material,
    onUpdateClick: () -> Unit
) {
    val isCritical = material.remainingStock < 20.0
    val progressColor = if (isCritical) Color(0xFFC62828) else Color(0xFF2E7D32)
    val progressBgColor = if (isCritical) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (isCritical) 1.5.dp else 1.dp,
            color = if (isCritical) Color(0xFFEF5350).copy(alpha = 0.8f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = material.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(progressBgColor)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${material.remainingStock} ${material.unit}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = progressColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stock progress indicator
            val progressPercentage = (material.remainingStock / 200.0).coerceIn(0.1, 1.0).toFloat()
            LinearProgressIndicator(
                progress = progressPercentage,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(10.dp)),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            if (isCritical) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFC62828),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Stok Kritis! Harap laporkan re-stock sesegera mungkin.",
                        color = Color(0xFFC62828),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Diperbarui Oleh: ${material.updatedBy}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    if (material.notes.isNotBlank()) {
                        Text(
                            text = "Keterangan: ${material.notes}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Button(
                    onClick = onUpdateClick,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Update stok",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Update", fontSize = 12.sp)
                }
            }
        }
    }
}


// --------------------------------------------------------------------------------------------------
// 4. ALKER SCREEN (WORKING UTILS INSPECTION)
// --------------------------------------------------------------------------------------------------
@Composable
fun AlkerScreen(
    alkers: List<Alker>,
    nakers: List<Naker>,
    onAddAlkerClick: () -> Unit,
    onUpdateAlkerClick: (Alker) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredAlkers = alkers.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
        it.code.contains(searchQuery, ignoreCase = true) ||
        it.heldBy.contains(searchQuery, ignoreCase = true)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                text = "Inspeksi Alat Kerja (Alker)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Pantau pemegang dan kondisi fisik alker naker di lapangan.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                shape = RoundedCornerShape(12.dp),
                placeholder = { Text("Cari alker, kode, pemegang...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredAlkers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Handyman,
                            contentDescription = "Empty tools",
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Belum ada alat kerja terdaftar.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredAlkers) { al ->
                        AlkerItemCard(
                            alker = al,
                            onUpdateClick = { onUpdateAlkerClick(al) }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onAddAlkerClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Icon(Icons.Default.Add, contentDescription = "Tambah Alker")
        }
    }
}

@Composable
fun AlkerItemCard(
    alker: Alker,
    onUpdateClick: () -> Unit
) {
    val conditionColor = when(alker.condition) {
        "Sangat Baik" -> Color(0xFF1B5E20)
        "Baik" -> Color(0xFF2E7D32)
        "Rusak Ringan" -> Color(0xFFEF6C00)
        "Rusak Berat" -> Color(0xFFC62828)
        else -> MaterialTheme.colorScheme.outline
    }

    val conditionBgColor = when(alker.condition) {
        "Sangat Baik" -> Color(0xFFE8F5E9)
        "Baik" -> Color(0xFFE8F5E9)
        "Rusak Ringan" -> Color(0xFFFFF3E0)
        "Rusak Berat" -> Color(0xFFFFEBEE)
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = alker.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Kode / SN: ${alker.code}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(conditionBgColor)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = alker.condition,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = conditionColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Badge,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Pemegang: ${alker.heldBy}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (alker.notes.isNotBlank()) {
                        Text(
                            text = "Catatan Kondisi: ${alker.notes}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Button(
                    onClick = onUpdateClick,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Tune",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Kondisi", fontSize = 12.sp)
                }
            }
        }
    }
}


// --------------------------------------------------------------------------------------------------
// 5. NAKER SCREEN (TEAM DIRECTORY)
// --------------------------------------------------------------------------------------------------
@Composable
fun NakerScreen(
    nakers: List<Naker>,
    onlineContacts: List<OnlineContact>,
    isSearchLoading: Boolean,
    onSearchOnline: (String) -> Unit,
    onRegisterOnline: (String, String, String, String) -> Unit,
    onAddOnlineToLocal: (OnlineContact) -> Unit,
    onAddNakerClick: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Lokal, 1: Online (Cloud)
    var searchQuery by remember { mutableStateOf("") }
    var onlineSearchQuery by remember { mutableStateOf("") }

    // State for cloud registration dialog
    var showCloudRegDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Screen Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Daftar Tenaga Kerja (Naker)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (selectedTab == 0) "Kelola staff / tim naker lokal di lapangan." else "Cari kontak & tim naker terdaftar secara nasional (Online).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tabs layout switching
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Tim Lokal", fontWeight = FontWeight.SemiBold)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Cari Cloud", fontWeight = FontWeight.SemiBold)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTab == 0) {
                // LOCAL TIM TAB
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    shape = RoundedCornerShape(12.dp),
                    placeholder = { Text("Cari nama atau peran lokal...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Berpindah")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                val filteredNakers = nakers.filter {
                    it.name.contains(searchQuery, ignoreCase = true) ||
                    it.role.contains(searchQuery, ignoreCase = true)
                }

                if (filteredNakers.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = "Empty Team",
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (searchQuery.isEmpty()) "Belum ada naker terdaftar." else "Tidak ada naker lokal cocok.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredNakers) { nk ->
                            NakerCard(naker = nk)
                        }
                    }
                }
            } else {
                // ONLINE CLOUD TAB
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = onlineSearchQuery,
                        onValueChange = { onlineSearchQuery = it },
                        shape = RoundedCornerShape(12.dp),
                        placeholder = { Text("Cari nama, peran, kota online...") },
                        leadingIcon = { Icon(Icons.Default.Public, contentDescription = "Online Directory") },
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { onSearchOnline(onlineSearchQuery) },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Text("Cari")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Suggestion/Prompt to register online
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = "Upload",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Mendaftarkan Profil Anda ke Cloud?",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Daftarkan nama Anda agar rekan kerja lain dapat mencari Anda.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        TextButton(
                            onClick = { showCloudRegDialog = true },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Daftar", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (isSearchLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(strokeWidth = 3.dp, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Menghubungkan ke pusat data online...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                } else {
                    if (onlineContacts.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudOff,
                                    contentDescription = "No results",
                                    modifier = Modifier.size(56.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Ketik kata kunci dan klik Cari untuk menjelajahi database kontak nasional.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                TextButton(
                                    onClick = { onSearchOnline("") }
                                ) {
                                    Text("Lihat Semua Kontak Rekomendasi 🌐")
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(onlineContacts) { contact ->
                                val isAlreadyAdded = nakers.any { it.phone == contact.phone || (it.name.equals(contact.name, ignoreCase = true) && it.role.equals(contact.role, ignoreCase = true)) }
                                OnlineNakerCard(
                                    contact = contact,
                                    isAlreadyAdded = isAlreadyAdded,
                                    onAddToContacts = { onAddOnlineToLocal(contact) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Extended Floating Action Button - available prominently on the frontend UI
        ExtendedFloatingActionButton(
            onClick = onAddNakerClick,
            icon = { Icon(Icons.Default.Add, contentDescription = "Daftar Baru Icon") },
            text = { Text("Daftar Baru 🌐", fontWeight = FontWeight.Bold) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    }

    // ONLINE CLOUD REGISTRATION DIALOG
    if (showCloudRegDialog) {
        var cloudName by remember { mutableStateOf("") }
        var cloudRole by remember { mutableStateOf("") }
        var cloudPhone by remember { mutableStateOf("") }
        var cloudLoc by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showCloudRegDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Daftar Direktori Cloud",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Profil Anda akan didistribusikan secara online ke jaringan database SiNaker nasional.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = cloudName,
                        onValueChange = { cloudName = it },
                        label = { Text("Nama Lengkap") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = cloudRole,
                        onValueChange = { cloudRole = it },
                        label = { Text("Peran / Skill FO") },
                        placeholder = { Text("Contoh: Teknisi Splicing Utama") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = cloudPhone,
                        onValueChange = { cloudPhone = it },
                        label = { Text("No. Whatsapp / Telp") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = cloudLoc,
                        onValueChange = { cloudLoc = it },
                        label = { Text("Kota Penugasan") },
                        placeholder = { Text("Contoh: Bandung, Jawa Barat") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showCloudRegDialog = false }) {
                            Text("Batal")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (cloudName.isNotBlank() && cloudRole.isNotBlank() && cloudPhone.isNotBlank() && cloudLoc.isNotBlank()) {
                                    onRegisterOnline(cloudName, cloudRole, cloudPhone, cloudLoc)
                                    showCloudRegDialog = false
                                }
                            }
                        ) {
                            Text("Daftar Sekarang")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OnlineNakerCard(
    contact: OnlineContact,
    isAlreadyAdded: Boolean,
    onAddToContacts: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circle with cloud icon as avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Cloud,
                    contentDescription = "Cloud Icon",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1.0f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = contact.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    // Online Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "CLOUD",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    text = contact.role,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Area: ${contact.location}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (contact.phone.isNotBlank()) {
                    Text(
                        text = "Telp: ${contact.phone}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // Quick actions
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
                // Phone Dial action
                if (contact.phone.isNotBlank()) {
                    IconButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:${contact.phone}")
                            }
                            context.startActivity(intent)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Panggil",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Add to contacts action
                if (isAlreadyAdded) {
                    IconButton(onClick = {}, enabled = false) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Sudah ditambahkan",
                            tint = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                } else {
                    IconButton(onClick = onAddToContacts) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "Tambahkan ke Lokal",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun NakerCard(naker: Naker) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Letter avatar
            val initial = naker.name.take(1).uppercase()
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1.0f)) {
                Text(
                    text = naker.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = naker.role,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                if (naker.phone.isNotBlank()) {
                    Text(
                        text = "Telp: ${naker.phone}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (naker.phone.isNotBlank()) {
                IconButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:${naker.phone}")
                        }
                        context.startActivity(intent)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Panggil",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}


// --------------------------------------------------------------------------------------------------
// REUSABLE POPUP DIALOG FORMS
// --------------------------------------------------------------------------------------------------

@Composable
fun AddNakerDialog(
    onDismiss: () -> Unit,
    onConfirmWithCloud: (String, String, String, String, Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("Bandung, Jawa Barat") }
    var uploadToCloud by remember { mutableStateOf(true) }

    val rolesSuggestions = listOf("Teknisi Splicing", "Team Leader FO", "Helper Pasang Baru", "Admin Logistik/Gudang")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Daftar Baru (Lokal & Cloud)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Lengkapi profil Anda / Naker rekan kerja untuk langsung terdaftar di sistem lokal dan cloud database.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Lengkap") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = role,
                    onValueChange = { role = it },
                    label = { Text("Peran / Jabatan") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick role selectors
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    rolesSuggestions.forEach { suggestion ->
                        InputChip(
                            selected = role == suggestion,
                            onClick = { role = suggestion },
                            label = { Text(suggestion, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("No. Telepon / WA") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Kota Penugasan") },
                    placeholder = { Text("Contoh: Bandung, Jawa Barat") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Instant Cloud toggler
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Hubungkan langsung ke Cloud (Online)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Otomatis masuk ke database pencarian nasional agar rekan lain dapat menemukan profil Anda.",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Switch(
                        checked = uploadToCloud,
                        onCheckedChange = { uploadToCloud = it }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal")
                    }
                    Button(
                        onClick = {
                            if (name.isNotBlank() && role.isNotBlank()) {
                                onConfirmWithCloud(name, role, phone, location, uploadToCloud)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(if (uploadToCloud) "Daftar & Sambung Cloud" else "Simpan Lokal Saja")
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAbsensiDialog(
    nakers: List<Naker>,
    onDismiss: () -> Unit,
    onConfirm: (Naker, String, String, String, String) -> Unit
) {
    var selectedNakerIndex by remember { mutableStateOf(0) }
    var selectedStatus by remember { mutableStateOf("Hadir") }
    var selectedType by remember { mutableStateOf("Check In") }
    var location by remember { mutableStateOf("-6.2088, 106.8456 (Gudang Jakarta)") }
    var notes by remember { mutableStateOf("") }

    var expandedNaker by remember { mutableStateOf(false) }

    val statusOptions = listOf("Hadir", "Sakit", "Izin", "Cuti")
    val typeOptions = listOf("Check In", "Check Out")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Lapor Absensi Naker",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Worker dropdown selector
                ExposedDropdownMenuBox(
                    expanded = expandedNaker,
                    onExpandedChange = { expandedNaker = !expandedNaker }
                ) {
                    OutlinedTextField(
                        value = nakers[selectedNakerIndex].name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Pilih Tenaga Kerja") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedNaker) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedNaker,
                        onDismissRequest = { expandedNaker = false }
                    ) {
                        nakers.forEachIndexed { idx, nk ->
                            DropdownMenuItem(
                                text = { Text("${nk.name} (${nk.role})") },
                                onClick = {
                                    selectedNakerIndex = idx
                                    expandedNaker = false
                                }
                            )
                        }
                    }
                }

                // Check Type Selector
                Text("Tipe Pelaporan:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    typeOptions.forEach { option ->
                        val isSel = selectedType == option
                        Button(
                            onClick = { selectedType = option },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text(option)
                        }
                    }
                }

                // Attendance Status Selector
                Text("Status Kehadiran:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    statusOptions.forEach { status ->
                        val isSel = selectedStatus == status
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSel) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(
                                        alpha = 0.5f
                                    )
                                )
                                .border(
                                    BorderStroke(
                                        1.dp,
                                        if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent
                                    ),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedStatus = status }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = status,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Lokasi (Simulasi GPS)") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Outlined.LocationOn, contentDescription = null) }
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan Lapangan / Keluhan") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal")
                    }
                    Button(
                        onClick = {
                            onConfirm(
                                nakers[selectedNakerIndex],
                                selectedStatus,
                                selectedType,
                                location,
                                notes
                            )
                        }
                    ) {
                        Text("Simpan Log")
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMaterialDialog(
    nakers: List<Naker>,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var initialStockStr by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("meter") }
    var selectedNakerIndex by remember { mutableStateOf(0) }
    var notes by remember { mutableStateOf("") }

    var expandedNaker by remember { mutableStateOf(false) }
    val units = listOf("meter", "buah", "saset", "set", "pack", "lonjor")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Daftarkan Material Baru",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Material") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = initialStockStr,
                        onValueChange = { initialStockStr = it },
                        label = { Text("Stok Sisa Awal") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Satuan") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Quick unit suggestions
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    units.forEach { suggestion ->
                        InputChip(
                            selected = unit == suggestion,
                            onClick = { unit = suggestion },
                            label = { Text(suggestion, fontSize = 11.sp) }
                        )
                    }
                }

                // Operator Dropdown Selector
                if (nakers.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = expandedNaker,
                        onExpandedChange = { expandedNaker = !expandedNaker }
                    ) {
                        OutlinedTextField(
                            value = nakers[selectedNakerIndex].name,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Petugas Logistik") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedNaker) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedNaker,
                            onDismissRequest = { expandedNaker = false }
                        ) {
                            nakers.forEachIndexed { idx, nk ->
                                DropdownMenuItem(
                                    text = { Text(nk.name) },
                                    onClick = {
                                        selectedNakerIndex = idx
                                        expandedNaker = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan / Keterangan Gudang") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal")
                    }
                    Button(
                        onClick = {
                            val stock = initialStockStr.toDoubleOrNull() ?: 0.0
                            val updaterName = if (nakers.isNotEmpty()) nakers[selectedNakerIndex].name else "Admin"
                            onConfirm(name, stock, unit, updaterName, notes)
                        }
                    ) {
                        Text("Simpan")
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateStockDialog(
    material: Material,
    nakers: List<Naker>,
    onDismiss: () -> Unit,
    onConfirm: (Int, Double, String, String) -> Unit
) {
    var stockInput by remember { mutableStateOf(material.remainingStock.toString()) }
    var selectedNakerIndex by remember { mutableStateOf(0) }
    var notes by remember { mutableStateOf("") }

    var expandedNaker by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Update Sisa Stok Material",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = material.name,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyLarge
                )

                OutlinedTextField(
                    value = stockInput,
                    onValueChange = { stockInput = it },
                    label = { Text("Sisa Stok Saat Ini (${material.unit})") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick increment / decrement buttons helper
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val current = stockInput.toDoubleOrNull() ?: 0.0
                            if (current >= 10.0) {
                                stockInput = (current - 10.0).toString()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("-10")
                    }

                    Button(
                        onClick = {
                            val current = stockInput.toDoubleOrNull() ?: 0.0
                            stockInput = (current + 10.0).toString()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+10")
                    }
                }

                // Operator Dropdown Selector
                if (nakers.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = expandedNaker,
                        onExpandedChange = { expandedNaker = !expandedNaker }
                    ) {
                        OutlinedTextField(
                            value = nakers[selectedNakerIndex].name,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Petugas Yang Melaporkan") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedNaker) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedNaker,
                            onDismissRequest = { expandedNaker = false }
                        ) {
                            nakers.forEachIndexed { idx, nk ->
                                DropdownMenuItem(
                                    text = { Text(nk.name) },
                                    onClick = {
                                        selectedNakerIndex = idx
                                        expandedNaker = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan / Alasan Perubahan") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal")
                    }
                    Button(
                        onClick = {
                            val newStockVal = stockInput.toDoubleOrNull() ?: material.remainingStock
                            val updaterName = if (nakers.isNotEmpty()) nakers[selectedNakerIndex].name else "Sistem"
                            onConfirm(material.id, newStockVal, updaterName, notes)
                        }
                    ) {
                        Text("Update Stok")
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAlkerDialog(
    nakers: List<Naker>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var selectedCondition by remember { mutableStateOf("Baik") }
    var selectedNakerIndex by remember { mutableStateOf(0) }
    var notes by remember { mutableStateOf("") }

    var expandedNaker by remember { mutableStateOf(false) }

    val conditions = listOf("Sangat Baik", "Baik", "Rusak Ringan", "Rusak Berat")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Daftarkan Alat Kerja (Alker)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Alat Kerja") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Kode Unit / Serial Number") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Kondisi Fisik:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    conditions.forEach { cond ->
                        FilterChip(
                            selected = selectedCondition == cond,
                            onClick = { selectedCondition = cond },
                            label = { Text(cond) }
                        )
                    }
                }

                // Operator Dropdown Selector
                if (nakers.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = expandedNaker,
                        onExpandedChange = { expandedNaker = !expandedNaker }
                    ) {
                        OutlinedTextField(
                            value = nakers[selectedNakerIndex].name,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Penanggung Jawab / Pemegang") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedNaker) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedNaker,
                            onDismissRequest = { expandedNaker = false }
                        ) {
                            nakers.forEachIndexed { idx, nk ->
                                DropdownMenuItem(
                                    text = { Text("${nk.name} (${nk.role})") },
                                    onClick = {
                                        selectedNakerIndex = idx
                                        expandedNaker = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan Detail / Keluhan") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal")
                    }
                    Button(
                        onClick = {
                            val holder = if (nakers.isNotEmpty()) nakers[selectedNakerIndex].name else "Belum diserahkan"
                            onConfirm(name, code, selectedCondition, holder, notes)
                        }
                    ) {
                        Text("Simpan")
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateAlkerConditionDialog(
    alker: Alker,
    nakers: List<Naker>,
    onDismiss: () -> Unit,
    onConfirm: (Int, String, String, String) -> Unit
) {
    var selectedCondition by remember { mutableStateOf(alker.condition) }
    var selectedNakerIndex by remember { mutableStateOf(0) }
    var notes by remember { mutableStateOf(alker.notes) }

    var expandedNaker by remember { mutableStateOf(false) }

    val conditions = listOf("Sangat Baik", "Baik", "Rusak Ringan", "Rusak Berat")

    // Find if current holder exists in nakers to select index
    LaunchedEffect(alker) {
        val idx = nakers.indexOfFirst { it.name == alker.heldBy }
        if (idx != -1) {
            selectedNakerIndex = idx
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Update Kondisi & Pemegang",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "${alker.name} (${alker.code})",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyLarge
                )

                Text("Kondisi Saat Ini:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    conditions.forEach { cond ->
                        FilterChip(
                            selected = selectedCondition == cond,
                            onClick = { selectedCondition = cond },
                            label = { Text(cond) }
                        )
                    }
                }

                // Operator Dropdown Selector (Custodian handover)
                if (nakers.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = expandedNaker,
                        onExpandedChange = { expandedNaker = !expandedNaker }
                    ) {
                        OutlinedTextField(
                            value = nakers[selectedNakerIndex].name,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Serahkan / Dipegang Oleh") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedNaker) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedNaker,
                            onDismissRequest = { expandedNaker = false }
                        ) {
                            nakers.forEachIndexed { idx, nk ->
                                DropdownMenuItem(
                                    text = { Text("${nk.name} (${nk.role})") },
                                    onClick = {
                                        selectedNakerIndex = idx
                                        expandedNaker = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan Perubahan / Laporan Kerusakan") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal")
                    }
                    Button(
                        onClick = {
                            val holder = if (nakers.isNotEmpty()) nakers[selectedNakerIndex].name else alker.heldBy
                            onConfirm(alker.id, selectedCondition, holder, notes)
                        }
                    ) {
                        Text("Simpan Update")
                    }
                }
            }
        }
    }
}
