# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an Android application project called "smstomail" built with Kotlin and Android SDK. The project follows standard Android project structure with Gradle as the build system.

## Development Commands

### Build Commands
- `./gradlew build` - Build the project
- `./gradlew assembleDebug` - Build debug APK
- `./gradlew assembleRelease` - Build release APK
- `./gradlew clean` - Clean build artifacts

### Testing Commands
- `./gradlew test` - Run unit tests
- `./gradlew connectedAndroidTest` - Run instrumented tests on connected device/emulator
- `./gradlew testDebugUnitTest` - Run debug unit tests specifically

### Development Tools
- `./gradlew lint` - Run Android lint checks
- `./gradlew checkDependencies` - Check for dependency updates

## Project Configuration

### Key Files
- `app/build.gradle.kts` - Main app module configuration
- `build.gradle.kts` - Root project configuration
- `settings.gradle.kts` - Project settings and module inclusion
- `gradle/libs.versions.toml` - Version catalog for dependencies

### Android Configuration
- **Package**: `com.example.smstomail`
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 36
- **Compile SDK**: 36
- **Java/Kotlin Target**: JVM 11

### Dependencies
- AndroidX Core KTX
- AppCompat
- Material Design Components
- JUnit for testing
- Espresso for UI testing

## Project Structure

The project follows standard Android app structure:
- `app/src/main/java/com/example/smstomail/` - Main source code (currently empty)
- `app/src/test/` - Unit tests
- `app/src/androidTest/` - Instrumented tests
- `app/src/main/res/` - Android resources (layouts, strings, etc.)
- `app/src/main/AndroidManifest.xml` - App manifest

## Architecture Notes

This is a freshly generated Android project with standard boilerplate. The main application code is not yet implemented - only the basic project structure and example tests exist.