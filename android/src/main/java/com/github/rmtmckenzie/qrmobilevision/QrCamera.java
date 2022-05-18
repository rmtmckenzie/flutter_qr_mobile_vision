package com.github.rmtmckenzie.qrmobilevision;

interface QrCamera {
    void start(final int cameraDirection) throws QrReader.Exception;
    void stop();
    void toggleFlash();
    int getOrientation();
    int getWidth();
    int getHeight();
}
