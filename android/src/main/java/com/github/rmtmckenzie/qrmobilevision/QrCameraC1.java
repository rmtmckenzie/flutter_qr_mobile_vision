package com.github.rmtmckenzie.qrmobilevision;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.WindowManager;

import com.google.android.gms.vision.Frame;
import com.google.mlkit.vision.common.InputImage;

import java.io.IOException;
import java.util.List;

/**
 * Implements QrCamera using Deprecated Camera API
 * NOTE: uses fully qualified names for android.hardware.Camera
 * so that deprecation warnings can be avoided.
 */
@TargetApi(16)
class QrCameraC1 {

    private static final String TAG = "cgr.qrmv.QrCameraC1";
    private static final int IMAGEFORMAT = ImageFormat.NV21;
    private final SurfaceTexture texture;
    private final QrDetector detector;
    private android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
    private int targetWidth, targetHeight;
    private android.hardware.Camera camera = null;
    private Context context;

    private AutoFocusManager autoFocusManager;

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    QrCameraC1(int width, int height, SurfaceTexture texture, Context context, QrDetector detector) {
        this.texture = texture;
        targetHeight = height;
        targetWidth = width;
        this.detector = detector;
        this.context = context;
    }

    private int getFirebaseOrientation() {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        int deviceRotation = windowManager.getDefaultDisplay().getRotation();
        int rotationCompensation = (ORIENTATIONS.get(deviceRotation) + info.orientation + 270) % 360;

        // Return the corresponding FirebaseVisionImageMetadata rotation value.
        return rotationCompensation;
    }

    public void start() throws QrReader.Exception {
        int numberOfCameras = android.hardware.Camera.getNumberOfCameras();
        info = new android.hardware.Camera.CameraInfo();
        for (int i = 0; i < numberOfCameras; i++) {
            android.hardware.Camera.getCameraInfo(i, info);
            if (info.facing == android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK) {
                camera = android.hardware.Camera.open(i);
                break;
            }
        }

        if (camera == null) {
            throw new QrReader.Exception(QrReader.Exception.Reason.noBackCamera);
        }

        final android.hardware.Camera.Parameters parameters = camera.getParameters();

        List<String> focusModes = parameters.getSupportedFocusModes();
        if (focusModes.contains(android.hardware.Camera.Parameters.FOCUS_MODE_AUTO)) {
            Log.i(TAG, "Initializing with autofocus on.");
            parameters.setFocusMode(android.hardware.Camera.Parameters.FOCUS_MODE_AUTO);
        } else {
            Log.i(TAG, "Initializing with autofocus off as not supported.");
        }

        List<android.hardware.Camera.Size> supportedSizes = parameters.getSupportedPreviewSizes();
        android.hardware.Camera.Size size = getAppropriateSize(supportedSizes);

        parameters.setPreviewSize(size.width, size.height);
        texture.setDefaultBufferSize(size.width, size.height);

        parameters.setPreviewFormat(IMAGEFORMAT);

        try {
            camera.setPreviewCallback(new android.hardware.Camera.PreviewCallback() {
                @Override
                public void onPreviewFrame(byte[] data, android.hardware.Camera camera) {
                    android.hardware.Camera.Size previewSize = camera.getParameters().getPreviewSize();

                    if (data != null) {
                        detector.detect(InputImage.fromByteArray(data, previewSize.width, previewSize.height, getFirebaseOrientation(), IMAGEFORMAT));
                    } else {
                        //TODO: something better here?
                        System.out.println("It's NULL!");
                    }
                }
            });
            camera.setPreviewTexture(texture);
            camera.startPreview();
            autoFocusManager = new AutoFocusManager(camera);
            autoFocusManager.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    // portrait orientation
    public int getWidth() {
        return camera.getParameters().getPreviewSize().height;
    }

    // portrait orientation
    public int getHeight() {
        return camera.getParameters().getPreviewSize().width;
    }

    public int getOrientation() {
        return (info.orientation + 270) % 360;
    }

    public void toggleFlash() {
        boolean wasAutoFocusManager = autoFocusManager != null;
        if (wasAutoFocusManager) {
            autoFocusManager.stop();
            autoFocusManager = null;
        }
        CameraConfigurationUtils.toggleTorch(camera);
        if (wasAutoFocusManager) {
            autoFocusManager = new AutoFocusManager(camera);
            autoFocusManager.start();
        }
    }

    public void stop() {
        try {
            if (autoFocusManager != null) {
                autoFocusManager.stop();
                autoFocusManager = null;
            }

            if (camera != null) {
                camera.stopPreview();
                camera.setPreviewCallback(null);
                camera.release();
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    //Size here is Camera.Size, not android.util.Size as in the QrCameraC2 version of this method
    private android.hardware.Camera.Size getAppropriateSize(List<android.hardware.Camera.Size> sizes) {
        // assume sizes is never 0
        if (sizes.size() == 1) {
            return sizes.get(0);
        }

        android.hardware.Camera.Size s = sizes.get(0);
        android.hardware.Camera.Size s1 = sizes.get(1);

        if (s1.width > s.width || s1.height > s.height) {
            // ascending
            if (info.orientation % 180 == 0) {
                for (android.hardware.Camera.Size size : sizes) {
                    s = size;
                    if (size.height > targetHeight && size.width > targetWidth) {
                        break;
                    }
                }
            } else {
                for (android.hardware.Camera.Size size : sizes) {
                    s = size;
                    if (size.height > targetWidth && size.width > targetHeight) {
                        break;
                    }
                }
            }
        } else {
            // descending
            if (info.orientation % 180 == 0) {
                for (android.hardware.Camera.Size size : sizes) {
                    if (size.height < targetHeight || size.width < targetWidth) {
                        break;
                    }
                    s = size;
                }
            } else {
                for (android.hardware.Camera.Size size : sizes) {
                    if (size.height < targetWidth || size.width < targetHeight) {
                        break;
                    }
                    s = size;
                }
            }
        }
        return s;
    }
}
