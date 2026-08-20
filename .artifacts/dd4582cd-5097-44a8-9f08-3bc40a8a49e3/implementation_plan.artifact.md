# Implementation Plan - UtilityHub Feature Suite

This plan outlines the steps to migrate the "Multi-Utility Calculator & Text Suite" from HTML/JS to a native Android application using Jetpack Compose.

## User Review Required

> [!IMPORTANT]
> The original HTML uses several external APIs (Google Translate, Datamuse, open.er-api.com). We will implement native wrappers for these. Some features like "Network Speedometer" might behave differently on native Android due to OS-level network restrictions or permissions.

## Proposed Changes

### 1. Dependency Updates
We need to add navigation, networking, and utility libraries.
#### [MODIFY] [build.gradle.kts](file:///C:/Users/KANNAN/AndroidStudioProjects/UtilityHub/app/build.gradle.kts)
- Add Navigation Compose.
- Add Retrofit & Gson for API calls.
- Add ZXing for QR generation.

### 2. Architecture & Navigation
Implement a unified navigation structure and theme.
#### [NEW] [NavRoutes.kt](file:///C:/Users/KANNAN/AndroidStudioProjects/UtilityHub/app/src/main/java/com/example/utilityhub/navigation/NavRoutes.kt)
- Define routes for all 9 tools.
#### [NEW] [UtilityHubApp.kt](file:///C:/Users/KANNAN/AndroidStudioProjects/UtilityHub/app/src/main/java/com/example/utilityhub/ui/UtilityHubApp.kt)
- Main entry point with `Scaffold` and `NavHost`.
- Navigation Drawer or Bottom Bar to switch between tools.
#### [MODIFY] [Theme.kt](file:///C:/Users/KANNAN/AndroidStudioProjects/UtilityHub/app/src/main/java/com/example/utilityhub/ui/theme/Theme.kt)
- Implement the "Warm Amber" and "Dark/Light" themes as defined in the CSS.

### 3. Feature Implementations

#### 3.1. Calculators (Percentage, Item Cost, EMI, Measurement)
- Port logic from JS to Kotlin.
- Create reusable UI components for input fields and results.

#### 3.2. Text Tools (Translator, Humanize)
- **Translator**: Use Retrofit to call a translation service.
- **Humanize**: Port the synonym logic and Datamuse API integration.

#### 3.3. Utilities (Speedometer, Currency, QR Code)
- **Speedometer**: Implement speed estimation logic.
- **Currency**: Fetch live rates using Retrofit.
- **QR Code**: Use ZXing to generate QR codes from text.

### 4. Integration
#### [MODIFY] [MainActivity.kt](file:///C:/Users/KANNAN/AndroidStudioProjects/UtilityHub/app/src/main/java/com/example/utilityhub/MainActivity.kt)
- Set up the main Compose entry point.

## Verification Plan

### Automated Tests
- Unit tests for calculator logic (EMI, Percentage, etc.).
- Mock API tests for Currency and Translator logic.

### Manual Verification
- Deploy to an Android device.
- Test each utility tool individually.
- Verify Dark/Light mode switching.
