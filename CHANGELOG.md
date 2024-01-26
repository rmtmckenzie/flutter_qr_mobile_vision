## [5.0.0] - January 26, 2024
* Bump version to match what is supported by MLKit library.
  No more Camera1 API needed!
* No major changes, but did recreate all plumbing with
  flutter create and updated qr_mobile_vision to 2.0
  which might require a flutter clean.

## [4.1.4]
* Bugfix for flashlight on phones with multiple cameras

## [4.1.3]
* Update android MLKit version, gradle, etc
* Use triple camera support for iOS when available
  (tested to work on phones without triple camera)

## [4.1.2]
* Small changes to support dart 3

## [4.1.1]
* Add support for 9.x.x versions of device_info_plus

## [4.1.0]
* Long overdue update to support newer versions of device_info_plus plugin
* Fixes some crashes due to frame closing on Android

## [4.0.1]
* Updates device_info_plus dependency

## [4.0.0]
* Same as 4.0.0-dev.1 but is release package

## [4.0.0-dev.1]
* Updates to flutter 3.0
* Adds toggling flash functionality
* Adds specific fix for Android 6.0 (not sure if this might cause problems for some devices though,
  it flips orientation for landscape
* Adds ability to choose front vs back camera
* Sorts list of resolutions before choosing the appropriate one

_Big thanks to all the PRs that helped with this release!_

_Lots of changes so I'm releasing as dev first - if no big issues pop up in the next couple days, I'll release a non-dev version_

## [3.0.1]
* Incorporate PRs for fix on iOS when receive null qr data, and to
  use screen's physical resolution rather than logical resolution.

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
