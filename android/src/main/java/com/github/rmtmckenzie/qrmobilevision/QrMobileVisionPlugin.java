package com.github.rmtmckenzie.qrmobilevision;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import io.flutter.view.TextureRegistry;


/**
 * QrMobileVisionPlugin
 */
public class QrMobileVisionPlugin implements MethodCallHandler, QRReaderCallbacks, QRReader.QRReaderStartedCallback {

    private static final String TAG = "c.g.r.QrMobVisPlugin";
    private final MethodChannel channel;
    private final Context context;
    private final TextureRegistry textures;
    private ReadingInstance readingInstance;

    public QrMobileVisionPlugin(MethodChannel channel, Context context, TextureRegistry textures) {
        this.textures = textures;
        this.channel = channel;
        this.context = context;
    }

    /**
     * Plugin registration.
     */
    public static void registerWith(Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "com.github.rmtmckenzie/qr_mobile_vision");
        channel.setMethodCallHandler(new QrMobileVisionPlugin(channel, registrar.activity(), registrar.textures()));
    }

    @Override
    public void onMethodCall(MethodCall methodCall, Result result) {
//        System.out.println("QRMobileVisionPlugin: Method call received: " + methodCall.method);
        switch (methodCall.method) {
            case "start": {
                if (readingInstance != null) {
                    result.error("ALREADY_RUNNING", "Start cannot be called when already running", "");
                } else {
                    Integer heartbeatTimeout = methodCall.argument("heartbeatTimeout");
                    Integer targetWidth = methodCall.argument("targetWidth");
                    Integer targetHeight = methodCall.argument("targetHeight");
                    List<String> formatStrings = methodCall.argument("formats");

                    if (targetWidth == null || targetHeight == null) {
                        result.error("INVALID_ARGUMENT", "Missing a required argument", "Expecting targetWidth, targetHeight, and optionally heartbeatTimeout");
                        break;
                    }

//                    System.out.print("Reading barcodes from formats:");
//                    for(String formatString: formatStrings) {
//                        System.out.print(" ");
//                        System.out.print(formatString);
//                    }
//                    System.out.println(".");

                    int barcodeFormats = BarcodeFormats.intFromStringList(formatStrings);

                    TextureRegistry.SurfaceTextureEntry textureEntry = textures.createSurfaceTexture();
                    QRReader reader = new QRReader(targetWidth, targetHeight, context, barcodeFormats,
                            this, this, textureEntry.surfaceTexture());

                    readingInstance = new ReadingInstance(reader, textureEntry, result);
                    try {
                        reader.start(
                                heartbeatTimeout == null ? 0 : heartbeatTimeout
                        );
                    } catch (IOException e) {
                        e.printStackTrace();
                        result.error("IOException", "Error starting camera because of IOException: " + e.getLocalizedMessage(), null);
                    } catch (QRReader.Exception e) {
                        e.printStackTrace();
                        result.error(e.reason().name(), "Error starting camera for reason: " + e.reason().name(), null);
                    }
                }
                break;
            }
            case "stop": {
                if (readingInstance != null) {
                    readingInstance.reader.stop();
                    readingInstance = null;
                }
                result.success(null);
                break;
            }
            case "heartbeat": {
                if (readingInstance != null) {
                    readingInstance.reader.heartBeat();
                }
                result.success(null);
                break;
            }
            default:
                result.notImplemented();
        }
    }

    @Override
    public void qrRead(String data) {
        channel.invokeMethod("qrRead", data);
    }

    @Override
    public void started() {
        Map<String, Object> response = new HashMap<>();
        response.put("surfaceWidth", readingInstance.reader.qrCamera.getWidth());
        response.put("surfaceHeight", readingInstance.reader.qrCamera.getHeight());
        response.put("surfaceOrientation", readingInstance.reader.qrCamera.getOrientation());
        response.put("textureId", readingInstance.textureEntry.id());
        readingInstance.startResult.success(response);
    }

    private List<String> stackTraceAsString(StackTraceElement[] stackTrace) {
        if (stackTrace == null) {
            return null;
        }

        List<String> stackTraceStrings = new ArrayList<>(stackTrace.length);
        for (StackTraceElement el : stackTrace) {
            stackTraceStrings.add(el.toString());
        }
        return stackTraceStrings;
    }

    @Override
    public void startingFailed(Throwable t) {
        Log.w(TAG, "Starting QR Mobile Vision failed", t);
        List<String> stackTraceStrings = stackTraceAsString(t.getStackTrace());

        if (t instanceof QRReader.Exception) {
            QRReader.Exception qrException = (QRReader.Exception) t;
            readingInstance.startResult.error("QRREADER_ERROR", qrException.reason().name(), stackTraceStrings);
        } else {
            readingInstance.startResult.error("UNKNOWN_ERROR", t.getMessage(), stackTraceStrings);
        }
    }

    private class ReadingInstance {
        final QRReader reader;
        final TextureRegistry.SurfaceTextureEntry textureEntry;
        final Result startResult;

        private ReadingInstance(QRReader reader, TextureRegistry.SurfaceTextureEntry textureEntry, Result startResult) {
            this.reader = reader;
            this.textureEntry = textureEntry;
            this.startResult = startResult;
        }
    }
}