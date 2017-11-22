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
  static const num target = 300;

  double _w, _h;

  @override
  initState() {
    super.initState();
  }

  void _getSize(List<List<num>> sizes) {
    List<num> size;
    List<num> save;
    for (List<num> s in sizes) {
      if (s[0] < target || s[1] < target) {
        size = save ?? s;
        break;
      }
      save = s;
    }

    _w = size[0].toDouble();
    _h = size[1].toDouble();
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
        ? new Preview(_h, _w, target.toDouble())
        : () {
            QrMobileVision.start(widget.qrCodeHandler).then((n) => setState(() {
                  _getSize(QrMobileVision.sizes);
                }));
            return new Text("Camera Loading...");
          }();
  }
}

class Preview extends StatelessWidget {
  final double height;
  final double width;
  final double target;
  Preview(this.height, this.width, this.target);
  @override
  Widget build(BuildContext context) {
    print("Width: $width, Height: $height, Target: $target");
    return new Container(decoration: new BoxDecoration(border: new Border.all(color: Colors.red)) ,child: new Stack(
      children: <Widget>[
        new Positioned(
          //top: 0.0,
            //bottom: 0.0,
            child: new Container(
          child: new Transform.rotate(
              angle: (QrMobileVision.orientation / (360)) * 2 * PI,
              child: new SizedBox(child: new Texture(textureId: QrMobileVision.textureId),height: height, width: width,)),
          //constraints: new BoxConstraints.tight(new Size(width, height)),
        )),
        new Center(
          child: new Container(
              height: height,
              width: width,
              decoration: new BoxDecoration(
                border: new Border(
                    top: new BorderSide(
                        color: Colors.white, width: (width - target) / 2),
                    bottom: new BorderSide(
                        color: Colors.white, width: (width - target) / 2),
                    right: new BorderSide(
                        color: Colors.white, width: (height - target) / 2),
                    left: new BorderSide(
                        color: Colors.white, width: (height - target) / 2)),
              ),
              constraints: new BoxConstraints.tight(new Size(height, width))),
        )
      ],
    ));
  }
}
