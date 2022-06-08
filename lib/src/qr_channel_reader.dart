import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'dart:typed_data';

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
        case 'qrReadBytes':
          if (qrCodeHandlerBytes != null) {
            assert(call.arguments is Uint8List);
            qrCodeHandlerBytes!(call.arguments);
          }
          break;
        default:
          print("QrChannelHandler: unknown method call received at "
              "${call.method}");
      }
    });
  }

  void setQrCodeHandler(ValueChanged<String?>? qrch, ValueChanged<Uint8List?>? qrchb) {
    this.qrCodeHandler = qrch;
    this.qrCodeHandlerBytes = qrchb;
  }

  MethodChannel channel;
  ValueChanged<String?>? qrCodeHandler;
  ValueChanged<Uint8List?>? qrCodeHandlerBytes;
}
