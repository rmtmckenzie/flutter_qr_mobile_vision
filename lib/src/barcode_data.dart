import 'package:flutter/widgets.dart';

final class BarcodeData {
  final String? rawValue;
  final Size barcodeSize;
  final List<Offset> corners;
  final Size cameraSize;
  final BoxFit boxFit;

  const BarcodeData({
    required this.corners,
    required this.rawValue,
    required this.barcodeSize,
    required this.cameraSize,
    required this.boxFit,
  });

  factory BarcodeData.fromNative(Map data) {
    final rawValue = data['rawValue'];
    final width = data['width'];
    final height = data['height'];
    final barcodeSize = width is num && height is num ? Size(width.toDouble(), height.toDouble()) : Size.zero;
    final List<Offset> corners;
    final items = data['corners'];
    if (items is! List) {
      corners = [];
    } else {
      corners = [
        for (int i = 0; i < items.length; i++)
          Offset(((items[i] as List).first as num).toDouble(), ((items[i] as List).last as num).toDouble())
      ];
    }
    return BarcodeData(
      corners: corners,
      rawValue: rawValue,
      barcodeSize: barcodeSize,
      cameraSize: Size.zero,
      boxFit: BoxFit.none,
    );
  }

  double get getLeft => corners.isEmpty ? 0 : corners.first.dx;

  double get getTop => corners.isEmpty ? 0 : corners.first.dy;

  @override
  String toString() => 'Raw Value:$rawValue\nbarcodeSize: $barcodeSize\nCameraSize: $cameraSize\nCorners: ${corners.join(', ')}';
}
