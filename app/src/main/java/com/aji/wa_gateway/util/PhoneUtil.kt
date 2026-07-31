package com.aji.wa_gateway.util

object PhoneUtil {
    private const val DEFAULT_COUNTRY_CODE = "62"

    fun normalize(number: String): String {
        var digits = number.replace(Regex("[^0-9]"), "")
        if (digits.isEmpty()) return digits

        if (digits.startsWith("0")) {
            digits = DEFAULT_COUNTRY_CODE + digits.drop(1)
        } else if (digits.startsWith("+")) {
            digits = digits.drop(1)
        }
        return digits
    }
}
