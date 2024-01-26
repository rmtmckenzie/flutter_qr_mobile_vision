import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:qr_mobile_vision/src/qr_mobile_vision_method_channel.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  MethodChannelQrMobileVision platform = MethodChannelQrMobileVision();
  const MethodChannel channel = MethodChannel('qr_mobile_vision');

  setUp(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(
      channel,
      (MethodCall methodCall) async {
        switch (methodCall.method) {
          case "heartbeat":
          case "stop":
          case "toggleFlash":
            return null;
          case "start":
            return {
              "surfaceWidth": 100,
              "surfaceHeight": 100,
              "textureId": 1,
              "surfaceOrientation": 270,
            };
        }
        throw PlatformException(code: "not_implemented");
      },
    );
  });

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(channel, null);
  });

  test('heartbeat', () async {
    await platform.heartbeat();
    // expect(await platform.getPlatformVersion(), '42');
  });
}
