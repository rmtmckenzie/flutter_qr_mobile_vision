package com.github.rmtmckenzie.qr_mobile_vision;

import android.graphics.Point;
import android.graphics.Rect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScannedData {
  private final Point[] corners;
  private final String rawValue;

  private final Rect boundingBox;

  ScannedData(Point[] corners, Rect boundingBox, String rawValue) {
    this.corners = corners;
    this.rawValue = rawValue;
    this.boundingBox = boundingBox;
  }

  Map<String, Object> geJson(Float x) {
    x = x == null ? 1 : x;
    HashMap<String, Object> result = new HashMap<>();
    List<List<Float>> points = new ArrayList<>();
    for (Point corner : corners) {
      List<Float> point = new ArrayList<>();
      point.add(corner.x / x);
      point.add(corner.y / x);
      points.add(point);
    }
    result.put("corners", points);
    result.put("rawValue", rawValue);
    result.put("width", boundingBox.width() / x);
    result.put("height", boundingBox.height() / x);
    return result;
  }
}
