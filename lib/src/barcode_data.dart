import 'package:flutter/widgets.dart';

final class BarcodeData {
  final String? rawValue;
  final Size size;
  final List<Offset> corners;

  const BarcodeData({
    required this.corners,
    required this.rawValue,
    required this.size,
  });

  factory BarcodeData.fromNative(Map data) {
    final rawValue = data['rawValue'];
    final width = data['width'];
    final height = data['height'];
    final size = width is num && height is num ? Size(width.toDouble(), height.toDouble()) : Size.zero;
    final List<Offset> corners;
    final items = data['corners'];
    if (items is! List) {
      corners = [];
    } else {
      corners = [
        for (int i = 0; i < items.length; i++)
          Offset(((items[i] as List).first as num).toDouble(), ((items[i] as List).last  as num).toDouble())
      ];
    }
    return BarcodeData(corners: corners, rawValue: rawValue, size: size);
  }

  @override
  String toString() => '$rawValue\n$size\n${corners.join(', ')}';
}
