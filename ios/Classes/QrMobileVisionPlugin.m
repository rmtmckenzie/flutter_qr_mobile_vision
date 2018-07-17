@import AVFoundation;

#import "QrMobileVisionPlugin.h"
#import <libkern/OSAtomic.h>
#import "GoogleMobileVision/GoogleMobileVision.h"

@interface NSError (FlutterError)
@property(readonly, nonatomic) FlutterError *flutterError;
@end

@implementation NSError (FlutterError)
- (FlutterError *)flutterError {
    return [FlutterError errorWithCode:[NSString stringWithFormat:@"Error %d", (int)self.code]
                               message:self.domain
                               details:self.localizedDescription];
}
@end

@interface QrReader: NSObject<FlutterTexture, AVCaptureVideoDataOutputSampleBufferDelegate>
@property(readonly, nonatomic) int64_t textureId;
@property(nonatomic, copy) void (^onFrameAvailable)(void);
@property(nonatomic) FlutterEventChannel *eventChannel;
@property(nonatomic) FlutterEventSink eventSink;
@property(readonly, nonatomic) AVCaptureSession *captureSession;
@property(readonly, nonatomic) AVCaptureDevice *captureDevice;
@property(readonly) CVPixelBufferRef volatile latestPixelBuffer;
@property(readonly, nonatomic) CMVideoDimensions previewSize;
@property(readonly, nonatomic) dispatch_queue_t mainQueue;

@property(nonatomic, strong) GMVDetector *barcodeDetector;
@property(nonatomic, copy) void (^onCodeAvailable)(NSString *);

- (instancetype)initWithErrorRef:(NSError **)error;
@end

@implementation QrReader

- (instancetype)initWithErrorRef:(NSError **)error {
    self = [super init];
    NSAssert(self, @"super init cannot be nil");
    _captureSession = [[AVCaptureSession alloc] init];
    
    if (@available(iOS 10.0, *)) {
        _captureDevice = [AVCaptureDevice defaultDeviceWithDeviceType:AVCaptureDeviceTypeBuiltInWideAngleCamera mediaType:AVMediaTypeVideo position:AVCaptureDevicePositionBack];
    } else {
        for(AVCaptureDevice* device in [AVCaptureDevice devicesWithMediaType:AVMediaTypeVideo]) {
            if (device.position == AVCaptureDevicePositionBack) {
                _captureDevice = device;
                break;
            }
        }

        if (_captureDevice == nil) {
            _captureDevice = [AVCaptureDevice defaultDeviceWithMediaType:AVMediaTypeVideo];
        }
    }
    
    _mainQueue = dispatch_get_main_queue();
    
    NSError *localError = nil;
    AVCaptureInput *input = [AVCaptureDeviceInput deviceInputWithDevice:_captureDevice error:&localError];
    if (localError) {
        *error = localError;
        return nil;
    }
    _previewSize = CMVideoFormatDescriptionGetDimensions([[_captureDevice activeFormat] formatDescription]);
    
    AVCaptureVideoDataOutput *output = [AVCaptureVideoDataOutput new];
    
    output.videoSettings =
    @{(NSString *)kCVPixelBufferPixelFormatTypeKey : @(kCVPixelFormatType_32BGRA) };
    [output setAlwaysDiscardsLateVideoFrames:YES];
    dispatch_queue_t queue = dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0);
    [output setSampleBufferDelegate:self queue:queue];
    
    AVCaptureConnection *connection =
    [AVCaptureConnection connectionWithInputPorts:input.ports output:output];
    connection.videoOrientation = AVCaptureVideoOrientationPortrait;
    
    [_captureSession addInputWithNoConnections:input];
    [_captureSession addOutputWithNoConnections:output];
    [_captureSession addConnection:connection];
    
    _barcodeDetector = [GMVDetector detectorOfType:GMVDetectorTypeBarcode options:nil];
    
    return self;
}

- (void)start {
    NSAssert(_onFrameAvailable, @"On Frame Available must be set!");
    NSAssert(_onCodeAvailable, @"On Code Available must be set!");
    [_captureSession startRunning];
}

- (void)stop {
    [_captureSession stopRunning];
}

// This returns a CGImageRef that needs to be released!
- (CGImageRef)cgImageRefFromCMSampleBufferRef:(CMSampleBufferRef)sampleBuffer {
    CIContext *ciContext = [CIContext contextWithOptions:nil];
    CVPixelBufferRef newBuffer1 = CMSampleBufferGetImageBuffer(sampleBuffer);
    CIImage *ciImage = [CIImage imageWithCVPixelBuffer:newBuffer1];
    return [ciContext createCGImage:ciImage fromRect:ciImage.extent];
}


- (void)captureOutput:(AVCaptureOutput *)output
didOutputSampleBuffer:(CMSampleBufferRef)sampleBuffer
       fromConnection:(AVCaptureConnection *)connection {
    // runs on main queue

    // create a new buffer in the form of a CGImage containing the image.
    // NOTE: it must be released manually!
    CGImageRef cgImageRef = [self cgImageRefFromCMSampleBufferRef:sampleBuffer];
    
    CVPixelBufferRef newBuffer = CMSampleBufferGetImageBuffer(sampleBuffer);
    
    CFRetain(newBuffer);
    CVPixelBufferRef old = _latestPixelBuffer;
    while (!OSAtomicCompareAndSwapPtrBarrier(old, newBuffer, (void **)&_latestPixelBuffer)) {
        old = _latestPixelBuffer;
    }
    if (old != nil) {
        CFRelease(old);
    }
    
    dispatch_sync(_mainQueue, ^{
        self.onFrameAvailable();
    });
    
    ///////// TODO: dispatch this to a background thread (or at least later on main thread?)!
    /// process the frame with GMV
    AVCaptureDevicePosition devicePosition = AVCaptureDevicePositionBack;
    UIDeviceOrientation deviceOrientation = [[UIDevice currentDevice] orientation];
    // TODO: last known orientation?
    GMVImageOrientation orientation = [GMVUtility imageOrientationFromOrientation:deviceOrientation withCaptureDevicePosition:devicePosition defaultDeviceOrientation:UIDeviceOrientationPortrait];
    
    NSDictionary *options = @{
        GMVDetectorImageOrientation: @(orientation)
    };
    
    UIImage *image = [UIImage imageWithCGImage:cgImageRef];
    NSArray<GMVBarcodeFeature *> *barcodes = [_barcodeDetector featuresInImage:image options:options];
    image = nil;
    CGImageRelease(cgImageRef);

    if (barcodes.count > 0) {
        GMVBarcodeFeature *barcode0 = barcodes[0];
        NSString * value = [barcode0 rawValue];
        NSLog(@"Detected barcode: %@", value);
        dispatch_async(_mainQueue, ^{
            self->_onCodeAvailable(value);
        });
    }
}

- (void)heartBeat {
    // TODO: implement
}

- (CVPixelBufferRef)copyPixelBuffer {
    CVPixelBufferRef pixelBuffer = _latestPixelBuffer;
    while (!OSAtomicCompareAndSwapPtrBarrier(pixelBuffer, nil, (void **)&_latestPixelBuffer)) {
        pixelBuffer = _latestPixelBuffer;
    }
    return pixelBuffer;
}

- (void)dealloc {
    if (_latestPixelBuffer) {
        CFRelease(_latestPixelBuffer);
    }
}

@end


@interface QrMobileVisionPlugin ()
@property(readonly, nonatomic) NSObject<FlutterTextureRegistry> *registry;
@property(readonly, nonatomic) FlutterMethodChannel *channel;

@property(readonly, nonatomic) QrReader *reader;
@end

@implementation QrMobileVisionPlugin

+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
    FlutterMethodChannel* channel = [FlutterMethodChannel
                                     methodChannelWithName:@"com.github.rmtmckenzie/qr_mobile_vision"
                                     binaryMessenger:[registrar messenger]];
    QrMobileVisionPlugin* instance = [[QrMobileVisionPlugin alloc] initWithRegistry:[registrar textures] channel:channel];
    [registrar addMethodCallDelegate:instance channel:channel];
}

- (instancetype)initWithRegistry:(NSObject<FlutterTextureRegistry> *)registry
                         channel:(FlutterMethodChannel *)channel {
    self = [super init];
    NSAssert(self, @"super init cannot be nil");
    _registry = registry;
    _channel = channel;
    _reader = nil;
    return self;
}

- (void)handleMethodCall:(FlutterMethodCall*)call result:(FlutterResult)result {
    
    if ([@"start" isEqualToString:call.method]) {
        // NSNumber *heartbeatTimeout = call.arguments[@"heartbeatTimeout"];
        NSNumber *targetWidth = call.arguments[@"targetWidth"];
        NSNumber *targetHeight = call.arguments[@"targetHeight"];
        
        if (targetWidth == nil || targetHeight == nil) {
            result([FlutterError errorWithCode:@"INVALID_ARGS"
                                       message: @"Missing a required argument"
                                       details: @"Expecting targetWidth, targetHeight, and optionally heartbeatTimeout"]);
            return;
        }
        [self startWithCallback:^(int width, int height, int orientation, int64_t textureId) {
            result(@{
                     @"surfaceWidth": @(width),
                     @"surfaceHeight": @(height),
                     @"surfaceOrientation": @(orientation),
                     @"textureId": @(textureId)
                     });
        } orFailure: ^ (NSError *error) {
            result(error.flutterError);
        }];
    } else if ([@"stop" isEqualToString:call.method]) {
        [self stop];
        result(nil);
    } else if ([@"heartbeat" isEqualToString:call.method]) {
        [self heartBeat];
        result(nil);
    } else {
        result(FlutterMethodNotImplemented);
    }
}

- (void)startWithCallback:(void (^)(int width, int height, int orientation, int64_t textureId))completedCallback orFailure:(void (^)(NSError *))failureCallback {
    
    if (_reader) {
        failureCallback([NSError errorWithDomain:@"qr_mobile_vision" code:1 userInfo:@{NSLocalizedDescriptionKey:NSLocalizedString(@"Reader already running.", nil)}]);
        return;
    }

    NSError* localError = nil;
    _reader = [[QrReader alloc] initWithErrorRef: &localError];

    if (localError) {
        failureCallback(localError);
        return;
    }

    NSObject<FlutterTextureRegistry> * __weak registry = _registry;
    int64_t textureId = [_registry registerTexture:_reader];
    _reader.onFrameAvailable = ^{
        [registry textureFrameAvailable:textureId];
    };

    FlutterMethodChannel * __weak channel = _channel;
    _reader.onCodeAvailable = ^(NSString *value) {
        [channel invokeMethod:@"qrRead" arguments: value];
    };

    [_reader start];

    ///////// texture, width, height
    completedCallback(_reader.previewSize.width, _reader.previewSize.height, 0, textureId);
}

- (void)stop {
    if (_reader) {
        [_reader stop];
        _reader = nil;
    }
}

- (void)heartBeat {
    if (_reader) {
        [_reader heartBeat];
    }
}

@end
