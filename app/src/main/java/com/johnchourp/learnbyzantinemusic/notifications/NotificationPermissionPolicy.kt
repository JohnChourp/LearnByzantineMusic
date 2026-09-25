package com.johnchourp.learnbyzantinemusic.notifications

/**
 * When to ask for the Android 13+ permission to post notifications: only where it exists, only while
 * it is missing, and only once per install — a «no» is an answer, not something to nag about
 * (ClickUp `869f5x273`). A «no» hides the notifications; it never stops what they belong to.
 */
object NotificationPermissionPolicy {
    /** Android 13 (TIRAMISU), where `POST_NOTIFICATIONS` became a runtime permission. */
    const val FIRST_SDK = 33

    fun shouldAsk(sdkInt: Int, granted: Boolean, alreadyAsked: Boolean): Boolean =
        sdkInt >= FIRST_SDK && !granted && !alreadyAsked
}
