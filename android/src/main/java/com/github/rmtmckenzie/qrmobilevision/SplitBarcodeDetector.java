package com.github.rmtmckenzie.qrmobilevision;

import android.graphics.Bitmap;
import android.util.SparseArray;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.nio.ByteBuffer;

public class SplitBarcodeDetector extends Detector<Barcode> {

    private final BarcodeDetector detector;
    private final FrameReceiver frameReceiver;
    private final QRReceiver qrReceiver;

    interface FrameReceiver {
        void receiveFrame(ByteBuffer frame);
    }

    interface QRReceiver {
        void receiveQr(Barcode data);
    }

    public SplitBarcodeDetector(Context context, FrameReceiver frameReceiver, QRReceiver qrReceiver) {
        this.detector = new BarcodeDetector.Builder(context.getApplicationContext()).setBarcodeFormats(
                Barcode.QR_CODE).build();
        this.frameReceiver = frameReceiver;
    }

    @Override
    public SparseArray<Barcode> detect(Frame frame) {
        Bitmap bitmap = frame.getBitmap();

        int bytes = bitmap.getByteCount();
        ByteBuffer buffer = ByteBuffer.allocate(bytes);
        bitmap.copyPixelsToBuffer(buffer);

        frameReceiver.receiveFrame(buffer);

        SparseArray<Barcode> detectedItems = detector.detect(frame);

        for(int i = 0; i < detectedItems.size(); ++i) {
            Barcode barcode = detectedItems.valueAt(0);
            communicator.qrRead(barcode.displayValue);
        }

        return detectedItems;
    }

    @Override
    public void receiveFrame(Frame frame) {
        detect(frame);
    }
}