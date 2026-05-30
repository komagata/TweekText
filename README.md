# TweekText

TweekText is a simple Android plain text editor for Android 12 and later.

## Scope

- Create and edit plain text files.
- Open and save files through Android's standard file picker.
- Use UTF-8.
- Preserve existing line endings when saving opened files.
- Use LF for new files.
- Provide search and replace.
- Do not include code-editor features such as syntax highlighting, completion, Git integration, terminal, or project management.

## UI

- Jetpack Compose
- Material 3
- Dynamic color on Android 12+
- Light and dark theme follow the system setting.

## Requirements

- Android Studio
- JDK 17
- Android SDK Platform 36
- Android SDK Build Tools 36.0.0
- Android SDK Platform Tools

This repository includes a Gradle Wrapper.

## Build

```sh
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home ./gradlew assembleDebug
```

The debug APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Emulator

This development machine has an AVD named `TweekText_API_36`.

```sh
ANDROID_HOME=/opt/homebrew/share/android-commandlinetools /opt/homebrew/share/android-commandlinetools/emulator/emulator -avd TweekText_API_36
```

## Test

Run the app UI tests on a connected Android device or emulator:

```sh
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home ./gradlew connectedDebugAndroidTest
```

The UI tests cover:

- Plain text editing
- Search
- Replace all
- Starting the save document picker
- Starting the open document picker
