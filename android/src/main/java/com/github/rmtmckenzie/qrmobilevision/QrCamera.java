package com.github.rmtmckenzie.qrmobilevision;

interface QrCamera {
    void start() throws QrReader.Exception;
    void stop();
    void toggleFlash();
    int getOrientation();
    int getWidth();
    int getHeight();
}
