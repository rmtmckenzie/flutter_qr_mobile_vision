import 'package:plugin_platform_interface/plugin_platform_interface.dart';
import 'package:qr_mobile_vision/src/barcode_formats.dart';
import 'package:qr_mobile_vision/src/camera_direction.dart';
import 'package:qr_mobile_vision/src/preview_details.dart';
import 'package:qr_mobile_vision/src/qr_mobile_vision_method_channel.dart';

abstract class QrMobileVisionPlatform extends PlatformInterface {
  /// Constructs a QrMobileVisionPlatform.
  QrMobileVisionPlatform() : super(token: _token);

  static final Object _token = Object();

  static QrMobileVisionPlatform _instance = MethodChannelQrMobileVision();

  /// The default instance of [QrMobileVisionPlatform] to use.
  ///
  /// Defaults to [MethodChannelQrMobileVision].
  static QrMobileVisionPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [QrMobileVisionPlatform] when
  /// they register themselves.
  static set instance(QrMobileVisionPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<PreviewDetails> start({
    required int width,
    required int height,
    required void Function(String?, BarcodeFormats?) qrCodeHandler,
    CameraDirection cameraDirection = CameraDirection.BACK,
    List<BarcodeFormats>? formats = defaultBarcodeFormats,
  });

  Future<void> toggleFlash();

  Future<void> stop();

  Future<void> heartbeat();
}
