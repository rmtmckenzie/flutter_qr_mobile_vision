import Foundation
import AVFoundation

import os.log

extension UIImage {
    func resizeCI(size:CGSize) -> UIImage? {
        let scale = (Double)(size.width) / (Double)(self.size.width)
        guard let image = self.ciImage else { return nil}
            
        let filter = CIFilter(name: "CILanczosScaleTransform")!
        filter.setValue(image, forKey: kCIInputImageKey)
        filter.setValue(NSNumber(value:scale), forKey: kCIInputScaleKey)
        filter.setValue(1.0, forKey:kCIInputAspectRatioKey)
        let outputImage = filter.value(forKey: kCIOutputImageKey) as! UIKit.CIImage
            
        let context = CIContext(options: [CIContextOption.useSoftwareRenderer: false])
        guard let cgImage = context.createCGImage(outputImage, from: outputImage.extent) else { return nil }
        return UIImage(cgImage: cgImage)
    }
    
    func parseQR() -> [String] {
        guard let image = CIImage(image: self) else {
            return []
        }

        let detector = CIDetector(ofType: CIDetectorTypeQRCode,
                                  context: nil,
                                  options: [CIDetectorAccuracy: CIDetectorAccuracyHigh])

        let features = detector?.features(in: image) ?? []

        return features.compactMap { feature in
            return (feature as? CIQRCodeFeature)?.messageString
        }
    }
}


class OrientationHandler {
  
  var lastKnownOrientation: UIDeviceOrientation!
  
  init() {
    setLastOrientation(UIDevice.current.orientation, defaultOrientation: .portrait)
    UIDevice.current.beginGeneratingDeviceOrientationNotifications()
    
    NotificationCenter.default.addObserver(forName: UIDevice.orientationDidChangeNotification, object: nil, queue: nil, using: orientationDidChange(_:))
  }
  
  func setLastOrientation(_ deviceOrientation: UIDeviceOrientation, defaultOrientation: UIDeviceOrientation?) {
    
    // set last device orientation but only if it is recognized
    switch deviceOrientation {
    case .unknown, .faceUp, .faceDown:
      lastKnownOrientation = defaultOrientation ?? lastKnownOrientation
      break
    default:
      lastKnownOrientation = deviceOrientation
    }
  }
  
  func orientationDidChange(_ notification: Notification) {
    let deviceOrientation = UIDevice.current.orientation
    
    let prevOrientation = lastKnownOrientation
    setLastOrientation(deviceOrientation, defaultOrientation: nil)
    
    if prevOrientation != lastKnownOrientation {
      //TODO: notify of orientation change??? (but mostly why bother...)
    }
  }
  
  deinit {
    UIDevice.current.endGeneratingDeviceOrientationNotifications()
  }
}

protocol QrReaderResponses {
  func surfaceReceived(buffer: CMSampleBuffer)
  func qrReceived(code: String)
}

class QrReader: NSObject {
  let targetWidth: Int
  let targetHeight: Int
  let textureRegistry: FlutterTextureRegistry
  let isProcessing = Atomic<Bool>(false)
  var captureFrame: CGRect = CGRect(x: 77, y: 296, width: 221, height: 221)
  var captureDevice: AVCaptureDevice!
  var captureSession: AVCaptureSession!
  var previewSize: CMVideoDimensions!
  var textureId: Int64!
  var pixelBuffer : CVPixelBuffer?
  let cameraPosition = AVCaptureDevice.Position.back
  let qrCallback: (_:String) -> Void
  
  init(targetWidth: Int, targetHeight: Int, textureRegistry: FlutterTextureRegistry, options: Any, qrCallback: @escaping (_:String) -> Void) {
    self.targetWidth = targetWidth
    self.targetHeight = targetHeight
    self.textureRegistry = textureRegistry
    self.qrCallback = qrCallback
    

    
    super.init()
    
    captureSession = AVCaptureSession()
    
    if #available(iOS 10.0, *) {
      captureDevice = AVCaptureDevice.default(AVCaptureDevice.DeviceType.builtInWideAngleCamera, for: AVMediaType.video, position: cameraPosition)
    } else {
      for device in AVCaptureDevice.devices(for: AVMediaType.video) {
        if device.position == cameraPosition {
          captureDevice = device
          break
        }
      }
    }
    
    if captureDevice == nil {
      captureDevice = AVCaptureDevice.default(for: AVMediaType.video)
    }
    
    if (captureDevice == nil) {
        return
    }
    
    // catch?
    let input = try! AVCaptureDeviceInput.init(device: captureDevice)
    previewSize = CMVideoFormatDescriptionGetDimensions(captureDevice.activeFormat.formatDescription)
    
    let output = AVCaptureVideoDataOutput()
    output.videoSettings = [kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_32BGRA]
    output.alwaysDiscardsLateVideoFrames = true
    let connection = AVCaptureConnection(inputPorts: input.ports, output: output)
//    connection.videoOrientation = .portrait
    
    
    let queue = DispatchQueue.global(qos: DispatchQoS.QoSClass.default)
    output.setSampleBufferDelegate(self, queue: queue)
    
    captureSession.addInputWithNoConnections(input)
    captureSession.addOutputWithNoConnections(output)
    captureSession.addConnection(connection)
  }
  
  func start() {
    captureSession.startRunning()
    self.textureId = textureRegistry.register(self)
  }
  
  func stop() {
    captureSession.stopRunning()
    pixelBuffer = nil
    textureRegistry.unregisterTexture(textureId)
    textureId = nil
  }
}

extension QrReader : FlutterTexture {
    func copyPixelBuffer() -> Unmanaged<CVPixelBuffer>? {
        if(pixelBuffer == nil){
            return nil
        }
        return  .passRetained(pixelBuffer!)
    }
}

extension QrReader: AVCaptureVideoDataOutputSampleBufferDelegate {
    func captureOutput(_ output: AVCaptureOutput, didOutput sampleBuffer: CMSampleBuffer, from connection: AVCaptureConnection) {
    // runs on dispatch queue
    
        pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer)!
        textureRegistry.textureFrameAvailable(self.textureId)
        
        guard !isProcessing.swap(true) else {
          return
        }
        guard let pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) else {
            return
        }
        let ciImage = CIImage(cvPixelBuffer: pixelBuffer)
        let imgRect = ciImage.extent
        let cropRect = CGRect(
            x: imgRect.width * 0.2 ,
            y: imgRect.height * 0.2,
            width: imgRect.width * 0.6,
            height: imgRect.height * 0.6
        )
        let crop = ciImage.cropped(to:cropRect)
        let img = UIImage(ciImage: crop).resizeCI(size: cropRect.size)
        DispatchQueue.global(qos: DispatchQoS.QoSClass.utility).async {
            if let qrs = img?.parseQR() {
                self.isProcessing.value = false
                print(qrs)
                for qr in qrs {
                    self.qrCallback(qr)
                }
            }
        }
    }
}
