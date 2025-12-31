# Deep Linking Support

Mars Rover Photos supports deep links to navigate directly to specific rovers and photos.

## Supported Deep Link Schemes

### Custom Scheme: `marsrover://`

#### Open Specific Rover
```
marsrover://rover/{roverId}
```

Examples:
- `marsrover://rover/5` - Open Curiosity rover
- `marsrover://rover/3` - Open Perseverance rover
- `marsrover://rover/6` - Open Opportunity rover
- `marsrover://rover/7` - Open Spirit rover
- `marsrover://rover/4` - Open InSight lander

#### Open Specific Photo
```
marsrover://photo/{photoId}
```

Example:
- `marsrover://photo/12345` - Open photo with ID 12345

### Web Deep Links (Future)

When the web app is deployed, these HTTPS links will also work:

```
https://marsroverphotos.app/rover/{roverId}
https://marsroverphotos.app/photo/{photoId}
```

## Rover IDs Reference

| Rover | ID | Status |
|-------|-----|--------|
| Perseverance | 3 | Active |
| InSight | 4 | Inactive (2018-2022) |
| Curiosity | 5 | Active |
| Opportunity | 6 | Inactive (2004-2018) |
| Spirit | 7 | Inactive (2004-2010) |

## Testing Deep Links

### Android (ADB)
```bash
# Test rover deep link
adb shell am start -W -a android.intent.action.VIEW \
  -d "marsrover://rover/5" \
  com.sirelon.marsroverphotos

# Test photo deep link
adb shell am start -W -a android.intent.action.VIEW \
  -d "marsrover://photo/12345" \
  com.sirelon.marsroverphotos
```

### iOS (Simulator)
```bash
xcrun simctl openurl booted "marsrover://rover/5"
```

### Web Browser
Simply click on links in browser (when web app is deployed):
- [Open Curiosity Rover](https://marsroverphotos.app/rover/5)
- [Open Perseverance Rover](https://marsroverphotos.app/rover/3)

## Implementation Details

### Android
Deep link configuration is in `androidApp/src/main/AndroidManifest.xml`:
- Custom scheme: `marsrover://`
- Web scheme: `https://marsroverphotos.app`
- Auto-verification enabled for HTTPS links

### iOS
Deep link configuration will be in `Info.plist` when iOS app is set up:
- URL types for custom scheme
- Associated domains for universal links

### Desktop
Desktop apps can register protocol handlers for the `marsrover://` scheme.

## Usage in Marketing

Deep links can be used in:
- **Social Media Posts**: Share specific rover discoveries
- **Email Campaigns**: Link to featured photos
- **QR Codes**: Print on educational materials
- **Push Notifications**: Navigate to relevant content

## Future Enhancements

Potential deep link additions:
- `marsrover://sol/{roverId}/{sol}` - Specific sol for a rover
- `marsrover://camera/{roverId}/{camera}` - Specific camera view
- `marsrover://date/{roverId}/{date}` - Photos from specific Earth date
- `marsrover://favorite` - User's favorite photos
- `marsrover://popular` - Popular photos feed

---

**Version**: 3.0.0
**Last Updated**: 2025-12-31
**Platform Support**: Android ✅ | iOS ⏳ | Desktop ⏳ | Web ⏳
