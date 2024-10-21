# QR Mobile Vision

[![pub package][version_badge]](https://pub.dartlang.org/packages/qr_mobile_vision)

_Reading QR codes and other barcodes using Firebase's MLKit._

This plugin uses Android & iOS native APIs for reading images from the device's camera.
It then pipes these images both to the MLKit Vision Barcode API which detects barcodes/qrcodes etc,
and outputs a preview image to be shown on a flutter texture.

The plugin includes a widget which performs all needed transformations on the camera
output to show within the defined area.

## Android Models

With this new version of MLKit, there are two separate models you can use to do the barcode scanning. Currently, this
apk chooses to use the build-in model.  This will increase your code size by ~2.2MB but will
result in better scanning and won't require a separate package to be downloaded in the background for barcode scanning
to work properly.

You could also use the Google Play Services and tell your app to download it on install from the play store. See the
instruction on the [ml-kit barcode-scanning documentation page](https://developers.google.com/ml-kit/vision/barcode-scanning/android)
for android. You would also have to remove the com.google.mlkit:barcode-scanning dependency; this hasn't been tested
but would probably go something like this:

```
configurations.all {
    exclude group: "com.google.mlkit", module:"barcode-scanning"
}
//  ...
dependencies {
  // ...
  // Use this dependency to use the dynamically downloaded model in Google Play Services
  implementation 'com.google.android.gms:play-services-mlkit-barcode-scanning:16.1.4'
}
```

Note that if you do this, you should tell your app to automatically download the model as in the above linked docs.MLKit
```
<application ...>
    ...
    <meta-data
        android:name="com.google.mlkit.vision.DEPENDENCIES"
        android:value="barcode" />
    <!-- To use multiple models: android:value="barcode,model2,model3" -->
</application>
```

If this doesn't work for you please open an issue.

## iOS

For recent versions of Google's Barcode Scanning library ('GoogleMLKit/BarcodeScanning'),
an iOS version of 15.5+ is required.

The podspec has not been updated to reflect this requirement as usage of older versions
of the dependency library is still possible (and necessary if you use an older
Firebase version). However, if you are having dependency errors when updating this plugin
in conjunction with the Firebase libraries, ensuring that your project has a version of 
15.5+ might be required.

The example project has been updated to reflect this.

## Usage

See the example for how to use this plugin; it is the best resource available as it shows
the plugin in use. However, these are the steps you need to take to
use this plugin.

First, figure out the area that you want the camera preview to be shown in. This is important
as the preview __needs__ to have a constrained size or it won't be able to build. This
is required due to the complex nature of the transforms needed to get the camera preview to
show correctly on both iOS and Android, while still working with the screen rotated etc.

It may be possible to get the camera preview to work without putting it in a SizedBox or Container,
but the recommended way is to put it in a SizedBox or Container.

You then need to include the package and instantiate the camera.

```
import 'package:qr_mobile_vision/qr_camera.dart';

...

new SizedBox(
  width: 300.0,
  height: 600.0,
  child: new QrCamera(
    qrCodeCallback: (code) {
      ...
    },
  ),
)
```

The QrCodeCallback can do anything you'd like, and wil keep receiving QR codes
until the camera is stopped.

There are also optional parameters to QrCamera.

### `fit`

Takes as parameter the flutter `BoxFit`.
Setting this to different values should get the preview image to fit in
different ways, but only `BoxFit = cover` has been tested extensively.

### `notStartedBuilder`

A callback that must return a widget if defined.
This should build whatever you want to show up while the camera is loading (which can take
from milliseconds to seconds depending on the device).

### `child`

Widget that is shown on top of the QrCamera. If you give it a specific size it may cause
weird issues so try not to.

### `key`

Standard flutter key argument. Can be used to get QrCameraState with a GlobalKey.

### `cameraDirection`

Uses a camera of the specified direction. Default is to use `BACK`

These are the support types:

```
  FRONT,
  BACK
```

### `offscreenBuilder`

A callback that must return a widget if defined.
This should build whatever you want to show up when the camera view is 'offscreen'.
i.e. when the app is paused. May or may not show up in preview of app.

### `onError`

Callback for if there's an error.

### `formats`

A list of supported formats, all by default. If you use all, you shouldn't define any others.

These are the supported types:

```
  ALL_FORMATS,
  AZTEC,
  CODE_128,
  CODE_39,
  CODE_93,
  CODABAR,
  DATA_MATRIX,
  EAN_13,
  EAN_8,
  ITF,
  PDF417,
  QR_CODE,
  UPC_A,
  UPC_E
```

### `direction`

Whether you want the front or the back camera to be used.

## Toggle flash

When the camera is running, you can use the `QrCamera.toggleFlash()` or `QrMobileVision.toggleFlash()` methods
to turn the flash on or off. A better solution is in the works for this, but I can't make a commitment for when
I'll have time to finish it.

## Push and Pop

If you push a new widget on top of a the current page using the navigator, the camera doesn't
necessarily know about it.

## Contributions

Anyone wanting to contribute to this project is very welcome to! I'll take a look at PR's as soon
 as I can, and any bug reports are appreciated. I've only a few devices on which to test this
 so feedback about other devices is appreciated as well.
 
This has been tested on:

- Nexus 5x
- Nexus 4
- Pixel 3a
- iPhone 7


[version_badge]: https://img.shields.io/pub/v/qr_mobile_vision.svg
