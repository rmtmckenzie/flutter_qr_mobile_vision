import 'dart:async';
import 'dart:io';

import 'package:device_info/device_info.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:native_device_orientation/native_device_orientation.dart';
import 'package:qr_mobile_vision/qr_mobile_vision.dart';

final WidgetBuilder _defaultNotStartedBuilder =
    (context) => new Text("Camera Loading ...");
final WidgetBuilder _defaultOffscreenBuilder =
    (context) => new Text("Camera Paused.");
final ErrorCallback _defaultOnError = (BuildContext context, Object error) {
  print("Error reading from camera: $error");
  return new Text("Error reading from camera...");
};

typedef Widget ErrorCallback(BuildContext context, Object error);

class QrCamera extends StatefulWidget {
  QrCamera({
    Key key,
    @required this.qrCodeCallback,
    this.child,
    this.fit = BoxFit.cover,
    WidgetBuilder notStartedBuilder,
    WidgetBuilder offscreenBuilder,
    ErrorCallback onError,
    this.formats,
    this.cameraDirection,
  })  : notStartedBuilder = notStartedBuilder ?? _defaultNotStartedBuilder,
        offscreenBuilder =
            offscreenBuilder ?? notStartedBuilder ?? _defaultOffscreenBuilder,
        onError = onError ?? _defaultOnError,
        assert(fit != null),
        super(key: key);

  final BoxFit fit;
  final ValueChanged<String> qrCodeCallback;
  final Widget child;
  final WidgetBuilder notStartedBuilder;
  final WidgetBuilder offscreenBuilder;
  final ErrorCallback onError;
  final List<BarcodeFormats> formats;
  final CameraDirection cameraDirection;

  @override
  QrCameraState createState() => new QrCameraState();
}

class QrCameraState extends State<QrCamera> with WidgetsBindingObserver {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
  }

  @override
  dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didUpdateWidget(QrCamera oldWidget) {
    if (oldWidget.cameraDirection != widget.cameraDirection) {
      QrMobileVision.stop();
      setState(() {
        _asyncInitOnce = null;
      });
    }
    super.didUpdateWidget(oldWidget);
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      setState(() => onScreen = true);
    } else {
      if (_asyncInitOnce != null && onScreen) {
        QrMobileVision.stop();
      }
      setState(() {
        onScreen = false;
        _asyncInitOnce = null;
      });
    }
  }

  bool onScreen = true;
  Future<PreviewDetails> _asyncInitOnce;

  Future<PreviewDetails> _asyncInit(num width, num height) async {
    var previewDetails = await QrMobileVision.start(
      width: width.toInt(),
      height: height.toInt(),
      qrCodeHandler: widget.qrCodeCallback,
      formats: widget.formats,
      cameraDirection: widget.cameraDirection,
    );
    return previewDetails;
  }

  /// This method can be used to restart scanning
  ///  the event that it was paused.
  void restart() {
    (() async {
      await QrMobileVision.stop();
      setState(() {
        _asyncInitOnce = null;
      });
    })();
  }

  /// This method can be used to manually stop the
  /// camera.
  void stop() {
    (() async {
      await QrMobileVision.stop();
    })();
  }

  @override
  deactivate() {
    super.deactivate();
    QrMobileVision.stop();
  }

  @override
  Widget build(BuildContext context) {
    return new LayoutBuilder(
        builder: (BuildContext context, BoxConstraints constraints) {
      if (_asyncInitOnce == null && onScreen) {
        _asyncInitOnce =
            _asyncInit(constraints.maxWidth, constraints.maxHeight);
      } else if (!onScreen) {
        return widget.offscreenBuilder(context);
      }

      DeviceInfoPlugin deviceInfo = DeviceInfoPlugin();

      return new FutureBuilder(
        future: _asyncInitOnce,
        builder: (BuildContext context, AsyncSnapshot<PreviewDetails> details) {
          switch (details.connectionState) {
            case ConnectionState.none:
            case ConnectionState.waiting:
              return widget.notStartedBuilder(context);
            case ConnectionState.done:
              if (details.hasError) {
                debugPrint(details.error.toString());
                return widget.onError(context, details.error);
              }
              Widget preview = new SizedBox(
                width: constraints.maxWidth,
                height: constraints.maxHeight,
                child: FutureBuilder<AndroidDeviceInfo>(
                    future: deviceInfo.androidInfo,
                    builder: (context, snapshot) {
                      return Preview(
                        previewDetails: details.data,
                        targetWidth: constraints.maxWidth,
                        targetHeight: constraints.maxHeight,
                        fit: widget.fit,
                        sdkInt:
                            snapshot.hasData ? snapshot.data.version.sdkInt : 0,
                      );
                    }),
              );

              if (widget.child != null) {
                return new Stack(
                  children: [
                    preview,
                    widget.child,
                  ],
                );
              }
              return preview;

            default:
              throw new AssertionError(
                  "${details.connectionState} not supported.");
          }
        },
      );
    });
  }
}

class Preview extends StatelessWidget {
  final double width, height;
  final double targetWidth, targetHeight;
  final int textureId;
  final int sensorOrientation;
  final BoxFit fit;
  final int sdkInt;

  Preview(
      {@required PreviewDetails previewDetails,
      @required this.targetWidth,
      @required this.targetHeight,
      @required this.fit,
      @required this.sdkInt})
      : assert(previewDetails != null),
        textureId = previewDetails.textureId,
        width = previewDetails.width.toDouble(),
        height = previewDetails.height.toDouble(),
        sensorOrientation = previewDetails.sensorOrientation;

  @override
  Widget build(BuildContext context) {
    return new NativeDeviceOrientationReader(
      builder: (context) {
        var nativeOrientation =
            NativeDeviceOrientationReader.orientation(context);

        int nativeRotation = 0;
        switch (nativeOrientation) {
          case NativeDeviceOrientation.portraitUp:
            if (sdkInt == 23) {
              nativeRotation = 180;
            } else {
              nativeRotation = 0;
            }
            break;
          case NativeDeviceOrientation.landscapeRight:
            nativeRotation = 90;
            break;
          case NativeDeviceOrientation.portraitDown:
            if (sdkInt == 23) {
              nativeRotation = 0;
            } else {
              nativeRotation = 180;
            }
            break;
          case NativeDeviceOrientation.landscapeLeft:
            nativeRotation = 270;
            break;
          case NativeDeviceOrientation.unknown:
          default:
            break;
        }

        int rotationCompensation =
            ((nativeRotation - sensorOrientation + 450) % 360) ~/ 90;

        double frameHeight = width;
        double frameWidth = height;

        return new FittedBox(
          fit: fit,
          child: new RotatedBox(
            quarterTurns: rotationCompensation,
            child: new SizedBox(
              width: frameWidth,
              height: frameHeight,
              child: new Texture(textureId: textureId),
            ),
          ),
        );
      },
    );
  }
}
