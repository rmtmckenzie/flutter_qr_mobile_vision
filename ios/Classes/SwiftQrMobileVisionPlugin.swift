import Flutter
import UIKit

class MapArgumentReader {
    
    let args: [String: Any]?
    
    init(_ args: [String: Any]?) {
        self.args = args
    }
    
    func string(key: String) -> String? {
        return args?[key] as? String
    }
    
    func int(key: String) -> Int? {
        return (args?[key] as? NSNumber)?.intValue
    }
    
}

public class SwiftQrMobileVisionPlugin: NSObject, FlutterPlugin {
    
    let textureRegistry: FlutterTextureRegistry
    let channel: FlutterMethodChannel
    
    init(channel: FlutterMethodChannel, textureRegistry: FlutterTextureRegistry) {
        self.textureRegistry = textureRegistry
        self.channel = channel
    }
    
    var reader: QrReader? = nil
    
    private func handleMethodCall(call: FlutterMethodCall, result: FlutterResult){
        let argReader = MapArgumentReader(call.arguments as? [String: Any])
        
        switch call.method{
        case "start":
            if reader != nil {
                result(FlutterError(code: "ALREADY_RUNNING", message: "Start cannot be called when already running", details: ""))
                return
            }
            
            let heartBeatTimeout = argReader.int(key: "heartBeatTimeout")
            guard let targetWidth = argReader.int(key: "targetWidth"),
                let targetHeight = argReader.int(key: "targetHeight")
                else {
                    result(FlutterError(code: "INVALID_ARGUMENT", message: "Missing a required argument", details: "Expecting targetWidth, targetHeight, and optionally heartbeatTimeout"))
                    return
            }
            
            let texture = TextureHandler(textureRegistry)
            
            reader = QrReader(targetWidth: targetWidth, targetHeight: targetHeight, textureHandler: texture, heartbeatTimeout: heartBeatTimeout)
            reader!.start();
        case "stop":
            reader?.stop();
            reader = nil
        case "heartBeat":
            reader?.heartBeat();
        default : result(FlutterMethodNotImplemented);
        }
    }
    
    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: "qr_mobile_vision", binaryMessenger: registrar.messenger());
        let instance = SwiftQrMobileVisionPlugin(channel: channel, textureRegistry: registrar.textures());       
        registrar.addMethodCallDelegate(instance, channel: channel)
    }
    
}

