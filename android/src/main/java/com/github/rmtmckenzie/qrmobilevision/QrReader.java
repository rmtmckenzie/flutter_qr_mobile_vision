package com.github.rmtmckenzie.qrmobilevision;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.util.Log;

import com.google.mlkit.vision.barcode.BarcodeScannerOptions;

import java.io.IOException;

class QrReader {
    private static final String TAG = "cgr.qrmv.QrReader";
    final QrCamera qrCamera;
    private final Activity context;
    private final QRReaderStartedCallback startedCallback;
    private Heartbeat heartbeat;

    QrReader(int width, int height, Activity context, BarcodeScannerOptions options,
             final QRReaderStartedCallback startedCallback, final QrReaderCallbacks communicator,
             final SurfaceTexture texture) {
        this.context = context;
        this.startedCallback = startedCallback;

        if (android.os.Build.VERSION.SDK_INT >= 21) {
            Log.i(TAG, "Using new camera API.");
            qrCamera = new QrCameraC2(width, height, texture, context, new QrDetector(communicator, options));
        } else {
            Log.i(TAG, "Using old camera API.");
            qrCamera = new QrCameraC1(width, height, texture, context, new QrDetector(communicator, options));
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

        qrCamera.stop();
    }

    void heartBeat() {
        if (heartbeat != null) {
            heartbeat.beat();
        }
    }

    private boolean hasCameraHardware(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
        } else {
            @SuppressLint("UnsupportedChromeOsCameraSystemFeature")
            boolean hasFeature = context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);

            return hasFeature;
        }
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

    static class Exception extends java.lang.Exception {
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
