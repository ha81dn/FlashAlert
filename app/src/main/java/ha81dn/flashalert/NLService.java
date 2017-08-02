package ha81dn.flashalert;

import android.app.Notification;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraManager;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.view.Display;

import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;


public class NLService extends NotificationListenerService {

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        boolean hasFlashed = false;
        int flashCount = 0;
        String sbnPackageName = "";
        String sbnText = "";
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
            sbnPackageName = sbn.getPackageName();

            Bundle extras = sbn.getNotification().extras;
            CharSequence chars = extras.getCharSequence(Notification.EXTRA_TEXT);
            if (!TextUtils.isEmpty(chars)) sbnText += chars.toString() + '\n';
            chars = extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
            if (!TextUtils.isEmpty(chars)) sbnText += chars.toString() + '\n';
            chars = extras.getCharSequence(Notification.EXTRA_INFO_TEXT);
            if (!TextUtils.isEmpty(chars)) sbnText += chars.toString() + '\n';
            chars = extras.getCharSequence(Notification.EXTRA_SUB_TEXT);
            if (!TextUtils.isEmpty(chars)) sbnText += chars.toString() + '\n';
            chars = extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT);
            if (!TextUtils.isEmpty(chars)) sbnText += chars.toString() + '\n';
            CharSequence[] lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
            if (lines != null && lines.length >= 1) {
                StringBuilder sb = new StringBuilder();
                for (CharSequence msg : lines)
                    if (!TextUtils.isEmpty(msg)) {
                        sb.append(msg.toString().trim());
                        sb.append('\n');
                    }
                if (sb.length() >= 1)
                    sbnText += sb.toString();
            }
            if (sbnText.length() >= 1 && sbnText.substring(sbnText.length() - 1, sbnText.length()).equals("\n"))
                sbnText = sbnText.substring(0, sbnText.length() - 1);

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
                                            flashCount++;
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
                                    break;
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
        try {
            PackageManager pm = this.getPackageManager();
            DatabaseHandler log = new DatabaseHandler(this);
            log.addLogEntry(pm.getApplicationLabel(pm.getApplicationInfo(sbnPackageName, 0)).toString(), sbnText, flashCount >= 1 ? new String(new char[flashCount]).replace("\0", "⚡") : "");
            log.close();
        } catch (Exception ignore) {
        }
        wakeLock.release();
    }
}
