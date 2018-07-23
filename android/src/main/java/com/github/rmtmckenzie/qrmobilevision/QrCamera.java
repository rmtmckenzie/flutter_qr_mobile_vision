package com.github.rmtmckenzie.qrmobilevision;

interface QrCamera {
    void start() throws QRReader.Exception;
    void stop();
    int getOrientation();
    int getWidth();
    int getHeight();
}
