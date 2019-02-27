library qr_mobile_vision;

import 'dart:async';
import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:native_device_orientation/native_device_orientation.dart';

part 'src/camera.dart';

part 'src/formats.dart';

part 'src/preview.dart';

part 'src/reader.dart';

class QrMobileVision {
  static const MethodChannel _channel = const MethodChannel('com.github.rmtmckenzie/qr_mobile_vision');
  static QrChannelReader channelReader = new QrChannelReader(_channel);

  //Set target size before starting
  static Future<PreviewDetails> start({
    @required int width,
    @required int height,
    @required QrCodeHandler qrCodeHandler,
    List<BarcodeFormats> formats = _defaultBarcodeFormats,
  }) async {
    final _formats = formats ?? _defaultBarcodeFormats;
    assert(_formats.length > 0);

    List<String> formatStrings = _formats.map((format) => format.toString().split('.')[1]).toList(growable: false);

    channelReader.setQrCodeHandler(qrCodeHandler);
    var details = await _channel.invokeMethod(
        'start', {'targetWidth': width, 'targetHeight': height, 'heartbeatTimeout': 0, 'formats': formatStrings});

    // invokeMethod returns Map<dynamic,...> in dart 2.0
    assert(details is Map<dynamic, dynamic>);

    int textureId = details["textureId"];
    num orientation = details["surfaceOrientation"];
    num surfaceHeight = details["surfaceHeight"];
    num surfaceWidth = details["surfaceWidth"];

    return new PreviewDetails(surfaceWidth, surfaceHeight, orientation, textureId);
  }

  static Future stop() {
    channelReader.setQrCodeHandler(null);
    return _channel.invokeMethod('stop').catchError(print);
  }

  static Future heartbeat() {
    return _channel.invokeMethod('heartbeat').catchError(print);
  }

  static Future<List<Size>> getSupportedSizes(QrCameraDescription description) async {
    List supportedSizeList = await _channel.invokeMethod('getSupportedSizes').catchError(print);

    if (supportedSizeList == null) {
      return null;
    }

    List<List> listOfLists = supportedSizeList.cast();

    return listOfLists.map((List map) {
      List<int> item = map.cast();
      var width = item[0];
      var height = item[1];

      return Size(width.toDouble(), height.toDouble());
    }).toList();
  }

  static Future<List<QrCameraDescription>> getCameras() async {
    List cameraList = await _channel.invokeMethod('getCameras').catchError(print);

    if (cameraList == null) {
      return null;
    }

    List<Map> listOfMaps = cameraList.cast();

    return listOfMaps.map((Map map) {
      Map<String, String> item = map.cast();
      var name = item["name"];
      var orientationString = item["orientation"];

      QrCameraType orientation;
      switch (orientationString) {
        case "forward":
          orientation = QrCameraType.front;
          break;
        case "back":
          orientation = QrCameraType.back;
          break;
        case "external":
          orientation = QrCameraType.external;
          break;
        default:
          orientation = QrCameraType.external;
          break;
      }

      return QrCameraDescription(type: orientation, name: name);
    }).toList();
  }
}

enum FrameRotation { none, ninetyCC, oneeighty, twoseventyCC }
