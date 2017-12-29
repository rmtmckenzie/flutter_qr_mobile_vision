import Flutter
import UIKit


public class SwiftQrMobileVisionPlugin: NSObject, FlutterPlugin {
    var reader = QrReader();
    private func handleMethodCall(call: FlutterMethodCall, result: FlutterResult){
        switch call.method{
        case "start": reader.start();
        case "stop": reader.stop();
        default : result(FlutterMethodNotImplemented);
        }
    }
    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: "qr_mobile_vision", binaryMessenger: registrar.messenger());
        let instance = SwiftQrMobileVisionPlugin();
        //channel.setMethodCallHandler(handleMethodCall(SwiftQrMobileVisionPlugin));
        //registrar.addMethodCallDelegate(instance, channel: channel);
    }
    
}

