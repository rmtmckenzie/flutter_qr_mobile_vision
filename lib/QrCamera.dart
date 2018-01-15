import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:qr_mobile_vision/qr_mobile_vision.dart';

class QrCamera extends StatefulWidget {
  QrCamera(
      {this.constraints,
      this.fill,
      this.width,
      this.height,
      this.qrCodeCallback});

  final BoxConstraints constraints;
  final bool fill;
  final num width, height;
  final ValueChanged<String> qrCodeCallback;

  void qrCodeHandler(String string) {
    qrCodeCallback(string);
  }

  @override
  QrCameraState createState() => new QrCameraState();
}

class QrCameraState extends State<QrCamera> {
  QrCameraState();

  double _longSide, _shortSide;

  @override
  initState() {
    super.initState();
    asyncInit();
  }

  Future asyncInit() async {

    await QrMobileVision.start(
        widget.width.toInt(), widget.height.toInt(), widget.qrCodeHandler);
    setState(() {
      _longSide = QrMobileVision.width;
      _shortSide = QrMobileVision.height;
    });
  }

  @override
  deactivate() {
    super.deactivate();
    QrMobileVision.stop();
  }

  @override
  Widget build(BuildContext context) {
    print('Texture Id: ${QrMobileVision.textureId}');

    return new LayoutBuilder(
        builder: (BuildContext context, BoxConstraints constraints) {
      num _targetWidth = constraints.maxWidth,
          _targetHeight = constraints.maxHeight;
      return QrMobileVision.textureId == null
          ? new Text("Camera Loading ...")
          : new Preview(
              _shortSide,
              _longSide,
              _targetWidth.toDouble(),
              _targetHeight.toDouble(),
            );
    });
  }
}

class Preview extends StatelessWidget {
  final double shortSide, longSide;
  final double targetWidth, targetHeight;

  Preview(this.shortSide, this.longSide, this.targetWidth, this.targetHeight);

  @override
  Widget build(BuildContext context) {
    double frameHeight;
    double frameWidth;
    double drawnTextureWidth;
    double drawnTextureHeight;
    double scale;
    bool rotated =
        (QrMobileVision.orientation == 90 || QrMobileVision.orientation == 270);
    //We are assuming that the sensor is oriented lengthways same as phone
    if (!rotated) {
      frameHeight = longSide;
      frameWidth = shortSide;
    } else {
      frameHeight = shortSide;
      frameWidth = longSide;
    }

    double targetRatio = targetWidth / targetHeight;
    double frameRatio = frameWidth / frameHeight;

    if (targetRatio < frameRatio) {
      drawnTextureWidth = targetWidth;
      drawnTextureHeight = targetWidth / frameRatio;
      scale = (rotated ? targetWidth : targetHeight) / drawnTextureHeight;
    } else {
      drawnTextureHeight = targetHeight;
      drawnTextureWidth = targetHeight * frameRatio;
      scale = (rotated ? targetHeight : targetWidth) / drawnTextureWidth;
    }

    print("Rotated: $rotated orientatin: ${QrMobileVision.orientation}\n" +
        "Long: $longSide, Short: $shortSide\n" +
        "Target Width: $targetWidth, Target Height: $targetHeight Target Ratio: $targetRatio\n" +
        "Frame Width: $frameWidth, Frame Height: $frameHeight, Frame Ratio: $frameRatio\n" +
        "Drawn Width: $drawnTextureWidth, Drawn Height: $drawnTextureHeight, Scale: $scale");

    return new Container(
      width: targetWidth,
      height: targetHeight,
      child: new ClipRect(
        child: new Transform(
          alignment: FractionalOffset.center,
          transform: new Matrix4.identity()..scale(scale, scale),
          child: new Transform.rotate(
            angle: 0.0, //(QrMobileVision.orientation / 180.0) * PI,
            child: new OverflowBox(
              maxHeight: drawnTextureHeight,
              maxWidth: drawnTextureWidth,
              minHeight: drawnTextureHeight,
              minWidth: drawnTextureWidth,
              child: new Texture(textureId: QrMobileVision.textureId),
            ),
          ),
        ),
      ),
    );
  }
}
