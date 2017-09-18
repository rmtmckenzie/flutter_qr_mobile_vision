import 'dart:async';
import 'dart:typed_data';
import 'dart:math';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:qr_mobile_vision/qr_mobile_vision.dart';
import 'dart:ui' as Ui;

void main() {
  runApp(new MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => new _MyAppState();
}

class _MyAppState extends State<MyApp> {

  Ui.Image image;
  FrameRotation imageRotation;

  @override
  initState() {
    super.initState();
    QrMobileVision.start(qrCodeHandler, cameraFrameHandler, 200, 100);
  }

  void qrCodeHandler(String string){
    print("QR CODE RECIEVED: $string");
  }

  Future cameraFrameHandler(Uint8List data, FrameRotation rotation) async {
    Future<Ui.Image> convertBytes(Uint8List data) {
      Completer<Ui.Image> completer = new Completer<Ui.Image>();
      Ui.decodeImageFromList(data, completer.complete);
      return completer.future;
    }

    Ui.Image image = await convertBytes(data);
    setState((){
      this.image = image;
      this.imageRotation = rotation;
    });

  }



  @override
  Widget build(BuildContext context) {
    return new MaterialApp(
      home: new Scaffold(
        appBar: new AppBar(
          title: new Text('Plugin example app'),
        ),
        body: new Center(
          child: new CustomPaint(
            foregroundPainter: new ImagePainter(image, imageRotation),
            size: Size.infinite,
          ),
        ),
      ),
    );
  }
}


class ImagePainter extends CustomPainter{
  ImagePainter(this.image, this.rotation);

  Ui.Image image;
  FrameRotation rotation;

  @override
  void paint(Canvas canvas,Size size){

    switch (rotation) {
      case FrameRotation.none:
        break;
      case FrameRotation.ninetyCC:
        canvas.scale(2.0, 2.0);
        canvas.translate(image.height.toDouble() - 1, 0.0);
        canvas.rotate(.5 * PI);
        break;
      case FrameRotation.oneeighty:
        canvas.rotate(PI);
        break;
      case FrameRotation.twoseventyCC:
        canvas.rotate(-.5 * PI);
        break;
    }

    if(image != null) {
      canvas.drawImage(image, Offset.zero, new Paint());
    }
  }

  @override
  bool shouldRepaint(ImagePainter old)=>image!=old.image;
}

