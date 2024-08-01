//ignore_for_file: constant_identifier_names

enum BarcodeFormats {
  ALL_FORMATS,
  AZTEC,
  CODE_128,
  CODE_39,
  CODE_93,
  CODABAR,
  DATA_MATRIX,
  EAN_13,
  EAN_8,
  ITF,
  PDF417,
  QR_CODE,
  UPC_A,
  UPC_E,
}

extension BarcodeFormat on BarcodeFormats {
  static BarcodeFormats fromString(String? value) {
    return BarcodeFormats.values.firstWhere(
      (e) =>
          e.toString().toUpperCase() == 'BarcodeFormats.$value'.toUpperCase(),
      orElse: () => BarcodeFormats.ALL_FORMATS,
    );
  }
}

const defaultBarcodeFormats = [
  BarcodeFormats.ALL_FORMATS,
];
