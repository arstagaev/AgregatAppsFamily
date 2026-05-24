package com.tagaev.trrcrm.push

/**
 * Trigger platform-specific post-login notification permission handling.
 * iOS implementation requests/checks permission and APNs registration.
 * Other targets are no-op.
 */
expect fun triggerPostLoginPushPermissionCheck()
