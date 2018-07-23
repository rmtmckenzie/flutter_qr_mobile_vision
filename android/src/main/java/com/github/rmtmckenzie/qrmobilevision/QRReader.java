package com.github.rmtmckenzie.qrmobilevision;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;

import com.google.android.gms.vision.CameraSource;

import java.io.IOException;


class QRReader {
    private final Context context;
    private Heartbeat heartbeat;
    private CameraSource camera;
    QrCamera qrCamera;

    interface QRReaderStartedCallback {
        void started();

        void startingFailed(Throwable t);
    }

    private QRReaderStartedCallback startedCallback;


    QRReader(int width, int height, Context context, int barcodeFormats, final QRReaderStartedCallback startedCallback, final QRReaderCallbacks communicator,
             final SurfaceTexture texture) {
        this.context = context;
        this.startedCallback = startedCallback;

        qrCamera = android.os.Build.VERSION.SDK_INT >= 21 ?
                new QrCameraC2(width, height, context, texture, new QrDetector(communicator, context, barcodeFormats)) :
                new QrCameraC1(width, height, texture, new QrDetector(communicator, context, barcodeFormats));
    }

    public static class Exception extends java.lang.Exception {
        private Reason reason;

        enum Reason {
            noHardware,
            noPermissions,
            noBackCamera
        }

        public Exception(Reason reason) {
            super("QR reader failed because " + reason.toString());
            this.reason = reason;
        }

        Reason reason() {
            return reason;
        }
    }


    void start(final int heartBeatTimeout) throws IOException, Exception {
        if (!hasCameraHardware(context)) {
            throw new Exception(Exception.Reason.noHardware);
        }

        if (!checkCameraPermission(context)) {
            PermissionsActivity.setCallback(new PermissionsActivity.PermissionsCallback() {
                @Override
                public void permissionsGranted(boolean wereGranted) {
                    if (wereGranted) {
                        System.out.println("PERMISSIONS WERE GRANTED");
                        try {
                            continueStarting(heartBeatTimeout);
                        } catch (IOException e) {
                            startedCallback.startingFailed(e);
                        }
                    } else {
                        startedCallback.startingFailed(new Exception(Exception.Reason.noPermissions));
                    }
                }
            });

            Intent intent = new Intent(context, PermissionsActivity.class);
            context.startActivity(intent);
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

    private boolean hasAutofocus(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_AUTOFOCUS);
    }

    private boolean hasCameraHardware(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    private boolean checkCameraPermission(Context context) {
        String[] permissions = {Manifest.permission.CAMERA};

        int res = context.checkCallingOrSelfPermission(permissions[0]);
        return res == PackageManager.PERMISSION_GRANTED;
    }
}
