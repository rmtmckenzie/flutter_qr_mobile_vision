import 'dart:async';
import 'package:flutter/services.dart';
import 'dart:typed_data';

class QrMobileVision {
  static const MethodChannel _channel =
      const MethodChannel('com.github.rmtmckenzie/qr_mobile_vision');
  static QrChannelReader channelReader = new QrChannelReader(_channel);

  static Future<Null> start(QRCodeHandler qrCodeHandler,
      CameraFrameHandler cameraFrameHandler, int height, int width, bool fill) {
    channelReader.setCameraFrameHandler(cameraFrameHandler);
    channelReader.setQrCodeHandler(qrCodeHandler);
    return _channel.invokeMethod('start', {
      'width': width,
      'height': height,
      'fill': fill,
      'heartbeatTimeout': 0
    }).catchError(print);
  }

  static Future<Null> stop() {
    channelReader.setCameraFrameHandler(null);
    channelReader.setQrCodeHandler(null);
    return _channel.invokeMethod('stop').catchError(print);
  }

  static Future<Null> heartbeat() {
    return _channel.invokeMethod('heartbeat').catchError(print);
  }

  static Future<List<List<int>>> getSupportedSizes() {
    return _channel.invokeMethod('getSupportedSizes').catchError(print);
  }
}

enum FrameRotation { none, ninetyCC, oneeighty, twoseventyCC }

typedef void CameraFrameHandler(Uint8List data, int rotation);

typedef void QRCodeHandler(String qr);

class QrChannelReader {
  QrChannelReader(this.channel) {
    channel.setMethodCallHandler((MethodCall call) async {
      switch (call.method) {
        case 'cameraFrame':
          if (cameraFrameHandler != null) {
            Uint8List frame = call.arguments[0];
            int rawRotation = call.arguments[1];

            cameraFrameHandler(frame, rawRotation);
          }
          break;
        case 'qrRead':
          if (qrCodeHandler != null) {
            String code = call.arguments;
            qrCodeHandler(code);
          }
          break;
      }
    });
  }

  void setCameraFrameHandler(CameraFrameHandler cfh) {
    this.cameraFrameHandler = cfh;
  }

  void setQrCodeHandler(QRCodeHandler qrch) {
    this.qrCodeHandler = qrch;
  }

  MethodChannel channel;
  CameraFrameHandler cameraFrameHandler;
  QRCodeHandler qrCodeHandler;
}
