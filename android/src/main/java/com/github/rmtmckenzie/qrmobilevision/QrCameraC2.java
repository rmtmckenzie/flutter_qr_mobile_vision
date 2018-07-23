package com.github.rmtmckenzie.qrmobilevision;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.*;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.support.annotation.NonNull;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements QrCamera using Camera2 API
 */
@TargetApi(21)
class QrCameraC2 implements QrCamera {

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
    private Size jpegSizes[] = null;
    private QrDetector detector;
    private int orientation;
    private CameraDevice cameraDevice;
    private CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice device) {
            cameraDevice = device;
            startCamera();
        }

        @Override
        public void onDisconnected(CameraDevice device) {
        }

        @Override
        public void onError(CameraDevice device, int error) {
        }
    };

    QrCameraC2(int width, int height, Context context, SurfaceTexture texture, QrDetector detector) {
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
        return orientation;
    }

    @Override
    public void start() {
        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {

            String id = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(id);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            try {
                // it seems as though the orientation is already corrected, so setting to 0
                orientation = 0;
                // orientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                size = getAppropriateSize(map.getOutputSizes(SurfaceTexture.class));
                //size = map.getOutputSizes(SurfaceTexture.class)[0];
                jpegSizes = map.getOutputSizes(ImageFormat.JPEG);
            } catch (java.lang.NullPointerException e) {
                e.printStackTrace();
            }


            manager.openCamera(id, stateCallback, null);

        } catch (SecurityException s) {
            System.out.println("### Security Exception ###");
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void startCamera() {
        List<Surface> list = new ArrayList<>();

        Size jpegSize = getAppropriateSize(jpegSizes);

        int width = jpegSize.getWidth(), height = jpegSize.getHeight();

        reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 2);
        list.add(reader.getSurface());

        ImageReader.OnImageAvailableListener imageAvailableListener = new ImageReader.OnImageAvailableListener() {

            @Override
            public void onImageAvailable(ImageReader reader) {

                try (Image image = reader.acquireLatestImage()) {
                    if (image == null) return;
                    Image.Plane[] planes = image.getPlanes();

//                    ByteBuffer b1 = planes[0].getBuffer(),
//                            b2 = planes[1].getBuffer(),
//                            b3 = planes[2].getBuffer();

                    ByteBuffer buffer = planes[0].getBuffer();
                    byte[] bytes = new byte[buffer.remaining()];
                    buffer.get(bytes);

//                    ByteBuffer bAll = ByteBuffer.allocateDirect(b1.remaining() + b2.remaining() + b3.remaining());
//                    bAll.put(b1);
//                    bAll.put(b3);
//                    bAll.put(b2);

                    detector.detect(bytes);
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
            previewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
//            previewBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(orientation));
        } catch (java.lang.Exception e) {
            e.printStackTrace();
            return;
        }


        try {
            cameraDevice.createCaptureSession(list, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    previewSession = session;
                    startPreview();

                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
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

        previewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
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
            if (orientation % 180 == 0) {
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
            if (orientation % 180 == 0) {
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
