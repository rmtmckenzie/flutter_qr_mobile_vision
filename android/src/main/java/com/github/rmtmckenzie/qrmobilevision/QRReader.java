package com.github.rmtmckenzie.qrmobilevision;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.util.Size;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.barcode.Barcode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

public class QRReader {
    private final Context context;
    private final QRReaderCallbacks communicator;
    private Heartbeat heartbeat;
    private SplitBarcodeDetector detector;
    private CameraSource camera;


    QRReader(Context context, final QRReaderCallbacks communicator) {
        this.context = context;
        this.communicator = communicator;
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
                communicator.cameraFrame(frame, rotation);
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

    void start(final int width, final int height, final boolean fill, final int heartBeatTimeout) throws IOException, Exception {
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
                            continueStarting(width, height, fill, heartBeatTimeout);
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
            continueStarting(width, height, fill, heartBeatTimeout);
        }
    }

    private void continueStarting(int width, int height, boolean fill, int heartBeatTimeout) throws IOException {
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

        if(android.os.Build.VERSION.SDK_INT>=21){
            res = getSupportedSizes();
        }
        else{
            System.out.println("Old API(<21)");
            res =getSupportedSizedDepreciated();
        }



//        System.out.println("Dimensions before: width-"+width+" height-"+height);
//        System.out.print("Available Dimensions:  ");
//        for(int[] size : res){
//            System.out.print(" "+size[0]+"x"+size[1]+" ");
//        }


        ListIterator i = res.listIterator();
        int[] pair=null;

        while(i.hasNext()){
            pair =(int[]) i.next();
            if(pair[0]<width || pair[1]<height){

                //Fill
                if(fill && i.previousIndex()==1){
                    //Iterator holds place in front of current element
                    i.previous();
                    pair=(int[])i.previous();
                    width = pair[0];
                    height = pair[1];
                    break;
                }
                //Fit
                else if(pair[0]<=width && pair[1]<=height){
                    width = pair[0];
                    height = pair[1];
                    break;
                }
            }
        }
        //if given dimensions are smaller than the smallest available, return smallest available
        if(!i.hasNext() && pair!=null){
            width = pair[0];
            height = pair[1];
        }

//        System.out.println("\nDimensions after: width-"+width+" height-"+height);

        camera = new CameraSource.Builder(context, detector)
                .setAutoFocusEnabled(this.hasAutofocus(context))
                .setRequestedPreviewSize(width, height)
                .build();

        camera.start();
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
    List<int[]> getSupportedSizes(){

        List<int[]> sizeOutput = new ArrayList<int[]>();

        try {
            CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            //Get List of Supported Sizes
            String[] cameraId = manager.getCameraIdList();
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId[0]);
            StreamConfigurationMap configs = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Size[] sizes= configs.getOutputSizes(ImageFormat.JPEG);

            for(Size size: sizes){
                int[] wxh = {size.getWidth(),size.getHeight()};
                sizeOutput.add(wxh);
            }
        }
        catch (CameraAccessException e){
            e.printStackTrace();
        }
        return sizeOutput;
    }


    //For API<21 i.e. Android 4.4 - KitKat
    List<int[]> getSupportedSizedDepreciated(){
        Camera c;
        List<int[]> sizeOutput = new ArrayList<int[]>();

        List<Camera.Size> sizes=null;
        try {
            c = Camera.open();
            Camera.Parameters parameters = c.getParameters();
            sizes = parameters.getSupportedPreviewSizes();

            for(Camera.Size size: sizes){
                int[] wxh = {size.width,size.height};
                sizeOutput.add(wxh);
            }
            c.release();


        }
        catch (java.lang.Exception e){
            e.printStackTrace();
            System.out.println("Couldnt open camera!?");
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
