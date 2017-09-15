import Flutter
import UIKit
    
public class SwiftQrMobileVisionPlugin: NSObject, FlutterPlugin {
  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(name: "qr_mobile_vision", binaryMessenger: registrar.messenger());
    let instance = SwiftQrMobileVisionPlugin();
    registrar.addMethodCallDelegate(instance, channel: channel);
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    result("iOS " + UIDevice.current.systemVersion);
  }
}
