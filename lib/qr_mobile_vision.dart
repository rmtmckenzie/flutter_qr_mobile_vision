import 'dart:async';
import 'package:flutter/services.dart';
import 'dart:typed_data';

class QrMobileVision {
  static int _textureId;
  static num _orientation;
  static List _sizes;
  static double _height;
  static double _width;

  static int get textureId => _textureId;
  static num get orientation => _orientation;
  static List get sizes => _sizes;
  static double get height => _height;
  static double get width => _width;

  
  static const MethodChannel _channel =
      const MethodChannel('com.github.rmtmckenzie/qr_mobile_vision');
  static QrChannelReader channelReader = new QrChannelReader(_channel);


  //Set target size before starting
  static Future<Null> start(int width, int height, QRCodeHandler qrCodeHandler,
      ) async{
    channelReader.setQrCodeHandler(qrCodeHandler);
    await _channel.invokeMethod('setTarget',{'width': width,'height': height}).catchError(print);
    _textureId = await _channel.invokeMethod('start', {
      'heartbeatTimeout': 0
    }).catchError(print);
    _orientation = (await _channel.invokeMethod('getOrientation',{}).catchError(print)).toDouble();
    List<num> size = (await _channel.invokeMethod('getSize',{}).catchError(print));
    _width = size[0].toDouble();
    _height = size[1].toDouble();
  }

//  static Future<Null> setTargetSize(int width, int height){
//    return _channel.invokeMethod('setTarget',{'width': width,'height': height}).catchError(print);
//  }

   static Future<Null> stop() {
    channelReader.setQrCodeHandler(null);
    _textureId = null;
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
