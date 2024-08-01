import 'package:flutter_test/flutter_test.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';
import 'package:qr_mobile_vision/qr_mobile_vision.dart';
import 'package:qr_mobile_vision/src/preview_details.dart';
import 'package:qr_mobile_vision/src/qr_mobile_vision_method_channel.dart';
import 'package:qr_mobile_vision/src/qr_mobile_vision_platform_interface.dart';

//ignore_for_file: avoid_print

class MockQrMobileVisionPlatform with MockPlatformInterfaceMixin implements QrMobileVisionPlatform {
  @override
  Future<void> heartbeat() async {
    return;
  }

  @override
  Future<PreviewDetails> start({
    required int width,
    required int height,
    required void Function(String?, BarcodeFormats?) qrCodeHandler,
    CameraDirection cameraDirection = CameraDirection.BACK,
    List<BarcodeFormats>? formats = defaultBarcodeFormats,
  }) async {
    return PreviewDetails(NativePreviewDetails(100, 100, 270, 1), 3);
  }

  @override
  Future<void> stop() async {
    return;
  }

  @override
  Future<void> toggleFlash() async {
    return;
  }
}

void main() {
  final QrMobileVisionPlatform initialPlatform = QrMobileVisionPlatform.instance;

  test('$MethodChannelQrMobileVision is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelQrMobileVision>());
  });

  group('mock tests', () {
    setUp(() {
      MockQrMobileVisionPlatform fakePlatform = MockQrMobileVisionPlatform();
      QrMobileVisionPlatform.instance = fakePlatform;
    });

    test('heartbeat', () async {
      await QrMobileVision.heartbeat();
    });

    test('toggleFlash', () async {
      await QrMobileVision.heartbeat();
    });

    test('stop', () async {
      await QrMobileVision.stop();
    });

    test('start', () async {
      handler(String? code, BarcodeFormats? format) => print(code);
      final details = await QrMobileVision.start(
        width: 100,
        height: 100,
        qrCodeHandler: handler,
        formats: [BarcodeFormats.QR_CODE, BarcodeFormats.AZTEC],
        cameraDirection: CameraDirection.FRONT,
      );
      assert(details.height == 100);
      assert(details.width == 100);
      assert(details.sdkInt == 3);
      assert(details.sensorOrientation == 270);
      assert(details.textureId == 1);
    });
  });
}
