# Mochi Study — Android widget foundation

This branch contains an isolated Android prototype. It does not change the published GitHub Pages app or its data.

## Current prototype
- Native Android host opens the existing Mochi Study website in a WebView.
- A home-screen AppWidget reads a JSON schedule snapshot from Android SharedPreferences.
- GitHub Actions can build a debug APK and publish it as a workflow artifact.

## Important integration work still required
The existing browser PWA and this Android WebView have separate localStorage. The app must explicitly send its current `mochiStudy` data through `window.MochiAndroid.saveSchedule(...)` after load and after every save/import. Until that bridge is wired and tested, the widget will show its sync placeholder. The user's existing browser data is not automatically copied into the APK.

The current uploaded backup contains 11 lesson records with blank time and room fields. The user has confirmed that the installed Mochi Study view shows the complete timetable; do not overwrite that local data with this backup. Export a fresh backup from the installed app before any migration or sync testing.

## Build
GitHub Actions workflow: Android APK build. Trigger manually from Actions or by pushing changes to this branch. The debug APK is a test build, not a Play Store release.
