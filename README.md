# QR Mobile Vision

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

There are also 2 optional parameters to QrCamera. The first is `fit`, which takes as parameter
the flutter `BoxFit`. Setting this to differnet values should get the preview image to fit in 
different ways, but only `BoxFit = cover` has been tested.

The other is `notStartedBuilder`, which is a callback that must return a widget if defined.
This should build whatever you want to show up while the camera is loading (which can take
from milliseconds to seconds depending on the device).


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