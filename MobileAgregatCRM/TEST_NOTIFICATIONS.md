# Testing Push Notifications Locally

This guide explains how to test push notifications and deep links locally using ADB.

## Quick Start

### Method 1: Using the Test Script (Easiest)

```bash
# Test Events screen without document ID
./test_deeplink.sh events

# Test Events screen with document ID
./test_deeplink.sh events "EVENT-12345"

# Test Work Orders with document ID
./test_deeplink.sh work_orders "WO-67890" "Work Order Alert" "Work Order requires attention"

# Test Cargo with document ID
./test_deeplink.sh cargo "CARGO-11111"

# Test Complaints with document ID
./test_deeplink.sh complaints "COMP-22222"

# Test Inner Orders with document ID
./test_deeplink.sh inner_orders "INNER-33333"
```

### Method 2: Direct ADB Commands

#### Test Notifications (Shows actual notification)

```bash
# Events without docId
adb shell am broadcast -a com.tagaev.trrcrm.TEST_NOTIFICATION \
  --es screen events \
  --es title "New Event" \
  --es body "You have a new event"

# Events with docId
adb shell am broadcast -a com.tagaev.trrcrm.TEST_NOTIFICATION \
  --es screen events \
  --es docId "EVENT-12345" \
  --es title "Event Update" \
  --es body "Event EVENT-12345 has been updated"

# Work Orders with docId
adb shell am broadcast -a com.tagaev.trrcrm.TEST_NOTIFICATION \
  --es screen work_orders \
  --es docId "WO-67890" \
  --es title "Work Order Alert" \
  --es body "Work Order WO-67890 requires attention"

# Cargo with docId
adb shell am broadcast -a com.tagaev.trrcrm.TEST_NOTIFICATION \
  --es screen cargo \
  --es docId "CARGO-11111" \
  --es title "Cargo Update" \
  --es body "Cargo CARGO-11111 status changed"

# Complaints with docId
adb shell am broadcast -a com.tagaev.trrcrm.TEST_NOTIFICATION \
  --es screen complaints \
  --es docId "COMP-22222" \
  --es title "New Complaint" \
  --es body "Complaint COMP-22222 has been filed"

# Inner Orders with docId
adb shell am broadcast -a com.tagaev.trrcrm.TEST_NOTIFICATION \
  --es screen inner_orders \
  --es docId "INNER-33333" \
  --es title "Inner Order Update" \
  --es body "Inner Order INNER-33333 needs review"
```

#### Direct Deep Link (Simulates notification click, no notification shown)

```bash
# Directly open Events screen
adb shell am start -n com.tagaev.trrcrm/.MainActivity --es screen events

# Directly open Events with document ID
adb shell am start -n com.tagaev.trrcrm/.MainActivity \
  --es screen events --es docId "EVENT-12345"

# Directly open Work Orders with document ID
adb shell am start -n com.tagaev.trrcrm/.MainActivity \
  --es screen work_orders --es docId "WO-67890"
```

## Supported Screen Names

- `events` - Events screen
- `work_orders` or `workorders` - Work Orders screen
- `cargo` - Cargo screen
- `complaints` - Complaints screen
- `inner_orders` or `innerorders` - Inner Orders screen

## How It Works

1. **TestNotificationReceiver** receives ADB broadcast
2. Creates notification with deep link data (screen + docId)
3. User clicks notification
4. **MainActivity** receives Intent with screen and docId
5. **RootComponent.onDeepLink()** is called
6. App navigates to target screen
7. Data is refreshed (`fullRefresh()`)
8. If docId provided, document is selected and detail panel shown

## Testing Checklist

- [ ] Notification appears in notification tray
- [ ] Clicking notification opens app
- [ ] App navigates to correct screen
- [ ] Data is refreshed (check logs for `fullRefresh()`)
- [ ] If docId provided, document is selected
- [ ] Detail panel shows selected document

## Troubleshooting

**Notification doesn't appear:**
- Check notification permissions are granted
- Verify app is installed and running
- Check ADB connection: `adb devices`

**Deep link doesn't work:**
- Verify screen name is correct (see Supported Screen Names)
- Check logcat for errors: `adb logcat | grep -i deeplink`
- Ensure app is not killed (keep it in foreground or background)

**Document not selected:**
- Verify docId matches an actual document ID in the list
- Check that `selectItemFromList()` is called (check logs)
- Ensure data refresh completes before selection
