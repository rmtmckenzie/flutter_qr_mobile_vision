import 'dart:async';

import 'package:flutter/services.dart';

class PreviewDetails {
  num width;
  num height;
  num orientation;
  int textureId;

  PreviewDetails(this.width, this.height, this.orientation, this.textureId);
}

class QrMobileVision {
  static const MethodChannel _channel =
      const MethodChannel('com.github.rmtmckenzie/qr_mobile_vision');
  static QrChannelReader channelReader = new QrChannelReader(_channel);

  //Set target size before starting
  static Future<PreviewDetails> start(
    int width,
    int height,
    QRCodeHandler qrCodeHandler,
  ) async {
    channelReader.setQrCodeHandler(qrCodeHandler);
    var details = await _channel.invokeMethod('start',
        {'targetWidth': width, 'targetHeight': height, 'heartbeatTimeout': 0});

    // invokeMethod returns Map<dynamic,...> in dart 2.0
    assert(details is Map<dynamic, dynamic>);
    print("Start response: $details");

    int textureId = details["textureId"];
    num orientation = details["surfaceOrientation"];
    num surfaceHeight = details["surfaceHeight"];
    num surfaceWidth = details["surfaceWidth"];

    return new PreviewDetails(
        surfaceWidth, surfaceHeight, orientation, textureId);
  }

  static Future stop() {
    channelReader.setQrCodeHandler(null);
    return _channel.invokeMethod('stop').catchError(print);
  }

  static Future heartbeat() {
    return _channel.invokeMethod('heartbeat').catchError(print);
  }

  static Future<List<List<int>>> getSupportedSizes() {
    return _channel.invokeMethod('getSupportedSizes').catchError(print);
  }
}

enum FrameRotation { none, ninetyCC, oneeighty, twoseventyCC }

typedef void QRCodeHandler(String qr);

class QrChannelReader {
  QrChannelReader(this.channel) {
    channel.setMethodCallHandler((MethodCall call) async {
      switch (call.method) {
        case 'qrRead':
          if (qrCodeHandler != null) {
            String code = call.arguments;
            qrCodeHandler(code);
          }
          break;
        default:
          print("QrChannelHandler: unknown method call received at ${call
              .method}");
      }
    });
  }

  void setQrCodeHandler(QRCodeHandler qrch) {
    this.qrCodeHandler = qrch;
  }

  MethodChannel channel;
  QRCodeHandler qrCodeHandler;
}
