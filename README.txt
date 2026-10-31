MOCHI STUDY — files for a small installable web app

This folder contains:
- index.html: app interface and features
- manifest.webmanifest: install metadata
- sw.js: offline cache service worker
- icon.svg: app icon

The app is designed for static HTTPS hosting (for example GitHub Pages). It is not yet published online.
Open index.html locally to preview basic features, but PWA installation and offline caching require HTTPS hosting or localhost.
Data is stored in the browser's localStorage on the device. Use “Экспортировать резервную копию” periodically.
The AI button opens ChatGPT; it does not include a paid API or automatic AI processing inside the app.
The timetable is intentionally not prefilled from the screenshot: the screenshot's tiny merged cells need a careful confirmed transcription to avoid wrong class times. Add/edit entries in the schedule section for now.
