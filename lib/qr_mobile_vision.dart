library qr_mobile_vision;

import 'dart:async';
import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';
import 'package:native_device_orientation/native_device_orientation.dart';
import 'package:qr_mobile_vision/qr_camera.dart';

part 'src/camera.dart';

part 'src/formats.dart';

part 'src/preview.dart';

part 'src/reader.dart';

class QrMobileVision {
  static const MethodChannel _channel = const MethodChannel('com.github.rmtmckenzie/flutter_qr_mobile_vision');
  static QrChannelReader channelReader = new QrChannelReader(_channel);

//
//  //Set target size before starting
//  static Future<PreviewDetails> start({
//    @required int width,
//    @required int height,
//    @required QrCodeHandler qrCodeHandler,
//    List<BarcodeFormats> formats = _defaultBarcodeFormats,
//  }) async {
//    final _formats = formats ?? _defaultBarcodeFormats;
//    assert(_formats.length > 0);
//
//    List<String> formatStrings = _formats.map((format) => format.toString().split('.')[1]).toList(growable: false);
//
//    channelReader.setQrCodeHandler(qrCodeHandler);
//    var details = await _channel.invokeMethod('start', {'targetWidth': width, 'targetHeight': height, 'heartbeatTimeout': 0, 'formats': formatStrings});
//
//    // invokeMethod returns Map<dynamic,...> in dart 2.0
//    assert(details is Map<dynamic, dynamic>);
//
//    int textureId = details["textureId"];
//    num orientation = details["surfaceOrientation"];
//    num surfaceHeight = details["surfaceHeight"];
//    num surfaceWidth = details["surfaceWidth"];
//
//    return new PreviewDetails(surfaceWidth, surfaceHeight, orientation, textureId);
//  }
//
//  static Future stop() {
//    channelReader.setQrCodeHandler(null);
//    return _channel.invokeMethod('stop').catchError(print);
//  }
//
//  static Future heartbeat() {
//    return _channel.invokeMethod('heartbeat').catchError(print);
//  }

  static Future<List<QrCameraResolution>> getResolutions(QrCameraDescription description) async {
    List supportedSizeList;
    try {
      supportedSizeList = await _channel.invokeMethod(
        'getResolutions',
        {
          'cameraName': description.name,
        },
      );
    } on PlatformException catch (e) {
      throw QrCameraException(e.code, e.message);
    }

    List<List> listOfLists = supportedSizeList.cast();

    return listOfLists.map((List map) {
      List<int> item = map.cast();
      var width = item[0];
      var height = item[1];

      return QrCameraResolution(width: width, height: height);
    }).toList();
  }

  static Future<List<QrCameraDescription>> getCameras() async {
    List cameraList;

    try {
      cameraList = await _channel.invokeMethod('getCameras');
    } on PlatformException catch (e) {
      throw QrCameraException(e.code, e.message);
    }

    List<Map> listOfMaps = cameraList.cast();

    return listOfMaps.map((Map map) {
      Map<String, String> item = map.cast();
      var name = item["name"];
      var typeString = item["type"];

      return QrCameraDescription(type: deserializeQrCameraType(typeString), name: name);
    }).toList();
  }

  //TODO
  static Future<QrInitializeResponse> initialize({
    @required QrCameraDescription description,
    @required QrCameraResolution resolution,
    List<BarcodeFormats> formats,
  }) async {
    final _formats = formats ?? _defaultBarcodeFormats;
    assert(_formats.length > 0);

    List<String> formatStrings = _formats.map((format) => format.toString().split('.')[1]).toList(growable: false);
    var details = await _channel.invokeMethod('start', {
      'cameraName': description.name,
      'targetWidth': resolution.width,
      'targetHeight': resolution.height,
      'formats': formatStrings,
    });

    // invokeMethod returns Map<dynamic,...> in dart 2.0
    assert(details is Map<dynamic, dynamic>);

    int textureId = details["textureId"];
    String rotationString = details["rotation"];
    int height = details["height"];
    int width = details["width"];

    var rotation = deserializeFrameRotation(rotationString);

    return QrInitializeResponse(
      textureId: textureId,
      width: width,
      height: height,
      rotation: rotation,
    );
  }

  static Stream stream(int textureId) {
    return EventChannel('com.github.rmtmckenzie/flutter_qr_mobile_vision/cameraEvents$textureId').receiveBroadcastStream();
  }

  static Future<void> dispose(int textureId) async {
    await _channel.invokeMethod(
      'dispose',
      {'textureId': textureId},
    );
    return null;
  }
}

class QrInitializeResponse {
  final int textureId;
  final int height;
  final int width;
  final FrameRotation rotation;

  QrInitializeResponse({
    @required this.textureId,
    @required this.height,
    @required this.width,
    @required this.rotation,
  });
}
