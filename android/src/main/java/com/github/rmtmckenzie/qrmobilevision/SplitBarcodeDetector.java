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

    interface FrameReceiver {
        void receiveFrame(ByteBuffer frame);
    }

    public SplitBarcodeDetector(BarcodeDetector detector, FrameReceiver frameReceiver) {
        this.detector = detector;
        this.frameReceiver = frameReceiver;
    }

    @Override
    public SparseArray<Barcode> detect(Frame frame) {
        Bitmap bitmap = frame.getBitmap();


        int bytes = bitmap.getByteCount();
        ByteBuffer buffer = ByteBuffer.allocate(bytes);
        bitmap.copyPixelsToBuffer(buffer);

        frameReceiver.receiveFrame(buffer);

        return detector.detect(frame);
    }
}