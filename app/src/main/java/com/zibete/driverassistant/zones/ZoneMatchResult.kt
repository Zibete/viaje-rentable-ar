package com.zibete.driverassistant.zones

data class ZoneMatchResult(
    val rule: AvoidZoneRule,
    val matchedKeyword: String
) {
    val policy: AvoidZonePolicy = rule.policy
}

