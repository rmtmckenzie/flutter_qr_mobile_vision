import 'dart:async';
import 'dart:typed_data';
import 'dart:math';

import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:qr_mobile_vision/qr_mobile_vision.dart';
import 'dart:ui' as Ui;

void main() {
  debugPaintSizeEnabled = true;
  runApp(new MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => new _MyAppState();
}

class _MyAppState extends State<MyApp> {


  @override
  initState() {
    super.initState();
  }


  @override
  Widget build(BuildContext context) {
    return new MaterialApp(
      home: new Scaffold(
          appBar: new AppBar(
            title: new Text('Plugin example app'),
          ),
          body: new Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: <Widget>[
              new Center(
                child: new Camera()
              ),
              new Text("sample"),
            ],
          )),
    );
  }
}
class Camera extends StatefulWidget{
  Camera();


  @override
  CameraState createState() => new CameraState();
}

class CameraState extends State<Camera>{
  CameraState();

  Uint8List bytes;
  int rotation;


  void qrCodeHandler(String string) {
    print("QR CODE RECIEVED: $string");
  }

  Future cameraFrameHandler(Uint8List data, int rotation) async {
    setState(() {
      if(rotation!=null){
        this.rotation = rotation;
      }
      if (data != null) {
        this.bytes = data;
      }
    });
  }

  @override
  initState(){
    super.initState();
    QrMobileVision.start(qrCodeHandler, cameraFrameHandler, 200, 100);
  }

  @override
  Widget build(BuildContext context){

    print("Break it up\n\n\n\n");

    context.visitAncestorElements((visitor){
      print(visitor.size);
      return false;
    });

    if(bytes!=null){
      return new Transform.rotate(
        angle: rotation*PI/2,
        child: new Image.memory(
          bytes,
          width: 300.0,
          height: 300.0,
          fit: BoxFit.cover,
          gaplessPlayback: true,
        ),
      );
    }
    else return new Text("Loading...");

  }
}
