package com.zibete.driverassistant.config

class LocalDriverConfigRepository(
    private val fallbackConfig: DriverConfig = DriverConfig.default()
) : DriverConfigRepository {
    override suspend fun getConfig(): DriverConfig = fallbackConfig
}

