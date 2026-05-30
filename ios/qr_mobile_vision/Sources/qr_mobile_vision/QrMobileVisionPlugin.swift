import Flutter
import UIKit
import MLKitVision
import MLKitBarcodeScanning

public class QrMobileVisionPlugin: NSObject, FlutterPlugin {
  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(name: "qr_mobile_vision", binaryMessenger: registrar.messenger())
    let instance = QrMobileVisionPlugin(channel: channel, textureRegistry: registrar.textures())
    registrar.addMethodCallDelegate(instance, channel: channel)
  }
  
  init(channel: FlutterMethodChannel, textureRegistry: FlutterTextureRegistry) {
    self.textureRegistry = textureRegistry
    self.channel = channel
  }
  
  
  let textureRegistry: FlutterTextureRegistry
  let channel: FlutterMethodChannel
  
  var reader: QrReader? = nil
  var torch: Bool = false
  
  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    let argReader = MapArgumentReader(call.arguments as? [String: Any])
    
    switch call.method {
    case "start":
      if reader != nil {
        result(FlutterError(code: "ALREADY_RUNNING", message: "Start cannot be called when already running", details: ""))
        return
      }
      
      //      let heartBeatTimeout = argReader.int(key: "heartBeatTimeout")
      
      guard let targetWidth = argReader.int(key: "targetWidth"),
            let targetHeight = argReader.int(key: "targetHeight"),
            let formatStrings = argReader.stringArray(key: "formats"),
            let cameraDirectionInt = argReader.int(key: "cameraDirection") else {
        result(FlutterError(code: "INVALID_ARGUMENT", message: "Missing a required argument", details: "Expecting targetWidth, targetHeight, formats, and optionally heartbeatTimeout"))
        return
      }
      
      let cameraDirection = cameraDirectionInt == 0 ? QrCameraDirection.front : QrCameraDirection.back
      
      let options = BarcodeScannerOptions(formatStrings: formatStrings)
      
      do {
        reader = try QrReader(
          targetWidth: targetWidth,
          targetHeight: targetHeight,
          direction: cameraDirection,
          textureRegistry: textureRegistry,
          options: options) { [unowned self] qr in
            self.channel.invokeMethod("qrRead", arguments: qr)
          }
        
        reader!.start();
        
        result([
          "surfaceWidth": reader!.previewSize.height,
          "surfaceHeight": reader!.previewSize.width,
          "surfaceOrientation": 0, //TODO: check on iPAD
          "textureId": reader!.textureId!
        ])
      } catch QrReaderError.noCamera {
        result(FlutterError(code: "CAMERA_ERROR", message: "QrReader couldn't open camera", details: nil))
      } catch {
        result(FlutterError(code: "PERMISSION_DENIED", message: "QrReader initialization threw an exception", details: error.localizedDescription))
      }
    case "stop":
      reader?.stop();
      reader = nil
      result(nil)
    case "heartBeat":
      //      reader?.heartBeat();
      result(nil)
    case "toggleFlash":
      torch = !torch
      reader?.toggleTorch(on: torch);
      result(nil)
    default: 
      result(FlutterMethodNotImplemented);
    }
  }
}
