package com.example.dacs3.shop

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.LocalMall
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.tasks.await

@SuppressLint("MissingPermission")
@Composable
fun SeasonalStoresMapScreen(
    season: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    // Có thể chỉnh bán kính ở đây.
    // 8000f = 8km, 12000f = 12km, 20000f = 20km.
    val radiusInMeters = 20000f

    var userLocation by remember {
        mutableStateOf<LatLng?>(null)
    }

    var isLoadingLocation by remember {
        mutableStateOf(true)
    }

    var locationError by remember {
        mutableStateOf<String?>(null)
    }

    var locationPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        locationPermissionGranted =
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (!locationPermissionGranted) {
            isLoadingLocation = false
            locationError = "Bạn chưa cấp quyền vị trí. App đang hiển thị shop theo khu vực Đà Nẵng."
        }
    }

    LaunchedEffect(Unit) {
        if (!locationPermissionGranted) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(locationPermissionGranted) {
        if (locationPermissionGranted) {
            isLoadingLocation = true
            locationError = null

            try {
                val fusedLocationClient =
                    LocationServices.getFusedLocationProviderClient(context)

                val location = fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    null
                ).await()

                if (location != null) {
                    userLocation = LatLng(location.latitude, location.longitude)
                    locationError = null
                } else {
                    locationError = "Không lấy được vị trí hiện tại. App đang hiển thị shop theo khu vực Đà Nẵng."
                }
            } catch (e: Exception) {
                locationError = "Không lấy được vị trí hiện tại. App đang hiển thị shop theo khu vực Đà Nẵng."
            } finally {
                isLoadingLocation = false
            }
        } else {
            isLoadingLocation = false
        }
    }

    val allDaNangStores = remember(season) {
        SeasonalStoreRepository.getStoresBySeason(
            context = context,
            season = season,
            provinceCode = "DN"
        )
    }

    val stores = remember(allDaNangStores, userLocation) {
        val currentLocation = userLocation

        if (currentLocation == null) {
            // Fallback: nếu chưa lấy được GPS, chỉ lấy tối đa 30 shop để map không nặng.
            allDaNangStores.take(30)
        } else {
            allDaNangStores.filter { store ->
                val result = FloatArray(1)

                Location.distanceBetween(
                    currentLocation.latitude,
                    currentLocation.longitude,
                    store.lat,
                    store.lng,
                    result
                )

                result[0] <= radiusInMeters
            }
        }
    }

    val daNangDefaultPosition = LatLng(16.0678, 108.2208)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            userLocation ?: stores.firstOrNull()?.let {
                LatLng(it.lat, it.lng)
            } ?: daNangDefaultPosition,
            13f
        )
    }

    LaunchedEffect(userLocation) {
        userLocation?.let { location ->
            cameraPositionState.position = CameraPosition.fromLatLngZoom(
                location,
                14f
            )
        }
    }

    var selectedStore by remember {
        mutableStateOf<SeasonalStoreDto?>(null)
    }

    LaunchedEffect(stores) {
        selectedStore = stores.firstOrNull()
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        SeasonalStoreMapTopBar(
            season = season,
            onBack = onBack,
            radiusInMeters = radiusInMeters,
            storeCount = stores.size
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    isMyLocationEnabled = locationPermissionGranted
                ),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = true,
                    mapToolbarEnabled = true
                )
            ) {
                userLocation?.let { location ->
                    Marker(
                        state = MarkerState(position = location),
                        title = "Vị trí của bạn",
                        snippet = "Đang tìm shop trong bán kính ${(radiusInMeters / 1000).toInt()} km"
                    )
                }

                stores.forEach { store ->
                    Marker(
                        state = MarkerState(
                            position = LatLng(store.lat, store.lng)
                        ),
                        title = store.name,
                        snippet = store.address,
                        onClick = {
                            selectedStore = store
                            false
                        }
                    )
                }
            }

            if (isLoadingLocation) {
                LocationStatusCard(
                    text = "Đang lấy vị trí hiện tại...",
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp)
                )
            }

            if (!isLoadingLocation && locationError != null) {
                LocationStatusCard(
                    text = locationError ?: "",
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp)
                )
            }

            if (!isLoadingLocation && stores.isEmpty()) {
                LocationStatusCard(
                    text = "Không tìm thấy shop nào trong bán kính ${(radiusInMeters / 1000).toInt()} km.",
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp)
                )
            }

            selectedStore?.let { store ->
                StorePreviewCard(
                    store = store,
                    season = season,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                )
            }
        }

        SeasonalStoreList(
            stores = stores,
            season = season,
            selectedStore = selectedStore,
            onStoreClick = { store ->
                selectedStore = store
                cameraPositionState.position = CameraPosition.fromLatLngZoom(
                    LatLng(store.lat, store.lng),
                    15f
                )
            }
        )
    }
}

@Composable
private fun SeasonalStoreMapTopBar(
    season: String,
    onBack: () -> Unit,
    radiusInMeters: Float,
    storeCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.Filled.ArrowBack,
                contentDescription = "Back"
            )
        }

        Column {
            Text(
                text = "Nearby Seasonal Stores",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Season: ${season.replaceFirstChar { it.uppercase() }} • ${storeCount} shop • ${(radiusInMeters / 1000).toInt()} km",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LocationStatusCard(
    text: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(14.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun StorePreviewCard(
    store: SeasonalStoreDto,
    season: String,
    modifier: Modifier = Modifier
) {
    val products = SeasonalStoreRepository.getProductsBySeason(store, season)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.LocalMall,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = store.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "${store.rating} ★ • ${store.distanceText}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = store.address,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Seasonal products",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(6.dp))

            if (products.isEmpty()) {
                Text(
                    text = "No seasonal products found.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                products.take(3).forEach { product ->
                    Text(
                        text = "• ${product.name} — ${product.priceRange}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun SeasonalStoreList(
    stores: List<SeasonalStoreDto>,
    season: String,
    selectedStore: SeasonalStoreDto?,
    onStoreClick: (SeasonalStoreDto) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (stores.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Text(
                        text = "Không có shop phù hợp để hiển thị.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(stores) { store ->
                val products = SeasonalStoreRepository.getProductsBySeason(store, season)
                val selected = selectedStore?.id == store.id

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onStoreClick(store) },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Text(
                            text = store.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = "${store.rating} ★ • ${store.distanceText}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = store.address,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        if (products.isNotEmpty()) {
                            Text(
                                text = products.take(2).joinToString(" • ") { it.name },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}