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
