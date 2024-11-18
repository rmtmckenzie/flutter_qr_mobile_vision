import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:qr_mobile_vision/src/barcode_data.dart';

class QrChannelReader {
  QrChannelReader(this.channel) {
    channel.setMethodCallHandler((MethodCall call) async {
      switch (call.method) {
        case 'qrRead':
          if (qrCodeHandler != null) {
            final data = call.arguments;
            assert(data is Map);
            qrCodeHandler!(BarcodeData.fromNative(data));
          }
          break;
        default:
          debugPrint("QrChannelHandler: unknown method call received at "
              "${call.method}");
      }
    });
  }

  void setQrCodeHandler(ValueChanged<BarcodeData>? qrch) {
    qrCodeHandler = qrch;
  }

  MethodChannel channel;
  ValueChanged<BarcodeData>? qrCodeHandler;
}
