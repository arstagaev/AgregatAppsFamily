#!/bin/bash
# Test script for deep link notifications
# Usage: ./test_deeplink.sh [screen] [docId] [title] [body]

ACTION="com.tagaev.trrcrm.TEST_NOTIFICATION"
SCREEN="${1:-events}"
DOC_ID="${2:-}"
TITLE="${3:-Test Notification}"
BODY="${4:-Testing deep link to $SCREEN}"

echo "Sending test notification..."
echo "Screen: $SCREEN"
echo "DocId: $DOC_ID"
echo "Title: $TITLE"
echo "Body: $BODY"
echo ""

if [ -z "$DOC_ID" ]; then
    adb shell am broadcast -a "$ACTION" \
        --es screen "$SCREEN" \
        --es title "$TITLE" \
        --es body "$BODY"
else
    adb shell am broadcast -a "$ACTION" \
        --es screen "$SCREEN" \
        --es docId "$DOC_ID" \
        --es title "$TITLE" \
        --es body "$BODY"
fi

echo ""
echo "Notification sent! Check your device."
