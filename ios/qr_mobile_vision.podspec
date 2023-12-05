Pod::Spec.new do |s|
  s.name             = 'qr_mobile_vision'
  s.version          = '0.0.1'
  s.summary          = 'Plugin for reading QR codes using Firebase&#x27;s Mobile Vision API.'
  s.description      = <<-DESC
Plugin for reading QR codes using Google&#x27;s Mobile Vision API.
                       DESC
  s.homepage         = 'https://github.com/rmtmckenzie/flutter_qr_mobile_vision'
  s.license          = { :file => '../LICENSE' }
  s.author           = { 'Morgan McKenzie' => 'rmtmckenzie@gmail.com' }
  s.source           = { :path => '.' }
  s.source_files = 'Classes/**/*'
  s.public_header_files = 'Classes/**/*.h'
  s.dependency 'Flutter'
  s.platform = :ios, '11.0'

  # Flutter.framework does not contain a i386 slice. Only x86_64 simulators are supported.
  s.pod_target_xcconfig = { 'DEFINES_MODULE' => 'YES', 'VALID_ARCHS[sdk=iphonesimulator*]' => 'x86_64' }
  # Mobile vision doesn't support 32 bit ios
  s.pod_target_xcconfig = { 'DEFINES_MODULE' => 'YES', 'VALID_ARCHS[sdk=iphoneos*]' => 'arm64' }
  s.swift_version = '5.0'

  s.dependency 'GoogleMLKit/BarcodeScanning'
  
  s.static_framework = true
end
