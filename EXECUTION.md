# Execution

`ExecutionEngine` owns one active task run, exposes `StateFlow<RunState>`, and supports:

- sequential steps and condition true/false branches;
- timeout-aware text/screen actions;
- retry policy (`STOP`, `RETRY`, `SKIP`);
- pause, resume and cancellation;
- RunRecord persistence through `RunRecordRepository`.

`AndroidActionExecutor` maps v2 actions to the existing Accessibility and MediaProjection/OCR services. Unsupported child-task bindings return a user-facing failure instead of throwing.

`OptionalShizukuActionExecutor` is the optional privileged channel. When no Shizuku provider is available it delegates to Accessibility and returns a readable fallback error rather than crashing.
