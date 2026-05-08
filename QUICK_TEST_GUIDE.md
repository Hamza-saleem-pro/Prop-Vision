# Quick Test Guide - Property Management Features

## 🧪 Quick Manual Testing Steps

### Step 1: Test Adding New Property with Images
```
1. Launch app → HomeActivity
2. Click "+" (green button) in bottom navigation
3. Select property type: "House", "Apartment", "Villa", or "Flat"
4. Click "Add Photos" button
5. Select 2-3 photos from gallery
6. Fill in:
   - Sell Price: 500000
   - Rent Price: 2500
   - Bedrooms: 3 (click + button)
   - Bathrooms: 2
7. Click "Select Location" and pick a location
8. Click "Finish"
   → EXPECT: "Property listed successfully!" toast
   → EXPECT: Notification created with timestamp
```

### Step 2: Test Notifications
```
1. After adding property, go back to Home
   → EXPECT: Notification badge appears with number
   → EXPECT: Red badge shows unread count
2. Click notification bell icon
   → EXPECT: NotificationsActivity opens
   → EXPECT: See your listing with timestamp (e.g. "2:45 PM")
   → EXPECT: Message shows property type and location
```

### Step 3: Test Property Display in Listings
```
1. From Home, scroll down to "Explore Nearby Estates"
   → EXPECT: New property appears at top of list
   → EXPECT: Shows thumbnail, location, price
2. Click property card
   → SUCCESS TEST: Property Details page opens
```

### Step 4: Test Image Gallery (KEY TEST!)
```
1. Click property you just added
   → EXPECT: Main image loads with smooth animation
   → EXPECT: If you added 2+ images:
      → Below main image: Horizontal scroll gallery appears
      → Shows thumbnail previews (100x100dp)
      → Each thumbnail has rounded corners
2. Click a thumbnail
   → EXPECT: Main image updates to that image
   → EXPECT: Smooth fade transition
3. If property has only 1 image:
   → EXPECT: Gallery section is hidden (not shown)
```

### Step 5: Test "View all on Map"
```
1. Scroll down in Property Details
2. Click "View all on map" card
   → EXPECT: Google Maps opens with pin at exact location
   → EXPECT: Location name shown in map marker
   → EXPECT: If no location selected → Toast error message
```

### Step 6: Test Validation Error Cases
```
A. Try adding property WITHOUT images:
   → Click "Finish" without adding photos
   → EXPECT: "Please add at least one photo" toast
   → EXPECT: Form doesn't submit

B. Try adding property WITHOUT location:
   → Don't select location, try "Finish"
   → EXPECT: "Please select a location" toast
   → EXPECT: Form doesn't submit

C. Try adding property WITHOUT price:
   → Don't fill rent or sell price, try "Finish"
   → EXPECT: "Please enter at least one price" toast
   → EXPECT: Form doesn't submit
```

---

## ✅ Expected Behavior After All Fixes

| Feature | Expected Behavior | Status |
|---------|------------------|--------|
| **Add Photos** | Can add 1-12 photos from gallery | ✅ Works |
| **Image Display** | Photos show in property details with smooth load | ✅ FIXED |
| **Image Gallery** | If 2+ photos, scrollable thumbnail gallery appears | ✅ NEW |
| **Map Location** | Opens Google Maps with exact coordinates | ✅ FIXED |
| **Map Validation** | Shows error if no location selected | ✅ FIXED |
| **Notification** | Creates notification with timestamp when property added | ✅ FIXED |
| **Badge Update** | Notification badge shows on home screen | ✅ Works |
| **Input Validation** | Requires images, location, and price before submit | ✅ Enhanced |

---

## 🐛 Troubleshooting

### Images still not showing?
- Make sure you're selecting images from device gallery
- Check app has permission to access gallery
- Try with a different image

### Map not opening?
- Ensure location was selected during property creation
- Verify device has Google Maps or browser installed
- Check coordinates in property details (should not be 0.0)

### Notification not appearing?
- Make sure you clicked "Finish" after adding property
- Check notification badge in home screen top right
- Verify you added at least one image

### Thumbnail gallery not visible?
- Gallery only appears if property has 2+ images
- Scroll down to see gallery below main image
- Try clicking thumbnail if visible

---

## 📊 Performance Notes

- **Image Loading**: Uses Coil library with lazy loading
- **Gallery**: Swipe-scrollable, lightweight
- **Maps**: Opens via intent, uses device's default maps app
- **Notifications**: In-memory storage (resets on app close)

---

## 🔐 Validation Rules

| Field | Requirement | Message |
|-------|-------------|---------|
| Images | Min 1, Max 12 | "Please add at least one photo" |
| Type | Required | Defaults to "House" |
| Price | Min 1 (rent OR sell) | "Please enter at least one price" |
| Location | Required | "Please select a location" |
| Details | Beds/Baths/Kitchen | Counters start at 1 |

---

## 🎯 Build Info
- Gradle: 9.4.1
- SDK: 34 (Target)
- Min SDK: 24
- Kotlin Version: Latest
- Coil Version: Latest Image Loading

---

## 📱 Device Requirements
- Android 7.0+ (API 24)
- Target Android 14+ (API 34)
- 1GB+ RAM recommended
- Google Maps app (for map feature)

---

## ✨ Summary of All Fixes

✅ **Images Display**: Fixed image loading with fallback mechanism
✅ **Map Display**: Added coordinate validation 
✅ **Notifications**: Implemented timestamp & badge update
✅ **Image Gallery**: Added multi-image gallery feature (BONUS)
✅ **Validation**: Enhanced form validation before submission

**Build Status**: ✅ SUCCESSFUL - No errors, ready for testing!

