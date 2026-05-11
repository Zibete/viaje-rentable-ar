package com.zibete.driverassistant.config

interface DriverConfigRepository {
    suspend fun getConfig(): DriverConfig
}

