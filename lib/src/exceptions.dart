import 'package:flutter/services.dart';

sealed class QrException implements Exception {
  const QrException();

  factory QrException.fromError(Object? error) {
    switch (error) {
      case PlatformException(code: final code):
        if (code == 'ALREADY_RUNNING') return AlreadyRunningException();
        if (code == 'QRREADER_ERROR') return CameraPermissionException();
        if (code == 'noHardware') return NoCameraException();
        return UnknownException();
      default:
        return UnknownException();
    }
  }
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
