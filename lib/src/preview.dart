part of qr_mobile_vision;

class PreviewDetails {
  final num width;
  final num height;
  final num orientation;
  final int textureId;

  Size get size {
    return Size(width, height);
  }

  const PreviewDetails(this.width, this.height, this.orientation, this.textureId);
}

class Preview extends StatelessWidget {
  final double width, height;
  final double targetWidth, targetHeight;
  final int textureId;
  final int orientation;
  final BoxFit fit;

  Preview({
    @required PreviewDetails previewDetails,
    @required this.targetWidth,
    @required this.targetHeight,
    @required this.fit,
  })  : assert(previewDetails != null),
        textureId = previewDetails.textureId,
        width = previewDetails.width.toDouble(),
        height = previewDetails.height.toDouble(),
        orientation = previewDetails.orientation;

  @override
  Widget build(BuildContext context) {
    double frameHeight, frameWidth;

    return new NativeDeviceOrientationReader(
      builder: (context) {
        var nativeOrientation = NativeDeviceOrientationReader.orientation(context);

        int baseOrientation = 0;
        if (orientation != 0 && (width > height)) {
          baseOrientation = orientation ~/ 90;
          frameWidth = width;
          frameHeight = height;
        } else {
          frameHeight = width;
          frameWidth = height;
        }

        int nativeOrientationInt;
        switch (nativeOrientation) {
          case NativeDeviceOrientation.landscapeLeft:
            nativeOrientationInt = Platform.isAndroid ? 3 : 1;
            break;
          case NativeDeviceOrientation.landscapeRight:
            nativeOrientationInt = Platform.isAndroid ? 1 : 3;
            break;
          case NativeDeviceOrientation.portraitDown:
            nativeOrientationInt = 2;
            break;
          case NativeDeviceOrientation.portraitUp:
          case NativeDeviceOrientation.unknown:
            nativeOrientationInt = 0;
        }

        return new FittedBox(
          fit: fit,
          child: new RotatedBox(
            quarterTurns: baseOrientation + nativeOrientationInt,
            child: new SizedBox(
              width: frameWidth,
              height: frameHeight,
              child: new Texture(textureId: textureId),
            ),
          ),
        );
      },
    );
  }
}
