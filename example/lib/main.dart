import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:qr_mobile_vision/qr_camera.dart';

void main() {
  debugPaintSizeEnabled = false;
  runApp(const HomePage());
}

class HomePage extends StatefulWidget {
  const HomePage({super.key});

  @override
  HomeState createState() => HomeState();
}

class HomeState extends State<HomePage> {
  @override
  Widget build(BuildContext context) {
    return const MaterialApp(home: MyApp());
  }
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  MyAppState createState() => MyAppState();
}

class MyAppState extends State<MyApp> {
  BarcodeData? qr;
  bool camState = false;
  bool dirState = false;

  @override
  initState() {
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Plugin example app'),
        actions: <Widget>[
          IconButton(icon: const Icon(Icons.light), onPressed: _swapBackLightState),
        ],
      ),
      body: Center(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.center,
          mainAxisAlignment: MainAxisAlignment.center,
          children: <Widget>[
            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                const Text("Back"),
                Switch(value: dirState, onChanged: (val) => setState(() => dirState = val)),
                const Text("Front"),
              ],
            ),
            Expanded(
                child: camState
                    ? Center(
                        child: SizedBox(
                          width: double.infinity,
                          height: 400.0,
                          child: Stack(
                            children: [
                              Positioned.fill(
                                child: QrCamera(
                                  timeout: 1000,
                                  detectionSpeed: QrDetectionSpeed.unrestricted,
                                  onError: (context, error) => Text(
                                    error.toString(),
                                    style: const TextStyle(color: Colors.red),
                                  ),
                                  cameraDirection: dirState ? CameraDirection.FRONT : CameraDirection.BACK,
                                  qrCodeCallback: (code) {
                                    debugPrint(code.toString());
                                    setState(() {
                                      qr = code;
                                    });
                                  },
                                ),
                              ),
                              if (qr != null)
                                Positioned(
                                  left: qr!.getLeft,
                                  top: qr!.getTop,
                                  child: Container(
                                    width: qr!.barcodeSize.width,
                                    height: qr!.barcodeSize.height,
                                    decoration: BoxDecoration(border: Border.all()),
                                  ),
                                ),
                            ],
                          ),
                        ),
                      )
                    : const Center(child: Text("Camera inactive"))),
            Text("QRCODE: ${qr?.rawValue}"),
          ],
        ),
      ),
      floatingActionButton: FloatingActionButton(
          child: const Text(
            "on/off",
            textAlign: TextAlign.center,
          ),
          onPressed: () {
            setState(() {
              camState = !camState;
            });
          }),
    );
  }

  _swapBackLightState() async {
    QrCamera.toggleFlash();
  }
}
