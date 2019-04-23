import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:qr_mobile_vision/qr_camera.dart';
import 'package:qr_mobile_vision/qr_mobile_vision.dart';

main() async {
  debugPaintSizeEnabled = false;
  List<QrCameraDescription> qrCameras = await getQrCameras();
  QrCameraDescription chosenCamera;
  for (QrCameraDescription camera in qrCameras) {
    if (camera.type == QrCameraType.back) {
      chosenCamera = camera;
      break;
    }
  }
  chosenCamera ??= qrCameras[0];
  runApp(new HomePage());
}

class HomePage extends StatefulWidget {
  QrCameraDescription cameraDescription;

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

  QrCameraController controller;

  asyncInit() async {
    var cameras = await QrMobileVision.getCameras();
    print("Cameras: $cameras");
    if (cameras.length == 0) {
      //TODO: some sort of error for user
      return;
    }
    QrCameraDescription camera;
    for(var cam in cameras) {
      if (cam.type == QrCameraType.back) {
        camera = cam;
        break;
      }
    }

    // fallback to first camera if there's no back camera
    camera = camera ?? cameras[0];

    var resolutions = await QrMobileVision.getResolutions(camera);
    print("resolutions: $resolutions");

  }

  @override
  initState() {
    super.initState();
    asyncInit();
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
            new Expanded(
                child: camState
                    ? new Center(
                        child: new SizedBox(
                          width: 300.0,
                          height: 600.0,
                          child: new QrCamera(
                            controller: controller,
                          ),
                        ),
                      )
                    : new Center(child: new Text("Camera inactive"))),
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
