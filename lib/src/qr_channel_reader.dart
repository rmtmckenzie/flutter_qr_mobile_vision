import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:qr_mobile_vision/qr_camera.dart';

class QrChannelReader {
  QrChannelReader(this.channel) {
    channel.setMethodCallHandler((MethodCall call) async {
      switch (call.method) {
        case 'qrRead':
          if (qrCodeHandler != null) {
            assert(call.arguments is List);
            assert((call.arguments as List).length ==2);
            qrCodeHandler!(call.arguments[0],BarcodeFormat.fromString(call.arguments[1]) );
          }
          break;
        default:
          debugPrint("QrChannelHandler: unknown method call received at "
              "${call.method}");
      }
    });
  }

  void setQrCodeHandler(void Function(String?, BarcodeFormats?)? handler) {
    qrCodeHandler = handler;
  }

  MethodChannel channel;
  void Function(String?, BarcodeFormats?)? qrCodeHandler;
}
