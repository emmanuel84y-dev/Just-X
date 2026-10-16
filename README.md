# Just X

Just X is a small native Android accessibility utility that shows a floating exit button only while a selected app is in the foreground.

## First build

The project uses Kotlin and Android Gradle Plugin. GitHub Actions builds a debug APK on each push to `main` and makes it available as a workflow artifact.

## Install and enable

1. Download `Just-X-debug-apk` from the successful **Android Debug APK** workflow run.
2. Extract `app-debug.apk` and install it on your Android phone. You may need to allow installs from your browser or file manager.
3. Open Just X and select the apps you want it to watch.
4. Tap **Open Accessibility Settings** and enable Just X.
5. Return to Just X. The floating ✕ appears while a selected app is in the foreground. Tap it to go to the home screen.

## Important

Android does not let ordinary apps force-stop another app. The X performs Android's supported Home action, taking you out of the app without force-stopping it. Accessibility access is powerful; this app only uses foreground-window events and a floating exit button. It does not inspect screen content or send data anywhere.
