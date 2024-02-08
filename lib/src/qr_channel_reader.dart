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
          debugPrint("QrChannelHandler: unknown method call received at "
              "${call.method}");
      }
    });
  }

  void setQrCodeHandler(ValueChanged<String?>? qrch) {
    qrCodeHandler = qrch;
  }

  MethodChannel channel;
  ValueChanged<String?>? qrCodeHandler;
}
