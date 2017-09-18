package com.github.rmtmckenzie.qrmobilevision;

public interface QRReaderCallbacks {
    void cameraFrame(byte[] frame);
    void qrRead(String data);
}
