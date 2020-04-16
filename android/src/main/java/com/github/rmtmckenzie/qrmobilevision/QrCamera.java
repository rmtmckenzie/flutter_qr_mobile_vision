package com.github.rmtmckenzie.qrmobilevision;

interface QrCamera {
    void start() throws QrReader.Exception;
    void stop();
    int getOrientation();
    void toggleFlash();
    int getWidth();
    int getHeight();
}
