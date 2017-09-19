package com.github.rmtmckenzie.qrmobilevision;

import android.content.Context;
import android.os.Handler;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.PluginRegistry.Registrar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.lang.Integer;
import java.lang.Runnable;

/**
 * QrMobileVisionPlugin
 */
public class QrMobileVisionPlugin implements MethodCallHandler, QRReaderCallbacks {
  /**
   * Plugin registration.
   */
  public static void registerWith(Registrar registrar) {
    final MethodChannel channel = new MethodChannel(registrar.messenger(), "com.github.rmtmckenzie/qr_mobile_vision");
    channel.setMethodCallHandler(new QrMobileVisionPlugin(channel, registrar.activity()));
  }

  private final MethodChannel channel;
  private final Context context;
  private final QRReader reader;

  public QrMobileVisionPlugin(MethodChannel channel, Context context) {
    this.channel = channel;
    this.context = context;
    this.reader = new QRReader(context, this);
  }

  @Override
  public void onMethodCall(MethodCall methodCall, Result result) {
    switch (methodCall.method) {
      case "start": {
        Integer width = methodCall.argument("width");
        Integer height = methodCall.argument("height");
        Integer heartbeatTimeout = methodCall.argument("heartbeatTimeout");

        try {
          reader.start(
                  width == null ? 500 : width,
                  height == null ? 700 : height,
                  heartbeatTimeout == null ? 0 : heartbeatTimeout
          );
          result.success(null);
        } catch (IOException e) {
          e.printStackTrace();
          result.error("IOException", "Error starting camera because of IOException: " + e.getLocalizedMessage(), null);
        } catch (QRReader.Exception e) {
          e.printStackTrace();
          result.error(e.reason().name(), "Error starting camera for reason: " + e.reason().name(), null);
        }

        break;
      }
      case "stop": {
        reader.stop();
        result.success(null);
        break;
      }
      case "heartbeat": {
        reader.heartBeat();
        result.success(null);
        break;
      }
      case "getSupportedSizes":{
        List<int[]> sizes = reader.getSupportedSizes();
        result.success(sizes);
        break;
      }
      default:
        result.notImplemented();
    }
  }

  /**
   *
   * @param frame
   * @param rotation - rotation of the camera frame; 0=none, 1=90 degress cc, 2=180, 3=270 degrees cc
   */
  @Override
  public void cameraFrame(byte[] frame, int rotation) {
    List<Object> arguments = new ArrayList<Object>(Arrays.asList(frame, new Integer(rotation)));
    channel.invokeMethod("cameraFrame", arguments);
  }

  @Override
  public void qrRead(String data) {
    System.out.println("Invoking qrRead");
    channel.invokeMethod("qrRead", data);
  }
}