import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:qr_mobile_vision/Camera.dart';

void main() {
  debugPaintSizeEnabled = false;
  runApp(new HomePage());
}

class HomePage extends StatefulWidget {
  @override
  HomeState createState() => new HomeState();
}

class HomeState extends State<HomePage> {
  @override
  Widget build(BuildContext context) {
    return new MaterialApp(home: new MyApp());
  }
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => new _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String qr;
  bool camState = false;

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
            camState
                ? new Camera(
                    fill: false,
                    width: 300.0,
                    height: 300.0,
                    qrCodeCallback: (code) {
                      setState(() {
                        qr = code;
                      });
                    },
                  )
                : new Text("Camera inActive"),
            new Text("QRCODE: $qr"),
          ],
        ),
      ),
      floatingActionButton: new FloatingActionButton(
          child: new Text(
            "press me",
            textAlign: TextAlign.center,
          ),
          onPressed: () {
            setState(() {
              camState = !camState;
            });
          }),
    );
  }
}
