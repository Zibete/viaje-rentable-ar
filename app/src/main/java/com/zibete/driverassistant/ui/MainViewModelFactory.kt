package com.zibete.driverassistant.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.zibete.driverassistant.config.DataStoreDriverConfigRepository
import com.zibete.driverassistant.geocoding.AndroidGeocodingRepository
import com.zibete.driverassistant.geocoding.CachedGeocodingRepository
import com.zibete.driverassistant.geocoding.HomeAddressConfigResolver

class MainViewModelFactory(
    private val appContext: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(
                configRepository = DataStoreDriverConfigRepository(context = appContext.applicationContext),
                homeAddressConfigResolver = HomeAddressConfigResolver(
                    geocodingRepository = CachedGeocodingRepository(
                        delegate = AndroidGeocodingRepository(appContext.applicationContext)
                    )
                )
            ) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
