# WidgetChat — Android SDK

> **Drop-in AI chat widget for Android (Jetpack Compose).** Add an AI chatbot to any Android app in
> minutes — themed, multilingual, with rich content and a no-code dashboard.

[![](https://jitpack.io/v/tajaouart/WidgetChat-Android.svg)](https://jitpack.io/#tajaouart/WidgetChat-Android)
[![Platform](https://img.shields.io/badge/minSdk-26-blue.svg)](https://developer.android.com)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4.svg)](https://developer.android.com/jetpack/compose)

Native **Jetpack Compose** SDK for [Widget-Chat](https://widget-chat.com) — a drop-in AI chat widget
for Android. Same backend and no-code dashboard as the Flutter, iOS, and web clients; rendered
natively with Compose (no webview).

<p align="center"><img src="assets/demo.gif" alt="Widget-Chat Android SDK demo — AI chat with rich product cards" width="300"></p>

## Install (JitPack)

**settings.gradle.kts**
```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

**app/build.gradle.kts**
```kotlin
implementation("com.github.tajaouart:WidgetChat-Android:0.0.1")
```

## Usage

```kotlin
import com.widgetchat.app.WidgetChat

setContent {
    WidgetChat(
        secretKey = "YOUR_PROJECT_KEY",   // from the dashboard (public, safe to ship)
        userId = currentUser.id,
    )
}
```

Optional parameters: `baseUrl`, `isReadOnly`, `modifier`.

## Requirements

- minSdk 26, compileSdk 34
- Jetpack Compose · Kotlin 2.0
- JDK 17

## Features

Dashboard-driven theming (light/dark, colors, avatar) · markdown & code · rich content
(cards, carousels, product lists, images, button groups, swatches) · image attachments ·
typing indicator · good/bad rating + report · sticky AI disclosure & consent · 100+ languages
with a locale picker · quota handling · message history.

## License

Proprietary — © Widget-Chat. See [LICENSE](LICENSE).
