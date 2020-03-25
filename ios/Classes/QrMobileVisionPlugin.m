#import "QrMobileVisionPlugin.h"
#if __has_include(<qr_mobile_vision/qr_mobile_vision-Swift.h>)
#import <qr_mobile_vision/qr_mobile_vision-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "qr_mobile_vision-Swift.h"
#endif

@implementation QrMobileVisionPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftQrMobileVisionPlugin registerWithRegistrar:registrar];
}
@end