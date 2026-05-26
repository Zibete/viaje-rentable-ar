package com.zibete.driverassistant.location

data class DriverLocationResult(
    val status: DriverLocationStatus,
    val location: DriverLocation? = null,
    val errorMessage: String? = null
) {
    companion object {
        fun available(location: DriverLocation): DriverLocationResult {
            return if (location.hasValidCoordinates()) {
                DriverLocationResult(
                    status = DriverLocationStatus.AVAILABLE,
                    location = location
                )
            } else {
                unavailable()
            }
        }

        fun permissionMissing(): DriverLocationResult {
            return DriverLocationResult(status = DriverLocationStatus.PERMISSION_MISSING)
        }

        fun unavailable(): DriverLocationResult {
            return DriverLocationResult(status = DriverLocationStatus.LOCATION_UNAVAILABLE)
        }

        fun error(message: String?): DriverLocationResult {
            return DriverLocationResult(
                status = DriverLocationStatus.ERROR,
                errorMessage = message
            )
        }
    }
}
