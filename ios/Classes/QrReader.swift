import Foundation
import AVFoundation
import FirebaseMLVision
import os.log


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
  let textureHandler: TextureHandler
  let isProcessing = Atomic<Bool>(false)
  
  var captureDevice: AVCaptureDevice!
  var captureSession: AVCaptureSession!
  var previewSize: CMVideoDimensions!
  let barcodeDetector: VisionBarcodeDetector
  let cameraPosition = AVCaptureDevice.Position.back
  let qrCallback: (_:String) -> Void
  
  init(targetWidth: Int, targetHeight: Int, textureHandler: TextureHandler, qrCallback: @escaping (_:String) -> Void) {
    self.targetWidth = targetWidth
    self.targetHeight = targetHeight
    self.textureHandler = textureHandler
    self.qrCallback = qrCallback
    
    let format = VisionBarcodeFormat.all
    let barcodeOptions = VisionBarcodeDetectorOptions(formats: format)
    let vision = Vision.vision()
    self.barcodeDetector = vision.barcodeDetector(options: barcodeOptions)
    
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
    
    let queue = DispatchQueue.global(qos: DispatchQoS.QoSClass.default)
    output.setSampleBufferDelegate(self, queue: queue)
    
    captureSession.addInput(input)
    captureSession.addOutput(output)
  }
  
  func start() {
    captureSession.startRunning()
  }
  
  func stop() {
    captureSession.stopRunning()
  }
  
}

extension QrReader: AVCaptureVideoDataOutputSampleBufferDelegate {
  func captureOutput(_ output: AVCaptureOutput, didOutput sampleBuffer: CMSampleBuffer, from connection: AVCaptureConnection) {
    // runs on dispatch queue
    
    textureHandler.setImageBuffer(buffer: sampleBuffer)
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
