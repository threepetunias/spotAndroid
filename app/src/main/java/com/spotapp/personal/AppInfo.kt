package com.spotapp.personal

// Values to replace before any store upload. Nothing here is a secret.
object AppInfo {
    const val DEVELOPER_NAME = "REPLACE_WITH_DEVELOPER_NAME"
    const val SUPPORT_EMAIL = "REPLACE_WITH_SUPPORT_EMAIL"
    const val PRIVACY_URL = "REPLACE_WITH_PRIVACY_POLICY_URL"
    const val TEND_PACKAGE = "com.tendapp.personal"

    fun isPlaceholder(v: String): Boolean = v.startsWith("REPLACE")

    // Only a plain https address is opened from here.
    fun isHttps(v: String): Boolean = v.startsWith("https://") && v.length > 8 && v.none { it.isWhitespace() }

    // One "@", no spaces, something on both sides.
    fun isEmail(v: String): Boolean {
        val at = v.indexOf('@')
        return at > 0 && at == v.lastIndexOf('@') && at < v.length - 1 && v.none { it.isWhitespace() }
    }
}
