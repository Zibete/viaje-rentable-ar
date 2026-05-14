package com.zibete.driverassistant.config

data class DriverConfigFormState(
    val minArsPerKm: String = "",
    val minArsPerHour: String = "",
    val minNetProfit: String = "",
    val costPerKm: String = "",
    val costPerMinute: String = "",
    val maxPickupKm: String = "",
    val maxPickupMinutes: String = "",
    val reviewTolerancePercent: String = ""
) {
    fun update(
        field: DriverConfigFormField,
        value: String
    ): DriverConfigFormState {
        return when (field) {
            DriverConfigFormField.MIN_ARS_PER_KM -> copy(minArsPerKm = value)
            DriverConfigFormField.MIN_ARS_PER_HOUR -> copy(minArsPerHour = value)
            DriverConfigFormField.MIN_NET_PROFIT -> copy(minNetProfit = value)
            DriverConfigFormField.COST_PER_KM -> copy(costPerKm = value)
            DriverConfigFormField.COST_PER_MINUTE -> copy(costPerMinute = value)
            DriverConfigFormField.MAX_PICKUP_KM -> copy(maxPickupKm = value)
            DriverConfigFormField.MAX_PICKUP_MINUTES -> copy(maxPickupMinutes = value)
            DriverConfigFormField.REVIEW_TOLERANCE_PERCENT -> copy(reviewTolerancePercent = value)
        }
    }

    fun toDriverConfig(
        currentConfig: DriverConfig
    ): DriverConfigFormValidationResult {
        val minArsPerKm = parseRequiredNonNegative(minArsPerKm, "Minimo ${'$'}/km")
        val minArsPerHour = parseRequiredNonNegative(minArsPerHour, "Minimo ${'$'}/h")
        val minNetProfit = parseRequiredNonNegative(minNetProfit, "Ganancia minima")
        val costPerKm = parseRequiredNonNegative(costPerKm, "Costo por km")
        val costPerMinute = parseRequiredNonNegative(costPerMinute, "Costo por minuto")
        val maxPickupKm = parseRequiredNonNegative(maxPickupKm, "Pickup maximo km")
        val maxPickupMinutes = parseRequiredNonNegative(maxPickupMinutes, "Pickup maximo min")
        val reviewTolerancePercent = parseRequiredNonNegative(reviewTolerancePercent, "Tolerancia")

        val error = listOf(
            minArsPerKm,
            minArsPerHour,
            minNetProfit,
            costPerKm,
            costPerMinute,
            maxPickupKm,
            maxPickupMinutes,
            reviewTolerancePercent
        ).firstOrNull { it.errorMessage != null }?.errorMessage

        if (error != null) {
            return DriverConfigFormValidationResult.Invalid(error)
        }

        return DriverConfigFormValidationResult.Valid(
            currentConfig.copy(
                minArsPerKm = minArsPerKm.value ?: currentConfig.minArsPerKm,
                minArsPerHour = minArsPerHour.value ?: currentConfig.minArsPerHour,
                minNetProfit = minNetProfit.value ?: currentConfig.minNetProfit,
                costPerKm = costPerKm.value ?: currentConfig.costPerKm,
                costPerMinute = costPerMinute.value ?: currentConfig.costPerMinute,
                maxPickupKm = maxPickupKm.value ?: currentConfig.maxPickupKm,
                maxPickupMinutes = maxPickupMinutes.value ?: currentConfig.maxPickupMinutes,
                reviewTolerancePercent = reviewTolerancePercent.value ?: currentConfig.reviewTolerancePercent
            )
        )
    }

    private fun parseRequiredNonNegative(
        rawValue: String,
        label: String
    ): ParsedConfigValue {
        val normalized = rawValue.trim().replace(",", ".")
        if (normalized.isBlank()) {
            return ParsedConfigValue(errorMessage = "$label no puede estar vacio")
        }

        val value = normalized.toDoubleOrNull()
            ?: return ParsedConfigValue(errorMessage = "$label debe ser numerico")
        if (value < 0.0) {
            return ParsedConfigValue(errorMessage = "$label no puede ser negativo")
        }

        return ParsedConfigValue(value = value)
    }

    private data class ParsedConfigValue(
        val value: Double? = null,
        val errorMessage: String? = null
    )

    companion object {
        fun fromConfig(config: DriverConfig): DriverConfigFormState {
            return DriverConfigFormState(
                minArsPerKm = config.minArsPerKm.toInputText(),
                minArsPerHour = config.minArsPerHour.toInputText(),
                minNetProfit = config.minNetProfit.toInputText(),
                costPerKm = config.costPerKm.toInputText(),
                costPerMinute = config.costPerMinute.toInputText(),
                maxPickupKm = config.maxPickupKm.toInputText(),
                maxPickupMinutes = config.maxPickupMinutes.toInputText(),
                reviewTolerancePercent = config.reviewTolerancePercent.toInputText()
            )
        }

        private fun Double.toInputText(): String {
            return if (this % 1.0 == 0.0) {
                toLong().toString()
            } else {
                toString()
            }
        }
    }
}

sealed interface DriverConfigFormValidationResult {
    data class Valid(val config: DriverConfig) : DriverConfigFormValidationResult
    data class Invalid(val message: String) : DriverConfigFormValidationResult
}
