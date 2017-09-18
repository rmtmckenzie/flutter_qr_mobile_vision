package com.github.rmtmckenzie.qrmobilevision;

public interface QRReaderCallbacks {
    void cameraFrame(byte[] frame, int rotation);
    void qrRead(String data);
}
