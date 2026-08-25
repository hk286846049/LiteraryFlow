# OCR and Perception

`OcrEngine2` is the replaceable OCR boundary. The MVP implementation uses ML Kit Chinese text recognition.

The intended pipeline is:

`ScreenshotProvider -> FrameChangeDetector -> RoiResolver -> OcrEngine2 -> OcrCache -> ScreenState`

OCR is requested by an action/condition or a detected frame change. `PerceptionMode.IDLE` does not capture or OCR. ROI values and bounding boxes use normalized 0..1 coordinates.

