package com.github.rmtmckenzie.qrmobilevision;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import android.util.Log;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import io.flutter.view.TextureRegistry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * QrMobileVisionPlugin
 */
public class QrMobileVisionPlugin implements MethodCallHandler, QrReaderCallbacks, QrReader.QRReaderStartedCallback, PluginRegistry.RequestPermissionsResultListener {

    private static final String TAG = "cgr.qrmv.QrMobVisPlugin";
    private static final int REQUEST_PERMISSION = 1;
    private final MethodChannel channel;
    private final Activity context;
    private final TextureRegistry textures;
    private Integer lastHeartbeatTimeout;
    private boolean waitingForPermissionResult;
    private boolean permissionDenied;
    private ReadingInstance readingInstance;

    public QrMobileVisionPlugin(MethodChannel channel, Activity context, TextureRegistry textures) {
        this.textures = textures;
        this.channel = channel;
        this.context = context;
    }

    /**
     * Plugin registration.
     */
    public static void registerWith(Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "com.github.rmtmckenzie/qr_mobile_vision");
        QrMobileVisionPlugin qrMobileVisionPlugin = new QrMobileVisionPlugin(channel, registrar.activity(), registrar.textures());
        channel.setMethodCallHandler(qrMobileVisionPlugin);
        registrar.addRequestPermissionsResultListener(qrMobileVisionPlugin);
    }

    @Override
    public boolean onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION) {
            waitingForPermissionResult = false;
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "Permissions request granted.");
                stopReader();
            } else {
                Log.i(TAG, "Permissions request denied.");
                permissionDenied = true;
                startingFailed(new QrReader.Exception(QrReader.Exception.Reason.noPermissions));
                stopReader();
            }
            return true;
        }
        return false;
    }

    private void stopReader() {
        readingInstance.reader.stop();
        readingInstance = null;
        lastHeartbeatTimeout = null;
    }

    @Override
    public void onMethodCall(MethodCall methodCall, Result result) {
        switch (methodCall.method) {
            case "start": {
                if (permissionDenied) {
                    permissionDenied = false;
                    result.error("QRREADER_ERROR", "noPermission", null);
                } else if (readingInstance != null) {
                    result.error("ALREADY_RUNNING", "Start cannot be called when already running", "");
                } else {
                    lastHeartbeatTimeout = methodCall.argument("heartbeatTimeout");
                    Integer targetWidth = methodCall.argument("targetWidth");
                    Integer targetHeight = methodCall.argument("targetHeight");
                    List<String> formatStrings = methodCall.argument("formats");

                    if (targetWidth == null || targetHeight == null) {
                        result.error("INVALID_ARGUMENT", "Missing a required argument", "Expecting targetWidth, targetHeight, and optionally heartbeatTimeout");
                        break;
                    }

                    int barcodeFormats = BarcodeFormats.intFromStringList(formatStrings);

                    TextureRegistry.SurfaceTextureEntry textureEntry = textures.createSurfaceTexture();
                    QrReader reader = new QrReader(targetWidth, targetHeight, context, barcodeFormats,
                        this, this, textureEntry.surfaceTexture());

                    readingInstance = new ReadingInstance(reader, textureEntry, result);
                    try {
                        reader.start(
                            lastHeartbeatTimeout == null ? 0 : lastHeartbeatTimeout
                        );
                    } catch (IOException e) {
                        e.printStackTrace();
                        result.error("IOException", "Error starting camera because of IOException: " + e.getLocalizedMessage(), null);
                    } catch (QrReader.Exception e) {
                        e.printStackTrace();
                        result.error(e.reason().name(), "Error starting camera for reason: " + e.reason().name(), null);
                    } catch (NoPermissionException e) {
                        waitingForPermissionResult = true;
                        ActivityCompat.requestPermissions(context,
                            new String[]{Manifest.permission.CAMERA}, REQUEST_PERMISSION);
                    }
                }
                break;
            }
            case "stop": {
                if (readingInstance != null && !waitingForPermissionResult) {
                    stopReader();
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

        if (t instanceof QrReader.Exception) {
            QrReader.Exception qrException = (QrReader.Exception) t;
            readingInstance.startResult.error("QRREADER_ERROR", qrException.reason().name(), stackTraceStrings);
        } else {
            readingInstance.startResult.error("UNKNOWN_ERROR", t.getMessage(), stackTraceStrings);
        }
    }

    private class ReadingInstance {
        final QrReader reader;
        final TextureRegistry.SurfaceTextureEntry textureEntry;
        final Result startResult;

        private ReadingInstance(QrReader reader, TextureRegistry.SurfaceTextureEntry textureEntry, Result startResult) {
            this.reader = reader;
            this.textureEntry = textureEntry;
            this.startResult = startResult;
        }
    }
}