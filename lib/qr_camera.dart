import 'dart:async';
import 'dart:io';

import 'package:device_info_plus/device_info_plus.dart';
import 'package:flutter/widgets.dart';
import 'package:native_device_orientation/native_device_orientation.dart';
import 'package:qr_mobile_vision/qr_mobile_vision.dart';

final WidgetBuilder _defaultNotStartedBuilder = (context) => Text("Camera Loading ...");
final WidgetBuilder _defaultOffscreenBuilder = (context) => Text("Camera Paused.");
final ErrorCallback _defaultOnError = (BuildContext context, Object? error) {
  print("Error reading from camera: $error");
  return Text("Error reading from camera...");
};

typedef Widget ErrorCallback(BuildContext context, Object? error);

class QrCamera extends StatefulWidget {
  QrCamera({
    Key? key,
    required this.qrCodeCallback,
    this.child,
    this.fit = BoxFit.cover,
    WidgetBuilder? notStartedBuilder,
    WidgetBuilder? offscreenBuilder,
    ErrorCallback? onError,
    this.cameraDirection = CameraDirection.BACK,
    this.formats,
  })  : notStartedBuilder = notStartedBuilder ?? _defaultNotStartedBuilder,
        offscreenBuilder = offscreenBuilder ?? notStartedBuilder ?? _defaultOffscreenBuilder,
        onError = onError ?? _defaultOnError,
        super(key: key);

  final BoxFit fit;
  final ValueChanged<String?> qrCodeCallback;
  final Widget? child;
  final WidgetBuilder notStartedBuilder;
  final WidgetBuilder offscreenBuilder;
  final ErrorCallback onError;
  final List<BarcodeFormats>? formats;
  final CameraDirection cameraDirection;

  static toggleFlash() {
    QrMobileVision.toggleFlash();
  }

  @override
  QrCameraState createState() => QrCameraState();
}

class QrCameraState extends State<QrCamera> with WidgetsBindingObserver {
  // needed for flutter < 3.0 to still be supported
  T? _ambiguate<T>(T? value) => value;

  @override
  void initState() {
    super.initState();
    _ambiguate(WidgetsBinding.instance)!.addObserver(this);
  }

  @override
  dispose() {
    _ambiguate(WidgetsBinding.instance)!.removeObserver(this);
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
  Future<PreviewDetails>? _asyncInitOnce;

  Future<PreviewDetails> _asyncInit(num width, num height) async {
    final devicePixelRatio = MediaQuery.of(context).devicePixelRatio;
    final deviceInfoFut = Platform.isAndroid ? DeviceInfoPlugin().androidInfo : Future.value(null);
    return await QrMobileVision.start(
      width: (devicePixelRatio * width.toInt()).ceil(),
      height: (devicePixelRatio * height.toInt()).ceil(),
      qrCodeHandler: widget.qrCodeCallback,
      formats: widget.formats,
      cameraDirection: widget.cameraDirection,
    );
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
    return LayoutBuilder(builder: (BuildContext context, BoxConstraints constraints) {
      if (_asyncInitOnce == null && onScreen) {
        _asyncInitOnce = _asyncInit(constraints.maxWidth, constraints.maxHeight);
      } else if (!onScreen) {
        return widget.offscreenBuilder(context);
      }

      return FutureBuilder(
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
              Widget preview = SizedBox(
                width: constraints.maxWidth,
                height: constraints.maxHeight,
                child: Preview(
                  previewDetails: details.data!,
                  targetWidth: constraints.maxWidth,
                  targetHeight: constraints.maxHeight,
                  fit: widget.fit,
                ),
              );

              if (widget.child != null) {
                return Stack(
                  children: [
                    preview,
                    widget.child!,
                  ],
                );
              }
              return preview;

            default:
              throw AssertionError("${details.connectionState} not supported.");
          }
        },
      );
    });
  }
}

class Preview extends StatelessWidget {
  final double width, height;
  final double targetWidth, targetHeight;
  final int? textureId;
  final int? sensorOrientation;
  final BoxFit fit;
  final int sdkInt;

  Preview({
    required PreviewDetails previewDetails,
    required this.targetWidth,
    required this.targetHeight,
    required this.fit,
  })  : textureId = previewDetails.textureId,
        width = previewDetails.width.toDouble(),
        height = previewDetails.height.toDouble(),
        sdkInt = previewDetails.sdkInt,
        sensorOrientation = previewDetails.sensorOrientation as int?;

  @override
  Widget build(BuildContext context) {
    return NativeDeviceOrientationReader(
      builder: (context) {
        var nativeOrientation = NativeDeviceOrientationReader.orientation(context);

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

        int rotationCompensation = ((nativeRotation - sensorOrientation! + 450) % 360) ~/ 90;

        double frameHeight = width;
        double frameWidth = height;

        return ClipRect(
          child: FittedBox(
            fit: fit,
            child: RotatedBox(
              quarterTurns: rotationCompensation,
              child: SizedBox(
                width: frameWidth,
                height: frameHeight,
                child: Texture(textureId: textureId!),
              ),
            ),
          ),
        );
      },
    );
  }
}
