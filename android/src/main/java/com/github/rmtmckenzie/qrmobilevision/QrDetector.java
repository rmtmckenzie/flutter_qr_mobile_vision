package com.github.rmtmckenzie.qrmobilevision;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.util.SparseArray;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.lang.ref.WeakReference;
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
    private boolean isNV21 = false;

    QrDetector(QrReaderCallbacks communicator, Context context, int formats) {
        this.communicator = communicator;
        this.detector = new BarcodeDetector.Builder(context.getApplicationContext()).setBarcodeFormats(formats).build();
    }

    /**
     * Detect a frame.
     * @param bytes
     * @param width
     * @param height
     * @param format can be ImageFormat.NV21, ImageFormat.NV16, ImageFormat.YV12
     */
    void detect(byte[] bytes, int width, int height, int format) {
        Frame.Builder frameBuilder = new Frame.Builder();
        Frame frame = frameBuilder.setImageData(ByteBuffer.wrap(bytes), width, height, format).build();
        new QrTask(frame, atomicCounter.incrementAndGet(), this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private static class QrTask extends AsyncTask<Void, Void, SparseArray<Barcode>> {

        final Frame frame;
        final int count;
        final WeakReference<QrDetector> qrDetector;

        QrTask(Frame frame, int count, QrDetector qrDetector) {
            this.frame = frame;
            this.count = count;
            this.qrDetector = new WeakReference<>(qrDetector);
        }

        @Override
        protected SparseArray<Barcode> doInBackground(Void... voids) {
            QrDetector qrDetector = this.qrDetector.get();
            if (qrDetector == null) return null;

            if (count < qrDetector.atomicCounter.get()) {
                return null;
            }

            return qrDetector.detector.detect(frame);
        }

        @Override
        protected void onPostExecute(SparseArray<Barcode> detectedItems) {
            QrDetector qrDetector = this.qrDetector.get();
            if (qrDetector == null) return;

            if (detectedItems == null) return;
            for (int i = 0; i < detectedItems.size(); ++i) {
                Barcode barcode = detectedItems.valueAt(i);
                Log.i(TAG, "Item read: " + barcode.rawValue + ", " + barcode.displayValue);
                qrDetector.communicator.qrRead(barcode.displayValue);
            }
        }
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
}
