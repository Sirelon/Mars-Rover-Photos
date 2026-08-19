# Push Notifications

Mars Rover Photos sends occasional broadcast notifications — a new release worth reading about, or
a nudge that fresh photos have landed. Low volume, nothing time-critical.

## How it works

Firebase Cloud Messaging, Firebase project `mars-rover-photos`. Every message goes to the single
topic **`mars-updates`**. There is no device-token registry, no backend and no Cloud Function: the
app subscribes to the topic on opt-in, and messages are composed in the Firebase console or posted
straight to the FCM API.

Android and iOS only. Desktop and Web bind `NoOpPushNotifications`, so nothing on those targets
reaches Cloud Messaging — which matters because the GitLive messaging JVM implementation throws.

## Payload contract

A message carries a `notification` block, which the OS renders, plus one data entry:

| Key | Value |
| --- | --- |
| `link` | a deep-link URI the app opens when the notification is tapped |

Valid `link` values:

| URI | Opens |
| --- | --- |
| `marsrover://whatsnew` | the full version-history list |
| `marsrover://whatsnew/{version}` | the story view for that release, e.g. `marsrover://whatsnew/4.2.0` |
| `marsrover://rover/{roverId}` | that rover's photo feed |
| `marsrover://photo/{photoId}` | a single photo |

A `whatsnew` link naming a version the installed build has no notes for falls back to the version
list, so it is safe to announce a release to users who haven't updated yet.

Rover IDs: Perseverance 3, InSight 4, Curiosity 5, Opportunity 6, Spirit 7.

Taps route through the same path as any other deep link — see [DEEP_LINKING.md](/DEEP_LINKING.md).

## Opt-in

There is no permission prompt at launch. The OS prompt appears only when the user turns on the
**Mars Updates** switch in About → Updates, which is also where it gets turned back off.

Android 13+ requires `POST_NOTIFICATIONS`; iOS requires `UNUserNotificationCenter` authorization.
Once the prompt has been used, the OS will not show it again — the About row then reads "Blocked"
and opens system settings instead of offering a switch.

A push that arrives while the app is in the foreground is deliberately not displayed, on either
platform.

## One-time setup

Both steps are required before **any** iOS notification is delivered:

1. **APNs Auth Key** — upload the `.p8` in Firebase Console → Project Settings → Cloud Messaging.
2. **Push Notifications capability** — enable it for the app identifier in the Apple Developer
   portal so the `aps-environment` entitlement signs.

Android needs no equivalent step; `androidApp/google-services.json` is committed.

**Play Console → Data safety** must declare *Device or other IDs* — the FCM registration token
counts. Re-check the App Store privacy answers at the same time.

## Sending

### Firebase console

Messaging → new campaign → compose title and body → **Target: Topic** `mars-updates` → under
*Additional options → Custom data*, add key `link` with one of the URIs above.

### FCM HTTP v1

```bash
curl -X POST \
  "https://fcm.googleapis.com/v1/projects/mars-rover-photos/messages:send" \
  -H "Authorization: Bearer $(gcloud auth print-access-token)" \
  -H "Content-Type: application/json" \
  -d '{
    "message": {
      "topic": "mars-updates",
      "notification": {
        "title": "Version 4.2.0 is here",
        "body": "New mission timeline and faster photo loading."
      },
      "data": {
        "link": "marsrover://whatsnew/4.2.0"
      }
    }
  }'
```

`gcloud` must be authenticated against a principal with the *Firebase Cloud Messaging API Admin*
role on the project.

## Testing

Android works on an emulator with Play Services. **iOS remote push cannot be tested on the
simulator** — it never registers with APNs, so a real device is required.

Before the first send on a new device, confirm the token round-trip. Debug builds log
`FCM registration token: …` on subscribe (`AndroidPushNotifications` / `IosPushNotifications`); on
Android, `adb logcat -s AndroidPushNotifications`. If it logs `No FCM registration token yet`, the
device has no APNs/registration token and nothing will be delivered. Without that check, a failed
delivery is indistinguishable between a missing APNs key, a missing entitlement, and a token that
never reached FCM.

Note the GitLive wrapper discards the result of `subscribeToTopic`, so a failed subscription is not
observable in-app. `App.kt` re-subscribes on every launch, which is what makes an attempt made
offline eventually stick.

## Troubleshooting

| Symptom | Cause |
| --- | --- |
| Android delivers, iOS never does | No APNs Auth Key uploaded, or the Push Notifications capability isn't enabled. Sends still report success — FCM accepts the message and drops it at the APNs boundary. |
| Notification shows as a white square | `ic_notification.xml` must be an alpha-only silhouette. Android renders only the alpha channel, so any filled background becomes a solid block. |
| Nothing arrives after a reinstall | Topic subscription is bound to the FCM registration token, which a reinstall replaces. The app re-subscribes on launch whenever the setting is on. |
| Notification lands in a generic channel | The channel named by `default_notification_channel_id` doesn't exist yet. `AndroidPushNotifications` creates it on opt-in; FCM never creates it. |
| Nothing appears while the app is open | Intended — foreground messages are suppressed on both platforms. |
