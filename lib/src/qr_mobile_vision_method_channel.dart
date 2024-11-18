import 'dart:io';

import 'package:device_info_plus/device_info_plus.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:qr_mobile_vision/src/barcode_data.dart';
import 'package:qr_mobile_vision/src/barcode_formats.dart';
import 'package:qr_mobile_vision/src/camera_direction.dart';
import 'package:qr_mobile_vision/src/preview_details.dart';
import 'package:qr_mobile_vision/src/qr_channel_reader.dart';
import 'package:qr_mobile_vision/src/qr_mobile_vision_platform_interface.dart';

/// An implementation of [QrMobileVisionPlatform] that uses method channels.
class MethodChannelQrMobileVision extends QrMobileVisionPlatform {
  @visibleForTesting
  final methodChannel = const MethodChannel('qr_mobile_vision');
  late final QrChannelReader channelReader;
  MethodChannelQrMobileVision() {
    channelReader = QrChannelReader(methodChannel);
  }

  @override
  Future<PreviewDetails> start({
    required int width,
    required int height,
    required ValueChanged<BarcodeData> qrCodeHandler,
    CameraDirection cameraDirection = CameraDirection.BACK,
    List<BarcodeFormats>? formats = defaultBarcodeFormats,
  }) async {
    assert(formats == null || formats.isNotEmpty);
    final formatsOrDefault = formats ?? defaultBarcodeFormats;

    List<String> formatStrings =
        formatsOrDefault.map((format) => format.toString().split('.')[1]).toList(growable: false);

    final deviceInfoFut = Platform.isAndroid ? DeviceInfoPlugin().androidInfo : Future.value(null);

    channelReader.setQrCodeHandler(qrCodeHandler);
    final details = (await methodChannel.invokeMapMethod<String, dynamic>('start', {
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

    final deets = NativePreviewDetails(surfaceWidth, surfaceHeight, orientation, textureId);
    final devInfo = await deviceInfoFut;

    return PreviewDetails(deets, devInfo?.version.sdkInt ?? -1);
  }

  @override
  Future toggleFlash() {
    return methodChannel.invokeMethod('toggleFlash').catchError(_printError);
  }

  @override
  Future stop() {
    channelReader.setQrCodeHandler(null);
    return methodChannel.invokeMethod('stop').catchError(_printError);
  }

  @override
  Future heartbeat() {
    return methodChannel.invokeMethod('heartbeat').catchError(_printError);
  }

  void _printError(dynamic error, StackTrace stackTrace) {
    debugPrint("QR Mobile Vision received error: $error");
    debugPrintStack(stackTrace: stackTrace);
  }
}
