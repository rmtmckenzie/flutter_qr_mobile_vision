package com.github.rmtmckenzie.qrmobilevision;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.util.SparseArray;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Allows QrCamera classes to send frames to a Detector
 */

class QrDetector {
    private static final String TAG = "cgr.qrmv.QrDetector";
    private final QrReaderCallbacks communicator;
    private final Detector<Barcode> detector;
    private AtomicInteger atomicCounter = new AtomicInteger();
    private int width = 0, height = 0;
    private boolean isNV21 = false;

    QrDetector(QrReaderCallbacks communicator, Context context, int formats) {
        Log.i(TAG, "Making detector for formats: " + formats);
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

    void detect(Frame frame) {
        new qrTask(frame, atomicCounter.incrementAndGet(), atomicCounter).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
    void detect(byte[] bytes) {
//        new qrTask(bytes, atomicCounter.incrementAndGet(), atomicCounter).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private class ImageFrame {
        final byte[] bytes;
        //final ByteBuffer bytes;
        final int count;
        ImageFrame(byte[] bytes, int count) {
            this.bytes = bytes;
            this.count = count;
        }
    }

    private class qrTask extends AsyncTask<Void, Void, SparseArray<Barcode>> {

        final Frame frame;
//        final byte[] bytes;
        final int count;
        final AtomicInteger counter;
        public qrTask(Frame frame, int count, AtomicInteger counter) {
//            this.bytes = bytes;
            this.frame = frame;
            this.count = count;
            this.counter = counter;
        }

        @Override
        protected SparseArray<Barcode> doInBackground(Void... voids) {
            if (count < atomicCounter.get()) {
                return null;
            }

//            Frame.Builder frameBuilder = new Frame.Builder();
//
//            Bitmap bmp;
//            if (isNV21) {
//                frameBuilder.setImageData(ByteBuffer.wrap(bytes), width, height, ImageFormat.NV21);
//            } else {
//                frameBuilder.setImageData(ByteBuffer.wrap(bytes), width, height, ImageFormat.YV12);
////                bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
////                if (bmp == null) return null;
////                frameBuilder.setBitmap(bmp);
//            }
//
//            Frame frame = frameBuilder.build();

            return detector.detect(frame);
        }

        @Override
        protected void onPostExecute(SparseArray<Barcode> detectedItems) {
            if (detectedItems == null) return;
            for (int i = 0; i < detectedItems.size(); ++i) {
                Log.i(TAG, "Item read: " + detectedItems.valueAt(i).rawValue + ", " + detectedItems.valueAt(i).displayValue);
                communicator.qrRead(detectedItems.valueAt(i).displayValue);
            }
        }
    }
}
