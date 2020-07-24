# QR Mobile Vision

![pub package][version_badge]

_Reading QR codes and other barcodes using Firebase's Mobile Vision API._

# UPDATE - Now using Firebase since apps using GoogleMobileVision have become unreleaseable on iOS.


This plugin uses Android & iOS native APIs for reading images from the device's camera.
It then pipes these images both to the Firebase Mobile Vision API which detects barcodes/qrcodes etc, 
and outputs a preview image to be shown on a flutter texture.

The plugin includes a widget which performs all needed transformations on the camera
output to show within the defined area.

If you are only targeting android and don't want to switch to Firebase Mobile Vision from Google Mobile Vision, use
a 0.* version of the plugin.

If you target iOS as well but don't want to integrate Firebase (and like this plugin over other ones available on pub.dev)
please head over to [this issue](https://github.com/rmtmckenzie/flutter_qr_mobile_vision/issues/121) and give it a +1, as 
that will tell the developer how much interest there is in it.

## Setting up Firebase

If you're using firebase elsewhere in your app, you should be able to simply use this plugin. However, if you're not,
you'll have to follow steps 1, 2, and 3 from [Firebase's Flutter instructions](https://firebase.google.com/docs/flutter/setup), 
which include setting up projects in Firebase and 

For android, that's all you need to do - the library doesn't seem to actually need initialization.

For iOS, you need to make sure that `FirebaseApp.configure()` is called somewhere before the plugin is used - 
for instance, in your app's AppDelegate. The example app does this.

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
- iPhone 6


[version_badge]: https://img.shields.io/pub/v/qr_mobile_vision.svg