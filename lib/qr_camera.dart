import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:meta/meta.dart';
import 'package:qr_mobile_vision/qr_mobile_vision.dart';

class QrCamera extends StatefulWidget {
  QrCamera({this.constraints,
    this.fill,
    this.qrCodeCallback});

  final BoxConstraints constraints;
  final bool fill;
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
  int _textureId;
  int _orientation;
  bool isStarted = false;

  @override
  initState() {
    super.initState();
  }

  Future asyncInitOnce(num width, num height) async {
    if (isStarted) return;
    isStarted = true;

    print("Camera starting, width: $width, height: $height");
    var previewDetails = await QrMobileVision.start(
        width: width.toInt(), height: height.toInt(), qrCodeHandler: widget.qrCodeHandler);
    print("Camera started, width: ${previewDetails
        .width}, height: ${previewDetails.height}, textureid: ${previewDetails
        .textureId}, orientation: ${previewDetails.orientation}");
    setState(() {
      _longSide = previewDetails.width.toDouble();
      _shortSide = previewDetails.height.toDouble();
      _textureId = previewDetails.textureId;
      _orientation = previewDetails.orientation.toInt();
    });
  }

  @override
  deactivate() {
    super.deactivate();
    print("Stopping");
    QrMobileVision.stop();
  }

  @override
  Widget build(BuildContext context) {
    print('Texture Id: $_textureId');

    return new LayoutBuilder(
        builder: (BuildContext context, BoxConstraints constraints) {
          asyncInitOnce(constraints.maxWidth, constraints.maxHeight);

          num _targetWidth = constraints.maxWidth,
              _targetHeight = constraints.maxHeight;
          return _textureId == null
              ? new Text("Camera Loading ...")
              : new Preview(
            textureId: _textureId,
            orientation: _orientation,
            shortSide: _shortSide,
            longSide: _longSide,
            targetWidth: _targetWidth.toDouble(),
            targetHeight: _targetHeight.toDouble(),
          );
        });
  }
}

class Preview extends StatelessWidget {
  final double shortSide, longSide;
  final double targetWidth, targetHeight;
  final int textureId;
  final int orientation;

  Preview({
    @required this.textureId,
    @required this.orientation,
    @required this.shortSide,
    @required this.longSide,
    @required this.targetWidth,
    @required this.targetHeight,
  });

  @override
  Widget build(BuildContext context) {
    double frameHeight;
    double frameWidth;
    double drawnTextureWidth;
    double drawnTextureHeight;
    double scale;

    bool rotated = (orientation == 90 || orientation == 270);
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

    print("Rotated: $rotated orientation: $orientation\n" +
        "Long: $longSide, Short: $shortSide\n" +
        "Target Width: $targetWidth, Target Height: $targetHeight Target Ratio: $targetRatio\n" +
        "Frame Width: $frameWidth, Frame Height: $frameHeight, Frame Ratio: $frameRatio\n" +
        "Drawn Width: $drawnTextureWidth, Drawn Height: $drawnTextureHeight, Scale: $scale");

//    // in progress - android
//    return new Container(
//      width: targetWidth,
//      height: targetHeight,
//      child: new ClipRect(
//          child: new Transform(
//              alignment: FractionalOffset.center,
//              transform: new Matrix4.identity()
//                ..scale(scale, scale),
//              child: new OverflowBox(
//                maxHeight: drawnTextureWidth,
//                maxWidth: drawnTextureHeight,
//                minHeight: drawnTextureWidth,
//                minWidth: drawnTextureHeight,
//                child: new Texture(textureId: textureId),
//              )
//
//          )
//      ),
//    );

    return new Container(
      width: targetWidth,
      height: targetHeight,
//      child: new Texture(textureId: textureId,)
      child: new ClipRect(
        child: new Transform(
          alignment: FractionalOffset.center,
          transform: new Matrix4.identity()..scale(scale, scale),
          child: new Transform.rotate(
            // TODO: implement; orientation disabled for now
            angle: 0.0, //(QrMobileVision.orientation / 180.0) * PI,
            child: new OverflowBox(
              maxHeight: drawnTextureHeight,
              maxWidth: drawnTextureWidth,
              minHeight: drawnTextureHeight,
              minWidth: drawnTextureWidth,
              child: new Texture(textureId: textureId),
            ),
          ),
        ),
      ),
    );
  }
}
