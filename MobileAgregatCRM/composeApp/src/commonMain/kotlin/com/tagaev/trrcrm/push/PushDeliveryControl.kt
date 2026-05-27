package com.tagaev.trrcrm.push

/**
 * Platform-specific push cleanup for logout state.
 * iOS implementation unregisters from APNs and clears notification center badge.
 * Android implementation is no-op.
 */
expect fun disablePushDeliveryForLoggedOutUser()

