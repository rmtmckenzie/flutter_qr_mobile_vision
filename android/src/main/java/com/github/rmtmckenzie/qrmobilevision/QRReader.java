package com.github.rmtmckenzie.qrmobilevision;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.SparseArray;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;
import java.nio.ByteBuffer;

public class QRReader {
    private final Context context;
    private final QRReaderCallbacks communicator;
    private Heartbeat heartbeat;
    private SplitBarcodeDetector detector;
    private CameraSource camera;


    QRReader(Context context, final QRReaderCallbacks communicator) {
        this.context = context;
        this.communicator = communicator;
    }

    public static class Exception extends java.lang.Exception {
        private Reason reason;

        enum Reason {
            noHardware,
            noPermissions
        }

        public Exception(Reason reason) {
            this.reason = reason;
        }

        public Reason reason() {
            return reason;
        }
    }

    void initializeDetector() {
        BarcodeDetector googleDetector = new BarcodeDetector.Builder(context.getApplicationContext()).setBarcodeFormats(
                Barcode.QR_CODE).build();

        googleDetector.setProcessor(new Detector.Processor<Barcode>() {

            @Override
            public void release() {
                // handled when camera is released
            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                SparseArray<Barcode> detectedItems = detections.getDetectedItems();

                for(int i = 0; i < detectedItems.size(); ++i) {
                    Barcode barcode = detectedItems.valueAt(0);
                    communicator.qrRead(barcode.displayValue);
                }
            }
        });

        SplitBarcodeDetector.FrameReceiver receiver = new SplitBarcodeDetector.FrameReceiver() {
            @Override
            public void receiveFrame(ByteBuffer frame) {
                communicator.cameraFrame(frame.array());
            }
        };

        detector = new SplitBarcodeDetector(googleDetector, receiver);
    }

    void start(int width, int height, int heartBeatTimeout) throws IOException, Exception {
        if (!hasCameraHardware(context)) {
            throw new Exception(Exception.Reason.noHardware);
        }

        if (!checkCameraPermission(context)) {
            throw new Exception(Exception.Reason.noPermissions);
        }

        initializeDetector();

        if (heartBeatTimeout > 0) {
            if (heartbeat != null) {
                heartbeat.stop();
            }
            heartbeat = new Heartbeat(heartBeatTimeout, new Runnable() {
                @Override
                public void run() {
                    stop();
                }
            });
        }

        camera = new CameraSource.Builder(context, detector)
                .setAutoFocusEnabled(this.hasAutofocus(context))
                .setRequestedPreviewSize(width, height)
                .build();

        camera.start();
    }

    void stop() {
        if (heartbeat != null) {
            heartbeat.stop();
        }

        if (camera != null) {
            camera.stop();
            // also stops detector
            camera.release();

            camera = null;
            detector = null;
        }

    }

    void heartBeat() {
        if (heartbeat != null) {
            heartbeat.beat();
        }
    }

    private boolean hasAutofocus(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_AUTOFOCUS);
    }

    private boolean hasCameraHardware(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    private boolean checkCameraPermission(Context context) {
        String permission = Manifest.permission.CAMERA;
        int res = context.checkCallingOrSelfPermission(permission);
        return res == PackageManager.PERMISSION_GRANTED;
    }
}
