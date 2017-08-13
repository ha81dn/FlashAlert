package ha81dn.flashalert;


import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity {
    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindNotificationPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index > 0
                                ? listPreference.getEntries()[index]
                                : preference.getContext().getResources().getString(R.string.setting_delete));

            } else if (preference instanceof SwitchPreference) {
                return true;
            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    private static Preference.OnPreferenceChangeListener sBindLogPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            if (preference instanceof MultiSelectListPreference) {
                Intent intentUpdate = new Intent();
                intentUpdate.setAction("ha81dn.flashalert.ASYNC_MAIN");
                intentUpdate.addCategory(Intent.CATEGORY_DEFAULT);
                preference.getContext().sendBroadcast(intentUpdate);
            }
            return true;
        }
    };

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    static void makeNewSettingsSet(final Context context, Preference cat, final Preference listPreference,
                                   Preference editTextBoxPreference1, Preference editTextBoxPreference2,
                                   Preference editTextBoxPreference3, Preference switchPreference) {
        String prefKey = String.valueOf(Calendar.getInstance().getTimeInMillis());
        cat.setKey(prefKey);
        cat.setTitle("ID " + prefKey);

        listPreference.setKey(prefKey + "_1package");
        listPreference.setTitle(context.getString(R.string.notification_package));
        listPreference.setSummary("");
        listPreference.setOnPreferenceChangeListener(sBindNotificationPreferenceSummaryToValueListener);
        setListPreferenceData(context, (ListPreference) listPreference, context.getResources().getString(R.string.package_null));
        listPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                setListPreferenceData(context, (ListPreference) listPreference, context.getResources().getString(R.string.package_null));
                return false;
            }
        });

        editTextBoxPreference1.setKey(prefKey + "_2beat");
        editTextBoxPreference1.setTitle(context.getString(R.string.notification_flash_beat));
        editTextBoxPreference1.setSummary("");
        editTextBoxPreference1.setOnPreferenceChangeListener(sBindNotificationPreferenceSummaryToValueListener);

        editTextBoxPreference2.setKey(prefKey + "_3include");
        editTextBoxPreference2.setTitle(context.getString(R.string.notification_words_include));
        editTextBoxPreference2.setSummary("");
        editTextBoxPreference2.setOnPreferenceChangeListener(sBindNotificationPreferenceSummaryToValueListener);

        editTextBoxPreference3.setKey(prefKey + "_4exclude");
        editTextBoxPreference3.setTitle(context.getString(R.string.notification_words_exclude));
        editTextBoxPreference3.setSummary("");
        editTextBoxPreference3.setOnPreferenceChangeListener(sBindNotificationPreferenceSummaryToValueListener);

        switchPreference.setKey(prefKey + "_5display");
        switchPreference.setTitle(context.getString(R.string.notification_display));
        switchPreference.setOnPreferenceChangeListener(sBindNotificationPreferenceSummaryToValueListener);
    }

    protected static void setListPreferenceData(Context context, ListPreference lp, String firstEntry) {
        final PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        List<String> list = new ArrayList<>();
        Map<String, String> map = new HashMap<>();

        for (ApplicationInfo packageInfo : packages) {
            try {
                map.put(pm.getApplicationLabel(pm.getApplicationInfo(packageInfo.packageName, 0)).toString(), packageInfo.packageName);
            } catch (Exception ignore) {
            }
        }
        SortedSet<String> sortedKeys = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        sortedKeys.addAll(map.keySet());
        for (String key : sortedKeys) {
            list.add(key);
        }
        list.add(0, firstEntry);
        lp.setEntries(list.toArray(new CharSequence[list.size()]));
        list.clear();
        for (String key : sortedKeys) {
            list.add(map.get(key));
        }
        list.add(0, "");
        lp.setEntryValues(list.toArray(new CharSequence[list.size()]));
    }

    protected static void setListPreferenceData(Context context, MultiSelectListPreference lp) {
        final PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        List<String> list = new ArrayList<>();
        Map<String, String> map = new HashMap<>();

        for (ApplicationInfo packageInfo : packages) {
            try {
                map.put(pm.getApplicationLabel(pm.getApplicationInfo(packageInfo.packageName, 0)).toString(), packageInfo.packageName);
            } catch (Exception ignore) {
            }
        }
        SortedSet<String> sortedKeys = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        sortedKeys.addAll(map.keySet());
        for (String key : sortedKeys) {
            list.add(key);
        }
        lp.setEntries(list.toArray(new CharSequence[list.size()]));
        list.clear();
        for (String key : sortedKeys) {
            list.add(map.get(key));
        }
        lp.setEntryValues(list.toArray(new CharSequence[list.size()]));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || LogPreferenceFragment.class.getName().equals(fragmentName)
                || NotificationPreferenceFragment.class.getName().equals(fragmentName);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public static class LogPreferenceFragment extends PreferenceFragment {
        private mainBroadcastReceiver mainBR;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_notification);
            setHasOptionsMenu(true);
            loadPreferences();
        }

        private void loadPreferences() {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

            AsyncTask<Set<String>, Preference, Void> settingsGetter;
            settingsGetter = new getLog();
            settingsGetter.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, prefs.getStringSet("log_filter_app", new HashSet<String>()));
        }

        @Override
        public void onPause() {
            super.onPause();
            getActivity().unregisterReceiver(mainBR);
        }

        @Override
        public void onResume() {
            super.onResume();
            mainBR = new mainBroadcastReceiver();
            IntentFilter intentFilter = new IntentFilter("ha81dn.flashalert.ASYNC_MAIN");
            intentFilter.addCategory(Intent.CATEGORY_DEFAULT);
            getActivity().registerReceiver(mainBR, intentFilter);
        }

        private class getLog extends AsyncTask<Set<String>, Preference, Void> {
            @Override
            protected Void doInBackground(Set<String>... params) {
                final Context context = getActivity();
                Set<String> app = params[0];
                ArrayList<String> oldApps = new ArrayList<>();
                ArrayList<String> sqlApps = new ArrayList<>();
                boolean flag = false;

                PreferenceCategory cat = new PreferenceCategory(context);
                cat.setTitle(getString(R.string.log_filter));

                final MultiSelectListPreference listPreference = new MultiSelectListPreference(context);
                listPreference.setKey("log_filter_app");
                listPreference.setTitle(getString(R.string.notification_package));
                listPreference.setOnPreferenceChangeListener(sBindLogPreferenceSummaryToValueListener);
                setListPreferenceData(context, listPreference);
                StringBuilder summary = new StringBuilder();
                SortedSet<String> sList = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
                for (String item : app) {
                    String label = item;
                    int pos = listPreference.findIndexOfValue(item);
                    if (pos >= 0) {
                        label = listPreference.getEntries()[pos].toString();
                        sList.add(label);
                        sqlApps.add(label);
                    } else
                        oldApps.add(item);
                }
                if (oldApps.size() >= 1) {
                    for (String item : oldApps) {
                        app.remove(item);
                    }
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putStringSet("log_filter_app", app);
                    editor.apply();
                }
                for (String item : sList) {
                    summary.append(", ");
                    summary.append(item);
                }
                if (summary.length() >= 1)
                    listPreference.setSummary(summary.toString().substring(2));
                else
                    listPreference.setSummary(getString(R.string.log_filter_app_all));

                publishProgress(cat, listPreference);

                DatabaseHandler log = new DatabaseHandler(context);
                ArrayList<Preference> stack = log.getLogEntries(context, sqlApps);
                log.close();

                cat = new PreferenceCategory(context);
                EditTextPreference editTextBoxPreference;

                for (Preference pref : stack) {
                    if (flag) {
                        editTextBoxPreference = (EditTextPreference) pref;
                        publishProgress(cat, editTextBoxPreference);
                    } else {
                        cat = (PreferenceCategory) pref;
                    }
                    flag = !flag;
                }
                return null;
            }

            @Override
            protected void onProgressUpdate(Preference... values) {
                PreferenceCategory cat = new PreferenceCategory(getActivity());
                PreferenceCategory catParam = (PreferenceCategory) values[0];
                cat.setTitle(catParam.getTitle());
                getPreferenceScreen().addPreference(cat);
                cat.addPreference(values[1]);
                if (values[1] instanceof EditTextPreference) values[1].setSelectable(false);
            }
        }

        public class mainBroadcastReceiver extends BroadcastReceiver {
            @Override
            public void onReceive(Context context, Intent intent) {
                getPreferenceScreen().removeAll();
                loadPreferences();
            }
        }
    }

    /**
     * This fragment shows notification preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public static class NotificationPreferenceFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_notification);
            setHasOptionsMenu(true);

            AsyncTask<Void, Preference, Void> settingsGetter;
            settingsGetter = new getSettings();
            settingsGetter.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        private class getSettings extends AsyncTask<Void, Preference, Void> {
            @Override
            protected Void doInBackground(Void... params) {
                final Context context = getActivity();
                EditTextPreference editTextBoxPreference1, editTextBoxPreference2, editTextBoxPreference3;
                SwitchPreference switchPreference;
                String packageName = "";
                String flashBeat = "";
                String includeWords = "";
                String excludeWords = "";
                String prefKey = "";
                String value;
                boolean notFirstItem = false;

                Intent myIntent = new Intent(context, NLService.class);
                context.startService(myIntent);

                // Bind the summaries of EditText/List/Dialog/Ringtone preferences
                // to their values. When their values change, their summaries are
                // updated to reflect the new value, per the Android Design
                // guidelines.
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                Map<String, ?> keys = prefs.getAll();

                SortedSet<String> sortedKeys = new TreeSet<>(keys.keySet());
                sortedKeys.add("z_1package");
                for (String key : sortedKeys) {
                    try {
                        value = keys.get(key).toString();
                    } catch (Exception ignore) {
                        value = "";
                    }
                    if (key.endsWith("_1package")) {
                        if (notFirstItem) {
                            if (!packageName.equals("")) {
                                PreferenceCategory cat = new PreferenceCategory(context);
                                cat.setKey(prefKey);
                                cat.setTitle("ID " + prefKey);

                                final ListPreference listPreference = new ListPreference(context);
                                listPreference.setKey(prefKey + "_1package");
                                listPreference.setTitle(getString(R.string.notification_package));
                                listPreference.setOnPreferenceChangeListener(sBindNotificationPreferenceSummaryToValueListener);
                                setListPreferenceData(context, listPreference, context.getResources().getString(R.string.package_null));
                                int index = listPreference.findIndexOfValue(packageName);
                                listPreference.setSummary(
                                        listPreference.findIndexOfValue(packageName) >= 0
                                                ? listPreference.getEntries()[index]
                                                : getString(R.string.setting_delete));
                                listPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                                    @Override
                                    public boolean onPreferenceClick(Preference preference) {
                                        setListPreferenceData(context, listPreference, context.getResources().getString(R.string.package_null));
                                        return false;
                                    }
                                });

                                editTextBoxPreference1 = new EditTextPreference(context);
                                editTextBoxPreference1.setKey(prefKey + "_2beat");
                                editTextBoxPreference1.setTitle(getString(R.string.notification_flash_beat));
                                editTextBoxPreference1.setSummary(flashBeat);
                                editTextBoxPreference1.setOnPreferenceChangeListener(sBindNotificationPreferenceSummaryToValueListener);

                                editTextBoxPreference2 = new EditTextPreference(context);
                                editTextBoxPreference2.setKey(prefKey + "_3include");
                                editTextBoxPreference2.setTitle(getString(R.string.notification_words_include));
                                editTextBoxPreference2.setSummary(includeWords);
                                editTextBoxPreference2.setOnPreferenceChangeListener(sBindNotificationPreferenceSummaryToValueListener);

                                editTextBoxPreference3 = new EditTextPreference(context);
                                editTextBoxPreference3.setKey(prefKey + "_4exclude");
                                editTextBoxPreference3.setTitle(getString(R.string.notification_words_exclude));
                                editTextBoxPreference3.setSummary(excludeWords);
                                editTextBoxPreference3.setOnPreferenceChangeListener(sBindNotificationPreferenceSummaryToValueListener);

                                switchPreference = new SwitchPreference(context);
                                switchPreference.setKey(prefKey + "_5display");
                                switchPreference.setTitle(getString(R.string.notification_display));
                                switchPreference.setOnPreferenceChangeListener(sBindNotificationPreferenceSummaryToValueListener);

                                publishProgress(cat, listPreference, editTextBoxPreference1, editTextBoxPreference2, editTextBoxPreference3, switchPreference);
                            }
                        }
                        notFirstItem = true;
                        prefKey = key.split("_")[0];
                        packageName = value;
                        flashBeat = "";
                        includeWords = "";
                        excludeWords = "";
                        if (packageName.equals("")) prefs.edit().remove(key).apply();
                    } else if (key.endsWith("_2beat")) {
                        flashBeat = value;
                        if (packageName.equals("")) prefs.edit().remove(key).apply();
                    } else if (key.endsWith("_3include")) {
                        includeWords = value;
                        if (packageName.equals("")) prefs.edit().remove(key).apply();
                    } else if (key.endsWith("_4exclude")) {
                        excludeWords = value;
                        if (packageName.equals("")) prefs.edit().remove(key).apply();
                    } else if (key.endsWith("_5display")) {
                        if (packageName.equals("")) prefs.edit().remove(key).apply();
                    }
                }

                PreferenceCategory cat = new PreferenceCategory(context);
                ListPreference listPreference = new ListPreference(context);
                editTextBoxPreference1 = new EditTextPreference(context);
                editTextBoxPreference2 = new EditTextPreference(context);
                editTextBoxPreference3 = new EditTextPreference(context);
                switchPreference = new SwitchPreference(context);

                makeNewSettingsSet(context, cat, listPreference, editTextBoxPreference1, editTextBoxPreference2, editTextBoxPreference3, switchPreference);
                publishProgress(cat, listPreference, editTextBoxPreference1, editTextBoxPreference2, editTextBoxPreference3, switchPreference);
                return null;
            }

            @Override
            protected void onProgressUpdate(Preference... values) {
                PreferenceCategory cat = new PreferenceCategory(getActivity());
                PreferenceCategory catParam = (PreferenceCategory) values[0];
                cat.setKey(catParam.getKey());
                cat.setTitle(catParam.getTitle());
                getPreferenceScreen().addPreference(cat);
                cat.addPreference(values[1]);
                cat.addPreference(values[2]);
                cat.addPreference(values[3]);
                cat.addPreference(values[4]);
                cat.addPreference(values[5]);
            }
        }
    }
}
