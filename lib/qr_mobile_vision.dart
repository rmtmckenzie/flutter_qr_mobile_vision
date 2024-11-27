import 'package:flutter/foundation.dart';
import 'package:qr_mobile_vision/src/barcode_data.dart';
import 'package:qr_mobile_vision/src/barcode_formats.dart';
import 'package:qr_mobile_vision/src/camera_direction.dart';
import 'package:qr_mobile_vision/src/preview_details.dart';
import 'package:qr_mobile_vision/src/qr_mobile_vision_platform_interface.dart';

export 'package:qr_mobile_vision/src/barcode_formats.dart';
export 'package:qr_mobile_vision/src/camera_direction.dart';

/// QR Mobile Vision wrapper allowing for convenient usage of Platform interface
class QrMobileVision {
  /// Start the QR reading. Attempts to find the closest camera resolution for
  /// the given width/height, chooses the appropriate direction, and tells the
  /// framework which formats to listen to.
  static Future<PreviewDetails> start({
    required int width,
    required int height,
    required ValueChanged<BarcodeData> qrCodeHandler,
    CameraDirection cameraDirection = CameraDirection.BACK,
    List<BarcodeFormats>? formats = defaultBarcodeFormats,
  }) async {
    return QrMobileVisionPlatform.instance.start(
      width: width,
      height: height,
      qrCodeHandler: qrCodeHandler,
      cameraDirection: cameraDirection,
      formats: formats,
    );
  }

  static Future<void> toggleFlash() {
    return QrMobileVisionPlatform.instance.toggleFlash();
  }

  static Future<void> stop() {
    return QrMobileVisionPlatform.instance.stop();
  }

  static Future<void> heartbeat() {
    return QrMobileVisionPlatform.instance.heartbeat();
  }
}
