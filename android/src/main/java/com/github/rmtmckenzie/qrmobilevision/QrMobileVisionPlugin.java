package com.github.rmtmckenzie.qrmobilevision;

import android.content.Context;

import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import io.flutter.view.TextureRegistry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.lang.Integer;
import java.util.Map;


/**
 * QrMobileVisionPlugin
 */
public class QrMobileVisionPlugin implements MethodCallHandler, QRReaderCallbacks, QRReader.QRReaderStartedCallback {
    /**
     * Plugin registration.
     */
    public static void registerWith(Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "com.github.rmtmckenzie/qr_mobile_vision");
        channel.setMethodCallHandler(new QrMobileVisionPlugin(channel, registrar.activity(), registrar.textures()));
    }

    private final MethodChannel channel;
    private final Context context;
    private final TextureRegistry textures;

    public QrMobileVisionPlugin(MethodChannel channel, Context context, TextureRegistry textures) {
        this.textures = textures;
        this.channel = channel;
        this.context = context;
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

    private ReadingInstance readingInstance;


    @Override
    public void onMethodCall(MethodCall methodCall, Result result) {
        System.out.println("QRMobileVisionPlugin: Method call received: " + methodCall.method);
        switch (methodCall.method) {
            case "start": {
                if (readingInstance != null) {
                    result.error("ALREADY_RUNNING", "Start cannot be called when already running", "");
                } else {
                    Integer heartbeatTimeout = methodCall.argument("heartbeatTimeout");
                    Integer targetWidth = methodCall.argument("targetWidth");
                    Integer targetHeight = methodCall.argument("targetHeight");
                    if (targetWidth == null || targetHeight == null) {
                        result.error("INVALID_ARGUMENT", "Missing a required argument", "Expecting targetWidth, targetHeight, and optionally heartbeatTimeout");
                        break;
                    }

                    TextureRegistry.SurfaceTextureEntry textureEntry = textures.createSurfaceTexture();
                    QRReader reader = new QRReader(targetWidth, targetHeight, context, this, this, textureEntry.surfaceTexture());

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
                if(readingInstance != null) {
                    readingInstance.reader.heartBeat();
                }
                result.success(null);
                break;
            }
            default:
                result.notImplemented();
        }
    }

    /**
     * @param frame
     * @param rotation - rotation of the camera frame; 0=none, 1=90 degress cc, 2=180, 3=270 degrees cc
     */
    @Override
    public void cameraFrame(byte[] frame, int rotation) {
        //TODO: remove completely if sure it's not being used any more
//        List<Object> arguments = new ArrayList<Object>(Arrays.asList(frame, rotation));
//        channel.invokeMethod("cameraFrame", arguments);
    }

    @Override
    public void qrRead(String data) {
        System.out.println("Invoking qrRead");
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

    @Override
    public void startingFailed(Throwable t) {
        if (t instanceof QRReader.Exception) {
            readingInstance.startResult.error("QRREADER_ERROR", t.getMessage(), t.getStackTrace());
        } else {
            readingInstance.startResult.error("UNKNOWN_ERROR", t.getMessage(), t.getStackTrace());
        }
    }
}