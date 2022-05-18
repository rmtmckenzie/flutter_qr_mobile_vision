import 'package:flutter/widgets.dart';
import 'package:native_device_orientation/native_device_orientation.dart';
import 'package:qr_mobile_vision/src/preview_details.dart';

class Preview extends StatelessWidget {
  final double width, height;
  final double targetWidth, targetHeight;
  final int? textureId;
  final int? sensorOrientation;
  final BoxFit fit;
  final int sdkInt;

  Preview({
    required PreviewDetails previewDetails,
    required this.targetWidth,
    required this.targetHeight,
    required this.fit,
  })  : textureId = previewDetails.textureId,
        width = previewDetails.width.toDouble(),
        height = previewDetails.height.toDouble(),
        sdkInt = previewDetails.sdkInt,
        sensorOrientation = previewDetails.sensorOrientation as int?;

  @override
  Widget build(BuildContext context) {
    return NativeDeviceOrientationReader(
      builder: (context) {
        var nativeOrientation = NativeDeviceOrientationReader.orientation(context);

        int nativeRotation = 0;
        switch (nativeOrientation) {
          case NativeDeviceOrientation.portraitUp:
            if (sdkInt == 23) {
              nativeRotation = 180;
            } else {
              nativeRotation = 0;
            }
            break;
          case NativeDeviceOrientation.landscapeRight:
            nativeRotation = 90;
            break;
          case NativeDeviceOrientation.portraitDown:
            if (sdkInt == 23) {
              nativeRotation = 0;
            } else {
              nativeRotation = 180;
            }
            break;
          case NativeDeviceOrientation.landscapeLeft:
            nativeRotation = 270;
            break;
          case NativeDeviceOrientation.unknown:
          default:
            break;
        }

        int rotationCompensation = ((nativeRotation - sensorOrientation! + 450) % 360) ~/ 90;

        double frameHeight = width;
        double frameWidth = height;

        return ClipRect(
          child: FittedBox(
            fit: fit,
            child: RotatedBox(
              quarterTurns: rotationCompensation,
              child: SizedBox(
                width: frameWidth,
                height: frameHeight,
                child: Texture(textureId: textureId!),
              ),
            ),
          ),
        );
      },
    );
  }
}
