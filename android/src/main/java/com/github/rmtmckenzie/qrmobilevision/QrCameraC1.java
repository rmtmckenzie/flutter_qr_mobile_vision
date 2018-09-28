package com.github.rmtmckenzie.qrmobilevision;

import android.annotation.TargetApi;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.util.Log;

import java.io.IOException;
import java.util.List;

/**
 * Implements QrCamera using Deprecated Camera API
 */
@TargetApi(16)
@SuppressWarnings("deprecation")
class QrCameraC1 implements QrCamera {

    private static final String TAG = "cgr.qrmv.QrCameraC1";
    private final SurfaceTexture texture;
    private final QrDetector detector;
    private Camera.CameraInfo info = new Camera.CameraInfo();
    private int targetWidth, targetHeight;
    private Camera camera = null;

    QrCameraC1(int width, int height, SurfaceTexture texture, QrDetector detector) {
        this.texture = texture;
        targetHeight = height;
        targetWidth = width;
        this.detector = detector;
    }

    @Override
    public void start() throws QrReader.Exception {
        int numberOfCameras = Camera.getNumberOfCameras();
        info = new Camera.CameraInfo();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                camera = Camera.open(i);
                break;
            }
        }

        if (camera == null) {
            throw new QrReader.Exception(QrReader.Exception.Reason.noBackCamera);
        }

        Camera.Parameters parameters = camera.getParameters();

        List<String> focusModes = parameters.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            Log.i(TAG, "Initializing with autofocus on.");
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        } else {
            Log.i(TAG, "Initializing with autofocus off as not supported.");
        }

        List<Size> supportedSizes = parameters.getSupportedPreviewSizes();
        Size size = getAppropriateSize(supportedSizes);

        parameters.setPreviewSize(size.width, size.height);

        texture.setDefaultBufferSize(size.width, size.height);

        detector.useNV21(size.width, size.height);

        try {
            camera.setPreviewCallback(new Camera.PreviewCallback() {
                @Override
                public void onPreviewFrame(byte[] data, Camera camera) {
                    if (data != null) detector.detect(data);
                    else System.out.println("It's NULL!");
                }
            });
            camera.setPreviewTexture(texture);
            camera.startPreview();
            camera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public int getWidth() {
        return camera.getParameters().getPreviewSize().width;
    }

    @Override
    public int getHeight() {
        return camera.getParameters().getPreviewSize().height;
    }

    @Override
    public int getOrientation() {
        return info.orientation;
    }

    @Override
    public void stop() {
        camera.stopPreview();
        camera.setPreviewCallback(null);
        camera.release();
    }

    //Size here is Camera.Size, not android.util.Size as in the QrCameraC2 version of this method
    private Size getAppropriateSize(List<Size> sizes) {
        // assume sizes is never 0
        if (sizes.size() == 1) {
            return sizes.get(0);
        }

        Size s = sizes.get(0);
        Size s1 = sizes.get(1);

        if (s1.width > s.width || s1.height > s.height) {
            // ascending
            if (info.orientation % 180 == 0) {
                for (Size size : sizes) {
                    s = size;
                    if (size.height > targetHeight && size.width > targetWidth) {
                        break;
                    }
                }
            } else {
                for (Size size : sizes) {
                    s = size;
                    if (size.height > targetWidth && size.width > targetHeight) {
                        break;
                    }
                }
            }
        } else {
            // descending
            if (info.orientation % 180 == 0) {
                for (Size size : sizes) {
                    if (size.height < targetHeight || size.width < targetWidth) {
                        break;
                    }
                    s = size;
                }
            } else {
                for (Size size : sizes) {
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
