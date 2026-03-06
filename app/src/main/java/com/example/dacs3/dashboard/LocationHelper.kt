package com.example.dacs3.dashboard

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

object LocationHelper {
    @SuppressLint("MissingPermission") // Bỏ qua cảnh báo vì ta sẽ check quyền ở UI trước khi gọi hàm này
    fun getCurrentLocation(
        context: Context,
        onLocationFetched: (lat: Double, lon: Double) -> Unit,
        onFailed: () -> Unit
    ) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        // Dùng PRIORITY_BALANCED_POWER_ACCURACY để lấy vị trí đủ tốt cho thời tiết mà không hao pin
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    onLocationFetched(location.latitude, location.longitude)
                } else {
                    onFailed()
                }
            }
            .addOnFailureListener {
                onFailed()
            }
    }
}