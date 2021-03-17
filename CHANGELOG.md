## [3.0.0]
* Null-safety

## [2.0.0]
* Switch to using ML Kit without firebase
* Fix a few small bugs that users reported

## [1.0.2]
* Fix for android exception when specify more than one type

## [1.0.1+1]
* Remove log statement from image close

## [1.0.1]
* Fix android crashing issue
* Fix preview upside down on android

## [1.0.0+3]
* code cleanup

## [1.0.0+2]
* typo in readme

## [1.0.0-1]
* lower SDK constraint for pub.dev.

## [1.0.0] 
_Breaking change!_
* Switch to using Firebase Mobile Vision
* Update to AndroidX
* Fix dependency compilation issues
* Handle formats choice properly on iOS
* Improved performance significantly esp. on iOS
    * moved processing to background
    * skip frames when necessary

## [0.3.1] - December 24, 2019
* Fix NPE on sms permission acceptance

## [0.3.0] - November 6, 2019
* Small maintenance upgrade because dependency wouldn't compile"

## [0.2.2] - March 5, 2019

* Fix detector for older phones as it wouldn't work if the returned preview size
  wasn't the same as what it requested.
  
* Fix lint warnings

## [0.2.1] - March 5, 2019

* Changes to Android Camera2 buffer system so that new buffers aren't being allocated constantly
  but rather the same buffers are reused.

## [0.2.0] - March 4, 2019

* Upgrade to AndroidX, hopefully fix an android crash issue and autofocus on some devices.

## [0.1.0] - Sept 28, 2018

* Switch to different method of camera capture for Camera2. Might be
  some cases where this fails, but shouldn't. The camera2 api is a bit weird
  about image formats, but the one being used should be supported by all phones.

  Also switched to using continuous autofocus if supported. Should hopefully 
  have better usability on some devices than before.

## [0.0.1] - June 5, 2018

* Initial release that supports scanning all types of barcodes
  that the QR Mobile Vision library supports.
