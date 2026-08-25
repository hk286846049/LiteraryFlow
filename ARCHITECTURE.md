# LiteraryFlow 2.0 Architecture

The Android app is split into five runtime boundaries:

- `ui2`: Compose screens and the complete Figwright page index.
- `data`: Room 2.0 entities, repository, import/export and legacy conversion.
- `automation`: cancellable execution state machine and action channels.
- `vision`: screenshot/OCR interfaces, ROI, frame change detection and cache.
- `scheduler`: schedule matching and queue adapters.

`Perception` never performs input. `AndroidActionExecutor` is the only MVP bridge from the state machine to Accessibility, OCR and app launching.

The Compose state flow mirrors the web prototype's view modes while keeping entities real:

`Tasks -> TaskDetail -> Wizard -> LiveRun -> RunSuccess/RunFailure -> RunDetail`.

History, app profiles, permission status, import/export, settings, template creation, batch task operations and OCR debug all read or write the Android data and system capability layers.
