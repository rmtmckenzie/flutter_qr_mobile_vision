import 'dart:async';
import 'dart:typed_data';
import 'dart:math';

import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:qr_mobile_vision/qr_mobile_vision.dart';
import 'dart:ui' as Ui;

void main() {
  debugPaintSizeEnabled = false;
  runApp(new HomePage());
}

class HomePage extends StatefulWidget{
  @override
  HomeState createState() => new HomeState();
}

class HomeState extends State<HomePage>{

  @override
  Widget build(BuildContext){
    return new MaterialApp(
      home: new MyApp()
    );
  }
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => new _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String qr;
  bool camState=false;

  @override
  initState() {
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return new Scaffold(
        appBar: new AppBar(
          title: new Text('Plugin example app'),
        ),
        body: new Center(
          child: new Column(
            crossAxisAlignment: CrossAxisAlignment.center,
            mainAxisAlignment: MainAxisAlignment.center,
            children: <Widget>[

              camState ? new LayoutBuilder(
                  builder: (BuildContext context, BoxConstraints constraints) {
                return new Camera(
                  constraints,
                  fill: false,
                  width: 300.0,
                  height: 300.0,
                  qrCodeCallback: (code) {
                    setState(() {
                      qr = code;
                    });
                  },
                );
              }): new Text("Camera inActive"),
              new Text("QRCODE: $qr"),
            ],
          ),
        ),
      floatingActionButton: new FloatingActionButton(
          child: new Text("press me"),
          onPressed: () {
            setState((){
              camState=!camState;
            });

//            Navigator
//                .of(context)
//                .push(new MaterialPageRoute(builder: (BuildContext context) {
//              return new Scaffold(
//                appBar: new AppBar(
//                  title: new Text("Next Page"),
//                ),
//              );
//            }));
          }),

      );
  }
}

class Camera extends StatefulWidget {
  Camera(this.constraints,
      {this.fill, this.width, this.height, this.qrCodeCallback});

  BoxConstraints constraints;
  bool fill;
  double width, height;
  String qrCode;
  ValueChanged<String> qrCodeCallback;





  void qrCodeHandler(String string) {
    qrCodeCallback(string);
  }

  @override
  CameraState createState() =>
      new CameraState(constraints, fill: fill, width: width, height: height);
}

class CameraState extends State<Camera> {
  CameraState(this.constraints, {this.fill, this.width, this.height});

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
    print(constraints);

    if (fill == null) {
      fill = false;
    }
    if (fill) {
      oWidth = width;
      oHeight = height;
      if (constraints.maxWidth != double.INFINITY && width == null)
        oWidth = constraints.maxWidth;
      if (constraints.maxHeight != double.INFINITY && height == null)
        oHeight = constraints.maxHeight;
    }

    if (constraints.maxWidth != double.INFINITY && width == null)
      width = constraints.maxWidth;
    else if (width == null) width = 2000.0;
    if (constraints.maxHeight != double.INFINITY && height == null)
      height = constraints.maxHeight;
    else if (height == null) height = 2000.0;

    print("$width and $height");
    QrMobileVision.start(widget.qrCodeHandler, cameraFrameHandler,
        height.toInt(), width.toInt(), fill); //height x width
  }

  @override
  deactivate() {
    super.deactivate();
    QrMobileVision.stop();
  }

  @override
  Widget build(BuildContext context) {



    if (bytes!=null) {
      return new Image.memory(
        bytes,
        width: oWidth,
        height: oHeight,
        fit: BoxFit.cover,
        gaplessPlayback: true,
      );
    } else
      return new Text("Camera Loading...");
  }
}

