# HTLM5 Android App

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Build Status](https://github.com/yourusername/HTLM5-Android-App/actions/workflows/release-workflows.yaml/badge.svg)](https://github.com/yourusername/HTLM5-Android-App/actions)

A modern Android application built with HTML5 and native Android components, featuring Huawei Mobile Services integration.

## Features

- Hybrid HTML5/Android native UI
- Huawei Mobile Services integration
- Biometric authentication
- Barcode/QR code scanning
- Network operations with Retrofit
- Local database with GreenDAO
- Image loading with Glide
- Modern Material Design components

## Prerequisites

- Android Studio Giraffe or later
- JDK 11
- Android SDK 34
- Huawei Mobile Services (HMS) Core

## Getting Started

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/HTLM5-Android-App.git
   ```

2. Open the project in Android Studio

3. Configure your `local.properties` with your Android SDK location:
   ```properties
   sdk.dir=/path/to/your/android/sdk
   ```

4. Build and run the project

## Building

To build a debug version:
```bash
./gradlew assembleDebug
```

To build a release version:
```bash
./gradlew assembleRelease
```

## Contributing

Please read [CONTRIBUTING](./CONTRIBUTING.md) for details on our code of conduct and the process for submitting pull requests.

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](./LICENSE.md) file for details.

## Security

For security-related issues, please see our [SECURITY](./SECURITY.md) document.
