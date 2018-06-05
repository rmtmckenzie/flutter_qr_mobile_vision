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
    boolean isNV21 = false;

    QrDetector(QRReaderCallbacks communicator, Context context, int formats) {
        System.out.println("Making detector for formats: " + formats);
        this.communicator = communicator;
        this.detector = new BarcodeDetector.Builder(context.getApplicationContext()).setBarcodeFormats(formats).build();
    }

    void useNV21(int width, int height) {
        isNV21 = true;
        this.width = width;
        this.height = height;
    }

    void useJPEG() {
        isNV21 = false;
        this.width = 0;
        this.height = 0;
    }

    void detect(byte[] bytes) {
        new qrTask(bytes, atomicCounter.incrementAndGet(), atomicCounter).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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

    private class qrTask extends AsyncTask<Void, Void, SparseArray<Barcode>> {

        public qrTask(byte[] bytes, int count, AtomicInteger counter) {
            this.bytes = bytes;
            this.count = count;
            this.counter = counter;
        }

        final byte[] bytes;
        final int count;
        final AtomicInteger counter;

        @Override
        protected SparseArray<Barcode> doInBackground(Void... voids) {
            if (count < atomicCounter.get()) {
                System.out.println("Dropping frame");
                return null;
            }

//            System.out.println("About to detect...");

            Frame.Builder frameBuilder = new Frame.Builder();

            Bitmap bmp;
            if (isNV21) {
                frameBuilder.setImageData(ByteBuffer.wrap(bytes), width, height, ImageFormat.NV21);
            } else {
                bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (bmp == null) return null;
                frameBuilder.setBitmap(bmp);
            }

            Frame frame = frameBuilder.build();

            return detector.detect(frame);
        }

        @Override
        protected void onPostExecute(SparseArray<Barcode> detectedItems) {
            if (detectedItems == null) return;
            for (int i = 0; i < detectedItems.size(); ++i) {
                System.out.println("Item read!: " + detectedItems.valueAt(i).rawValue);
                communicator.qrRead(detectedItems.valueAt(i).displayValue);
            }
        }
    }
}
