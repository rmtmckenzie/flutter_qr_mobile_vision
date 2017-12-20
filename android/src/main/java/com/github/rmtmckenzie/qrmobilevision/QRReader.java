package com.github.rmtmckenzie.qrmobilevision;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.util.Size;

import io.flutter.view.TextureRegistry;
import io.flutter.plugin.common.MethodChannel.Result;

import com.google.android.gms.vision.CameraSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


class QRReader {
    private final Context context;
    private Heartbeat heartbeat;
    private CameraSource camera;
    QrCamera qrCamera;
    private long textureId;


    private Result result;


    QRReader(int width, int height, Context context, final QRReaderCallbacks communicator,
             final TextureRegistry.SurfaceTextureEntry textureEntry, Result result) {
        this.context = context;
        this.result = result;

        SurfaceTexture texture = textureEntry.surfaceTexture();
        this.textureId = textureEntry.id();
        qrCamera = android.os.Build.VERSION.SDK_INT >= 21 ?
                new QrCameraC2(width,height,context,texture,new QrDetector(communicator,context)) :
                new QrCameraC1(width,height,texture,new QrDetector(communicator,context));
//        qrCamera = new QrCameraC1(width,height,texture,new QrDetector(communicator,context));
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
                            //TODO: return properly
                            e.printStackTrace();
                        }
                    } else {
                        System.out.print("PERMISSIONS WERE NOT GRANTED");
                    }
                }
            });

            Intent intent = new Intent(context, PermissionsActivity.class);
            context.startActivity(intent);

//            throw new Exception(Exception.Reason.noPermissions);
        } else {
            continueStarting(heartBeatTimeout);
        }
    }

    private void continueStarting(int heartBeatTimeout) throws IOException {

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

        result.success(textureId);

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


    //Only works on api>=21
    @TargetApi(21)
    List<int[]> getSupportedSizes() {

        List<int[]> sizeOutput = new ArrayList<int[]>();

        try {
            CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            //Get List of Supported Sizes
            String[] cameraId = manager.getCameraIdList();
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId[0]);
            StreamConfigurationMap configs = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Size[] sizes = configs.getOutputSizes(ImageFormat.JPEG);

            for (Size size : sizes) {
                int[] wxh = {size.getWidth(), size.getHeight()};
                sizeOutput.add(wxh);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return sizeOutput;
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
