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
  CameraState createState() => new CameraState();
}

class CameraState extends State<Camera> {
  CameraState();
  static const num target = 350;

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
        ? new Preview(_shortSide, _longSide, target.toDouble())
        : () {
            QrMobileVision.setTarget(target).then((n) => QrMobileVision
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
  final double shortSide;
  final double longSide;
  final double target;
  Preview(this.shortSide, this.longSide, this.target);

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

    double height = target * frameHeight / frameWidth;
    double width = target;

    double scale = target/width;

    print("Long: $longSide, Short: $shortSide, Target: $target");
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
