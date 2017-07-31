package ha81dn.flashalert;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.camera2.CameraManager;
import android.hardware.display.DisplayManager;
import android.os.PowerManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.view.Display;

import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;


public class NLService extends NotificationListenerService {

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        boolean hasFlashed = false;
        String[] list = {};
        CameraManager cm = null;

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "sbnWakeLock");
        wakeLock.acquire();
        try {
            boolean isDisplayOn = true;
            DisplayManager dm = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
            for (Display display : dm.getDisplays()) {
                int state = display.getState();
                if (isDisplayOn)
                    isDisplayOn = state != Display.STATE_OFF && state != Display.STATE_DOZE && state != Display.STATE_DOZE_SUSPEND;
                if (!isDisplayOn) break;
            }

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            String sbnPackageName = sbn.getPackageName();
            String sbnText = sbn.getNotification().tickerText == null ? "" : sbn.getNotification().tickerText.toString();
            String packageName = "";
            String flashBeat = "";
            String includeWords = "";
            String excludeWords = "";
            String displayOn = "";
            String value;
            boolean notFirstItem = false;
            boolean skip;
            boolean flashNow;
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
                                if (!skip && (!isDisplayOn || displayOn.equals("true"))) {
                                    flashNow = true;
                                    cm = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
                                    list = cm.getCameraIdList();
                                    for (String item : flashBeat.split(",")) {
                                        if (flashNow) {
                                            for (String id : list) {
                                                try {
                                                    hasFlashed = true;
                                                    cm.setTorchMode(id, true);
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
                                                    cm.setTorchMode(id, false);
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
                        displayOn = "";
                    } else if (key.endsWith("_2beat")) {
                        flashBeat = value;
                    } else if (key.endsWith("_3include")) {
                        includeWords = value;
                    } else if (key.endsWith("_4exclude")) {
                        excludeWords = value;
                    } else if (key.endsWith("_5display")) {
                        displayOn = value;
                    }
                } catch (Exception ignore) {}
            }
        } catch (Exception ignore) {}
        try {
            if (hasFlashed) {
                for (String id : list) {
                    try {
                        cm.setTorchMode(id, false);
                    } catch (Exception ignore) {
                    }
                }
            }
        } catch (Exception ignore) {
        }
        wakeLock.release();
    }
}
