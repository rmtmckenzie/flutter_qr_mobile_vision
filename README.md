# QR Mobile Vision

![pub package][version_badge]

_Reading QR codes and other barcodes using Google's Mobile Vision API._

This plugin uses Android & iOS native APIs for reading images from the device's camera.
It then pipes these images both to the Mobile Vision API which detects barcodes/qrcodes etc, 
outputs a preview image to be shown on a flutter texture.

The plugin includes a widget which performs all needed transformations on the camera
output to show within the defined area.

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

### `offscreenBuilder`

A callback that must return a widget if defined.
This should build whatever you want to show up when the camera view is 'offscreen'.
i.e. when the app is paused. May or may not show up in preview of app.

### `onError`

Callback for if there's an error.

### 'formats'

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
- Alcatel Idol X Plus
- Essential Phone
- iPhone 6


[version_badge]: https://img.shields.io/pub/v/qr_mobile_vision.svg