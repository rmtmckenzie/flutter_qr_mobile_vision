//package com.github.rmtmckenzie.qrmobilevision;
//
//import android.annotation.TargetApi;
//import android.content.Context;
//import android.media.Image;
//import android.util.Log;
//
//import androidx.annotation.GuardedBy;
//import androidx.annotation.NonNull;
//
//import com.google.android.gms.tasks.OnFailureListener;
//import com.google.android.gms.tasks.OnSuccessListener;
//import com.google.firebase.ml.vision.FirebaseVision;
//import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
//import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector;
//import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions;
//import com.google.firebase.ml.vision.common.FirebaseVisionImage;
//
//import java.util.List;
//
///**
// * Allows QrCamera classes to send frames to a Detector
// */
//@TargetApi(21)
//class QrDetector2 implements OnSuccessListener<List<FirebaseVisionBarcode>>, OnFailureListener {
//    private static final String TAG = "cgr.qrmv.QrDetector";
//    private final QrReaderCallbacks communicator;
//    private final FirebaseVisionBarcodeDetector detector;
//
//    @GuardedBy("this")
//    private Frame latestFrame;
//    @GuardedBy("this")
//    private Frame processingFrame;
//
//    QrDetector2(QrReaderCallbacks communicator, Context context, FirebaseVisionBarcodeDetectorOptions options) {
//        Log.i(TAG, "Making detector2 for formats: " + options);
//        this.communicator = communicator;
//
//        this.detector = FirebaseVision.getInstance().getVisionBarcodeDetector(options);
//    }
//
//
//    void detect(Image image, int firebaseOrientation) {
//        latestFrame = new QrDetector2.Frame(image, firebaseOrientation);
//
//        if (processingFrame == null) {
//            processLatest();
//        }
//    }
//
//    private synchronized void processLatest() {
//        processingFrame = latestFrame;
//        latestFrame = null;
//        if (processingFrame != null) {
//            processFrame(processingFrame);
//        }
//    }
//
//    private void processFrame(Frame frame) {
//        detector.detectInImage(frame.toImage())
//            .addOnSuccessListener(this)
//            .addOnFailureListener(this);
//    }
//
//    @Override
//    public void onSuccess(List<FirebaseVisionBarcode> firebaseVisionBarcodes) {
//        for (FirebaseVisionBarcode barcode : firebaseVisionBarcodes) {
//            communicator.qrRead(barcode.getRawValue());
//        }
//        processLatest();
//    }
//
//    @Override
//    public void onFailure(@NonNull Exception e) {
//        Log.w(TAG, "Barcode Reading Failure: ", e);
//    }
//
//    static class Frame {
//        final Image image;
//        final int firebaseOrientation;
//
//        Frame(Image image, int firebaseOrientation) {
//            this.image = image;
//            this.firebaseOrientation = firebaseOrientation;
//        }
//
//        FirebaseVisionImage toImage() {
//            return FirebaseVisionImage.fromMediaImage(image, firebaseOrientation);
//        }
//    }
//
//}
