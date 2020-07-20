package com.github.rmtmckenzie.qrmobilevision;

import android.annotation.TargetApi;
import android.hardware.Camera;
import android.os.Build;

import java.util.Collection;
import java.util.List;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
final class CameraConfigurationUtils {

    private static final String TAG = "CameraConfiguration";

    private CameraConfigurationUtils() {
    }

    static void toggleTorch(Camera camera) {
        setTorch(camera, !getTorchState(camera));
    }

    static boolean getTorchState(Camera camera) {
        if (camera != null) {
            Camera.Parameters parameters = camera.getParameters();
            if (parameters != null) {
                String flashMode = parameters.getFlashMode();
                return flashMode != null &&
                    (Camera.Parameters.FLASH_MODE_ON.equals(flashMode) ||
                        Camera.Parameters.FLASH_MODE_TORCH.equals(flashMode));
            }
        }
        return false;
    }

    static void setTorch(Camera camera, boolean on) {
        Camera.Parameters parameters = camera.getParameters();
        List<String> supportedFlashModes = parameters.getSupportedFlashModes();
        String flashMode = on ?
            findSettableValue(supportedFlashModes,
                Camera.Parameters.FLASH_MODE_TORCH,
                Camera.Parameters.FLASH_MODE_ON) :
            findSettableValue(supportedFlashModes,
                Camera.Parameters.FLASH_MODE_OFF);
        if (flashMode != null && !flashMode.equals(parameters.getFlashMode())) {
            parameters.setFlashMode(flashMode);
            camera.setParameters(parameters);
        }
    }

    private static String findSettableValue(Collection<String> supportedValues,
                                            String... desiredValues) {
        if (supportedValues != null) {
            for (String desiredValue : desiredValues) {
                if (supportedValues.contains(desiredValue)) {
                    return desiredValue;
                }
            }
        }
        return null;
    }
}
