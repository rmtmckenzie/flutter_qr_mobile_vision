package com.github.rmtmckenzie.qrmobilevision;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.os.AsyncTask;
import android.util.SparseArray;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Allows QrCamera classes to send frames to a Detector
 */

class QrDetector {
    private AtomicInteger atomicCounter = new AtomicInteger();
    private final QRReaderCallbacks communicator;
    private final Detector<Barcode> detector;
    private int width = 0, height = 0;
    boolean NV21 = false;

    QrDetector(QRReaderCallbacks communicator,Context context){
        this.communicator = communicator;
        this.detector = new BarcodeDetector.Builder(context.getApplicationContext()).setBarcodeFormats(
                Barcode.QR_CODE).build();
    }

    void useNV21(int width,int height){
        NV21 = true;
        this.width = width;
        this.height = height;
    }

    void useJPEG(){
        NV21 = false;
        this.width = 0;
        this.height = 0;
    }

    void detect(byte[] bytes){
        new qrTask().execute(new ImageFrame(bytes, atomicCounter.incrementAndGet()));
    }

    private class ImageFrame {
        ImageFrame(byte[] bytes, int count) {
            this.bytes = bytes;
            this.count = count;
        }

        final byte[] bytes;
        //final ByteBuffer bytes;
        final int count;
    }

    private class qrTask extends AsyncTask<ImageFrame, Void, SparseArray<Barcode>> {
        @Override
        protected SparseArray<Barcode> doInBackground(ImageFrame... imageFrames) {
            ImageFrame imageFrame = imageFrames[0];

            if (imageFrame.count < atomicCounter.get()) return null;

//            System.out.println("About to detect...");

            Frame.Builder frameBuilder = new Frame.Builder();

            Bitmap bmp;
            if(NV21) frameBuilder.setImageData(ByteBuffer.wrap(imageFrame.bytes), width, height, ImageFormat.NV21);
            else{
                bmp = BitmapFactory.decodeByteArray(imageFrame.bytes, 0, imageFrame.bytes.length);
                if(bmp == null) return null;
                frameBuilder.setBitmap(bmp);
            }

            Frame frame = frameBuilder.build();

            return detector.detect(frame);
        }

        @Override
        protected void onPostExecute(SparseArray<Barcode> detectedItems) {
            if (detectedItems == null) return;
            for (int i = 0; i < detectedItems.size(); ++i) {
                communicator.qrRead(detectedItems.valueAt(i).displayValue);
            }
        }
    }
}
