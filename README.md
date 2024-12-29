# **feat_ocr**

A feature module for card scanning and Optical Character Recognition (OCR) on Android.

---

## **Overview**

`feat_ocr` is an Android module designed for robust OCR-based card scanning and management:
- Captures card information like card numbers, holder names, expiry dates, and issuing networks.
- Provides a modern Compose-based UI for scanning and displaying results.
- Leverages CameraX and ML Kit for efficient real-time text recognition.
- Includes modular utilities for image preprocessing and permission management.

---

## **Features**

- ✅ **Card Scanning**: Real-time OCR-based detection of card details.
- ✅ **Torch Control**: Built-in support for toggling the flashlight during scanning.
- ✅ **Image Preprocessing**: Enhances OCR accuracy with advanced preprocessing techniques.
- ✅ **Dynamic UI**: Interactive Compose-based UI for card scanning and result management.
- ✅ **ViewModel Integration**: Uses StateFlow for reactive updates.

---

## **Requirements**

| Requirement        | Minimum Version         |
|--------------------|-------------------------|
| **Android OS**     | 11 (API 30)             |
| **Kotlin**         | 1.9.22                  |
| **Android Studio** | Giraffe (2022.3.1)      |
| **Gradle**         | 8.0                     |

---

## **Setup**

### **1. Add feat_ocr to Your Project**

To include `feat_ocr` via **JitPack**, follow these steps:

1. Add JitPack to your project-level `build.gradle` file:

    ```gradle
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
    ```

2. Add `feat_ocr` to your module-level `build.gradle` file:

    ```gradle
    dependencies {
        implementation 'com.github.clodymade:feat_ocr:1.0.0'
    }
    ```

3. Sync your project.

### **2. Permissions**

Add the required permissions to your AndroidManifest.xml:

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera" android:required="false" />
```

Ensure runtime permissions are handled in your app:

```kotlin
val permissions = arrayOf(android.Manifest.permission.CAMERA)
ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE)
```

---

## **Usage**

### **1. Start Card Scanning**

Integrate the scanner into your activity:

```kotlin
HiCardScanner.start(
    activity = this,
    previewView = previewView,
    roiState = mutableStateOf(Rect.Zero),
    preProcessedState = mutableStateOf(null)
) { result ->
    when (result) {
        is HiOcrResult -> {
            println("Card Number: ${result.cardNumber}")
            println("Holder Name: ${result.holderName}")
        }
        else -> {
            println("Error: ${result.error}")
        }
    }
}
```

### **2. Stop Card Scanning**

Stop scanning when it’s no longer needed:

```kotlin
HiCardScanner.stop()
```

### **3. Toggle Torch (Flashlight)**

Control the flashlight during scanning:

```kotlin
HiCardScanner.toggleTorch(true) // Turn on
HiCardScanner.toggleTorch(false) // Turn off
```

---

## **HiOcrResult**

OCR scan results are encapsulated in the HiOcrResult class. Key properties include:

| Property          | Type             | Description                         |
|-------------------|------------------|-------------------------------------|
| cardNumber        | String           | Detected card number.               |
| holderName        | String           | Detected holder name.               |
| expiryDate        | String           | Detected expiry date.               |
| issuingNetwork    | String           | Card issuing network.               |
| error             | String           | Error message, if any.              |

---

## **Example UI**

To display scanned card details using a Jetpack Compose-based UI:

```kotlin
@Composable
fun HiCardNumberListScreen(
    cardNumbers: StateFlow<List<HiCardNumber>>,
    onBackPressed: () -> Unit
) {
    val cardNumberList by cardNumbers.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Card Number List") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Default.Close, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier.padding(paddingValues)
        ) {
            items(cardNumberList) { cardNumber ->
                HiCardNumberItem(cardNumber)
            }
        }
    }
}
```

---

## **License**

feat_ocr is licensed under the Apache License, Version 2.0.
```
http://www.apache.org/licenses/LICENSE-2.0
```

---

## **Contributing**

Contributions are welcome! To contribute:

1. Fork this repository.
2. Create a feature branch:
```
git checkout -b feature/your-feature
```
3. Commit your changes:
```
git commit -m "Add feature: description"
```
4. Push to the branch:
```
git push origin feature/your-feature
```
5. Submit a Pull Request.

---

## **Author**

### **netcanis**
iOS GitHub: https://github.com/netcanis
Android GitHub: https://github.com/clodymade

---
