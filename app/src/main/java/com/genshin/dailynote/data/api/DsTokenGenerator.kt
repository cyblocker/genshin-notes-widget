package com.genshin.dailynote.data.api

import java.security.MessageDigest
import kotlin.random.Random

object DsTokenGenerator {
    // Known salts for Hoyolab / Miyoushe mobile client v2 / v1
    private const val SALT_CN_PROD = "xV8vVuQm0SvdflgIc2miRYivBl1gqqAc"
    private const val SALT_OS_PROD = "okrZaF2m10v8t05k5x14v197g10011"

    /**
     * Generates standard dynamic secret DS token for miHoYo/HoYoverse APIs
     * Format: {timestamp},{random_string},{md5(salt=...&t=...&r=...)}
     */
    fun generateDs(isCn: Boolean = false): String {
        val salt = if (isCn) SALT_CN_PROD else SALT_OS_PROD
        val timestamp = System.currentTimeMillis() / 1000
        val randomStr = generateRandomString(6)

        val hashContent = "salt=$salt&t=$timestamp&r=$randomStr"
        val md5Hash = md5(hashContent)

        return "$timestamp,$randomStr,$md5Hash"
    }

    private fun generateRandomString(length: Int): String {
        val allowedChars = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        return (1..length)
            .map { allowedChars[Random.nextInt(allowedChars.size)] }
            .joinToString("")
    }

    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
