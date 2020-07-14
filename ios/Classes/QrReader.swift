import Foundation
import AVFoundation
import FirebaseMLVision
import os.log


extension VisionBarcodeDetectorOptions {
  convenience init(formatStrings: [String]) {
    let formats = formatStrings.map { (format) -> VisionBarcodeFormat? in
      switch format  {
      case "ALL_FORMATS":
        return .all
      case "AZTEC":
        return .aztec
      case "CODE_128":
        return .code128
      case "CODE_39":
        return .code39
      case "CODE_93":
        return .code93
      case "CODABAR":
        return .codaBar
      case "DATA_MATRIX":
        return .dataMatrix
      case "EAN_13":
        return .EAN13
      case "EAN_8":
        return .EAN8
      case "ITF":
        return .ITF
      case "PDF417":
        return .PDF417
      case "QR_CODE":
        return .qrCode
      case "UPC_A":
        return .UPCA
      case "UPC_E":
        return .UPCE
      default:
        // ignore any unknown values
        return nil
      }
    }.reduce([]) { (result, format) -> VisionBarcodeFormat in
      guard let format = format else {
        return result
      }
      return result.union(format)
    }
    
    self.init(formats: formats)
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
  
  var captureDevice: AVCaptureDevice!
  var captureSession: AVCaptureSession!
  var previewSize: CMVideoDimensions!
  var textureId: Int64!
  var pixelBuffer : CVPixelBuffer?
  let barcodeDetector: VisionBarcodeDetector
  let cameraPosition = AVCaptureDevice.Position.back
  let qrCallback: (_:String) -> Void
  
  init(targetWidth: Int, targetHeight: Int, textureRegistry: FlutterTextureRegistry, options: VisionBarcodeDetectorOptions, qrCallback: @escaping (_:String) -> Void) {
    self.targetWidth = targetWidth
    self.targetHeight = targetHeight
    self.textureRegistry = textureRegistry
    self.qrCallback = qrCallback
    
    let vision = Vision.vision()
    self.barcodeDetector = vision.barcodeDetector(options: options)
    
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
      captureDevice = AVCaptureDevice.default(for: AVMediaType.video)!
    }
    
    // catch?
    let input = try! AVCaptureDeviceInput.init(device: captureDevice)
    previewSize = CMVideoFormatDescriptionGetDimensions(captureDevice.activeFormat.formatDescription)
    
    let output = AVCaptureVideoDataOutput()
    output.videoSettings = [kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_32BGRA]
    output.alwaysDiscardsLateVideoFrames = true
    let connection = AVCaptureConnection(inputPorts: input.ports, output: output)
    connection.videoOrientation = .portrait
    
    
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
    
    let metadata = VisionImageMetadata()
    metadata.orientation = imageOrientation(
      deviceOrientation: UIDevice.current.orientation,
      defaultOrientation: .portrait
    )
    
    guard !isProcessing.swap(true) else {
      return
    }
    
    let image = VisionImage(buffer: sampleBuffer)
    image.metadata = metadata
    
    DispatchQueue.global(qos: DispatchQoS.QoSClass.utility).async {
      self.barcodeDetector.detect(in: image) { features, error in
        self.isProcessing.value = false
        
        guard error == nil else {
          if #available(iOS 10.0, *) {
            os_log("Error decoding barcode %@", error!.localizedDescription)
          } else {
            // Fallback on earlier versions
            NSLog("Error decoding barcode %@", error!.localizedDescription)
          }
          return
        }
        
        guard let features = features, !features.isEmpty else {
          return
        }
                
        for feature in features {
          self.qrCallback(feature.rawValue!)
        }
      }
    }
  }
  
  func imageOrientation(
    deviceOrientation: UIDeviceOrientation,
    defaultOrientation: UIDeviceOrientation
  ) -> VisionDetectorImageOrientation {
    switch deviceOrientation {
    case .portrait:
      return cameraPosition == .front ? .leftTop : .rightTop
    case .landscapeLeft:
      return cameraPosition == .front ? .bottomLeft : .topLeft
    case .portraitUpsideDown:
      return cameraPosition == .front ? .rightBottom : .leftBottom
    case .landscapeRight:
      return cameraPosition == .front ? .topRight : .bottomRight
    case .faceDown, .faceUp, .unknown:
      fallthrough
    @unknown default:
      return imageOrientation(deviceOrientation: defaultOrientation, defaultOrientation: .portrait)
    }
  }
}
