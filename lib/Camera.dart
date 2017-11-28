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
  final num width, height;
  final ValueChanged<String> qrCodeCallback;

  void qrCodeHandler(String string) {
    qrCodeCallback(string);
  }

  @override
  CameraState createState() => new CameraState(width,height);
}

class CameraState extends State<Camera> {
  CameraState(this._targetWidth,this._targetHeight);
  final num _targetWidth , _targetHeight;

  double _longSide, _shortSide;

  @override
  initState() {
    super.initState();
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
        ? new Preview(_shortSide, _longSide, _targetWidth.toDouble(),_targetHeight.toDouble())
        : () {
            QrMobileVision.setTarget(_targetWidth.toInt(),_targetHeight.toInt()).then((n) => QrMobileVision
                .start(widget.qrCodeHandler)
                .then((n) => setState(() {
                      _longSide = QrMobileVision.width;
                      _shortSide = QrMobileVision.height;
                    })));
            return new Text("Camera Loading...");
          }();
  }
}

class Preview extends StatelessWidget {
  final double shortSide, longSide;
  final double targetWidth,targetHeight;
  Preview(this.shortSide, this.longSide, this.targetWidth, this.targetHeight);

  @override
  Widget build(BuildContext context) {
    double frameHeight;
    double frameWidth;
    if(QrMobileVision.orientation == 0 || QrMobileVision.orientation == 180){
      frameHeight = longSide;
      frameWidth = shortSide;
    }
    else{
      frameHeight = shortSide;
      frameWidth = longSide;
    }

    double height = targetWidth * frameHeight / frameWidth;
    double width = targetWidth;

    double scale = targetWidth/width;

    print("Long: $longSide, Short: $shortSide, Target Width: $targetWidth, Target Height: $targetHeight");
    return new Center(child: new Container(
      //constraints: new BoxConstraints.tight(new Size(shortSide,longSide)),
      child: new Transform(
        alignment: FractionalOffset.center,
        transform: new Matrix4.identity()..scale(scale,scale),
        child: new Transform.rotate(
            angle: (QrMobileVision.orientation / (360)) * 2 * PI,
            child: new SizedBox(
              child: new Texture(textureId: QrMobileVision.textureId),
              height: height,
              width: width,
            )),
      ),
    ));


  }
}
