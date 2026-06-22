package com.example.dacs3.shop

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.tasks.await
import com.google.android.gms.maps.model.MapStyleOptions

@SuppressLint("MissingPermission")
@Composable
fun SeasonalStoresMapScreen(
    season: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    // ==========================================
    // CỤM 1: CẤU HÌNH CÁC GIÁ TRỊ CÓ THỂ SỬA (CONFIGURABLE VALUES)
    // - Bán kính quét cửa hàng (radiusInMeters). Mặc định là 20.000m (20km).
    // - Có thể tăng/giảm giá trị này để mở rộng hoặc thu hẹp phạm vi quét.
    // ==========================================
    val radiusInMeters = 20000f

    // Trạng thái tọa độ thiết bị (Latitude và Longitude)
    var userLocation by remember {
        mutableStateOf<LatLng?>(null)
    }

    var isLoadingLocation by remember {
        mutableStateOf(true)
    }

    var locationError by remember {
        mutableStateOf<String?>(null)
    }

    // ==========================================
    // CỤM 2: QUẢN LÝ QUYỀN TRUY CẬP VỊ TRÍ (LOCATION PERMISSIONS)
    // - Kiểm tra trạng thái cấp quyền truy cập vị trí chính xác (FINE_LOCATION) 
    //   hoặc tương đối (COARSE_LOCATION) của ứng dụng.
    // ==========================================
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

    // Trình khởi chạy yêu cầu cấp quyền vị trí
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        locationPermissionGranted =
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (!locationPermissionGranted) {
            isLoadingLocation = false
            locationError = "Location permission denied. Showing stores near Da Nang."
        }
    }

    // Tự động kích hoạt hộp thoại yêu cầu cấp quyền vị trí nếu chưa được cấp
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

    // ==========================================
    // CỤM 3: TRUY VẤN VỊ TRÍ THỰC TẾ CỦA THIẾT BỊ (GET CURRENT GPS LOCATION)
    // - Sử dụng FusedLocationProviderClient của Google Play Services.
    // - Yêu cầu độ chính xác cao (Priority.PRIORITY_HIGH_ACCURACY).
    // - Có thể sửa đổi độ chính xác hoặc cấu hình thời gian chờ (Timeout) tại đây.
    // ==========================================
    LaunchedEffect(locationPermissionGranted) {
        if (locationPermissionGranted) {
            isLoadingLocation = true
            locationError = null

            try {
                val fusedLocationClient =
                    LocationServices.getFusedLocationProviderClient(context)

                // Lấy vị trí GPS của thiết bị
                val location = fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    null
                ).await()

                if (location != null) {
                    userLocation = LatLng(location.latitude, location.longitude)
                    locationError = null
                } else {
                    locationError = "Could not get current location. Showing stores near Da Nang."
                }
            } catch (e: Exception) {
                locationError = "Could not get current location. Showing stores near Da Nang."
            } finally {
                isLoadingLocation = false
            }
        } else {
            isLoadingLocation = false
        }
    }

    // ==========================================
    // CỤM 4: TRUY VẤN DANH SÁCH CỬA HÀNG THEO MÙA (FETCH STORES FROM DATA SOURCE)
    // - Gọi hàm getStoresBySeason từ repository để lấy toàn bộ cửa hàng thuộc mùa hiện tại.
    // - Có thể sửa: "provinceCode" hiện tại cố định là "DN" (Đà Nẵng). 
    //   Có thể chuyển thành tham số động để mở rộng quy mô tỉnh thành khác.
    // ==========================================
    val allDaNangStores = remember(season) {
        SeasonalStoreRepository.getStoresBySeason(
            context = context,
            season = season,
            provinceCode = "DN"
        )
    }

    // ==========================================
    // CỤM 5: TÍNH KHOẢNG CÁCH & LỌC CỬA HÀNG TRONG BÁN KÍNH (RADIUS FILTERING)
    // - Sử dụng hàm Location.distanceBetween để tính khoảng cách đường chim bay 
    //   từ vị trí GPS thiết bị đến tọa độ từng cửa hàng.
    // - Lọc các cửa hàng có khoảng cách nhỏ hơn hoặc bằng bán kính thiết lập (radiusInMeters).
    // - Có thể sửa: Nếu userLocation = null (do tắt GPS hoặc chưa cấp quyền), 
    //   hệ thống sẽ lấy tối đa 30 cửa hàng mặc định làm phương án dự phòng.
    // ==========================================
    val stores = remember(allDaNangStores, userLocation) {
        val currentLocation = userLocation

        if (currentLocation == null) {
            // Fallback: nếu chưa lấy được GPS, chỉ lấy tối đa 30 shop để tránh làm quá tải bản đồ.
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

    // Tọa độ mặc định ở Trung tâm Đà Nẵng
    val daNangDefaultPosition = LatLng(16.0678, 108.2208)

    // ==========================================
    // CỤM 6: QUẢN LÝ KHUNG NHÌN BẢN ĐỒ (CAMERA POSITION MANAGEMENT)
    // - Khởi tạo vị trí Camera mặc định tập trung vào vị trí thiết bị, hoặc cửa hàng đầu tiên, hoặc tọa độ mặc định Đà Nẵng.
    // - Tự động di chuyển camera đến vị trí thiết bị khi định vị thành công.
    // - Có thể sửa: Độ phóng thu (zoom level) hiện tại được cấu hình là 13f (khi khởi tạo) và 14f (khi có vị trí).
    // ==========================================
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

    // Quản lý cửa hàng đang được chọn để hiển thị chi tiết
    var selectedStore by remember {
        mutableStateOf<SeasonalStoreDto?>(null)
    }

    LaunchedEffect(stores) {
        selectedStore = stores.firstOrNull()
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // ==========================================
        // CỤM 7: THANH TIÊU ĐỀ TRÊN CÙNG (TOP APP BAR)
        // - Hiển thị tên chức năng, mùa hiện tại, số lượng cửa hàng và bán kính tìm kiếm.
        // - Cung cấp nút quay lại giao diện trước đó.
        // ==========================================
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
            // ==========================================
            // CỤM 8: THÀNH PHẦN BẢN ĐỒ GOOGLE MAPS (GOOGLE MAPS COMPONENT)
            // - Hiển thị bản đồ, nút định vị vị trí hiện tại và toolbar chỉ đường.
            // - Đánh dấu (Marker) vị trí của thiết bị và vị trí từng cửa hàng.
            // - Phản hồi sự kiện nhấn chọn cửa hàng để cập nhật trạng thái hiển thị.
            // ==========================================
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    isMyLocationEnabled = locationPermissionGranted,

//                    mapStyleOptions = MapStyleOptions(
//                        """
//                        [
//                          {
//                            "elementType": "geometry",
//                            "stylers": [{ "color": "#242f3e" }]
//                          },
//                          {
//                            "elementType": "labels.text.fill",
//                            "stylers": [{ "color": "#746855" }]
//                          },
//                          {
//                            "elementType": "labels.text.stroke",
//                            "stylers": [{ "color": "#242f3e" }]
//                          },
//                          {
//                            "featureType": "administrative.locality",
//                            "elementType": "labels.text.fill",
//                            "stylers": [{ "color": "#d59563" }]
//                          },
//                          {
//                            "featureType": "road",
//                            "elementType": "geometry",
//                            "stylers": [{ "color": "#38414e" }]
//                          },
//                          {
//                            "featureType": "water",
//                            "elementType": "geometry",
//                            "stylers": [{ "color": "#17263c" }]
//                          }
//                        ]
//                        """.trimIndent()
//                    )
                ),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = true,
                    mapToolbarEnabled = true
                )
            ) {
                userLocation?.let { location ->
                    Marker(
                        state = rememberMarkerState(position = location),
                        title = "Your Location",
                        snippet = "Finding stores within ${(radiusInMeters / 1000).toInt()} km"
                    )
                }

                stores.forEach { store ->
                    Marker(
                        state = rememberMarkerState(
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

            // ==========================================
            // CỤM 9: THẺ TRẠNG THÁI TRÊN BẢN ĐỒ (STATUS CARDS ON MAP)
            // - Hiển thị các thông báo trạng thái: đang định vị, lỗi GPS, hoặc không tìm thấy cửa hàng.
            // ==========================================
            if (isLoadingLocation) {
                LocationStatusCard(
                    text = "Getting your current location...",
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
                    text = "No stores found within ${(radiusInMeters / 1000).toInt()} km.",
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp)
                )
            }

            // ==========================================
            // CỤM 10: THẺ XEM TRƯỚC CỬA HÀNG ĐANG CHỌN (STORE PREVIEW CARD)
            // - Hiển thị thông tin tóm tắt cửa hàng (đánh giá, khoảng cách, sản phẩm tiêu biểu)
            //   ở phía dưới bản đồ khi có cửa hàng được chọn.
            // ==========================================
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

        // ==========================================
        // CỤM 11: DANH SÁCH CỬA HÀNG CUỘN DƯỚI CHÂN TRANG (BOTTOM SCROLLABLE STORE LIST)
        // - Hiển thị toàn bộ danh sách cửa hàng tìm được dưới dạng danh sách cuộn dọc.
        // - Cho phép nhấn chọn cửa hàng để định tâm camera bản đồ vào tọa độ của cửa hàng đó.
        // ==========================================
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

// ==========================================
// THÀNH PHẦN HỖ TRỢ 1: THANH TIÊU ĐỀ TRÊN BẢN ĐỒ (TOP BAR)
// - Hiển thị tên chức năng, thông tin mùa, số lượng shop và bán kính tìm kiếm.
// - Hỗ trợ nút điều hướng quay lại.
// ==========================================
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

// ==========================================
// THÀNH PHẦN HỖ TRỢ 2: THẺ THÔNG BÁO TRẠNG THÁI (LOCATION STATUS CARD)
// - Hiển thị các đoạn văn bản trạng thái (đang định vị, thông tin lỗi, không tìm thấy shop).
// ==========================================
@Composable
private fun LocationStatusCard(
    text: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
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

// ==========================================
// THÀNH PHẦN HỖ TRỢ 3: THẺ XEM TRƯỚC CHI TIẾT CỬA HÀNG (STORE PREVIEW CARD)
// - Hiển thị chi tiết về tên shop, đánh giá sao, khoảng cách địa lý, địa chỉ cụ thể 
//   và danh sách các sản phẩm quần áo đặc trưng của mùa hiện tại tại shop đó.
// ==========================================
@Composable
private fun StorePreviewCard(
    store: SeasonalStoreDto,
    season: String,
    modifier: Modifier = Modifier
) {
    val products = SeasonalStoreRepository.getProductsBySeason(store, season)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.LocalMall,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = store.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${store.rating} ★",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Distance: ${store.distanceText}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = store.address,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Seasonal Products",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (products.isEmpty()) {
                Text(
                    text = "No seasonal products found.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                products.take(3).forEach { product ->
                    Text(
                        text = "• ${product.name}  —  ${product.priceRange}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// THÀNH PHẦN HỖ TRỢ 4: DANH SÁCH CỬA HÀNG CUỘN (SCROLLABLE LIST)
// - Hiển thị danh sách dọc chứa các shop gần vị trí.
// - Nhấp chọn vào mỗi dòng sẽ cập nhật trạng thái lựa chọn và di chuyển camera bản đồ đến đó.
// ==========================================
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
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                ) {
                    Text(
                        text = "No matching stores to display.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(stores, key = { it.id }) { store ->
                val products = SeasonalStoreRepository.getProductsBySeason(store, season)
                val selected = selectedStore?.id == store.id

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onStoreClick(store) },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                        }
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (selected) 4.dp else 1.dp
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = store.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${store.rating} ★",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Distance: ${store.distanceText}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
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
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}