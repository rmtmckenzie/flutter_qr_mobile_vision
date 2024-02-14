package com.github.rmtmckenzie.qr_mobile_vision;

import android.os.Handler;
import android.os.Looper;

public class Heartbeat {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable runner;
    private final int timeout;

    public Heartbeat(int timeout, Runnable runner) {
        this.timeout = timeout;
        this.runner = runner;

        handler.postDelayed(runner, timeout);
    }

    public void beat() {
        handler.removeCallbacks(runner);
        handler.postDelayed(runner, timeout);
    }

    public void stop() {
        handler.removeCallbacks(runner);
    }

}
