part of qr_camera;

enum FrameRotation { none, ninetyCC, oneeighty, twoseventyCC }

FrameRotation deserializeFrameRotation(String rotation) {
  switch (rotation) {
    case "ninetyCC":
      return FrameRotation.ninetyCC;
    case "oneeighty":
      return FrameRotation.oneeighty;
    case "twoSeventyCC":
      return FrameRotation.twoseventyCC;
    case "none":
    default:
      return FrameRotation.none;
  }
}

/// The state of a [QrCameraController].
class QrCameraValue {
  const QrCameraValue({
    this.isInitialized,
    this.errorDescription,
    this.previewSize,
    this.previewOrientation,
  });

  const QrCameraValue.uninitialized() : this(isInitialized: false);

  /// True after [CameraController.initialize] has completed successfully.
  final bool isInitialized;

  final String errorDescription;

  final FrameRotation previewOrientation;

  /// The size of the preview in pixels.
  ///
  /// Is `null` until  [isInitialized] is `true`.
  final QrCameraResolution previewSize;

  /// Convenience getter for `previewSize.height / previewSize.width`.
  ///
  /// Can only be called when [initialize] is done.
  double get aspectRatio => previewSize.height / previewSize.width;

  bool get hasError => errorDescription != null;

  QrCameraValue copyWith({
    bool isInitialized,
    String errorDescription,
    QrCameraResolution previewSize,
  }) {
    return QrCameraValue(
      isInitialized: isInitialized ?? this.isInitialized,
      errorDescription: errorDescription,
      previewSize: previewSize ?? this.previewSize,
    );
  }

  @override
  String toString() {
    return '$runtimeType('
        'isInitialized: $isInitialized, '
        'errorDescription: $errorDescription, '
        'previewSize: $previewSize)';
  }
}

class QrCameraController extends ValueNotifier<QrCameraValue> {
  QrCameraController({
    @required this.description,
    @required this.resolution,
    this.formats,
  }) : super(const QrCameraValue.uninitialized());

  final QrCameraDescription description;
  final QrCameraResolution resolution;
  final List<BarcodeFormats> formats;

  int _textureId;
  bool _isDisposed = false;
  Completer<void> _creatingCompleter;
  StreamSubscription<dynamic> _eventSubscription;

  Future<void> initialize() async {
    if (_isDisposed) {
      return Future<void>.value();
    }

    if (_isDisposed) {
      return Future<void>.value();
    }
    try {
      _creatingCompleter = Completer<void>();

      final response = await QrMobileVision.initialize(description: description, resolution: resolution);

      _textureId = response.textureId;
      value = value.copyWith(
        isInitialized: true,
        previewSize: QrCameraResolution(
          width: response.width,
          height: response.height,
        ),
      );
    } on PlatformException catch (e) {
      throw QrCameraException(e.code, e.message);
    }

    _eventSubscription = QrMobileVision.stream(_textureId).listen(_listener);

    _creatingCompleter.complete();
    return _creatingCompleter.future;
  }

  /// Listen to events from the native plugins.
  ///
  /// A "cameraClosing" event is sent when the camera is closed automatically by the system (for example when the app go to background). The plugin will try to reopen the camera automatically but any ongoing recording will end.
  void _listener(dynamic event) {
    final Map<dynamic, dynamic> map = event;
    if (_isDisposed) {
      return;
    }

    switch (map['eventType']) {
      case 'error':
        value = value.copyWith(errorDescription: event['errorDescription']);
        break;
      case 'cameraClosing':
//        value = value.copyWith(isRecordingVideo: false);
        break;
    }
  }

  /// Releases the resources of this camera.
  @override
  Future<void> dispose() async {
    if (_isDisposed) {
      return;
    }
    _isDisposed = true;
    super.dispose();
    if (_creatingCompleter != null) {
      await _creatingCompleter.future;

      await QrMobileVision.dispose(_textureId);
      await _eventSubscription?.cancel();
    }
  }
}
