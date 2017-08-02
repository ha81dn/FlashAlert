package ha81dn.flashalert;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

class DatabaseHandler extends SQLiteOpenHelper {

    DatabaseHandler(Context context) {
        super(context, "logdb", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table if not exists logfile (datim text primary key, app text, msg text, lit text)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //db.execSQL("drop table if exists logfile");
        //if (oldVersion<=0 && newVersion==1) db.execSQL("alter table logfile add column sev text default 0");
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //db.execSQL("drop table if exists logfile");
        onCreate(db);
    }

    void addLogEntry(String app, String msg, String lit) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor;
        Calendar c = Calendar.getInstance();

        String datim, tmp;

        // Zeitstempel formatieren
        datim = new SimpleDateFormat("yyyyMMddHHmmss", Locale.GERMAN).format(c.getTime());

        // sicherstellen, dass der PK nicht verletzt wird; notfalls ans datim 'ne Null hinten dran
        do {
            cursor = db.rawQuery("select count(*) from logfile where datim = ?", new String[]{datim});
            if (cursor.moveToFirst()) {
                tmp = cursor.getString(0);
                if (tmp.equals("0")) break;
                datim += "0";
            } else {
                break;
            }
            cursor.close();
        } while (true);

        // nur die letzten 20 Einträge aufbewahren
        cursor = db.rawQuery("select count(*) from logfile", null);
        if (cursor.moveToFirst() && Integer.parseInt(cursor.getString(0)) >= 21) {
            cursor.close();
            cursor = db.rawQuery("select min(datim) from logfile order by datim desc limit 21", null);
            if (cursor.moveToFirst()) {
                db.execSQL("delete from logfile where datim <= ?", new String[]{cursor.getString(0)});
            }
            cursor.close();
        }

        // neuen Eintrag schreiben
        ContentValues values = new ContentValues();
        values.put("datim", datim);
        values.put("app", app);
        values.put("msg", msg);
        values.put("lit", lit);
        db.insert("logfile", null, values);

        db.close();
    }

    ArrayList<Preference> getLogEntries(Context context) {
        SQLiteDatabase db = this.getReadableDatabase();
        ArrayList<Preference> stack = new ArrayList<>();
        Cursor cursor;
        String datim;

        cursor = db.rawQuery("select datim,app,msg,lit from logfile order by datim desc", null);
        if (cursor.moveToFirst()) {
            // ermittelte Einträge zu einem Preference-Stapel zusammenfummeln
            do {
                datim = cursor.getString(0);
                datim = datim.substring(6, 8) + "." + datim.substring(4, 6) + "." + datim.substring(0, 4)
                        + " " + datim.substring(8, 10) + ":" + datim.substring(10, 12) + ":" + datim.substring(12, 14);

                PreferenceCategory cat = new PreferenceCategory(context);
                cat.setTitle(datim + "   " + cursor.getString(3));

                EditTextPreference editTextBoxPreference = new EditTextPreference(context);
                editTextBoxPreference.setTitle(cursor.getString(1));
                editTextBoxPreference.setSummary(cursor.getString(2));

                stack.add(cat);
                stack.add(editTextBoxPreference);
            } while (cursor.moveToNext());
        }
        cursor.close();

        return stack;
    }
}