part of qr_mobile_vision;

enum QrCameraType {
  front,
  back,
  external,
}

class QrCameraDescription {
  final QrCameraType type;
  final String name;

  QrCameraDescription({@required this.type, @required this.name});
}
