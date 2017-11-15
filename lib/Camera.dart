import 'dart:async';
import 'dart:typed_data';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:qr_mobile_vision/qr_mobile_vision.dart';
import 'dart:math';

class Camera extends StatefulWidget {
  Camera(
      {this.constraints,
      this.fill,
      this.width,
      this.height,
      this.qrCodeCallback});

  final BoxConstraints constraints;
  final bool fill;
  final double width, height;
  final ValueChanged<String> qrCodeCallback;

  void qrCodeHandler(String string) {
    qrCodeCallback(string);
  }

  @override
  CameraState createState() => new CameraState(
      constraints: constraints, fill: fill, width: width, height: height);
}

class CameraState extends State<Camera> {
  CameraState({this.constraints, this.fill, this.width, this.height});

  Uint8List bytes;
  BoxConstraints constraints;
  bool fill;
  double width, height;
  double oWidth, oHeight;

  Future cameraFrameHandler(Uint8List data, int rotation) async {
    setState(() {
      if (data != null) {
        this.bytes = data;
      }
    });
  }

  @override
  initState() {
    super.initState();

    if (fill == null) {
      fill = false;
    }
    if (fill) {
      oWidth = width;
      oHeight = height;
      if (constraints != null &&
          constraints.maxWidth != double.INFINITY &&
          width == null) oWidth = constraints.maxWidth;
      if (constraints != null &&
          constraints.maxHeight != double.INFINITY &&
          height == null) oHeight = constraints.maxHeight;
    }

    if (constraints != null &&
        constraints.maxWidth != double.INFINITY &&
        width == null)
      width = constraints.maxWidth;
    else if (width == null) width = 2000.0;
    if (constraints != null &&
        constraints.maxHeight != double.INFINITY &&
        height == null)
      height = constraints.maxHeight;
    else if (height == null) height = 2000.0;

    print("$width and $height");
  }

  @override
  deactivate() {
    super.deactivate();
    QrMobileVision.stop();
  }

  @override
  Widget build(BuildContext context) {
    print('Texture Id: ${QrMobileVision.textureId}');
    return QrMobileVision.textureId != null
        ? new Transform.rotate(
            angle: (QrMobileVision.orientation / (360) ) * 2 * PI,
            child: new Texture(textureId: QrMobileVision.textureId))
        : () {
            QrMobileVision
                .start(widget.qrCodeHandler, cameraFrameHandler, height.toInt(),
                    width.toInt(), fill)
                .then((n) => setState(() {}));
            return new Text("Camera Loading...");
          }();
//    if (bytes != null) {
//      return new Image.memory(
//        bytes,
//        width: oWidth,
//        height: oHeight,
//        fit: BoxFit.cover,
//        gaplessPlayback: true,
//      );
//    } else
//      return new Text("Camera Loading...");
  }
}
