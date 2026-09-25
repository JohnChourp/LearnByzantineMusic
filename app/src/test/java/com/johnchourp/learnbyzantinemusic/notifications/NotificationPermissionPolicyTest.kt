package com.johnchourp.learnbyzantinemusic.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The notifications prompt is asked at most once per install, only on Android 13+, and only while
 * the permission is missing (ClickUp `869f5x273`). A «no» is an answer: the recording proceeds, its
 * notification is hidden, and the app does not ask again.
 */
class NotificationPermissionPolicyTest {

    @Test
    fun itIsAskedOnlyWhereTheRuntimePermissionExists() {
        assertEquals("Android 13, TIRAMISU", 33, NotificationPermissionPolicy.FIRST_SDK)
        assertFalse(NotificationPermissionPolicy.shouldAsk(sdkInt = 32, granted = false, alreadyAsked = false))
        assertTrue(NotificationPermissionPolicy.shouldAsk(sdkInt = 33, granted = false, alreadyAsked = false))
        assertTrue(NotificationPermissionPolicy.shouldAsk(sdkInt = 34, granted = false, alreadyAsked = false))
    }

    @Test
    fun itIsNotAskedWhenAlreadyGranted() {
        assertFalse(NotificationPermissionPolicy.shouldAsk(sdkInt = 34, granted = true, alreadyAsked = false))
    }

    @Test
    fun itIsAskedOnceAndNeverAgain() {
        assertFalse(NotificationPermissionPolicy.shouldAsk(sdkInt = 34, granted = false, alreadyAsked = true))
    }
}
