package com.github.rmtmckenzie.qrmobilevision;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.util.Size;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.Surface;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CaptureRequest;

import io.flutter.view.TextureRegistry;
import io.flutter.plugin.common.MethodChannel.Result;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


public class QRReader {
    int targetHeight, targetWidth;
    Size size;
    private Size jpegSizes[] = null;
    private final Context context;
    private final QRReaderCallbacks communicator;
    private Heartbeat heartbeat;
    private Detector<Barcode> detector;
    private CameraSource camera;
    private CaptureRequest.Builder previewBuilder;
    private CameraCaptureSession previewSession;
    private AtomicInteger atomicCounter = new AtomicInteger(0);
    private ImageReader reader;


    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private Result result;

    int orientation;

    private CameraDevice cameraDevice;
    private final TextureRegistry.SurfaceTextureEntry textureEntry;


    QRReader(Context context, final QRReaderCallbacks communicator, final TextureRegistry.SurfaceTextureEntry textureEntry, Result result) {
        this.textureEntry = textureEntry;
        this.context = context;
        this.communicator = communicator;
        this.result = result;
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

        detector = new BarcodeDetector.Builder(context.getApplicationContext()).setBarcodeFormats(
                Barcode.QR_CODE).build();
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


        List<int[]> res; //wxh where [0]=w and [1]=h

        if (android.os.Build.VERSION.SDK_INT >= 21) {
            res = getSupportedSizes();
        } else {
            System.out.println("Old API(<21)");
            res = getSupportedSizedDepreciated();
        }
        

        openCamera();

        result.success(textureEntry.id());

    }


    @TargetApi(21)
    private void startPreview() {
        CameraCaptureSession.CaptureCallback listener = new CameraCaptureSession.CaptureCallback() {
            @Override
            public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                super.onCaptureCompleted(session, request, result);
                //startQr();
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


    private class ImageFrame {
        ImageFrame(byte[] bytes, int count,int width, int height) {
            this.bytes = bytes;
            this.count = count;
            this.width = width;
            this.height = height;
        }
        final byte[] bytes;
        //final ByteBuffer bytes;
        final int count;
        final int width;
        final int height;
    }

    private class qrTask extends AsyncTask<ImageFrame, Void, SparseArray<Barcode>> {
        @TargetApi(19)
        @Override
        protected SparseArray<Barcode> doInBackground(ImageFrame... imageFrames) {
            ImageFrame imageFrame = imageFrames[0];

            if (imageFrame.count < atomicCounter.get()) return null;


           Bitmap bmp = BitmapFactory.decodeByteArray(imageFrame.bytes, 0, imageFrame.bytes.length);


            Frame.Builder frameBuilder = new Frame.Builder();
//            frameBuilder.setImageData(imageFrame.bytes, imageFrame.width, imageFrame.height, ImageFormat.NV21);
            frameBuilder.setBitmap(bmp);
            Frame frame = frameBuilder.build();

            return detector.detect(frame);
        }

        @Override
        protected void onPostExecute(SparseArray<Barcode> detectedItems) {
            if(detectedItems == null) return;
            for(int i = 0; i < detectedItems.size(); ++i) {
                communicator.qrRead(detectedItems.valueAt(i).displayValue);
            }
        }
    }


    @TargetApi(21)
    private void startCamera() {
        List<Surface> list = new ArrayList<Surface>();

        Size jpegSize = getAppropriateSize(jpegSizes);

        int width = jpegSize.getWidth(), height = jpegSize.getHeight();

        for (Size s : jpegSizes) {
            System.out.print(s.getWidth());
            System.out.print("  ");
            System.out.println(s.getHeight());
        }

        System.out.println("JPEG WIDTH: " + jpegSize.getWidth());
        System.out.println("JPEG HEIGHT: " + jpegSize.getHeight());

        reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 2);
        list.add(reader.getSurface());

        ImageReader.OnImageAvailableListener imageAvailableListener = new ImageReader.OnImageAvailableListener() {

            @Override
            public void onImageAvailable(ImageReader reader) {


                int count = atomicCounter.incrementAndGet();

                try(Image image = reader.acquireLatestImage()){
                    if(image == null) return;
                    Image.Plane[] planes = image.getPlanes();

//                    ByteBuffer b1 = planes[0].getBuffer(),
//                            b2 = planes[1].getBuffer(),
//                            b3 = planes[2].getBuffer();

                    ByteBuffer buffer = planes[0].getBuffer();
                    byte[] bytes  = new byte[buffer.remaining()];
                    buffer.get(bytes);

//                    ByteBuffer bAll = ByteBuffer.allocateDirect(b1.remaining() + b2.remaining() + b3.remaining());
//                    bAll.put(b1);
//                    bAll.put(b3);
//                    bAll.put(b2);

                    new qrTask().execute(new ImageFrame(bytes, count,image.getWidth(),image.getHeight()));
                }catch(Throwable t){
                    t.printStackTrace();
                }

            }
        };
        reader.setOnImageAvailableListener(imageAvailableListener, null);
        SurfaceTexture texture = textureEntry.surfaceTexture();
        texture.setDefaultBufferSize(size.getWidth(), size.getHeight());
        list.add(new Surface(texture));
        try {
            previewBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewBuilder.addTarget(list.get(0));
            previewBuilder.addTarget(list.get(1));
            previewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            previewBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(orientation));
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


    //@TargetApi(21)
    private CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice device) {
            cameraDevice = device;
            startCamera();
            //startQr();
        }

        @Override
        public void onDisconnected(CameraDevice device) {
        }

        @Override
        public void onError(CameraDevice device, int error) {
        }
    };

    @TargetApi(21)
    private Size getAppropriateSize(Size[] sizes) {
        Size s = sizes[0];
        if(orientation % 180 == 0) {
            for (Size size : sizes) {
                if (size.getHeight() < targetHeight || size.getWidth() < targetWidth) {
                    break;
                }
                s = size;
            }
        }
        else{
            for (Size size : sizes) {
                if (size.getHeight() < targetWidth || size.getWidth() < targetHeight) {
                    break;
                }
                s = size;
            }
        }
        return s;
    }

    @TargetApi(21)
    private void openCamera() { //REQUIRES API LEVEL 21
        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {

            String id = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(id);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            try {
                orientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                size = getAppropriateSize( map.getOutputSizes(SurfaceTexture.class));
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
        if(cameraDevice != null){
            cameraDevice.close();
            reader.close();
        }

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


    //For API<21 i.e. Android 4.4 - KitKat
    List<int[]> getSupportedSizedDepreciated() {
        Camera c;
        List<int[]> sizeOutput = new ArrayList<int[]>();

        List<Camera.Size> sizes = null;
        try {
            c = Camera.open();
            Camera.Parameters parameters = c.getParameters();
            sizes = parameters.getSupportedPreviewSizes();

            for (Camera.Size size : sizes) {
                int[] wxh = {size.width, size.height};
                sizeOutput.add(wxh);
            }
            c.release();


        } catch (java.lang.Exception e) {
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
