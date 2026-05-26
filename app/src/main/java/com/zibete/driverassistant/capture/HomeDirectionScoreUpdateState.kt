package com.zibete.driverassistant.capture

internal class HomeDirectionScoreUpdateState {
    private var activeKey: TripOfferSignature? = null

    fun markActive(key: TripOfferSignature) {
        activeKey = key
    }

    fun shouldApply(key: TripOfferSignature): Boolean {
        return key == activeKey
    }

    fun clear() {
        activeKey = null
    }
}
