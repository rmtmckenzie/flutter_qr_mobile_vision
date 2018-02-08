import Foundation
import AVFoundation
import GoogleMobileVision

class OrientationHandler {
    
    var lastKnownOrientation: UIDeviceOrientation!
    
    init() {
        setLastOrientation(UIDevice.current.orientation, defaultOrientation: .portrait)
        UIDevice.current.beginGeneratingDeviceOrientationNotifications()

        NotificationCenter.default.addObserver(forName: NSNotification.Name.UIDeviceOrientationDidChange, object: nil, queue: nil, using: orientationDidChange(_:))
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

class QrReader {
    
    let targetWidth: Int
    let targetHeight: Int
    let textureHandler: TextureHandler
    let heartbeatTimeout: Int?
    
//    let orientationHandler = OrientationHandler()
    
    var session: AVCaptureSession!
    var videoDataOutput: AVCaptureVideoDataOutput! = nil
    var videoDataOutputQueue: DispatchQueue!
    
    var barcodeDetector: GMVDetector!

    var surfaceReceived: (CMSampleBuffer)
    var qrReceived: (String)
    
    init(surfaceReceived: @escaping (CMSampleBuffer), qrReceived: @escaping (String), heartbeatTimeout: Int?) {
        self.heartbeatTimeout = heartbeatTimeout
        self.surfaceReceived = surfaceReceived
        self.qrReceived = qrReceived
    }
    
    func start() {
        session = AVCaptureSession();
        session.sessionPreset = .medium;
        do {
            try updateCameraSelection();
        } catch {
            print("Couldn't update camera selection: \(error)")
        }
        setUpVideoProcessing()
    }
    
    func updateCameraSelection() throws {
        session.beginConfiguration()
        guard let device = captureDeviceForPosition(.back) else {
            session.commitConfiguration();
            return;
        }
        
        //remove old inputs
        for input in session.inputs {
            session.removeInput(input)
        }
        session.addInput(try AVCaptureDeviceInput(device: device));
        session.commitConfiguration();
    }
    
    func captureDeviceForPosition(_ capturePosition: AVCaptureDevice.Position) -> AVCaptureDevice? {
        return AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: capturePosition);
    }
    
    func setUpVideoProcessing() {
        videoDataOutput = AVCaptureVideoDataOutput();
        videoDataOutput.videoSettings = [kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_32BGRA]
        guard session.canAddOutput(videoDataOutput) else {
            //TODO: throw?
            cleanupVideoProcessing()
            return
        }
        videoDataOutput.alwaysDiscardsLateVideoFrames = true
        videoDataOutput.setSampleBufferDelegate(self, queue: videoDataOutputQueue)
        session.addOutput(videoDataOutput)
    }

    ///// camera capture stuff
    func captureOutput(_ output: AVCaptureOutput, didOutput sampleBuffer: CMSampleBuffer, from connection: AVCaptureConnection) {
        
        print("Frame received")
        
        // todo: use this instead....
        CMSampleBufferGetImageBuffer(buffer)
        textureHandler.setImageBuffer(buffer: )
        
        let image = GMVUtility.sampleBufferTo32RGBA(sampleBuffer)
        
        let gvmOrientation = GMVUtility.imageOrientation(from: UIDevice.current.orientation, with: .back, defaultDeviceOrientation: lastKnownOrientation)
        
        let barcodes = barcodeDetector.features(in: image, options: [GMVDetectorImageOrientation: gvmOrientation.rawValue])
        
        if let barcodes = barcodes {
            if barcodes.count > 0 {
                let barcode = barcodes[0] as! GMVBarcodeFeature
                print("Detected \(barcode.rawValue!)")
                // guard so that don't spam the router with 10 requests to close
                if (!detected) {
                    DispatchQueue.main.async {
                        //TODO: notify about barcode =D
//                        self.router.complete {
//                            self.codeDelegate?.pairingCode(code: barcode.rawValue!)
//                        }
                    }
                }
            }
        }
    }
    
    func stop(){
        session?.stopRunning()
        if let videoDataOutput = videoDataOutput {
            session?.removeOutput(videoDataOutput)
        }
        videoDataOutput = nil
    }
    
    func heartBeat() {
        //TODO
    }
    
    deinit {
        stop()
    }
}
