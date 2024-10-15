Pod::Spec.new do |s|
  s.name             = 'qr_mobile_vision'
  s.version          = '0.0.2'
  s.summary          = 'Plugin for reading QR codes using Firebase&#x27;s Mobile Vision API.'
  s.description      = <<-DESC
Plugin for reading QR codes using Google&#x27;s Mobile Vision API.
                       DESC
  s.homepage         = 'https://github.com/rmtmckenzie/flutter_qr_mobile_vision'
  s.license          = { :file => '../LICENSE' }
  s.author           = { 'Morgan McKenzie' => 'rmtmckenzie@gmail.com' }
  s.source           = { :path => '.' }
  s.source_files = 'Classes/**/*'
  s.dependency 'Flutter'
  s.platform = :ios, '11.0'

  # Flutter.framework does not contain a i386 slice.
  s.pod_target_xcconfig = { 'DEFINES_MODULE' => 'YES', 'EXCLUDED_ARCHS[sdk=iphonesimulator*]' => 'i386' }
  s.swift_version = '5.0'

  s.dependency 'GoogleMLKit/BarcodeScanning', '~> 7.0.0'
  s.platform = :ios, '15.5.0'
  s.static_framework = true
end
