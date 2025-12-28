# Mars Rover Mission Data

This folder contains mission facts data for all Mars rovers.

## Files

- `curiosity.json` - Curiosity rover (ID: 5)
- `opportunity.json` - Opportunity rover (ID: 6)
- `spirit.json` - Spirit rover (ID: 7)
- `insight.json` - InSight lander (ID: 4)
- `perseverance.json` - Perseverance rover (ID: 3)

## Data Structure

Each JSON file contains:
- `roverId`: Unique rover identifier
- `roverName`: Name of the rover/lander
- `objectives`: Array of mission objectives
- `funFacts`: Array of interesting facts about the mission

## Uploading to Firebase

### Option 1: Using the Debug Panel (Easiest)

Add this to any debug screen:

```kotlin
import com.sirelon.marsroverphotos.firebase.mission.MissionDataUploadDebugPanel

@Composable
fun YourDebugScreen() {
    // Your screen content...

    if (BuildConfig.DEBUG) {
        MissionDataUploadDebugPanel()
    }
}
```

### Option 2: Programmatic Upload

```kotlin
import com.sirelon.marsroverphotos.firebase.mission.uploadMissionDataToFirebase
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

// In Activity or ViewModel
lifecycleScope.launch {
    uploadMissionDataToFirebase(context) { result ->
        if (result.isFullSuccess) {
            Log.d("Upload", "All ${result.successCount} missions uploaded!")
        } else {
            Log.e("Upload", "${result.failureCount} missions failed to upload")
        }
    }
}
```

### Option 3: Direct Class Usage

```kotlin
import com.sirelon.marsroverphotos.firebase.mission.MissionDataUploader
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

lifecycleScope.launch {
    val uploader = MissionDataUploader(context)

    // Upload all missions
    val uploadResult = uploader.uploadAllMissions()
    Log.d("Upload", "Uploaded: ${uploadResult.successCount}/${uploadResult.totalProcessed}")

    // Verify the upload
    val verifyResult = uploader.verifyUploadedData()
    Log.d("Verify", "Found: ${verifyResult.foundCount}/${verifyResult.totalChecked}")
}
```

## Firebase Setup

Make sure your Firebase project has:
1. Firestore enabled
2. A collection named `rover-missions`
3. Appropriate security rules (see below)

### Recommended Security Rules

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /rover-missions/{roverId} {
      // Allow anyone to read mission data
      allow read: if true;

      // Only authenticated admins can write
      allow write: if request.auth != null && request.auth.token.admin == true;
    }
  }
}
```

## Notes

- This is a one-time data seeding operation
- Uploading will overwrite existing data for each rover
- The app will automatically fetch this data when displaying mission information
- All data is sourced from NASA's official mission documentation
