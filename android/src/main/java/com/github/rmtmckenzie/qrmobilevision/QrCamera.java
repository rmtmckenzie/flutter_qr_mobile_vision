package com.github.rmtmckenzie.qrmobilevision;

/**
 * Created by baraka on 29/11/2017.
 */

interface QrCamera {
    void start();
    void stop();
    int getOrientation();
    int getWidth();
    int getHeight();
}
