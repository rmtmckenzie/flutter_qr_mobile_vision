import 'dart:async';

import 'package:flutter/widgets.dart';
import 'package:qr_mobile_vision/qr_mobile_vision.dart';
import 'package:qr_mobile_vision/src/barcode_data.dart';
import 'package:qr_mobile_vision/src/exceptions.dart';
import 'package:qr_mobile_vision/src/preview.dart';
import 'package:qr_mobile_vision/src/preview_details.dart';

Widget _defaultNotStartedBuilder(BuildContext context) => const Text("Camera Loading ...");
Widget _defaultOffscreenBuilder(BuildContext context) => const Text("Camera Paused ...");
Widget _defaultOnError(BuildContext context, Object? error) {
  debugPrint("Error reading from camera: $error");
  return const Text("Error reading from camera...");
}

typedef ErrorCallback = Widget Function(BuildContext context, QrException error);

class ScannerController {
  final _controller = StreamController<void>.broadcast();

  void restart() {
    if (_controller.isClosed) return;
    _controller.add(null);
  }

  void dispose() => _controller.close();

  Stream<void> get stream => _controller.stream;
}

enum QrDetectionSpeed {
  noDuplicates,
  unrestricted;
}

class _Throttler {
  Timer? _timer;
  bool _isRunning = false;
  final Duration duration;

  _Throttler(this.duration);

  void run(void Function() func) {
    if (_isRunning) return;
    _isRunning = true;
    dispose();
    func();
    _timer = Timer(duration, () {
      _isRunning = false;
      dispose();
    });
  }

  void dispose() {
    _timer?.cancel();
    _timer = null;
  }
}

class QrCamera extends StatefulWidget {
  const QrCamera({
    super.key,
    required this.qrCodeCallback,
    this.controller,
    this.child,
    this.fit = BoxFit.cover,
    this.detectionSpeed = QrDetectionSpeed.noDuplicates,
    this.timeout = 250,
    WidgetBuilder? notStartedBuilder,
    WidgetBuilder? offscreenBuilder,
    ErrorCallback? onError,
    this.cameraDirection = CameraDirection.BACK,
    this.formats,
  })  : notStartedBuilder = notStartedBuilder ?? _defaultNotStartedBuilder,
        offscreenBuilder = offscreenBuilder ?? notStartedBuilder ?? _defaultOffscreenBuilder,
        onError = onError ?? _defaultOnError;

  final QrDetectionSpeed detectionSpeed;
  final ScannerController? controller;
  final BoxFit fit;
  final ValueChanged<BarcodeData> qrCodeCallback;
  final Widget? child;
  final WidgetBuilder notStartedBuilder;
  final WidgetBuilder offscreenBuilder;
  final ErrorCallback onError;
  final List<BarcodeFormats>? formats;
  final CameraDirection cameraDirection;
  final int timeout;

  static toggleFlash() {
    QrMobileVision.toggleFlash();
  }

  @override
  QrCameraState createState() => QrCameraState();
}

class QrCameraState extends State<QrCamera> with WidgetsBindingObserver {
  // needed for flutter < 3.0 to still be supported
  T? _ambiguate<T>(T? value) => value;
  String? _lastScannedValue;
  StreamSubscription<void>? _sub;
  late final _throttler = _Throttler(Duration(milliseconds: widget.timeout));
  @override
  void initState() {
    super.initState();
    _ambiguate(WidgetsBinding.instance)!.addObserver(this);
    if (widget.controller != null) {
      _sub?.cancel();
      _sub = widget.controller?.stream.listen((_) {
        restart();
      });
    }
  }

  @override
  void reassemble() {
    restart();
    super.reassemble();
  }

  @override
  dispose() {
    _ambiguate(WidgetsBinding.instance)!.removeObserver(this);
    _throttler.dispose();
    _sub?.cancel();
    _sub = null;
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

  Future<PreviewDetails> _asyncInit(double width, double height) async {
    final devicePixelRatio = MediaQuery.of(context).devicePixelRatio;
    return await QrMobileVision.start(
      width: (devicePixelRatio * width).ceil(),
      height: (devicePixelRatio * height).ceil(),
      qrCodeHandler: (value) => _qrCodeHandler(value, Size(width, height)),
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
        _lastScannedValue = null;
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
                return widget.onError(context, QrException.fromError(details.error));
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

  void _qrCodeHandler(BarcodeData barcode, Size cameraSize) async {
    switch (widget.detectionSpeed) {
      case QrDetectionSpeed.noDuplicates:
        if (_lastScannedValue != null && barcode.rawValue == _lastScannedValue) return;
        break;
      case QrDetectionSpeed.unrestricted:
        break;
    }
    _throttler.run(() {
      _lastScannedValue = barcode.rawValue;
      final devicePixelRatio = MediaQuery.of(context).devicePixelRatio;
      widget.qrCodeCallback(barcode._normalize(devicePixelRatio, cameraSize, widget.fit));
    });
  }
}

extension _BarcodeDataExt on BarcodeData {
  BarcodeData _normalize(double pr, Size cameraSize, BoxFit boxFit) {
    return BarcodeData(
      corners: corners.map((e) => e / pr).toList(),
      rawValue: rawValue,
      barcodeSize: barcodeSize / pr,
      cameraSize: cameraSize,
      boxFit: boxFit,
    );
  }
}
