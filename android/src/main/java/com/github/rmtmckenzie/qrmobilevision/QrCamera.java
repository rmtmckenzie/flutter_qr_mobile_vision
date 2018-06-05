package com.github.rmtmckenzie.qrmobilevision;

interface QrCamera {
    void start();
    void stop();
    int getOrientation();
    int getWidth();
    int getHeight();
}
