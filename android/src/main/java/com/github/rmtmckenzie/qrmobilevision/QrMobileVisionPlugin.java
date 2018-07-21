package com.github.rmtmckenzie.qrmobilevision;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
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
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import io.flutter.view.TextureRegistry;


/**
 * QrMobileVisionPlugin
 */
public class QrMobileVisionPlugin implements MethodCallHandler, QRReaderCallbacks, QRReader.QRReaderStartedCallback, PluginRegistry.RequestPermissionsResultListener {

	private static final String TAG = "c.g.r.QrMobVisPlugin";
	private static final int REQUEST_PERMISSION = 1;

	/**
	 * Plugin registration.
	 */
	public static void registerWith(Registrar registrar) {
		final MethodChannel channel = new MethodChannel(registrar.messenger(), "com.github.rmtmckenzie/qr_mobile_vision");
		QrMobileVisionPlugin qrMobileVisionPlugin = new QrMobileVisionPlugin(channel, registrar.activity(), registrar.textures());
		channel.setMethodCallHandler(qrMobileVisionPlugin);
		registrar.addRequestPermissionsResultListener(qrMobileVisionPlugin);
	}

	private final MethodChannel channel;
	private final Activity context;
	private final TextureRegistry textures;
	private Integer lastHeartbeatTimeout;
	private boolean waitingForPermissionResult;
	private boolean permissionDenied;

	public QrMobileVisionPlugin(MethodChannel channel, Activity context, TextureRegistry textures) {
		this.textures = textures;
		this.channel = channel;
		this.context = context;
	}

	@Override
	public boolean onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		if (requestCode == REQUEST_PERMISSION) {
			waitingForPermissionResult = false;
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				System.out.println("Granted");
				stopReader();
			} else {
				System.out.println("Denied");
				permissionDenied = true;
				startingFailed(new QRReader.Exception(QRReader.Exception.Reason.noPermissions));
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
//        System.out.println("QRMobileVisionPlugin: Method call received: " + methodCall.method);
		switch (methodCall.method) {
			case "start": {
				if(permissionDenied) {
					permissionDenied = false;
					result.error("QRREADER_ERROR", "noPermission", null);
				}
				else if (readingInstance != null) {
					result.error("ALREADY_RUNNING", "Start cannot be called when already running", "");
				}
				else {
					lastHeartbeatTimeout = methodCall.argument("heartbeatTimeout");
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
								lastHeartbeatTimeout == null ? 0 : lastHeartbeatTimeout
						);
					}
					catch (IOException e) {
						e.printStackTrace();
						result.error("IOException", "Error starting camera because of IOException: " + e.getLocalizedMessage(), null);
					}
					catch (QRReader.Exception e) {
						e.printStackTrace();
						result.error(e.reason().name(), "Error starting camera for reason: " + e.reason().name(), null);
					}
					catch (NoPermissionException e) {
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

	@Override
	public void startingFailed(Throwable t) {
		Log.w(TAG, "Starting QR Mobile Vision failed", t);
		StackTraceElement[] stackTrace = t.getStackTrace();
		List<String> stackTraceStrings = new ArrayList<>(stackTrace.length);
		for (StackTraceElement el : stackTrace) {
			stackTraceStrings.add(el.toString());
		}

		if (t instanceof QRReader.Exception) {
			QRReader.Exception qrException = (QRReader.Exception) t;
			readingInstance.startResult.error("QRREADER_ERROR", qrException.reason().name(), stackTraceStrings);
		}
		else {
			readingInstance.startResult.error("UNKNOWN_ERROR", t.getMessage(), stackTraceStrings);
		}
	}
}