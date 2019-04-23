library qr_camera;

import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';

import 'qr_mobile_vision.dart';

part 'src/controller.dart';
part 'src/orientation.dart';

final WidgetBuilder _defaultNotStartedBuilder = (context) => new Text("Camera Loading ...");
final WidgetBuilder _defaultOffscreenBuilder = (context) => new Text("Camera Paused.");
final ErrorCallback _defaultOnError = (BuildContext context, Object error) {
  print("Error reading from camera: $error");
  return new Text("Error reading from camera...");
};

typedef Widget ErrorCallback(BuildContext context, Object error);

Future<List<QrCameraResolution>> getQrPreviewSizes(QrCameraDescription description) {
  return QrMobileVision.getResolutions(description);
}

Future<List<QrCameraDescription>> getQrCameras() {
  return QrMobileVision.getCameras();
}

class QrCameraException implements Exception {
  QrCameraException(this.code, this.description);

  final String code;
  final String description;

  @override
  String toString() => '$runtimeType($code, $description)';
}

class _AutoQrCameraStopping extends StatefulWidget {
  final QrCameraController controller;
  final Widget child;

  const _AutoQrCameraStopping({Key key, this.controller, this.child}) : super(key: key);

  @override
  _AutoQrCameraStoppingState createState() => _AutoQrCameraStoppingState();
}

class _AutoQrCameraStoppingState extends State<_AutoQrCameraStopping> with WidgetsBindingObserver {
  @override
  Widget build(BuildContext context) {
    return widget.child;
  }

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
  }

  @override
  dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  bool shouldRestart = false;

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
//    if (state == AppLifecycleState.resumed && shouldRestart) {
//      shouldRestart = false;
//      widget.controller.start();
//    } else {
//      if (widget.controller.started) {
//        shouldRestart = true;
//        widget.controller.stop();
//      }
//    }
  }
}

class QrCamera extends StatelessWidget {
  QrCamera({
    @required this.controller,
    this.autoStopping = true,
    WidgetBuilder notStartedBuilder,
  }) : notStartedBuilder = notStartedBuilder ?? _defaultNotStartedBuilder;

  final bool autoStopping;
  final QrCameraController controller;
  final WidgetBuilder notStartedBuilder;

  @override
  Widget build(BuildContext context) {
    if (controller == null || !controller.value.isInitialized) {
      return notStartedBuilder(context);
    }
//    if (!controller.value.isCapturing) {
//      return offscreenBuilder(context);
//    }

    return Texture(textureId: controller._textureId);
  }
}
