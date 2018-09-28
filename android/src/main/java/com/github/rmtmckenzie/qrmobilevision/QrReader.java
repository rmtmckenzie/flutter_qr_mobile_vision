package com.github.rmtmckenzie.qrmobilevision;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.util.Log;
import com.google.android.gms.vision.CameraSource;

import java.io.IOException;

class QrReader {
    private static final String TAG = "cgr.qrmv.QrReader";
    final QrCamera qrCamera;
    private final Activity context;
    private final QRReaderStartedCallback startedCallback;
    private Heartbeat heartbeat;
    private CameraSource camera;

    QrReader(int width, int height, Activity context, int barcodeFormats,
             final QRReaderStartedCallback startedCallback, final QrReaderCallbacks communicator,
             final SurfaceTexture texture) {
        this.context = context;
        this.startedCallback = startedCallback;

        if (android.os.Build.VERSION.SDK_INT >= 21) {
            Log.i(TAG, "Using new camera API.");
            qrCamera = new QrCameraC2(width, height, context, texture, new QrDetector2(communicator, context, barcodeFormats));
        } else {
            Log.i(TAG, "Using old camera API.");
            qrCamera = new QrCameraC1(width, height, texture, new QrDetector(communicator, context, barcodeFormats));
        }
    }

    void start(final int heartBeatTimeout) throws IOException, NoPermissionException, Exception {
        if (!hasCameraHardware(context)) {
            throw new Exception(Exception.Reason.noHardware);
        }

        if (!checkCameraPermission(context)) {
            throw new NoPermissionException();
        } else {
            continueStarting(heartBeatTimeout);
        }
    }

    private void continueStarting(int heartBeatTimeout) throws IOException {
        try {
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

            qrCamera.start();
            startedCallback.started();
        } catch (Throwable t) {
            startedCallback.startingFailed(t);
        }
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
        }
        qrCamera.stop();
    }

    void heartBeat() {
        if (heartbeat != null) {
            heartbeat.beat();
        }
    }

    private boolean hasCameraHardware(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    private boolean checkCameraPermission(Context context) {
        String[] permissions = {Manifest.permission.CAMERA};

        int res = context.checkCallingOrSelfPermission(permissions[0]);
        return res == PackageManager.PERMISSION_GRANTED;
    }

    interface QRReaderStartedCallback {
        void started();

        void startingFailed(Throwable t);
    }

    public static class Exception extends java.lang.Exception {
        private Reason reason;

        Exception(Reason reason) {
            super("QR reader failed because " + reason.toString());
            this.reason = reason;
        }

        Reason reason() {
            return reason;
        }

        enum Reason {
            noHardware,
            noPermissions,
            noBackCamera
        }
    }
}
