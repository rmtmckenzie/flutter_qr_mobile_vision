package com.github.rmtmckenzie.qrmobilevision;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import static android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK;
import static android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT;


public class CameraInfo {

    private static final String TAG = "cgr.qrmv.CameraInfo";

    public final String name;
    private final Context context;

    CameraInfo(Context context, String name) {
        this.context = context;
        this.name = name;
    }

    public static List<CameraInfo> getCameraInfos(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            int numberOfCameras = Camera.getNumberOfCameras();
            List<CameraInfo> cameraInfos = new ArrayList<>(numberOfCameras);
            for (int i = 0; i < numberOfCameras; i++) {
                // just setting name to 'camera#' as doesn't have a name with this API
                cameraInfos.add(new CameraInfo(context, "camera" + i));
            }

            return cameraInfos;
        } else {
            CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            if (manager == null) {
                throw new RuntimeException("Couldn't get camera manager system service.");
            }
            try {
                String[] cameraIdList = manager.getCameraIdList();
                List<CameraInfo> cameraInfos = new ArrayList<>(cameraIdList.length);
                for (String id : cameraIdList) {
                    cameraInfos.add(new CameraInfo(context, id));
                }
                return cameraInfos;
            } catch (CameraAccessException e) {
                Log.w(TAG, "Error getting camera info.", e);
                throw new RuntimeException(e);
            }

        }
    }

    public CameraOrientation getOrientation() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            int id = Integer.parseInt(this.name.substring(6));
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(id, cameraInfo);

            int facing = cameraInfo.facing;

            switch (facing) {
                case CAMERA_FACING_BACK:
                    return CameraOrientation.back;
                case CAMERA_FACING_FRONT:
                    return CameraOrientation.forward;
                default:
                    // I guess if not set to front or back assume external.
                    return CameraOrientation.external;
            }
        } else {
            CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            if (manager == null) {
                throw new RuntimeException("Couldn't get camera manager system service.");
            }
            CameraCharacteristics cameraCharacteristics;
            try {
                cameraCharacteristics = manager.getCameraCharacteristics(name);
            } catch (CameraAccessException e) {
                Log.w(TAG, "Error getting camera characteristics.", e);
                throw new RuntimeException(e);
            }

            Integer orientationInt = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);

            // set default if null
            orientationInt = orientationInt == null ? CameraMetadata.LENS_FACING_EXTERNAL : orientationInt;

            switch (orientationInt) {
                case CameraMetadata.LENS_FACING_FRONT:
                    return CameraOrientation.forward;
                case CameraMetadata.LENS_FACING_BACK:
                    return CameraOrientation.back;
                case CameraMetadata.LENS_FACING_EXTERNAL:
                default:
                    return CameraOrientation.external;
            }
        }
    }

    public List<Size> getSupportedSizes() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            int id;
            if (this.name.startsWith("camera")) {
                id = Integer.parseInt(this.name.substring(6));
            } else {
                throw new IllegalArgumentException("Camera IDs only supported with format 'camera#' for Camera 1 API");
            }
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(id, cameraInfo);

            Camera camera = Camera.open(id);
            Camera.Parameters parameters = camera.getParameters();
            List<Camera.Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
            List<Size> sizes = new ArrayList<>(supportedPreviewSizes.size());
            for (Camera.Size size : supportedPreviewSizes) {
                sizes.add(new Size(size.width, size.height));
            }
            return sizes;
        } else {
            CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            if (manager == null) {
                throw new RuntimeException("Couldn't get camera manager system service.");
            }
            CameraCharacteristics cameraCharacteristics;
            try {
                cameraCharacteristics = manager.getCameraCharacteristics(name);
            } catch (CameraAccessException e) {
                Log.w(TAG, "Error getting camera info.", e);
                throw new RuntimeException(e);
            }


            StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (map == null) {
                throw new RuntimeException("Couldn't get stream configuration map");
            }
            android.util.Size[] outputSizes = map.getOutputSizes(SurfaceTexture.class);

            List<Size> sizes = new ArrayList<>(outputSizes.length);
            for (android.util.Size size : outputSizes) {
                sizes.add(new Size(size.getWidth(), size.getHeight()));
            }

            return sizes;
        }
    }


    enum CameraOrientation {
        forward, back, external
    }

    public static class Size {
        final int width;
        final int height;

        Size(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }

}
