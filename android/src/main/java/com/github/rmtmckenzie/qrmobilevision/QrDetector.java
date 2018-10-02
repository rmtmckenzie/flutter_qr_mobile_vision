package com.github.rmtmckenzie.qrmobilevision;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.ImageFormat;
import android.os.AsyncTask;
import android.util.Log;
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
    private static final String TAG = "cgr.qrmv.QrDetector";
    private final QrReaderCallbacks communicator;
    private final Detector<Barcode> detector;
    private AtomicInteger atomicCounter = new AtomicInteger();
    private int width = 0, height = 0;

    QrDetector(QrReaderCallbacks communicator, Context context, int formats) {
        Log.i(TAG, "Making detector for formats: " + formats);
        this.communicator = communicator;
        this.detector = new BarcodeDetector.Builder(context.getApplicationContext()).setBarcodeFormats(formats).build();
    }

    void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    void detect(byte[] bytes) {
        new QrTask(bytes, atomicCounter.incrementAndGet()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @SuppressLint("StaticFieldLeak")
    private class QrTask extends AsyncTask<Void, Void, SparseArray<Barcode>> {

        final byte[] bytes;
        final int count;

        private QrTask(byte[] bytes, int count) {
            this.bytes = bytes;
            this.count = count;
        }

        @Override
        protected SparseArray<Barcode> doInBackground(Void... voids) {
            if (count < atomicCounter.get()) {
                return null;
            }

            Frame.Builder frameBuilder = new Frame.Builder();
            frameBuilder.setImageData(ByteBuffer.wrap(bytes), width, height, ImageFormat.NV21);

            return detector.detect(frameBuilder.build());
        }

        @Override
        protected void onPostExecute(SparseArray<Barcode> detectedItems) {
            if (detectedItems == null) return;
            for (int i = 0; i < detectedItems.size(); ++i) {
                Log.i(TAG, "Item read with type: " + detectedItems.valueAt(i).valueFormat
                    + ", display: " + detectedItems.valueAt(i).displayValue
                    + ", raw: " + detectedItems.valueAt(i).rawValue);
                communicator.qrRead(detectedItems.valueAt(i).rawValue);
            }
        }
    }
}
