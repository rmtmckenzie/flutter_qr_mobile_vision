package com.github.rmtmckenzie.qrmobilevision;

import android.content.Context;
import android.graphics.Rect;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;

import java.util.List;

/**
 * Allows QrCamera classes to send frames to a Detector
 */

class QrDetector implements OnSuccessListener<List<Barcode>>, OnFailureListener {
    private static final String TAG = "cgr.qrmv.QrDetector";
    private final QrMobileVisionPlugin communicator;
    private final BarcodeScanner detector;

    @GuardedBy("this")
    private InputImage latestFrame;

    @GuardedBy("this")
    private InputImage processingFrame;
    private int width;
    private int height;

    QrDetector(QrMobileVisionPlugin communicator, BarcodeScannerOptions opts) {
        this.communicator = communicator;
        this.detector = BarcodeScanning.getClient(opts);
    }

    void detect(InputImage frame) {
        latestFrame = frame;

        if (processingFrame == null) {
            processLatest();
        }
    }

    private synchronized void processLatest() {
        processingFrame = latestFrame;
        latestFrame = null;
        if (processingFrame != null) {
            processFrame(processingFrame);
        }
    }

    private void processFrame(InputImage frame) {
        if (this.width == 0 && this.height == 0) {
            this.width = frame.getWidth();
            this.height = frame.getHeight();
        }

        detector.process(frame)
            .addOnSuccessListener(this)
            .addOnFailureListener(this);
    }

    @Override
    public void onSuccess(List<Barcode> firebaseVisionBarcodes) {
        for (Barcode barcode : firebaseVisionBarcodes) {
            if (processingFrame != null) {
                Rect rect = barcode.getBoundingBox();
                Rect center = new Rect(width / 3, height / 3, width * 2 / 3, height * 2 / 3);
                if (rect.intersect(center)) {
                    communicator.qrRead(barcode.getRawValue());
                }
            }
        }
        processLatest();
    }

    @Override
    public void onFailure(@NonNull Exception e) {
        Log.w(TAG, "Barcode Reading Failure: ", e);
    }
}