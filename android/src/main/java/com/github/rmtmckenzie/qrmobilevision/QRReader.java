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
        SplitBarcodeDetector.FrameReceiver frameReceiver = new SplitBarcodeDetector.FrameReceiver() {
            @Override
            public void receiveFrame(ByteBuffer frame) {
                communicator.cameraFrame(frame.array());
            }
        };

        SplitBarcodeDetector.QRReceiver qrReceiver = new SplitBarcodeDetector.QRReceiver() {
            @Override
            public void receiveQr(Barcode barcode) {
                communicator.qrRead(barcode.displayValue);
            }
        }

        detector = new SplitBarcodeDetector(context, googleDetector, frameReceiver);
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
