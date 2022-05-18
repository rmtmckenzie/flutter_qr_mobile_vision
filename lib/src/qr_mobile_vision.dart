import 'dart:async';
import 'dart:io';

import 'package:device_info_plus/device_info_plus.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:qr_mobile_vision/src/barcode_formats.dart';
import 'package:qr_mobile_vision/src/camera_direction.dart';
import 'package:qr_mobile_vision/src/preview_details.dart';
import 'package:qr_mobile_vision/src/qr_channel_reader.dart';

class QrMobileVision {
  static const MethodChannel _channel = const MethodChannel('com.github.rmtmckenzie/qr_mobile_vision');
  static QrChannelReader channelReader = QrChannelReader(_channel);

  //Set target size before starting
  static Future<PreviewDetails> start({
    required int width,
    required int height,
    required ValueChanged<String?> qrCodeHandler,
    CameraDirection cameraDirection = CameraDirection.BACK,
    List<BarcodeFormats>? formats = defaultBarcodeFormats,
  }) async {
    final _formats = formats ?? defaultBarcodeFormats;
    assert(_formats.length > 0);

    List<String> formatStrings = _formats.map((format) => format.toString().split('.')[1]).toList(growable: false);

    final deviceInfoFut = Platform.isAndroid ? DeviceInfoPlugin().androidInfo : Future.value(null);

    channelReader.setQrCodeHandler(qrCodeHandler);
    final details = (await _channel.invokeMapMethod<String, dynamic>('start', {
      'targetWidth': width,
      'targetHeight': height,
      'heartbeatTimeout': 0,
      'cameraDirection': (cameraDirection == CameraDirection.FRONT ? 0 : 1),
      'formats': formatStrings,
    }))!;

    int? textureId = details["textureId"];
    num? orientation = details["surfaceOrientation"];
    num surfaceHeight = details["surfaceHeight"];
    num surfaceWidth = details["surfaceWidth"];

    final deets = await NativePreviewDetails(surfaceWidth, surfaceHeight, orientation, textureId);
    final devInfo = await deviceInfoFut;

    return PreviewDetails(deets, devInfo?.version.sdkInt ?? -1);
  }

  static Future toggleFlash() {
    return _channel.invokeMethod('toggleFlash').catchError(print);
  }

  static Future stop() {
    channelReader.setQrCodeHandler(null);
    return _channel.invokeMethod('stop').catchError(print);
  }

  static Future heartbeat() {
    return _channel.invokeMethod('heartbeat').catchError(print);
  }

  static Future<List<List<int>>?> getSupportedSizes() {
    return _channel.invokeMethod('getSupportedSizes').catchError(print) as Future<List<List<int>>?>;
  }
}
