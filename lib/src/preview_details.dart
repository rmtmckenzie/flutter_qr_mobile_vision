class NativePreviewDetails {
  num width;
  num height;
  num? sensorOrientation;
  int? textureId;

  NativePreviewDetails(this.width, this.height, this.sensorOrientation, this.textureId);
}

class PreviewDetails {
  PreviewDetails(this._nativePreviewDetails, this.sdkInt);

  NativePreviewDetails _nativePreviewDetails;
  int sdkInt;

  num get width => _nativePreviewDetails.width;
  num get height => _nativePreviewDetails.height;
  num? get sensorOrientation => _nativePreviewDetails.sensorOrientation;
  int? get textureId => _nativePreviewDetails.textureId;
}
