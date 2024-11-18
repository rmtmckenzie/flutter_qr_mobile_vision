sealed class QrException implements Exception {
  const QrException();
}

/// ALREADY_RUNNING
final class AlreadyRunningException extends QrException {
  const AlreadyRunningException();
}

/// QRREADER_ERROR
final class CameraPermissionException extends QrException {
  const CameraPermissionException();
}

/// noHardware
final class NoCameraException extends QrException {
  const NoCameraException();
}

/// OTHERS
final class UnknownException extends QrException {
  const UnknownException();
}
