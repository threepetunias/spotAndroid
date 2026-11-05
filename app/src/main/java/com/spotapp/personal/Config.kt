package com.spotapp.personal

// Values to replace before any store upload. Nothing here is a secret.
object Config {
    const val APP_VERSION = "1.0.0"
    const val DEVELOPER_NAME = "REPLACE_WITH_DEVELOPER_NAME"
    const val SUPPORT_EMAIL = "REPLACE_WITH_SUPPORT_EMAIL"
    const val PRIVACY_URL = "REPLACE_WITH_PRIVACY_POLICY_URL"
    // Tend's Android package is not confirmed. Check Tend's applicationId and change this to match.
    const val TEND_PACKAGE = "com.tendapp.personal"
    fun isPlaceholder(v: String) = v.startsWith("REPLACE")
}
