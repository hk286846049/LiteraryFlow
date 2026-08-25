# Testing

Run with the complete JDK configured in `JAVA_HOME`:

```powershell
$env:JAVA_HOME = "D:\Android\Java\jdk-17.0.19+10"
.\gradlew.bat --no-daemon :app:testDebugUnitTest :app:assembleDebug
```

Unit coverage includes:

- legacy JSON conversion (camelCase, snake_case and encoded JSON fields);
- coordinate transformation with insets;
- execution state, condition branches and retry;
- daily/weekly schedule matching.

The debug artifact is `app/build/outputs/apk/debug/app-debug.apk`.
