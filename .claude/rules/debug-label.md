# Rule: Debug Build Label

**Before building a debug APK, write a short feature name to `androidApp/src/main/assets/debug_label.txt`.**

The app displays it as a badge in the top-right corner of every debug build. This lets the owner immediately identify which feature is installed when multiple agents have built and installed APKs in parallel.

```
echo "your-feature-name" > androidApp/src/main/assets/debug_label.txt
```

Use the branch name or a 1–3 word slug (e.g. `sol-feed`, `release-notes`, `dark-theme`). The file is gitignored and picked up automatically by the next `assembleDebug` — no Gradle sync needed.
