import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

class QrChannelReader {
  QrChannelReader(this.channel) {
    channel.setMethodCallHandler((MethodCall call) async {
      switch (call.method) {
        case 'qrRead':
          if (qrCodeHandler != null) {
            assert(call.arguments is String);
            qrCodeHandler!(call.arguments);
          }
          break;
        default:
          print("QrChannelHandler: unknown method call received at "
              "${call.method}");
      }
    });
  }

  void setQrCodeHandler(ValueChanged<String?>? qrch) {
    this.qrCodeHandler = qrch;
  }

  MethodChannel channel;
  ValueChanged<String?>? qrCodeHandler;
}
