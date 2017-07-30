package ha81dn.flashalert;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.camera2.CameraManager;
import android.hardware.Camera;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.view.Display;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;


public class NLService extends NotificationListenerService {

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        try {
            DisplayManager dm = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
            for (Display display : dm.getDisplays()) {
                int state = display.getState();
                if (state != Display.STATE_OFF && state != Display.STATE_DOZE && state != Display.STATE_DOZE_SUSPEND) return;
            }

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            String sbnPackageName = sbn.getPackageName();
            String sbnText = sbn.getNotification().tickerText == null ? "" : sbn.getNotification().tickerText.toString();
            String packageName = "";
            String flashBeat = "";
            String includeWords = "";
            String excludeWords = "";
            String value;
            boolean notFirstItem = false;
            boolean skip;
            boolean flashNow;
            boolean isTorchOn;
            long sleepMillis;
            Map<String,?> keys = prefs.getAll();
            SortedSet<String> sortedKeys = new TreeSet<>(keys.keySet());
            sortedKeys.add("z_1package");
            for (String key : sortedKeys) {
                skip = false;
                try {
                    value = keys.get(key).toString();
                } catch (Exception ignore) {
                    value = "";
                }
                try {
                    if (key.endsWith("_1package")) {
                        if (notFirstItem) {
                            if (!packageName.equals("") && !flashBeat.equals("") && sbnPackageName.equals(packageName)) {
                                if (!excludeWords.equals("")) {
                                    for (String ex : excludeWords.split(",")) {
                                        if (sbnText.contains(ex)) {
                                            skip = true;
                                            break;
                                        }
                                    }
                                }
                                if (!skip && !includeWords.equals("")) {
                                    skip = true;
                                    for (String in : includeWords.split(",")) {
                                        if (sbnText.contains(in)) {
                                            skip = false;
                                            break;
                                        }
                                    }
                                }
                                if (!skip) {
                                    flashNow = true;
                                    try {
                                        isTorchOn = Camera.open().getParameters().getFlashMode().equals("torch");
                                    } catch (Exception ignore) {
                                        isTorchOn = false;
                                    }
                                    CameraManager cm = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
                                    String[] list = cm.getCameraIdList();
                                    for (String item : flashBeat.split(",")) {
                                        if (flashNow) {
                                            for (String id : list) {
                                                try {
                                                    cm.setTorchMode(id, !isTorchOn);
                                                } catch (Exception ignore) {}
                                            }
                                        }
                                        try {
                                            sleepMillis = Long.parseLong(item.trim());
                                        } catch (Exception ignore) {
                                            sleepMillis = 100;
                                        }
                                        SystemClock.sleep(sleepMillis);
                                        if (flashNow) {
                                            for (String id : list) {
                                                try {
                                                    cm.setTorchMode(id, isTorchOn);
                                                } catch (Exception ignore) {}
                                            }
                                        }
                                        flashNow = !flashNow;
                                    }
                                    return;
                                }
                            }
                        }
                        notFirstItem = true;
                        packageName = value;
                        flashBeat = "";
                        includeWords = "";
                        excludeWords = "";
                    } else if (key.endsWith("_2beat")) {
                        flashBeat = value;
                    } else if (key.endsWith("_3include")) {
                        includeWords = value;
                    } else if (key.endsWith("_4exclude")) {
                        excludeWords = value;
                    }
                } catch (Exception ignore) {}
            }
        } catch (Exception ignore) {}
    }
}
