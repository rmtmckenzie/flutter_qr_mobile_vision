import 'dart:async';

import 'package:flutter/services.dart';

class QrMobileVision {
  static const MethodChannel _channel =
      const MethodChannel('com.github.rmtmckenzie/qr_mobile_vision');

  static Future<String> get platformVersion =>
      _channel.invokeMethod('getPlatformVersion');
}
