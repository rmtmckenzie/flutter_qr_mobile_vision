package com.github.rmtmckenzie.qrmobilevision;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.ImageFormat;
import android.media.Image;
import android.os.AsyncTask;
import android.util.Log;
import android.util.SparseArray;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Allows QrCamera classes to send frames to a Detector
 */
@TargetApi(21)
class QrDetector2 {
    private static final String TAG = "cgr.qrmv.QrDetector";
    private final QrReaderCallbacks communicator;
    private final Detector<Barcode> detector;
    private final Lock imageToCheckLock = new ReentrantLock();
    private final Lock nextImageLock = new ReentrantLock();
    private final AtomicBoolean isScheduled = new AtomicBoolean(false);
    private final AtomicBoolean needsScheduling = new AtomicBoolean(false);


    private final AtomicBoolean nextImageSet = new AtomicBoolean(false);

    private QrImage imageToCheck = new QrImage();
    private QrImage nextImage = new QrImage();

    QrDetector2(QrReaderCallbacks communicator, Context context, int formats) {
        Log.i(TAG, "Making detector2 for formats: " + formats);
        this.communicator = communicator;
        this.detector = new BarcodeDetector.Builder(context.getApplicationContext()).setBarcodeFormats(formats).build();
    }

    private void maybeStartProcessing() {
        // start processing, only if scheduling is needed and
        // there isn't currently a scheduled task.
        if (needsScheduling.get() && !isScheduled.get()) {
            isScheduled.set(true);
            new QrTaskV2(this).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
        }
    }

    void detect(Image image) {
        needsScheduling.set(true);

        if (imageToCheckLock.tryLock()) {
            // copy image if not in use
            try {
                nextImageSet.set(false);
                imageToCheck.copyImage(image);
            } finally {
                imageToCheckLock.unlock();
            }
        } else if (nextImageLock.tryLock()) {
            // if first image buffer is in use, use second buffer
            // one or the other should always be free but if not this
            // frame is dropped..
            try {
                nextImageSet.set(true);
                nextImage.copyImage(image);
            } finally {
                nextImageLock.unlock();
            }
        }
        maybeStartProcessing();
    }

    static class QrImage {
        int width;
        int height;
        int yPlanePixelStride;
        int uPlanePixelStride;
        int vPlanePixelStride;
        int yPlaneRowStride;
        int uPlaneRowStride;
        int vPlaneRowStride;
        byte[] yPlaneBytes = new byte[0];
        byte[] uPlaneBytes = new byte[0];
        byte[] vPlaneBytes = new byte[0];

        void copyImage(Image image) {
            Image.Plane[] planes = image.getPlanes();
            Image.Plane yPlane = planes[0];
            Image.Plane uPlane = planes[1];
            Image.Plane vPlane = planes[2];

            ByteBuffer yBufferDirect = yPlane.getBuffer(),
                uBufferDirect = uPlane.getBuffer(),
                vBufferDirect = vPlane.getBuffer();

            if (yPlaneBytes.length != yBufferDirect.capacity()) {
                yPlaneBytes = new byte[yBufferDirect.capacity()];
            }
            if (uPlaneBytes.length != uBufferDirect.capacity()) {
                uPlaneBytes = new byte[uBufferDirect.capacity()];
            }
            if (vPlaneBytes.length != vBufferDirect.capacity()) {
                vPlaneBytes = new byte[vBufferDirect.capacity()];
            }

            yBufferDirect.get(yPlaneBytes);
            uBufferDirect.get(uPlaneBytes);
            vBufferDirect.get(vPlaneBytes);

            width = image.getWidth();
            height = image.getHeight();

            yPlanePixelStride = yPlane.getPixelStride();
            uPlanePixelStride = uPlane.getPixelStride();
            vPlanePixelStride = vPlane.getPixelStride();

            yPlaneRowStride = yPlane.getRowStride();
            uPlaneRowStride = uPlane.getRowStride();
            vPlaneRowStride = vPlane.getRowStride();
        }

        private ByteBuffer toNv21(boolean greyScale) {
            int halfWidth = width / 2;
            int numPixels = width * height;

            byte[] nv21ImageBytes = new byte[numPixels * 2];

            if (greyScale) {
                Arrays.fill(nv21ImageBytes, (byte) 127);
            }

            ByteBuffer nv21Buffer = ByteBuffer.wrap(nv21ImageBytes);


            for (int i = 0; i < height; ++i) {
                nv21Buffer.put(yPlaneBytes, i * yPlaneRowStride, width);
            }

            if (!greyScale) {
                for (int row = 0; row < height / 2; ++row) {
                    int uRow = row * uPlaneRowStride, vRow = row * vPlaneRowStride;
                    for (int count = 0, u = 0, v = 0; count < halfWidth; u += uPlanePixelStride, v += vPlanePixelStride, count++) {
                        nv21Buffer.put(uPlaneBytes[uRow + u]);
                        nv21Buffer.put(vPlaneBytes[vRow + v]);
                    }
                }
            }

            return nv21Buffer;
        }
    }

    private static class QrTaskV2 extends AsyncTask<Void, Void, SparseArray<Barcode>> {

        private final WeakReference<QrDetector2> qrDetector;

        private QrTaskV2(QrDetector2 qrDetector) {
            this.qrDetector = new WeakReference<>(qrDetector);
        }

        @Override
        protected SparseArray<Barcode> doInBackground(Void... voids) {

            QrDetector2 qrDetector = this.qrDetector.get();
            if (qrDetector == null) return null;

            qrDetector.needsScheduling.set(false);
            qrDetector.isScheduled.set(false);

            ByteBuffer imageBuffer;
            int width;
            int height;
            if (qrDetector.nextImageSet.get()) {
                try {
                    qrDetector.nextImageLock.lock();
                    imageBuffer = qrDetector.nextImage.toNv21(false);
                    width = qrDetector.nextImage.width;
                    height = qrDetector.nextImage.height;
                } finally {
                    qrDetector.nextImageLock.unlock();
                }
            } else {
                try {
                    qrDetector.imageToCheckLock.lock();
                    imageBuffer = qrDetector.imageToCheck.toNv21(false);
                    width = qrDetector.imageToCheck.width;
                    height = qrDetector.imageToCheck.height;
                } finally {
                    qrDetector.imageToCheckLock.unlock();
                }
            }

            Frame.Builder builder = new Frame.Builder().setImageData(imageBuffer, width, height, ImageFormat.NV21);
            return qrDetector.detector.detect(builder.build());
        }

        @Override
        protected void onPostExecute(SparseArray<Barcode> detectedItems) {
            QrDetector2 qrDetector = this.qrDetector.get();
            if (qrDetector == null) return;

            if (detectedItems != null) {
                for (int i = 0; i < detectedItems.size(); ++i) {
                    Log.i(TAG, "Item read: " + detectedItems.valueAt(i).rawValue);
                    qrDetector.communicator.qrRead(detectedItems.valueAt(i).rawValue);
                }
            }

            // if needed keep processing.
            qrDetector.maybeStartProcessing();
        }
    }
}
