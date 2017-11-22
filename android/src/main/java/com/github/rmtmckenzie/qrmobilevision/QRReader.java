package com.github.rmtmckenzie.qrmobilevision;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CaptureRequest;

import io.flutter.view.TextureRegistry;
import io.flutter.plugin.common.MethodChannel.Result;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;

import java.io.IOException;
import java.lang.annotation.Target;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;


public class QRReader {
    private Size size;
    private Size jpegSizes[] = null;
    private final Context context;
    private final QRReaderCallbacks communicator;
    private Heartbeat heartbeat;
    private SplitBarcodeDetector detector;
    private CameraSource camera;
    private CaptureRequest.Builder previewBuilder;
    private CameraCaptureSession previewSession;


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
        SplitBarcodeDetector.FrameReceiver frameReceiver = new SplitBarcodeDetector.FrameReceiver() {
            @Override
            public void receiveFrame(byte[] frame, int rotation) {
                //communicator.cameraFrame(frame, rotation); //Baraka
            }
        };

        SplitBarcodeDetector.QRReceiver qrReceiver = new SplitBarcodeDetector.QRReceiver() {
            @Override
            public void receiveQr(Barcode barcode) {
                communicator.qrRead(barcode.displayValue);
            }
        };

        detector = new SplitBarcodeDetector(context, frameReceiver, qrReceiver);
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


//        System.out.println("Dimensions before: width-"+width+" height-"+height);
//        System.out.print("Available Dimensions:  ");
//        for(int[] size : res){
//            System.out.print(" "+size[0]+"x"+size[1]+" ");
//        }

//
//        ListIterator i = res.listIterator();
//        int[] pair = null;
//
//        while (i.hasNext()) {
//            pair = (int[]) i.next();
//            if (pair[0] < width || pair[1] < height) {
//
//                //Fill
//                if (fill && i.previousIndex() == 1) {
//                    //Iterator holds place in front of current element
//                    i.previous();
//                    pair = (int[]) i.previous();
//                    width = pair[0];
//                    height = pair[1];
//                    break;
//                }
//                //Fit
//                else if (pair[0] <= width && pair[1] <= height) {
//                    width = pair[0];
//                    height = pair[1];
//                    break;
//                }
//            }
//        }
//        //if given dimensions are smaller than the smallest available, return smallest available
//        if (!i.hasNext() && pair != null) {
//            width = pair[0];
//            height = pair[1];
//        }

        openCamera();

//        System.out.println("\nDimensions after: width-"+width+" height-"+height);

//        camera = new CameraSource.Builder(context, detector)
//                .setAutoFocusEnabled(this.hasAutofocus(context))
//                .setRequestedPreviewSize(width, height)
//                .build();
//
//        //Surface surface = new Surface(textureEntry.surfaceTexture());
//        try{
//            camera.start();
//        } catch(SecurityException e){
//            e.printStackTrace();
//        }

        result.success(textureEntry.id());

    }



    @TargetApi(21)
    private void startPreview() {
        CameraCaptureSession.CaptureCallback listener = new CameraCaptureSession.CaptureCallback(){
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


    @TargetApi(21)
    private void startQr(){

        int width = jpegSizes[0].getWidth(), height = jpegSizes[0].getHeight();
        ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
        Surface surface = reader.getSurface();
        final CaptureRequest.Builder captureBuilder;
        try{
            captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE,CameraMetadata.CONTROL_MODE_AUTO);
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION,ORIENTATIONS.get(orientation));


        ImageReader.OnImageAvailableListener imageAvailableListener = new ImageReader.OnImageAvailableListener(){
            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image = null;
                try {
                    image = reader.acquireLatestImage();
                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();

                    Frame.Builder frameBuilder = new Frame.Builder();
                    frameBuilder.setImageData(buffer, image.getWidth(), image.getHeight(), image.getFormat());
                    Frame frame = frameBuilder.build();

                    detector.receiveFrame(frame);

                } catch (java.lang.Exception e) {
                    e.printStackTrace();
                } finally {
                    if (image != null) image.close();
                }
            }
        };
        HandlerThread handlerThread = new HandlerThread("qr_capture");
        handlerThread.start();
        final Handler handler = new Handler(handlerThread.getLooper());
        reader.setOnImageAvailableListener(imageAvailableListener,handler);
        try {
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        session.setRepeatingRequest(captureBuilder.build(),null,handler);

                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {

                }
            }, handler);
        }catch(Throwable t){
            t.printStackTrace();
        }
        } catch(Throwable t){
            t.printStackTrace();
        }

    }

    @TargetApi(21)
    private void startCamera() {
        List<Surface> list = new ArrayList<Surface>();

        Size jpegSize = getAppropriateSize(500,jpegSizes);
        //Size jpegSize = jpegSizes[0];

        int width = jpegSize.getWidth(), height = jpegSize.getHeight();

        for(Size s : jpegSizes){
            System.out.print(s.getWidth());
            System.out.print("  ");
            System.out.println(s.getHeight());
        }

        System.out.println("JPEG WIDTH: " + jpegSize.getWidth());
        System.out.println("JPEG HEIGHT: " + jpegSize.getHeight());

        ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 2);
        list.add(reader.getSurface());

        ImageReader.OnImageAvailableListener imageAvailableListener = new ImageReader.OnImageAvailableListener(){
            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image = null;
                try {
                    image = reader.acquireLatestImage();


                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();

                    int remaining = buffer.remaining();
                    byte[] bytes = new byte[remaining];
                    buffer.get(bytes);
                    Bitmap bmp = BitmapFactory.decodeByteArray(bytes,0,bytes.length);

//                    System.out.println("BUFFER SIZE: " + buffer.capacity());
//                    System.out.println("IMAGE WIDTH: " + image.getWidth());
//                    System.out.println("IMAGE HEIGHT: " + image.getHeight());

                    Frame.Builder frameBuilder = new Frame.Builder();
                    frameBuilder.setBitmap(bmp);
                    Frame frame = frameBuilder.build();



                    detector.receiveFrame(frame);

                } catch (java.lang.Exception e) {
                    e.printStackTrace();
                } finally {
                    if (image != null) image.close();
                }
            }
        };
        reader.setOnImageAvailableListener(imageAvailableListener,null);
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
    private Size getAppropriateSize(int target,Size[] sizes){
        Size s = sizes[0];
        for(Size size : sizes){
            if(size.getHeight() < target || size.getWidth() < target){
                break;
            }
            s = size;
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
                size = getAppropriateSize(500,map.getOutputSizes(SurfaceTexture.class));
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
