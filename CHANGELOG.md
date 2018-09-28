## [0.0.1] - June 5, 2018

* Initial release that supports scanning all types of barcodes
  that the QR Mobile Vision library supports.
  

## [0.1.0] - Sept 28, 2018

* Switch to different method of camera capture for Camera2. Might be
  some cases where this fails, but shouldn't. The camera2 api is a bit weird
  about image formats, but the one being used should be supported by all phones.

  Also switched to using continuous autofocus if supported. Should hopefully 
  have better usability on some devices than before.