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

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Allows QrCamera classes to send frames to a Detector
 */
@TargetApi(21)
class QrDetector2 {
    private static final String TAG = "cgr.qrmv.QrDetector";
    private final QrReaderCallbacks communicator;
    private final Detector<Barcode> detector;
    private AtomicInteger atomicCounter = new AtomicInteger();

    QrDetector2(QrReaderCallbacks communicator, Context context, int formats) {
        Log.i(TAG, "Making detector2 for formats: " + formats);
        this.communicator = communicator;
        this.detector = new BarcodeDetector.Builder(context.getApplicationContext()).setBarcodeFormats(formats).build();
    }

    void detect(QrImage image) {
        new QrTask(image, atomicCounter.incrementAndGet(), atomicCounter).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    static class QrImage {
        final int width;
        final int height;
        final byte[] yPlaneBytes;
        final byte[] uPlaneBytes;
        final byte[] vPlaneBytes;
        final int yPlanePixelStride;
        final int uPlanePixelStride;
        final int vPlanePixelStride;
        final int yPlaneRowStride;
        final int uPlaneRowStride;
        final int vPlaneRowStride;

        public QrImage(Image image) {
            Image.Plane[] planes = image.getPlanes();
            Image.Plane yPlane = planes[0];
            Image.Plane uPlane = planes[1];
            Image.Plane vPlane = planes[2];

            ByteBuffer yBufferDirect = yPlane.getBuffer(),
                uBufferDirect = uPlane.getBuffer(),
                vBufferDirect = vPlane.getBuffer();

            yPlaneBytes = new byte[yBufferDirect.capacity()];
            uPlaneBytes = new byte[uBufferDirect.capacity()];
            vPlaneBytes = new byte[vBufferDirect.capacity()];

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

    private class QrTask extends AsyncTask<Void, Void, SparseArray<Barcode>> {

        final QrImage image;
        final int count;
        final AtomicInteger counter;

        QrTask(QrImage image, int count, AtomicInteger counter) {
            this.image = image;
            this.count = count;
            this.counter = counter;
        }

        @Override
        protected SparseArray<Barcode> doInBackground(Void... voids) {
            // ignore this frame if the next item has already been queued.
            if (count < atomicCounter.get()) {
                return null;
            }

            // Don't need to bother making it colour as qr works the same on
            // just greyscale.
            ByteBuffer imageBuffer = image.toNv21(false);

            Frame.Builder builder = new Frame.Builder().setImageData(imageBuffer, image.width, image.height, ImageFormat.NV21);
            return detector.detect(builder.build());
        }

        @Override
        protected void onPostExecute(SparseArray<Barcode> detectedItems) {
            if (detectedItems == null) return;
            for (int i = 0; i < detectedItems.size(); ++i) {
                Log.i(TAG, "Item read: " + detectedItems.valueAt(i).rawValue);
                communicator.qrRead(detectedItems.valueAt(i).rawValue);
            }
        }
    }
}
