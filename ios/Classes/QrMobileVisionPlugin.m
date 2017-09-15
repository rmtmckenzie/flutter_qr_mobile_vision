#import "QrMobileVisionPlugin.h"
#import <qr_mobile_vision/qr_mobile_vision-Swift.h>

@implementation QrMobileVisionPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftQrMobileVisionPlugin registerWithRegistrar:registrar];
}
@end
