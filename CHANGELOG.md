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
