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
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;


public class NLService extends NotificationListenerService {

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        boolean hasFlashed = false;
        int flashCount = 0;
        String sbnPackageName;
        String sbnPackageLabel;
        StringBuilder sbnTextBuilder = new StringBuilder();
        String sbnText;
        String[] list = {};
        CameraManager cm = null;

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "sbnWakeLock");
        wakeLock.acquire();
        try {
            DatabaseHandler log = new DatabaseHandler(this);
            sbnPackageName = sbn.getPackageName();
            PackageManager pm = this.getPackageManager();
            try {
                sbnPackageLabel = pm.getApplicationLabel(pm.getApplicationInfo(sbnPackageName, 0)).toString();
            } catch (Exception ignore) {
                sbnPackageLabel = sbnPackageName;
            }
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

                ArrayList<String> charList = new ArrayList<>();
                Bundle extras = sbn.getNotification().extras;
                CharSequence chars = extras.getCharSequence(Notification.EXTRA_TITLE_BIG);
                addCharItem(charList, chars, sbnTextBuilder);
                chars = extras.getCharSequence(Notification.EXTRA_TITLE);
                addCharItem(charList, chars, sbnTextBuilder);
                chars = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE);
                addCharItem(charList, chars, sbnTextBuilder);
                chars = extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
                addCharItem(charList, chars, sbnTextBuilder);
                chars = extras.getCharSequence(Notification.EXTRA_TEXT);
                addCharItem(charList, chars, sbnTextBuilder);
                chars = extras.getCharSequence(Notification.EXTRA_INFO_TEXT);
                addCharItem(charList, chars, sbnTextBuilder);
                chars = extras.getCharSequence(Notification.EXTRA_SUB_TEXT);
                addCharItem(charList, chars, sbnTextBuilder);
                chars = extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT);
                addCharItem(charList, chars, sbnTextBuilder);
                CharSequence[] lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
                if (lines != null && lines.length >= 1) {
                    for (CharSequence msg : lines)
                        if (!TextUtils.isEmpty(msg)) {
                            addCharItem(charList, msg, sbnTextBuilder);
                        }
                }
                sbnText = sbnTextBuilder.toString();
                if (sbnText.length() >= 1 && sbnText.substring(sbnText.length() - 1, sbnText.length()).equals("\n"))
                    sbnText = sbnText.substring(0, sbnText.length() - 1);

                if (!log.equalsRecentNotification(sbnPackageLabel, sbnText, 5)) {
                    String packageName = "";
                    String flashBeat = "";
                    String includeWords = "";
                    String excludeWords = "";
                    String displayOn = "";
                    String value;
                    boolean notFirstItem = false;
                    boolean skip;
                    boolean flashNow;
                    boolean notDefined = true;
                    long sleepMillis;
                    Map<String, ?> keys = prefs.getAll();
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
                                        notDefined = false;
                                        if (!excludeWords.equals("")) {
                                            for (String ex : excludeWords.split(",")) {
                                                if (sbnText.contains(ex)) {
                                                    skip = true;
                                                    break;
                                                }
                                            }
                                            if (skip)
                                                log.addLogEntry(sbnPackageLabel, sbnText, getString(R.string.reason_exclude));
                                        }
                                        if (!skip && !includeWords.equals("")) {
                                            skip = true;
                                            for (String in : includeWords.split(",")) {
                                                if (sbnText.contains(in)) {
                                                    skip = false;
                                                    break;
                                                }
                                            }
                                            if (skip)
                                                log.addLogEntry(sbnPackageLabel, sbnText, getString(R.string.reason_not_include));
                                        }
                                        if (!skip && (!isDisplayOn || displayOn.equals("true"))) {
                                            if (log.hasFlashedRecently(5)) {
                                                log.addLogEntry(sbnPackageLabel, sbnText, getString(R.string.reason_recently));
                                            } else {
                                                flashCount = (int) Math.ceil((flashBeat.length() - flashBeat.replace(",", "").length()) / 2 + 0.5);
                                                log.addLogEntry(sbnPackageLabel, sbnText, flashCount >= 1 ? new String(new char[flashCount]).replace("\0", "⚡") : "");
                                                flashNow = true;
                                                cm = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
                                                list = cm.getCameraIdList();
                                                for (String item : flashBeat.split(",")) {
                                                    if (flashNow) {
                                                        for (String id : list) {
                                                            try {
                                                                hasFlashed = true;
                                                                cm.setTorchMode(id, true);
                                                            } catch (Exception ignore) {
                                                            }
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
                                                            } catch (Exception ignore) {
                                                            }
                                                        }
                                                    }
                                                    flashNow = !flashNow;
                                                }
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
                        } catch (Exception ignore) {
                        }
                    }
                    if (notDefined)
                        log.addLogEntry(sbnPackageLabel, sbnText, getString(R.string.reason_no_flash));
                }
            } catch (Exception ex) {
                Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
            }
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
                log.close();
            } catch (Exception ignore) {
            }
        } catch (Exception ignore) {
        }
        wakeLock.release();
    }

    private void addCharItem(ArrayList<String> list, CharSequence chars, StringBuilder plainText) {
        if (!TextUtils.isEmpty(chars)) {
            String tmp;
            tmp = chars.toString();
            if (!tmp.equals("")) {
                for (String line : tmp.split("[\\n]+")) {
                    tmp = line.trim();
                    if (!tmp.equals("") && !list.contains(line)) {
                        plainText.append(tmp);
                        plainText.append('\n');
                        list.add(line);
                    }
                }
            }
        }
    }
}
