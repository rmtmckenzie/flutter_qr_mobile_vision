import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';

import 'qr_mobile_vision.dart';

final WidgetBuilder _defaultNotStartedBuilder = (context) => new Text("Camera Loading ...");
final WidgetBuilder _defaultOffscreenBuilder = (context) => new Text("Camera Paused.");
final ErrorCallback _defaultOnError = (BuildContext context, Object error) {
  print("Error reading from camera: $error");
  return new Text("Error reading from camera...");
};

typedef Widget ErrorCallback(BuildContext context, Object error);

class QrCameraValue {
  final bool isInitialized;
  final bool isCapturing;
  final PreviewDetails previewDetails;

  const QrCameraValue({this.isInitialized, this.isCapturing, this.previewDetails});

  const QrCameraValue.uninitialized()
      : isInitialized = false,
        isCapturing = false,
        previewDetails = null;

  QrCameraValue copyWith({bool isInitialized, bool isCapturing, PreviewDetails previewDetails}) {
    return QrCameraValue(
      isInitialized: isInitialized ?? this.isInitialized,
      isCapturing: isCapturing ?? this.isCapturing,
      previewDetails: previewDetails ?? this.previewDetails,
    );
  }
}

Future<List<Size>> getQrPreviewSizes(QrCameraDescription description) {
  return QrMobileVision.getSupportedSizes(description);
}

Future<List<QrCameraDescription>> getQrCameras() {
  return QrMobileVision.getCameras();
}

class QrCameraException implements Exception {
  QrCameraException(this.code, this.description);

  final String code;
  final String description;

  @override
  String toString() => '$runtimeType($code, $description)';
}

class QrCameraController extends ValueNotifier<QrCameraValue> {
  QrCameraController({
    @required this.formats,
    @required this.type,
    @required this.size,
    this.handler,
  }) : super(QrCameraValue.uninitialized());

  final QrCameraType type;
  final Size size;
  final List<BarcodeFormats> formats;
  final QrCodeHandler handler;

  bool _isDisposed;

  bool get started => value.isCapturing;

  Future<void> initialize() async {}

  Future start({QrCodeHandler handler}) async {
    if (_isDisposed) {
      return Future<void>.value();
    }

    try {
      var previewDetails = await QrMobileVision.start(
        width: size.width.toInt(),
        height: size.height.toInt(),
        qrCodeHandler: handler ?? this.handler,
      );
      value = QrCameraValue(isInitialized: true, isCapturing: true, previewDetails: previewDetails);
    } on PlatformException catch (e) {
      throw QrCameraException(e.code, e.message);
    }
  }

  Future stop() async {
    await QrMobileVision.stop();
    value = value.copyWith(isCapturing: false, previewDetails: null);
  }

  @override
  Future<void> dispose() async {
    if (_isDisposed) {
      return;
    }

    _isDisposed = true;
    super.dispose();

    await stop();
  }
}

class _AutoQrCameraStopping extends StatefulWidget {
  final QrCameraController controller;
  final Widget child;

  const _AutoQrCameraStopping({Key key, this.controller, this.child}) : super(key: key);

  @override
  _AutoQrCameraStoppingState createState() => _AutoQrCameraStoppingState();
}

class _AutoQrCameraStoppingState extends State<_AutoQrCameraStopping> with WidgetsBindingObserver {
  @override
  Widget build(BuildContext context) {
    return widget.child;
  }

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

  bool shouldRestart = false;

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed && shouldRestart) {
      shouldRestart = false;
      widget.controller.start();
    } else {
      if (widget.controller.started) {
        shouldRestart = true;
        widget.controller.stop();
      }
    }
  }
}

class NewQrCamera extends StatelessWidget {
  NewQrCamera({
    @required this.controller,
    this.autoStopping = true,
    this.fit = BoxFit.cover,
    WidgetBuilder notStartedBuilder,
    WidgetBuilder offscreenBuilder,
  })  : notStartedBuilder = notStartedBuilder ?? _defaultNotStartedBuilder,
        offscreenBuilder = offscreenBuilder ?? notStartedBuilder ?? _defaultOffscreenBuilder;

  final bool autoStopping;
  final QrCameraController controller;
  final BoxFit fit;
  final WidgetBuilder notStartedBuilder;
  final WidgetBuilder offscreenBuilder;

  @override
  Widget build(BuildContext context) {
    if (!controller.value.isInitialized) {
      return notStartedBuilder(context);
    }

    if (!controller.value.isCapturing) {
      return offscreenBuilder(context);
    }

    Widget widget = LayoutBuilder(builder: (BuildContext context, BoxConstraints constraints) {
      return Preview(
        previewDetails: controller.value.previewDetails,
        targetWidth: constraints.maxWidth,
        targetHeight: constraints.maxHeight,
        fit: fit,
      );
    });

    if (autoStopping) {
      widget = _AutoQrCameraStopping(controller: controller, child: widget);
    }

    return widget;
  }
}

class QrCamera extends StatefulWidget {
  QrCamera({
    Key key,
//    this.controller,
    @required this.qrCodeCallback,
    this.child,
    this.fit = BoxFit.cover,
    WidgetBuilder notStartedBuilder,
    WidgetBuilder offscreenBuilder,
    ErrorCallback onError,
    this.formats,
  })  : notStartedBuilder = notStartedBuilder ?? _defaultNotStartedBuilder,
        offscreenBuilder = offscreenBuilder ?? notStartedBuilder ?? _defaultOffscreenBuilder,
        onError = onError ?? _defaultOnError,
        assert(fit != null),
        super(key: key);

  final BoxFit fit;
//  final QrCameraController controller;
  final ValueChanged<String> qrCodeCallback;
  final Widget child;
  final WidgetBuilder notStartedBuilder;
  final WidgetBuilder offscreenBuilder;
  final ErrorCallback onError;
  final List<BarcodeFormats> formats;

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
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      setState(() => onScreen = true);
    } else {
      if (_asyncInitOnce != null && onScreen) {
//        widget.controller.stop();
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
    return new LayoutBuilder(builder: (BuildContext context, BoxConstraints constraints) {
      if (_asyncInitOnce == null && onScreen) {
        _asyncInitOnce = _asyncInit(constraints.maxWidth, constraints.maxHeight);
      } else if (!onScreen) {
        return widget.offscreenBuilder(context);
      }

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
                child: Preview(
                  previewDetails: details.data,
                  targetWidth: constraints.maxWidth,
                  targetHeight: constraints.maxHeight,
                  fit: widget.fit,
                ),
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
              throw new AssertionError("${details.connectionState} not supported.");
          }
        },
      );
    });
  }
}
