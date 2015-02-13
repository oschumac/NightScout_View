package de.schumis_net.olli.nightscout_view;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import java.sql.ResultSet;
import java.util.Date;

import static java.lang.System.currentTimeMillis;

/**
 * Created by OLLI on 16.01.2015.
 */

public class SQL_lite extends SQLiteOpenHelper {

    private static final String TAG =
    SQL_lite.class.getSimpleName();

    // Name und Version der Datenbank
    private static final String DATABASE_NAME = "Nightscout_View.db";
    private static final int DATABASE_VERSION = 16;

    public static final String _ID = "_id";

    public static final String TABLE_NAME_ENTRIES = "entries";
    public static final String ENTRIES_DEVICE = "device";
    public static final String ENTRIES_DATE = "date";
    public static final String ENTRIES_DATESTRING = "dateString";
    public static final String ENTRIES_SGV = "sgv";
    public static final String ENTRIES_DIRECTION = "direction";
    public static final String ENTRIES_FILTERED = "filtered";
    public static final String ENTRIES_UNFILTERED = "unfiltered";
    public static final String ENTRIES_RSSI = "rssi";
    public static final String ENTRIES_TYPE = "type";
    public static boolean CGMAlarm=false;

    private static final String TABLE_ENTRIES_CREATE
            = "CREATE TABLE "
            + TABLE_NAME_ENTRIES + " (" + _ID
            + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + ENTRIES_DEVICE + " STRING, "
            + ENTRIES_DATE + " LONG, "
            + ENTRIES_DATESTRING + " STRING,  "
            + ENTRIES_SGV + " LONG, "
            + ENTRIES_DIRECTION + " STRING, "
            + ENTRIES_TYPE + " STRING, "
            + ENTRIES_FILTERED + " LONG, "
            + ENTRIES_UNFILTERED + " LONG, "
            + ENTRIES_RSSI + " LONG);";


    public static final String TABLE_NAME_TREATMENTS = "treatments";
    public static final String TREATMENTS_ENTEREDBY = "enteredBy";
    public static final String TREATMENTS_EVENTTYPE = "eventType";
    public static final String TREATMENTS_GLYKOSE="glucose";
    public static final String TREATMENTS_GLYKOSETYPE="glucoseType";
    public static final String TREATMENTS_INSULIN="insulin";
    public static final String TREATMENTS_NOTES="notes";
    public static final String TREATMENTS_CREATED_AT="created_at";
    public static final String TREATMENTS_CARBS="carbs";

    private static final String TABLE_TREATMENTS_CREATE
            = "CREATE TABLE "
            + TABLE_NAME_TREATMENTS + " (" + _ID
            + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + TREATMENTS_ENTEREDBY + " STRING, "
            + TREATMENTS_EVENTTYPE + " STRING, "
            + TREATMENTS_GLYKOSE + " STRING, "
            + TREATMENTS_GLYKOSETYPE + " STRING, "
            + TREATMENTS_INSULIN + " STRING, "
            + TREATMENTS_NOTES + " STRING, "
            + TREATMENTS_CREATED_AT + " STRING, "
            + TREATMENTS_CARBS + " STRING); ";

    public static final String TABLE_NAME_DEVICESTATUS = "devicestatus";
    public static final String DEVICESTATUS_UPLOADERBATTERY = "uploaderBattery";
    public static final String DEVICESTATUS_DATETIME= "datetime";

    private static final String TABLE_DEVICESTATUS_CREATE
            = "CREATE TABLE "
            + TABLE_NAME_DEVICESTATUS + " (" + _ID
            + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + DEVICESTATUS_UPLOADERBATTERY + " LONG, "
            + DEVICESTATUS_DATETIME + " STRING);";


    private static final String TABLE_ENTRIES_DATE_INDEX
            = "create index "
            + TABLE_NAME_ENTRIES + "_" + ENTRIES_DATE + "_IDX "
            + " on "
            + TABLE_NAME_ENTRIES + " ("+ ENTRIES_DATE  +"); ";

    private static final String TABLE_ENTRIES_SGV_INDEX
            = "create index "
            + TABLE_NAME_ENTRIES + "_" + ENTRIES_SGV + "_IDX "
            + " on "
            + TABLE_NAME_ENTRIES + " ("+ ENTRIES_SGV  +"); ";

    private static final String TABLE_ENTRIES_TYPE_INDEX
            = "create index "
            + TABLE_NAME_ENTRIES + "_" + ENTRIES_TYPE + "_IDX "
            + " on "
            + TABLE_NAME_ENTRIES + " ("+ ENTRIES_TYPE  +"); ";


    private static final String TABLE_TREATMENTS_CREATED_AT_INDEX
            = "create index "
            + TABLE_NAME_TREATMENTS + "_" + TREATMENTS_CREATED_AT + "_IDX "
            + " on "
            + TABLE_NAME_TREATMENTS + " ("+ TREATMENTS_CREATED_AT  +"); ";

    private static final String TABLE_TREATMENTS_EVENTTYPE_INDEX
            = "create index "
            + TABLE_NAME_TREATMENTS + "_" + TREATMENTS_EVENTTYPE + "_IDX "
            + " on "
            + TABLE_NAME_TREATMENTS + " ("+ TREATMENTS_EVENTTYPE  +"); ";

    private static final String TABLE_DEVICESTATUS_DATE_INDEX
            = "create index "
            + TABLE_NAME_DEVICESTATUS + "_" + DEVICESTATUS_DATETIME + "_IDX "
            + " on "
            + TABLE_NAME_DEVICESTATUS + " ("+ DEVICESTATUS_DATETIME  +"); ";



    // create index myIndex on myTable (myColumn);






    // Tabelle mood löschen
        private static final String TABLE_DEVICESTATUS_DROP =
                "DROP TABLE IF EXISTS "
                        + TABLE_NAME_DEVICESTATUS;

        private static final String TABLE_TREATMENTS_DROP =
                "DROP TABLE IF EXISTS "
                        + TABLE_NAME_TREATMENTS;

        private static final String TABLE_ENTRIES_DROP =
                    "DROP TABLE IF EXISTS "
                            + TABLE_NAME_ENTRIES;

    SQL_lite(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }
        @Override

        public void onCreate(SQLiteDatabase db) {

            try {
                db.execSQL(TABLE_ENTRIES_CREATE);

            } catch (SQLiteException e) {
                Log.e(TAG, "oncreate TABLE_ENTRIES_DROP", e);
            } finally {
                Log.d(TAG, "onCreate TABLE_ENTRIES_CREATE successfull");
            }

            try {

                db.execSQL(TABLE_ENTRIES_DATE_INDEX);
                db.execSQL(TABLE_ENTRIES_SGV_INDEX);
                db.execSQL(TABLE_ENTRIES_TYPE_INDEX);

            } catch (SQLiteException e) {
                Log.e(TAG, "oncreate TABLE_ENTRIES_INDEX", e);
            } finally {
                Log.d(TAG, "onCreate TABLE_ENTRIES_INDEX successfull");
            }

            try {
                db.execSQL(TABLE_DEVICESTATUS_CREATE);

            } catch (SQLiteException e) {
                Log.e(TAG, "oncreate TABLE_DEVICESTATUS_CREATE", e);
            } finally {
                Log.d(TAG, "onCreate TABLE_DEVICESTATUS_CREATE successfull");
            }

            try {
                db.execSQL(TABLE_DEVICESTATUS_DATE_INDEX);

            } catch (SQLiteException e) {
                Log.e(TAG, "oncreate TABLE_DEVICESTATUS_DATE_INDEX", e);
            } finally {
                Log.d(TAG, "onCreate TABLE_DEVICESTATUS_DATE_INDEX successfull");
            }


            try {
                db.execSQL(TABLE_TREATMENTS_CREATE);

            } catch (SQLiteException e) {
                Log.e(TAG, "oncreate TABLE_TREATMENTS_CREATE", e);
            } finally {
                Log.d(TAG, "onCreate TABLE_TREATMENTS_CREATE successfull");
            }

            try {
                db.execSQL(TABLE_TREATMENTS_CREATED_AT_INDEX);
                db.execSQL(TABLE_TREATMENTS_EVENTTYPE_INDEX);

            } catch (SQLiteException e) {
                Log.e(TAG, "oncreate TABLE_TREATMENTS_CREATED_AT_INDEX", e);
            } finally {
                Log.d(TAG, "onCreate TABLE_TREATMENTS_CREATED_AT_INDEX successfull");
            }
        }


    @Override
        public void onUpgrade(SQLiteDatabase db,
                              int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrade der Datenbank von Version "
                    + oldVersion + " zu "
                    + newVersion
                    + "; alle Daten werden gelöscht");
            db.execSQL(TABLE_ENTRIES_DROP);
            db.execSQL(TABLE_DEVICESTATUS_DROP);
            db.execSQL(TABLE_TREATMENTS_DROP);

            onCreate(db);
        }

        public void insertentries(Bundle B) {

            long rowId = -1;
            try {
                SQLiteDatabase db = getWritableDatabase();
                ContentValues values = new ContentValues();

                values.put(ENTRIES_SGV, (Long.parseLong(B.get("sgv").toString())));
                values.put(ENTRIES_DATE, (Long.parseLong(B.get("date").toString())));
                values.put(ENTRIES_DATESTRING, (B.get("dateString").toString()));
                values.put(ENTRIES_DEVICE, (B.get("device").toString()));
                values.put(ENTRIES_RSSI, (Long.parseLong(B.get("rssi").toString())));
                values.put(ENTRIES_TYPE, (B.get("type").toString()));
                values.put(ENTRIES_DIRECTION, (B.get("direction").toString()));
                values.put(ENTRIES_FILTERED, (Long.parseLong(B.get("filtered").toString())));
                values.put(ENTRIES_UNFILTERED, (Long.parseLong(B.get("unfiltered").toString())));

                rowId = db.update(TABLE_NAME_ENTRIES, values,ENTRIES_DATE + " = ?", new String[] { B.get("date").toString() });
                if (rowId==0) {
                    rowId = db.insert(TABLE_NAME_ENTRIES, null, values);
                    Log.d(TAG,"insertentries Trage neuen Datensatz ein. SGV: " + Long.parseLong(B.get("sgv").toString()) + " Datum: " + B.get("dateString").toString());
                }


            } catch (SQLiteException e) {
                Log.e(TAG, "insertentries()", e);
            } finally {
            }

        }


    public Cursor getentries(long date) {
        Cursor cursor=null;
        long eintaginmillis=1000*60*60*24;

        try {

            SQLiteDatabase db = getWritableDatabase();


            // String[] args = new String[] { Long.toString(date).toString() ,Long.toString(date+eintaginmillis).toS tring() };

            //String[] args = new String[] { Long.toString(date)};

            String SQL = "SELECT * FROM " + TABLE_NAME_ENTRIES + " WHERE date>=" + date + " AND date<= " + (date+eintaginmillis) + "  LIMIT 500";

            Log.d(TAG,"Action SQL_lite SQL: " + SQL);

            cursor = db.rawQuery(SQL, null);
            // For better performance no order by statement


            // cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME_ENTRIES + " WHERE date>=?", args);

            //cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME_ENTRIES + " WHERE 1", null);

            // Log.d(TAG, "getentries(): count=" +cursor.getCount());
            //if (cursor.moveToFirst()){
            //    temp_address = cursor.getString(cursor.getColumnIndex("lastchapter"));
            //}
            // cursor.close();


        } catch (SQLiteException e) {
            Log.e(TAG, "getentries()", e);
        } finally {
            Log.d(TAG, "getentries(): rowId=" );
        }


        return cursor;

    }


    public Bundle getAktData() {
        Cursor cursor=null;
        Bundle bundle = new Bundle();


        try {

            SQLiteDatabase db = getWritableDatabase();

            String[] args = new String[] { "sgv" };

            cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME_ENTRIES + " WHERE type like ? ORDER by date DESC LIMIT 1", args);

            Date dateakt = new Date();
            long dateaktlng = dateakt.getTime() - 60000;

            Log.d(TAG,"DatenSQL.getentries dateakat " + dateaktlng ) ;


            //
            Log.d(TAG, "getentries(): count=" + cursor.getCount());
            bundle.putString("date", "fuck off");


            if (cursor.getCount()==1) {

                cursor.moveToFirst();

                bundle.putString(_ID, cursor.getString(cursor.getColumnIndex(_ID)));
                bundle.putString(ENTRIES_DATESTRING, cursor.getString(cursor.getColumnIndex(ENTRIES_DATESTRING)));
                bundle.putString(ENTRIES_DATE, cursor.getString(cursor.getColumnIndex(ENTRIES_DATE)));
                bundle.putString(ENTRIES_DIRECTION, cursor.getString(cursor.getColumnIndex(ENTRIES_DIRECTION)));
                bundle.putString(ENTRIES_DEVICE, cursor.getString(cursor.getColumnIndex(ENTRIES_DEVICE)));
                bundle.putString(ENTRIES_SGV, cursor.getString(cursor.getColumnIndex(ENTRIES_SGV)));
                bundle.putString(ENTRIES_RSSI, cursor.getString(cursor.getColumnIndex(ENTRIES_RSSI)));
                bundle.putString(ENTRIES_TYPE, cursor.getString(cursor.getColumnIndex(ENTRIES_TYPE)));
                bundle.putString(ENTRIES_UNFILTERED, cursor.getString(cursor.getColumnIndex(ENTRIES_UNFILTERED)));
                bundle.putString(ENTRIES_FILTERED, cursor.getString(cursor.getColumnIndex(ENTRIES_FILTERED)));

                long sTime = (int) ((currentTimeMillis() - Long.valueOf(cursor.getString(cursor.getColumnIndex(ENTRIES_DATE))).longValue())/1000/60);
                bundle.putString("time_since", sTime + "");

            } else {
                bundle=null;
            }
            cursor.close();



            //cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME_ENTRIES + " WHERE 1", null);

            //if (cursor.moveToFirst()){
            //    temp_address = cursor.getString(cursor.getColumnIndex("lastchapter"));
            //}
            // cursor.close();


        } catch (SQLiteException e) {
            Log.e(TAG, "getAktData()", e);
        } finally {
            Log.d(TAG, "getAktData() OKAY" );
        }

        return bundle;

    }

}
