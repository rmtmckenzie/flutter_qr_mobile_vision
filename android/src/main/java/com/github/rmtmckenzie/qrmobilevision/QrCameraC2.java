package com.github.rmtmckenzie.qrmobilevision;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.google.mlkit.vision.common.InputImage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static android.hardware.camera2.CameraMetadata.CONTROL_AF_MODE_AUTO;
import static android.hardware.camera2.CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE;
import static android.hardware.camera2.CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO;
import static android.hardware.camera2.CameraMetadata.LENS_FACING_BACK;

/**
 * Implements QrCamera using Camera2 API
 */
@TargetApi(21)
@RequiresApi(21)
class QrCameraC2 implements QrCamera {

    private static final String TAG = "cgr.qrmv.QrCameraC2";
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private final int targetWidth;
    private final int targetHeight;
    private final Context context;
    private final SurfaceTexture texture;
    private Size size;
    private ImageReader reader;
    private CaptureRequest.Builder previewBuilder;
    private CameraCaptureSession previewSession;
    private Size[] jpegSizes = null;
    private QrDetector detector;
    private int sensorOrientation;
    private CameraDevice cameraDevice;
    private CameraCharacteristics cameraCharacteristics;
    private Frame latestFrame;

    QrCameraC2(int width, int height, SurfaceTexture texture, Context context, QrDetector detector) {
        this.targetWidth = width;
        this.targetHeight = height;
        this.context = context;
        this.texture = texture;
        this.detector = detector;
    }

    @Override
    public int getWidth() {
        return size.getWidth();
    }

    @Override
    public int getHeight() {
        return size.getHeight();
    }

    @Override
    public int getOrientation() {
        // ignore sensor orientation of devices with 'reverse landscape' orientation of sensor
        // as camera2 api seems to already rotate the output.
        return sensorOrientation == 270 ? 90 : sensorOrientation;
    }

    private int getFirebaseOrientation() {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        int deviceRotation = windowManager.getDefaultDisplay().getRotation();
        int rotationCompensation = (ORIENTATIONS.get(deviceRotation) + sensorOrientation + 270) % 360;

        // Return the corresponding FirebaseVisionImageMetadata rotation value.
        int result;
        switch (rotationCompensation) {
            case 0:
                result = 0;
                break;
            case 90:
                result = 90;
                break;
            case 180:
                result = 180;
                break;
            case 270:
                result = 270;
                break;
            default:
                result = 0;
                Log.e(TAG, "Bad rotation value: " + rotationCompensation);
        }
        return result;
    }

    @Override
    public void start() throws QrReader.Exception {
        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);

        if (manager == null) {
            throw new RuntimeException("Unable to get camera manager.");
        }

        String cameraId = null;
        try {
            String[] cameraIdList = manager.getCameraIdList();
            for (String id : cameraIdList) {
                CameraCharacteristics cameraCharacteristics = manager.getCameraCharacteristics(id);
                Integer integer = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
                if (integer != null && integer == LENS_FACING_BACK) {
                    cameraId = id;
                    break;
                }
            }
        } catch (CameraAccessException e) {
            Log.w(TAG, "Error getting back camera.", e);
            throw new RuntimeException(e);
        }

        if (cameraId == null) {
            throw new QrReader.Exception(QrReader.Exception.Reason.noBackCamera);
        }

        try {
            cameraCharacteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Integer sensorOrientationInteger = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            sensorOrientation = sensorOrientationInteger == null ? 0 : sensorOrientationInteger;

            size = getAppropriateSize(map.getOutputSizes(SurfaceTexture.class));
            jpegSizes = map.getOutputSizes(ImageFormat.JPEG);

            manager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice device) {
                    cameraDevice = device;
                    startCamera();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice device) {
                }

                @Override
                public void onError(@NonNull CameraDevice device, int error) {
                    Log.w(TAG, "Error opening camera: " + error);
                }
            }, null);
        } catch (CameraAccessException e) {
            Log.w(TAG, "Error getting camera configuration.", e);
        }
    }

    private Integer afMode(CameraCharacteristics cameraCharacteristics) {

        int[] afModes = cameraCharacteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);

        if (afModes == null) {
            return null;
        }

        HashSet<Integer> modes = new HashSet<>(afModes.length * 2);
        for (int afMode : afModes) {
            modes.add(afMode);
        }

        if (modes.contains(CONTROL_AF_MODE_CONTINUOUS_VIDEO)) {
            return CONTROL_AF_MODE_CONTINUOUS_VIDEO;
        } else if (modes.contains(CONTROL_AF_MODE_CONTINUOUS_PICTURE)) {
            return CONTROL_AF_MODE_CONTINUOUS_PICTURE;
        } else if (modes.contains(CONTROL_AF_MODE_AUTO)) {
            return CONTROL_AF_MODE_AUTO;
        } else {
            return null;
        }
    }

    static class Frame implements QrDetector.Frame {
        final Image image;
        final int firebaseOrientation;

        Frame(Image image, int firebaseOrientation) {
            this.image = image;
            this.firebaseOrientation = firebaseOrientation;
        }

        @Override
        public InputImage toImage() {
            return InputImage.fromMediaImage(image, firebaseOrientation);
        }

        @Override
        public void close() {
            image.close();
        }

    }

    private void startCamera() {
        List<Surface> list = new ArrayList<>();

        Size jpegSize = getAppropriateSize(jpegSizes);

        final int width = jpegSize.getWidth(), height = jpegSize.getHeight();
        reader = ImageReader.newInstance(width, height, ImageFormat.YUV_420_888, 5);

        list.add(reader.getSurface());

        ImageReader.OnImageAvailableListener imageAvailableListener = new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                try {
                    Image image = reader.acquireLatestImage();
                    if (image == null) return;
                    latestFrame = new Frame(image, getFirebaseOrientation());
                    detector.detect(latestFrame);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        };

        reader.setOnImageAvailableListener(imageAvailableListener, null);

        texture.setDefaultBufferSize(size.getWidth(), size.getHeight());
        list.add(new Surface(texture));
        try {
            previewBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewBuilder.addTarget(list.get(0));
            previewBuilder.addTarget(list.get(1));

            Integer afMode = afMode(cameraCharacteristics);

            previewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

            if (afMode != null) {
                previewBuilder.set(CaptureRequest.CONTROL_AF_MODE, afMode);
                Log.i(TAG, "Setting af mode to: " + afMode);
                if (afMode == CONTROL_AF_MODE_AUTO) {
                    previewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
                } else {
                    previewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);
                }
            }
        } catch (java.lang.Exception e) {
            e.printStackTrace();
            return;
        }

        try {
            cameraDevice.createCaptureSession(list, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    previewSession = session;
                    startPreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    System.out.println("### Configuration Fail ###");
                }
            }, null);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void startPreview() {
        CameraCaptureSession.CaptureCallback listener = new CameraCaptureSession.CaptureCallback() {
            @Override
            public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                super.onCaptureCompleted(session, request, result);
            }
        };

        if (cameraDevice == null) return;

        try {
            previewSession.setRepeatingRequest(previewBuilder.build(), listener, null);
        } catch (java.lang.Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        if (cameraDevice != null) {
            cameraDevice.close();
        }
        if (reader != null) {
            if (latestFrame != null) latestFrame.close();
            latestFrame = null;
            reader.close();
        }
    }

    private Size getAppropriateSize(Size[] sizes) {
        // assume sizes is never 0
        if (sizes.length == 1) {
            return sizes[0];
        }

        Size s = sizes[0];
        Size s1 = sizes[1];

        if (s1.getWidth() > s.getWidth() || s1.getHeight() > s.getHeight()) {
            // ascending
            if (sensorOrientation % 180 == 0) {
                for (Size size : sizes) {
                    s = size;
                    if (size.getHeight() > targetHeight && size.getWidth() > targetWidth) {
                        break;
                    }
                }
            } else {
                for (Size size : sizes) {
                    s = size;
                    if (size.getHeight() > targetWidth && size.getWidth() > targetHeight) {
                        break;
                    }
                }
            }
        } else {
            // descending
            if (sensorOrientation % 180 == 0) {
                for (Size size : sizes) {
                    if (size.getHeight() < targetHeight || size.getWidth() < targetWidth) {
                        break;
                    }
                    s = size;
                }
            } else {
                for (Size size : sizes) {
                    if (size.getHeight() < targetWidth || size.getWidth() < targetHeight) {
                        break;
                    }
                    s = size;
                }
            }
        }
        return s;
    }
}
