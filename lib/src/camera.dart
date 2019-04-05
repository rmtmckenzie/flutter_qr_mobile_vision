part of qr_mobile_vision;

enum QrCameraType {
  front,
  back,
  external,
}


QrCameraType deserializeQrCameraType(String typeString) {
  switch (typeString) {
    case "front":
      return QrCameraType.front;
    case "external":
      return QrCameraType.external;
    case "back":
    default:
      return QrCameraType.back;
  }
}

String serializeQrCameraType(QrCameraType type) {
  switch (type) {
    case QrCameraType.front:
      return "front";
    case QrCameraType.external:
      return "external";
    case QrCameraType.back:
      return "back";
  }
}

class QrCameraDescription {
  final QrCameraType type;
  final String name;

  QrCameraDescription({@required this.type, @required this.name});
}


class QrCameraResolution {
  final int width;
  final int height;

  QrCameraResolution({@required this.width, @required this.height});

  @override
  String toString() {
    return '$runtimeType('
      'width: $width, '
      'height: $height)';
  }
}