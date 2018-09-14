package com.github.rmtmckenzie.qrmobilevision;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.util.SparseArray;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

@TargetApi(17)
public class SplitBarcodeDetector extends Detector<Barcode> {

    private final BarcodeDetector detector;
    private final FrameReceiver frameReceiver;
    private final QRReceiver qrReceiver;

    interface FrameReceiver {
        void receiveFrame(byte[] frame, int rotation);
    }

    interface QRReceiver {
        void receiveQr(Barcode data);
    }

    RenderScript rs;
    Allocation aIn;
    Bitmap bmpout;
    Allocation aOut;
    ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic;

    public SplitBarcodeDetector(Context context, FrameReceiver frameReceiver, QRReceiver qrReceiver) {
        this.detector = new BarcodeDetector.Builder(context.getApplicationContext()).setBarcodeFormats(
                Barcode.QR_CODE).build();
        this.frameReceiver = frameReceiver;
        this.qrReceiver = qrReceiver;

        rs = RenderScript.create(context);
        yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));
    }

    Bitmap rotateBitmap(Bitmap source, int rotation){
        Matrix matrix = new Matrix();
        matrix.postRotate(rotation*90);
        source = Bitmap.createBitmap(source,0,0,source.getWidth(),source.getHeight(),matrix,true);
        return source;
    }


    @Override
    public SparseArray<Barcode> detect(Frame frame) {
        Bitmap bitmap = frame.getBitmap();

        if (bitmap == null) {
            Frame.Metadata metadata = frame.getMetadata();
            int width = metadata.getWidth();
            int height = metadata.getHeight();
            int format = metadata.getFormat();
            int rotation = metadata.getRotation(); // 0=0,1=90,2=180,3=270 : CC from upright orientation

            //TODO: allocate these arrays in a smarter way
            int yuvDatalength = width * height * 3/2;  // this is 12 bit per pixel
            aIn = Allocation.createSized(rs, Element.U8(rs), yuvDatalength);
            bmpout = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            aOut = Allocation.createFromBitmap(rs, bmpout);
            yuvToRgbIntrinsic.setInput(aIn);

            ByteBuffer imageData = frame.getGrayscaleImageData();
            aIn.copyFrom(imageData.array());
            yuvToRgbIntrinsic.forEach(aOut);
            aOut.copyTo(bmpout);

            bmpout=rotateBitmap(bmpout,rotation);


            int bytes = bmpout.getByteCount();
            ByteBuffer buffer = ByteBuffer.allocate(bytes);
            bmpout.copyPixelsToBuffer(buffer);

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            bmpout.compress(Bitmap.CompressFormat.JPEG, 100, os);

            frameReceiver.receiveFrame(os.toByteArray(), rotation);

            //
//            YuvImage image = new YuvImage(buffer.array(), format, width, height, null);
//            ByteArrayOutputStream os = new ByteArrayOutputStream();
//            //TODO: check that it worked
//            image.compressToJpeg(new Rect(0, 0, width, height), 100, os);
//
//            byte[] jpegArray = os.toByteArray();
//
//            if (jpegArray == null) {
//                System.out.println("jpg image is null");
//            } else {
//                frameReceiver.receiveFrame(jpegArray, rotation);
//            }
        } else {
            // for now not being used
            System.out.println("Bitmap received");
            int bytes = bitmap.getByteCount();
            ByteBuffer buffer = ByteBuffer.allocate(bytes);
            bitmap.copyPixelsToBuffer(buffer);

            frameReceiver.receiveFrame(buffer.array(), 0);
        }


        SparseArray<Barcode> detectedItems = detector.detect(frame);

        for(int i = 0; i < detectedItems.size(); ++i) {
            qrReceiver.receiveQr(detectedItems.valueAt(0));
        }

        return detectedItems;
    }

    @Override
    public void receiveFrame(Frame frame) {
        detect(frame);
    }
}