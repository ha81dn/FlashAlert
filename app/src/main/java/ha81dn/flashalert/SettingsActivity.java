package ha81dn.flashalert;


import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
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

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
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
                || NotificationPreferenceFragment.class.getName().equals(fragmentName);
    }

    /**
     * This fragment shows notification preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public static class NotificationPreferenceFragment extends PreferenceFragment {
        protected static void setListPreferenceData(Context context, ListPreference lp) {
            final PackageManager pm = context.getPackageManager();
            List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
            List<String> list = new ArrayList<>();
            Map<String, String> map = new HashMap<>();

            for (ApplicationInfo packageInfo : packages) {
                try {
                    map.put(pm.getApplicationLabel(pm.getApplicationInfo(packageInfo.packageName, 0)).toString(), packageInfo.packageName);
                } catch (Exception ignore) {}
            }
            SortedSet<String> sortedKeys = new TreeSet<>(map.keySet());
            for (String key : sortedKeys) {
                list.add(key);
            }
            list.add(0, context.getResources().getString(R.string.package_null));
            lp.setEntries(list.toArray(new CharSequence[list.size()]));
            list.clear();
            for (String key : sortedKeys) {
                list.add(map.get(key));
            }
            list.add(0, "");
            lp.setEntryValues(list.toArray(new CharSequence[list.size()]));
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_notification);
            setHasOptionsMenu(true);

            final Context context = getActivity();
            EditTextPreference editTextBoxPreference;
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
            Map<String,?> keys = prefs.getAll();

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
                            getPreferenceScreen().addPreference(cat);

                            final ListPreference listPreference = new ListPreference(context);
                            listPreference.setKey(prefKey + "_1package");
                            listPreference.setTitle(getString(R.string.notification_package));
                            listPreference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
                            setListPreferenceData(context, listPreference);
                            int index = listPreference.findIndexOfValue(packageName);
                            listPreference.setSummary(
                                    listPreference.findIndexOfValue(packageName) >= 0
                                            ? listPreference.getEntries()[index]
                                            : getString(R.string.setting_delete));
                            listPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                                @Override
                                public boolean onPreferenceClick(Preference preference) {
                                    setListPreferenceData(context, listPreference);
                                    return false;
                                }
                            });
                            cat.addPreference(listPreference);

                            editTextBoxPreference = new EditTextPreference(context);
                            editTextBoxPreference.setKey(prefKey + "_2beat");
                            editTextBoxPreference.setTitle(getString(R.string.notification_flash_beat));
                            editTextBoxPreference.setSummary(flashBeat);
                            editTextBoxPreference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
                            cat.addPreference(editTextBoxPreference);

                            editTextBoxPreference = new EditTextPreference(context);
                            editTextBoxPreference.setKey(prefKey + "_3include");
                            editTextBoxPreference.setTitle(getString(R.string.notification_words_include));
                            editTextBoxPreference.setSummary(includeWords);
                            editTextBoxPreference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
                            cat.addPreference(editTextBoxPreference);

                            editTextBoxPreference = new EditTextPreference(context);
                            editTextBoxPreference.setKey(prefKey + "_4exclude");
                            editTextBoxPreference.setTitle(getString(R.string.notification_words_exclude));
                            editTextBoxPreference.setSummary(excludeWords);
                            editTextBoxPreference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
                            cat.addPreference(editTextBoxPreference);

                            switchPreference = new SwitchPreference(context);
                            switchPreference.setKey(prefKey + "_5display");
                            switchPreference.setTitle(getString(R.string.notification_display));
                            switchPreference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
                            cat.addPreference(switchPreference);
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

            prefKey = String.valueOf(Calendar.getInstance().getTimeInMillis());
            PreferenceCategory cat = new PreferenceCategory(context);
            cat.setKey(prefKey);
            cat.setTitle("ID " + prefKey);
            getPreferenceScreen().addPreference(cat);

            final ListPreference listPreference = new ListPreference(context);
            listPreference.setKey(prefKey + "_1package");
            listPreference.setTitle(getString(R.string.notification_package));
            listPreference.setSummary("");
            listPreference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
            setListPreferenceData(context, listPreference);
            listPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    setListPreferenceData(context, listPreference);
                    return false;
                }
            });
            cat.addPreference(listPreference);

            editTextBoxPreference = new EditTextPreference(context);
            editTextBoxPreference.setKey(prefKey + "_2beat");
            editTextBoxPreference.setTitle(getString(R.string.notification_flash_beat));
            editTextBoxPreference.setSummary("");
            editTextBoxPreference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
            cat.addPreference(editTextBoxPreference);

            editTextBoxPreference = new EditTextPreference(context);
            editTextBoxPreference.setKey(prefKey + "_3include");
            editTextBoxPreference.setTitle(getString(R.string.notification_words_include));
            editTextBoxPreference.setSummary("");
            editTextBoxPreference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
            cat.addPreference(editTextBoxPreference);

            editTextBoxPreference = new EditTextPreference(context);
            editTextBoxPreference.setKey(prefKey + "_4exclude");
            editTextBoxPreference.setTitle(getString(R.string.notification_words_exclude));
            editTextBoxPreference.setSummary("");
            editTextBoxPreference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
            cat.addPreference(editTextBoxPreference);

            switchPreference = new SwitchPreference(context);
            switchPreference.setKey(prefKey + "_5display");
            switchPreference.setTitle(getString(R.string.notification_display));
            switchPreference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
            cat.addPreference(switchPreference);

            //bindPreferenceSummaryToValue(findPreference("notifications_new_message_ringtone"));
        }
    }
}
