Pod::Spec.new do |s|
  s.name             = 'qr_mobile_vision'
  s.version          = '0.0.1'
  s.summary          = 'Plugin for reading QR codes using Google&#x27;s Mobile Vision API.'
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
  
  s.ios.deployment_target = '8.0'
  
  s.dependency 'GoogleMobileVision/BarcodeDetector'
  
  s.static_framework = true
end
