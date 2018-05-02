package com.github.rmtmckenzie.qrmobilevision;

import com.google.android.gms.vision.barcode.Barcode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum BarcodeFormats {

    ALL_FORMATS(Barcode.ALL_FORMATS),
    CODE_128(Barcode.CODE_128),
    CODE_39(Barcode.CODE_39),
    CODE_93(Barcode.CODE_93),
    CODABAR(Barcode.CODABAR),
    DATA_MATRIX(Barcode.DATA_MATRIX),
    EAN_13(Barcode.EAN_13),
    EAN_8(Barcode.EAN_8),
    ITF(Barcode.ITF),
    QR_CODE(Barcode.QR_CODE),
    UPC_A(Barcode.UPC_A),
    UPC_E(Barcode.UPC_E),
    PDF417(Barcode.PDF417),
    AZTEC(Barcode.AZTEC);

    BarcodeFormats(int intValue) {
        this.intValue = intValue;
    }

    public final int intValue;

    private static Map<String, Integer> formatsMap;

    static {
        BarcodeFormats[] values = BarcodeFormats.values();
        formatsMap = new HashMap<>(values.length * 4 / 3);
        for (BarcodeFormats value : values) {
            formatsMap.put(value.name(), value.intValue);
        }
    }

    /**
     * Return the integer value resuling from OR-ing all of the values
     * of the supplied strings.
     * <p>
     * Note that if ALL_FORMATS is defined as well as other values, ALL_FORMATS
     * will be ignored (following how it would work with just OR-ing the ints).
     *
     * @param strings - list of strings representing the various formats
     * @return integer value corresponding to OR of all the values.
     */
    static int intFromStringList(List<String> strings) {
        if (strings == null) return BarcodeFormats.ALL_FORMATS.intValue;
        int val = 0;
        for (String string : strings) {
            Integer asInt = BarcodeFormats.formatsMap.get(string);
            if (asInt != null) {
                val |= asInt;
            }
        }
        return val;
    }


}
