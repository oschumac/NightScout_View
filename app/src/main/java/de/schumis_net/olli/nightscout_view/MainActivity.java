package de.schumis_net.olli.nightscout_view;


import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Formatter;

// Fertig Bundle mit Inittialisierungsdaten versehen.
// Fertig DataCollection mit Datensichern
//


import static java.lang.System.currentTimeMillis;


// Fertig MongoDB Verbindung Visualisieren -- I.O.
// Todo:  Nach APP restart Mongo Anfrage Starten
// Todo:  Alarm funktioniert nicht richtig im Hintergrund ! Muss debuged werden ?
// Todo:  Zeitverschleppung optimieren bei Disconect
// Todo:  nach ersten connect wieder default laden.


public class MainActivity extends ActionBarActivity{
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int SOCKET_TIMEOUT = 60000;
    private static final int CONNECTION_TIMEOUT = 30000;
    private Context mContext;
    private Boolean enableRESTUpload;
    private Boolean enableMongoUpload;
    public SharedPreferences prefs;
    private Button btnClick;
    private long Glob_date;
    private String BZ_VALUE;
    private String DATESTRING;
    // private DataCollection Daten = new DataCollection();
    private boolean firstcycle=true;
    private int firstalldata=0;
    private boolean MongoConnectionState = true;
    private boolean MongoConnectionStateOld = true;

    private CanvasView customCanvas;
    private static boolean CGMAlarm=false;
    // ImageView Iview;


    private Boolean APPINRUN=true;
    private static long pause_sleep_time=1000*60*10;
    final long HOUR = 3600 * 1000;
    final long DAY = HOUR * 24;
    final int HOURS = 24;
    public int mHeight;
    public int mWidth;
    boolean flankem1=false;
    boolean flankem2=false;
    boolean flankem3=false;
    boolean FlagSartsync=false;
    boolean doUpdate=false;

    private SQL_lite DatenSQL;
    private static long MongoCollCount=0;


    public Thread t_GUIUPDATE;
    public Thread t_TALKWGUI;
    public Thread t_MinuteUpdate;
    public Thread t_Alarmhandling;

    public Runnable GUIUPDATE;
    public Runnable TALKWGUI;
    public Runnable MinuteUpdate;
    public Runnable Alarmhandling;


    public long Zeitverschleppung=100000;
    public long MongoOldDate=0;

    // This example will cause the phone to vibrate "SOS" in Morse Code
// In Morse Code, "s" = "dot-dot-dot", "o" = "dash-dash-dash"
// There are pauses to separate dots/dashes, letters, and words
// The following numbers represent millisecond lengths
    Vibrator AlarmVib;
    int dot = 100;      // Length of a Morse Code "dot" in milliseconds
    int dash = 250;     // Length of a Morse Code "dash" in milliseconds
    int short_gap = 100;    // Length of Gap Between dots/dashes
    int medium_gap = 250;   // Length of Gap Between Letters
    int long_gap = 500;    // Length of Gap Between Words
    long[] patternSOS = {
            0,  // Start immediately
            dot, short_gap, dot, short_gap, dot,    // s
            medium_gap,
            dash, short_gap, dash, short_gap, dash, // o
            medium_gap,
            dot, short_gap, dot, short_gap, dot,    // s
            long_gap
    };

    long[] pattern1 = {
            0,  // Start immediately
            300, 200, 100
    };
    long[] pattern2 = {
            0,  // Start immediately
            200, 100
    };

    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
    }

    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume  APPINRUN=true Start_Threads;");
        APPINRUN=true;
        Start_Threads();
        GETSQLUPDATETOGUI();
    }

    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause APPINRUN=false  Stop_Threads;");
        APPINRUN=false;  // Stoppt Threads
        Interupt_Threads();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AlarmVib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        customCanvas = (CanvasView) findViewById(R.id.signature_canvas);

        Log.d(TAG, "onCreate started");

        mContext = customCanvas.context;
        prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        DatenSQL = new SQL_lite(this);


        // Einen Thread nur zum Updaten der GUI und mongodaten
        GUIUPDATE = new Runnable() {
            private long sTime=2000;
            long countsleeppause=1;

            Bundle ThBundle = new Bundle();
            long olddate = 0;

            public void run(){
                Log.d(TAG,"APPINRUN THREAD GUIUPDATE start" );
                while (true) {

                    // ---------------------  Erster Zyklus alternativer Ablauf
                    if (firstcycle) {
                        firstcycle = false;

                        try {
                            Log.d(TAG,"Mal Schauen ob in der DB etwas enthalten ist");
                            ThBundle = DatenSQL.getAktData();

                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.e(TAG,"DB nix gut: " + e);
                        }
                        if (!(ThBundle==null)) {
                            olddate=Long.valueOf(ThBundle.getString("date").toString());
                            Message msg = new Message();
                            msg.setData(ThBundle);
                            handler.sendMessage(msg);
                        }
                    }

                    // ---------------------  MongoDB abfragen
                    try {
                        doUpdate=false;
                        sTime=GetMongodata();
                        Log.d(TAG,"mongodb  sTime:"+sTime);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e(TAG,"DB nix gut: " + e);
                        sTime=60000;
                    }


                    // ---------------------  Daten aus der internen DB holen
                    try {
                        Log.d(TAG,"Mal Schauen ob in der DB etwas enthalten ist");
                        ThBundle = DatenSQL.getAktData();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e(TAG,"DB nix gut: " + e);
                    }

                    // ---------------------  Wenn date verändert dann UI update via Händler
                    if (!(ThBundle==null)) {
                        if ((!(olddate == Long.valueOf(ThBundle.getString("date").toString()))) || (!(MongoConnectionStateOld==MongoConnectionState))  ) {
                            MongoConnectionStateOld=MongoConnectionState;
                            olddate = Long.valueOf(ThBundle.getString("date").toString());
                            Message msg = new Message();
                            msg.setData(ThBundle);
                            handler.sendMessage(msg);
                        }
                    }

                    // -------x--------------  Standart sleep routine
                    try {
                        long i=0;
                        Log.d(TAG,"GUIUPDATE  sTime: " + (sTime/1000) + " Sekunden");
                        while ((i<sTime/1000) & !doUpdate ) {
                            i++;
                            Thread.sleep(1000);
                            if (customCanvas.getupdateview()) {
                                i=sTime;
                            }
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        t_GUIUPDATE = new Thread(GUIUPDATE);
        t_GUIUPDATE.start();

        // Einen Thread Nur für das Alarmhändling
        Alarmhandling = new Runnable() {
            private long sTime=1000;

            boolean enable_VIB = prefs.getBoolean("notifications_new_message_vibrate",false);

            public void run() {
                Log.d(TAG, "APPINRUN THREAD Alarmhandling start");

                customCanvas.resetAlarmquitt();
                while (CGMAlarm & enable_VIB) {

                    AlarmVib.vibrate(pattern2, 0);

                    try {
                        Thread.sleep(sTime);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                }
                Log.d(TAG,"APPINRUN THREAD Alarmhandling beendet" );
                AlarmVib.cancel();
                // Dann UI updaten
                Message msg = new Message();
                msg.setData(DatenSQL.getAktData());
                handler.sendMessage(msg);
                sTime=10;

            }
        };


        // Einen Thread um mit der UI zu Reden
        TALKWGUI = new Runnable() {
            private long sTime=1000;
            Bundle ThBundle = new Bundle();

            public void run(){
                Log.d(TAG,"APPINRUN THREAD TALKWGUI start" );
                while (APPINRUN) {

                    if (!firstcycle) {
                        // irgendwie den customcanvas abfragen !!

                        if (customCanvas.getsimpleupdate()) {
                            ThBundle = DatenSQL.getAktData();
                            Message msg = new Message();
                            msg.setData(ThBundle);
                            handler.sendMessage(msg);
                            sTime=10;
                        }

                        if (customCanvas.getSizeChanged()) {
                            ThBundle=DatenSQL.getAktData();
                            Message msg = new Message();
                            msg.setData(ThBundle);
                            handler.sendMessage(msg);
                        }
                    }

                    try {

                        Thread.sleep(sTime);
                        if(sTime<1000) {
                            sTime=sTime+50;
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                Log.d(TAG,"APPINRUN THREAD TALKWGUI beendet" );
            }
        };

        t_TALKWGUI = new Thread(TALKWGUI);
        t_TALKWGUI.start();


        MinuteUpdate = new Runnable() {
            @Override
            public void run(){
                Bundle ThBundle = new Bundle();
                String THStr;
                long sleeptime = 60000;

                Log.d(TAG,"APPINRUN THREAD MinuteUpdate Start" );
                while (APPINRUN) {

                    ThBundle = DatenSQL.getAktData();
                    if (!(ThBundle==null)) {
                        THStr = ThBundle.getString("date");
                        if (!THStr.isEmpty()) {
                            Message msg = new Message();
                            msg.setData(ThBundle);
                            handler.sendMessage(msg);
                        }
                    }
                    try {
                        Thread.sleep(sleeptime);
                    } catch (InterruptedException e) {

                    }
                }
                Log.d(TAG,"APPINRUN THREAD MinuteUpdate beendet" );
            }
        };
        t_MinuteUpdate = new Thread(MinuteUpdate);
        t_MinuteUpdate.start();
}


    public void Start_Threads(){
        Log.d(TAG, "Start_Threads ausgefüht!");
        if(!t_TALKWGUI.isAlive()) {
            t_TALKWGUI = new Thread(TALKWGUI);
            t_TALKWGUI.start();
        }
        if(!t_MinuteUpdate.isAlive()) {
            t_MinuteUpdate = new Thread(MinuteUpdate);
            t_MinuteUpdate.start();
        }
    }



    public void Interupt_Threads(){
        Log.d(TAG,"Interupt_Threads ausgefüht!");
        t_TALKWGUI.interrupt();
        t_MinuteUpdate.interrupt();
    }

    public void imageAlarmClick(View v) {
        Log.d(TAG,"onClick imageAlarmClick");
        CGMAlarm=false;
        AlarmVib.cancel();
    }


    public void textCGMValClick(View v) {
        Log.d(TAG,"onClick textCGMValClick!");
        doUpdate=true;
    }

    //
    // Funktion updateView
    // Kümmert sich um die GUI
    // Die WorkerThreads rufen die funktion per händler auf
    // Parameter ist ein Bundle in dem die Daten der GUI eingetragen sind
    //
    private int updateView(Bundle bundle) {
        int myNum,y;
        int myNumT;


        String test = prefs.getString("Hypowarnschwelle1","55");
        long Hypowarnschwelle1 = Long.parseLong(prefs.getString("Hypowarnschwelle1","55"));
        long Hypowarnschwelle2 = Long.parseLong(prefs.getString("Hypowarnschwelle2","70"));
        long Hyperwarnschwelle = Long.parseLong(prefs.getString("Hyperwarnschwelle","250"));

        // Log.d(TAG,"updateView : Hypo1: " + test);
        Log.d(TAG,"updateView : Hypo1:" + Hypowarnschwelle1 + "  Hypo2:" + Hypowarnschwelle2 + " Hyper:" + Hyperwarnschwelle);
        Log.d(TAG,"updateView mit Alarm verarbeitung läuft APPINRUN: " + APPINRUN);

        if (bundle.getString("sgv")==null) {return 1;};



        setContentView(R.layout.activity_main);
        customCanvas = (CanvasView) findViewById(R.id.signature_canvas);

        myNum = Integer.parseInt(bundle.getString("sgv"));

        TextView myTextView_1 = (TextView) findViewById(R.id.textView_1);
        if (myNum < 30) {
            myTextView_1.setText("xxx" + getarrows(bundle.getString("direction")));
        } else {
            myTextView_1.setText(bundle.getString("sgv") + " " + getarrows(bundle.getString("direction")));
        }

        if (prefs.getBoolean("alarm_use_Trends",false)==true) {
             myNumT = myNum + getarrowsValues(bundle.getString("direction"));
             Log.d(TAG,"updateView myNum: " + myNum + " korrigiert mit Trend " +getarrows(bundle.getString("direction"))+ " : " + myNumT);
        } else {
            myNumT=myNum;
        }

        myTextView_1.setTextColor(Color.WHITE);
        // default 55  getarrowsValues
        if (myNumT <= Hypowarnschwelle1) {
            myTextView_1.setTextColor(Color.RED);
            if (!flankem1) {
                flankem1=true;
                customCanvas.resetAlarmquitt();
                CGMAlarm=true;
                t_Alarmhandling = new Thread(Alarmhandling);
                t_Alarmhandling.start();
            }
        } else {
            flankem1=false;
        }

        // default 70  getarrowsValues
        if (myNumT <= Hypowarnschwelle2) {
            myTextView_1.setTextColor(Color.RED);
            if (!flankem2) {
                flankem2=true;
                CGMAlarm=true;
                customCanvas.resetAlarmquitt();
                t_Alarmhandling = new Thread(Alarmhandling);
                t_Alarmhandling.start();
            }
        } else {
            flankem2=false;
        }


        // default 250  getarrowsValues
        if (myNumT >= Hyperwarnschwelle) {
            myTextView_1.setTextColor(Color.BLUE);
            if (!flankem3) {
                flankem3=true;
                CGMAlarm=true;
                customCanvas.resetAlarmquitt();
                t_Alarmhandling = new Thread(Alarmhandling);
                t_Alarmhandling.start();

            }
        } else {
            flankem3=false;
        }

        TextView myTextViewdate1 = (TextView) findViewById(R.id.textView_Date1);
        SimpleDateFormat formatter = new SimpleDateFormat("EEE dd.MM.yyyy - HH:mm:ss");

        // myTextViewdate1.setText(bundle.getString("dateString"));

        myTextViewdate1.setText(formatter.format(Long.valueOf(bundle.getString("date")).longValue()));

        myNum = (int) ((currentTimeMillis() - Long.valueOf(bundle.getString("date")).longValue())/1000/60);
        // myNum = Integer.parseInt(bundle.getString("time_since"));
        TextView myTextViewdate2 = (TextView) findViewById(R.id.textView_2);
        if (myNum > 20) {
            myTextViewdate2.setTextColor(Color.RED);
        } else {
            myTextViewdate2.setTextColor(Color.WHITE);
        }

        if (!MongoConnectionState) {
            myTextViewdate2.setTextColor(Color.GRAY);
        }

        myTextViewdate2.setText(myNum + " Min");

        ImageView myImageView1 = (ImageView) findViewById(R.id.imageView);
        if (CGMAlarm) {
            myImageView1.setVisibility(View.VISIBLE);
        } else {
            myImageView1.setVisibility(View.INVISIBLE);

        }
        // Log.d(TAG,"Action Update UI ! refresh()");
        // customCanvas.refresh();


        Cursor c;
        Date dateakt = new Date();
        // long dateaktlng = dateakt.getTime() - 60000;

        long dateaktlng = dateakt.getTime();


        Log.d(TAG,"Action Update UI ! SQL");

        //

        if (!APPINRUN==true) {
            Log.d(TAG,"Action DatenSQL. APPINRUN=false keine SQL verarbeitung !!! ");
            return 1;
        };


        Log.d(TAG,"Action DatenSQL.getentries dateakat1.1 " + formatter.format(dateaktlng) + " timeval: " + dateaktlng) ;
        Log.d(TAG,"Action DatenSQL.getentries dateakat1.2: " + formatter.format(dateaktlng-customCanvas.get0hourofday(dateaktlng)) + " timeval: " + (dateaktlng-customCanvas.get0hourofday(dateaktlng)));



        c = DatenSQL.getentries(dateaktlng-customCanvas.get0hourofday(dateaktlng));

        Date dateakt2 = new Date();
        long dateaktlng2 = dateakt2.getTime();
        Log.d(TAG,"Action DatenSQL.getentries dateakat2 " + (dateaktlng2-dateaktlng)) ;

        long anzahl=0;
        while (c.moveToNext()) {
            anzahl++;
            customCanvas.setChartPoint(c.getLong(c.getColumnIndex("date")),c.getLong(c.getColumnIndex("sgv")));
        }
        c.close();
        Date dateakt3 = new Date();
        long dateaktlng3 = dateakt3.getTime();
        Log.d(TAG,"Action DatenSQL.getentries dateakat3 " + (dateaktlng3-dateaktlng) + " Anzahl:" + anzahl);

        Log.d(TAG,"Action Update UI ! refresh()");
        customCanvas.refresh();
        Log.d(TAG,"Action Update UI ! fertig");

        return 1;
    }

    // Das ist der GUI Händler für den Timer Thread
    public Handler handler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            Bundle bundle = msg.getData();
            removeMessages(0); //this is very important
            updateView(bundle);
        }
    };


    // Das ist eine Update Funktion die einen Thread startet
    // für die interne SQL Anfrage
    // anschließend wird der Händler für das Update der GUI angestartet.
    public void GETSQLUPDATETOGUI() {
        Log.d(TAG,"GETSQLUPDATETOGUI");
        new Thread(new Runnable() {
            public void run() {
                Bundle ThBundle = DatenSQL.getAktData();
                if (!(ThBundle==null)) {
                    Message msg = new Message();
                    msg.setData(ThBundle);
                    handler.sendMessage(msg);
                }
            }
        }).start();
    };


    //
    // Das ist ein Datenbank händler zum Syncronisieren der Daten zwischen SQL_lite und MongoDB
    // es übergabe parameter ist wieviele Datensätze der cloud DB (MongoDB) -> local DB (SQL_lite DB)
    // verglichen werden sollen.
    //
    //

    public Handler MongoDBl_handler= new Handler() {
        public void handleMessage(android.os.Message msg) {

            final long l_MongoCollCount = msg.arg1;
            if (l_MongoCollCount<=0) {return;}

            removeMessages(0); //this is very important
            // Ein Thread zum Datensyncronisieren

            Runnable DB_HANDL_TH = new Runnable() {
                @Override
                public void run() {
                    try {
                        Log.d(TAG, "MongoDB1 GetMongodataL(1) im eigenen Thread gestartet");

                        Message msgtoast1 = new Message();
                        Bundle b2 = new Bundle();
                        b2.putString("text","Start Mongo DB lesen!");
                        msgtoast1.setData(b2);
                        Toast_handler.sendMessage(msgtoast1);

                        int I;
                        // GetMongodataL(1,0,100);

                        for (I=0;I<l_MongoCollCount;I=I+100) {
                            Log.d(TAG, "MongoDB1 GetMongodataL(1) im eigenen Thread gestartet I:" + I);
                            GetMongodataL(1,I,100);
                        }

                        // ---  Visu Update
                        Bundle ThBundle=DatenSQL.getAktData();
                        Message msg2 = new Message();
                        msg2.setData(ThBundle);
                        handler.sendMessage(msg2);

                        // toast senden
                        Message msgtoast = new Message();
                        Bundle b = new Bundle();
                        b.putString("text","Mongo DB gelesen !! :" + l_MongoCollCount);
                        msgtoast.setData(b);
                        Toast_handler.sendMessage(msgtoast);

                    }   catch (Exception e) {
                        Log.e(TAG, "MongoDB1  Fehler !!! Code: ", e);
                    }

                }
            };

            Thread T_DB_HANDL_TH = new Thread (DB_HANDL_TH);
            T_DB_HANDL_TH.start();

        }
    };


    // Damit aus einen Thread auch ein Toast gesendet werden kann.
    public Handler Toast_handler= new Handler() {
        public void handleMessage(android.os.Message msg) {
            String text = msg.getData().get("text").toString();
            Toast toast = Toast.makeText(mContext , text, Toast.LENGTH_LONG);
            toast.show();
        }
    };


    //
    // Das ist eine Hilfsfunktion für die Pfeile in der Anzeige
    //
    public String getarrows(String Name){
        String CodeStr;
        CodeStr="";

        if (Name.equals("Flat")) {CodeStr="\u2192";}
        // Konstant: Ihr Gewebeglukosewert ist konstant (steigt/fällt nicht um mehr als
        // 1 mg/dl pro Minute). Ihr Gewebeglukosewert kann in 15 Minuten um bis zu
        // 15 mg/dl steigen oder fallen.
        if (Name.equals("FortyFiveUp")) {CodeStr="\u2197";}
        // Langsam ansteigend: Ihr Gewebeglukosewert steigt mit 1 bis
        // 2 mg/dl pro Minute. Falls er weiter mit dieser Rate ansteigt, kann Ihr
        // Gewebeglukosewert in 15 Minuten um 30 mg/dl ansteigen.

        if (Name.equals("SingleUp")) {CodeStr="\u2191";}
        // Ansteigend: Ihr Gewebeglukosewert steigt mit 2 bis 3 mg/dl pro Minute.
        // Falls er weiter mit dieser Rate ansteigt, kann Ihr Gewebeglukosewert in 15
        // Minuten um 45 mg/dl ansteigen.

        if (Name.equals("DoubleUp")) {CodeStr="\u21C8";}
        // Schnell ansteigend: Ihr Gewebeglukosewert steigt mit mehr als
        // 3 mg/dl pro Minute. Falls er weiter mit dieser Rate ansteigt, kann Ihr
        // Gewebeglukosewert in 15 Minuten um mehr als 45 mg/dl ansteigen.


        if (Name.equals("FortyFiveDown")) {CodeStr="\u2198";}
        // Langsam abfallend: Ihr Gewebeglukosewert fällt mit 1 bis 2 mg/dl pro
        // Minute. Falls er weiter mit dieser Rate fällt, kann Ihr Gewebeglukosewert in
        // 15 Minuten um 30 mg/dl fallen.

        if (Name.equals("SingleDown")) {CodeStr="\u2193";}
        //  Abfallend: Ihr Gewebeglukosewert fällt mit 2 bis 3 mg/dl pro Minute. Falls er
        // weiter mit dieser Rate fällt, kann Ihr Gewebeglukosewert in 15 Minuten um
        // 45 mg/dl fallen.

        if (Name.equals("DoubleDown")) {CodeStr="\u21CA";}
        // Schnell abfallend: Ihr Gewebeglukosewert fällt mit mehr als 3 mg/dl pro
        // Minute. Falls er weiter mit dieser Rate fällt, kann Ihr Gewebeglukosewert in
        // 15 Minuten um mehr als 45 mg/dl fallen.

        if (Name.equals("NOT COMPUTABLE")) {CodeStr="-";}
        // Keine Daten vom Sensor zum Thema Trend

        if (Name.isEmpty()) {CodeStr="-";}
        // Keine Daten vom Sensor zum Thema Trend Fehler

        return CodeStr;
    }

    //
    // Das ist eine Hilfsfunktion für die Pfeile in der Anzeige
    //
    public int getarrowsValues(String Name){
        int CodeStr;
        CodeStr=0;

        if (Name.equals("Flat")) {CodeStr=-15;}
        // Konstant: Ihr Gewebeglukosewert ist konstant (steigt/fällt nicht um mehr als
        // 1 mg/dl pro Minute). Ihr Gewebeglukosewert kann in 15 Minuten um bis zu
        // 15 mg/dl steigen oder fallen.
        if (Name.equals("FortyFiveUp")) {CodeStr=30;}
        // Langsam ansteigend: Ihr Gewebeglukosewert steigt mit 1 bis
        // 2 mg/dl pro Minute. Falls er weiter mit dieser Rate ansteigt, kann Ihr
        // Gewebeglukosewert in 15 Minuten um 30 mg/dl ansteigen.

        if (Name.equals("SingleUp")) {CodeStr=45;}
        // Ansteigend: Ihr Gewebeglukosewert steigt mit 2 bis 3 mg/dl pro Minute.
        // Falls er weiter mit dieser Rate ansteigt, kann Ihr Gewebeglukosewert in 15
        // Minuten um 45 mg/dl ansteigen.

        if (Name.equals("DoubleUp")) {CodeStr=60;}
        // Schnell ansteigend: Ihr Gewebeglukosewert steigt mit mehr als
        // 3 mg/dl pro Minute. Falls er weiter mit dieser Rate ansteigt, kann Ihr
        // Gewebeglukosewert in 15 Minuten um mehr als 45 mg/dl ansteigen.


        if (Name.equals("FortyFiveDown")) {CodeStr=-30;}
        // Langsam abfallend: Ihr Gewebeglukosewert fällt mit 1 bis 2 mg/dl pro
        // Minute. Falls er weiter mit dieser Rate fällt, kann Ihr Gewebeglukosewert in
        // 15 Minuten um 30 mg/dl fallen.

        if (Name.equals("SingleDown")) {CodeStr=-45;}
        //  Abfallend: Ihr Gewebeglukosewert fällt mit 2 bis 3 mg/dl pro Minute. Falls er
        // weiter mit dieser Rate fällt, kann Ihr Gewebeglukosewert in 15 Minuten um
        // 45 mg/dl fallen.

        if (Name.equals("DoubleDown")) {CodeStr=-60;}
        // Schnell abfallend: Ihr Gewebeglukosewert fällt mit mehr als 3 mg/dl pro
        // Minute. Falls er weiter mit dieser Rate fällt, kann Ihr Gewebeglukosewert in
        // 15 Minuten um mehr als 45 mg/dl fallen.

        if (Name.equals("NOT COMPUTABLE")) {CodeStr=0;}
        // Keine Daten vom Sensor zum Thema Trend

        if (Name.isEmpty()) {CodeStr=0;}
        // Keine Daten vom Sensor zum Thema Trend Fehler

        return CodeStr;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Log.d(TAG,"Action Settings gestartet");
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);

            // View update
            Bundle ThBundle=DatenSQL.getAktData();
            Message msg2 = new Message();
            msg2.setData(ThBundle);
            handler.sendMessage(msg2);

            // Mongo



            return true;
        }
        if (id == R.id.action_sync) {
            Log.d(TAG,"Action sync gestartet");
            Message msg = new Message();
            msg.arg1=300;
            MongoDBl_handler.sendMessage(msg);

            Bundle ThBundle=DatenSQL.getAktData();
            Message msg2 = new Message();
            msg2.setData(ThBundle);
            handler.sendMessage(msg2);
            return true;
        }
        if (id == R.id.action_syncall) {
            Log.d(TAG,"Action syncall gestartet");
            Message msg = new Message();
            if (!(MongoCollCount==0)) {
                msg.arg1=(int)MongoCollCount;
            } else {
                msg.arg1=280*5; // Daten von 8 tagen Syncronisieren
            }
            MongoDBl_handler.sendMessage(msg);
            return true;
        }


        return super.onOptionsItemSelected(item);
    }

    // Funktion zum Abfragen der aktuellen Mongodaten
    private long GetMongodata() {

        // String dbURI = "mongodb://Lars_2009:Lars65535@ds056727.mongolab.com:56727/larscgm";
        // String collectionName = "entries";
        // String dsCollectionName = "devicestatus";

        String dbURI = prefs.getString("cloud_storage_mongodb_uri", null);
        String collectionName = prefs.getString("cloud_storage_mongodb_collection", null);
        String dsCollectionName = prefs.getString("cloud_storage_mongodb_device_status_collection", "devicestatus");

        Boolean EnableMongo = prefs.getBoolean("enable_mongo",false);

        int sTime = 60000;

        if (dbURI != null && collectionName != null && EnableMongo) {
            try {
                Log.i(TAG, dbURI);
                MongoConnectionState=false;
                // connect to db
                MongoClientURI uri = new MongoClientURI(dbURI.trim());
                MongoClient client = new MongoClient(uri);

                // get db
                DB db = client.getDB(uri.getDatabase());

                DBCollection coll = db.getCollection(collectionName);
                MongoCollCount = coll.count();

                BasicDBObject query = new BasicDBObject("type", "sgv");
                // { "_id" : { "$oid" : "54a3ef7a3777e50db741594c"} , "device" : "dexcom" , "date" : 1420029696000 , "dateString" : "Wed Dec 31 13:41:36 MEZ 2014" , "sgv" : 127 ,
                //  "direction" : "Flat" , "type" : "sgv" , "filtered" : 169280 , "unfiltered" : 166048 , "rssi" : 175}

                // CGM error !!! Datensatz muss noch eingepflegt werden
                // { "_id" : { "$oid" : "54a6e6ce3777e50db7417699"} , "device" : "dexcom" , "date" : 1420224129000 , "dateString" : "Fri Jan 02 19:42:09 CET 2015" , "sgv" : 10 ,
                // "direction" : "NOT COMPUTABLE" , "type" : "sgv" , "filtered" : 176736 , "unfiltered" : 205216 , "rssi" : 177}
                DBCursor Cursor = coll.find().sort(new BasicDBObject("date", -1)).limit(1);


                try {
                    if (!(Cursor==null)) {
                        while (Cursor.hasNext()) {
                            DBObject theObj = Cursor.next();
                            // Log.i(TAG, "MongoDB Gelesen Datensatz :" + theObj.toString());

                            if (theObj.get("type").toString().equals("sgv")) {
                                Bundle bundle = new Bundle();
                                bundle.putString("sgv", theObj.get("sgv").toString());
                                bundle.putString("dateString", theObj.get("dateString").toString());
                                bundle.putString("date", theObj.get("date").toString());
                                bundle.putString("direction", theObj.get("direction").toString());
                                bundle.putString("filtered", theObj.get("filtered").toString());
                                bundle.putString("unfiltered", theObj.get("unfiltered").toString());
                                bundle.putString("rssi", theObj.get("rssi").toString());
                                bundle.putString("device", theObj.get("device").toString());
                                bundle.putString("type", theObj.get("type").toString());

                                sTime = (int) ((currentTimeMillis() - Long.valueOf(theObj.get("date").toString()).longValue())/1000/60);
                                bundle.putString("time_since", sTime + "");
                                MongoConnectionState=true;

                                // Todo !!!!
                                sTime = (int) (300000 + Zeitverschleppung - (currentTimeMillis() - Long.valueOf(theObj.get("date").toString()).longValue()));
                                if (sTime < 60000) {
                                    sTime = 60000;
                                    Zeitverschleppung=Zeitverschleppung+10000;
                                    if ((Zeitverschleppung>=300000) & (!(MongoOldDate==Long.valueOf(theObj.get("date").toString()).longValue()))) {
                                        Zeitverschleppung=300000;
                                    }
                                }  else {
                                    if (!(Zeitverschleppung==0 & (!(MongoOldDate==Long.valueOf(theObj.get("date").toString()).longValue())))) {
                                        Zeitverschleppung=Zeitverschleppung-10000;
                                    }
                                }

                                SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss SSS");
                                // Log.d(TAG,"timeto current date: " + format.format(calender.getTime()));

                                Log.i(TAG, "Neue anfrage in : " + sTime / 1000 + " Sekunden zeitstempel ist: " +theObj.get("dateString").toString() + " SGV:" + theObj.get("sgv").toString() );
                                Log.i(TAG, "Neue anfrage um : " + format.format(currentTimeMillis()+sTime) + " Zeitverschleppung: " + (Zeitverschleppung/1000)+"s");

                                DatenSQL.insertentries(bundle);
                                MongoOldDate=Long.valueOf(theObj.get("date").toString()).longValue();
                            }
                        }

                        MongoConnectionState = true;
                    } else {
                        Log.d(TAG,"Mongo keine Daten bekommen evtl keine Netzverbindung");
                        MongoConnectionState = false;
                        sTime = 60000;
                    }

                } finally {
                    Cursor.close();
                }

                client.close();

                // Log.i(TAG, "MongoDB gelesen fertig");
                return sTime;

            } catch (Exception e) {
                Log.e(TAG, "MongoDB Fehler !!! Code: ", e);
            }
        }
        return sTime;
    }

    // Funktion zum Abfragen der Mongodaten für das Chart
    private void GetMongodataL(int behavior, int skip, int limit) {


        // String dbURI = "mongodb://Lars_2009:Lars65535@ds056727.mongolab.com:56727/larscgm";
        // String collectionName = "entries";
        // String dsCollectionName = "devicestatus";

        String dbURI = prefs.getString("cloud_storage_mongodb_uri", null);
        Log.i(TAG, dbURI);
        String collectionName = prefs.getString("cloud_storage_mongodb_collection", null);
        String dsCollectionName = prefs.getString("cloud_storage_mongodb_device_status_collection", "devicestatus");
        Boolean EnableMongo = prefs.getBoolean("enable_mongo", false);


        if (dbURI != null && collectionName != null  && EnableMongo) {
            try {

                // connect to db
                MongoClientURI uri = new MongoClientURI(dbURI.trim());
                MongoClient client = new MongoClient(uri);

                // get db
                DB db = client.getDB(uri.getDatabase());


                DBCollection coll = db.getCollection(collectionName);

                MongoCollCount = coll.count();

                Date akt = new Date();
                SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss SSS");

                GregorianCalendar calender = new GregorianCalendar();
                // Log.d(TAG,"timeto current date: " + format.format(calender.getTime()));

                int millis = calender.get(GregorianCalendar.MILLISECOND);
                int sec = calender.get(GregorianCalendar.SECOND);
                int min = calender.get(GregorianCalendar.MINUTE);
                int hour = calender.get(GregorianCalendar.HOUR_OF_DAY);

                long timeToPass = millis + (sec * 1000) + (min * 1000 * 60) + (hour * 1000 * 60 * 60);



                long aktdate = ((akt.getTime() - timeToPass)+10);
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd - HH:mm:ss  ");


                Log.d(TAG,"MongoDB Liste aktuelles Daten heute nacht: " + formatter.format(aktdate) );


                // BasicDBObject query = new BasicDBObject("type", "sgv");

                BasicDBObject query = new BasicDBObject();
                BasicDBObject queryvalue = new BasicDBObject();

                queryvalue.put("$gt",aktdate);
                query.put("date",queryvalue);

                // { "_id" : { "$oid" : "54a3ef7a3777e50db741594c"} , "device" : "dexcom" , "date" : 1420029696000 , "dateString" : "Wed Dec 31 13:41:36 MEZ 2014" , "sgv" : 127 ,
                //  "direction" : "Flat" , "type" : "sgv" , "filtered" : 169280 , "unfiltered" : 166048 , "rssi" : 175}

                // CGM error !!! Datensatz muss noch eingepflegt werden
                // { "_id" : { "$oid" : "54a6e6ce3777e50db7417699"} , "device" : "dexcom" , "date" : 1420224129000 , "dateString" : "Fri Jan 02 19:42:09 CET 2015" , "sgv" : 10 ,
                // "direction" : "NOT COMPUTABLE" , "type" : "sgv" , "filtered" : 176736 , "unfiltered" : 205216 , "rssi" : 177}

                DBCursor Cursor=null;
                if (behavior==0) {
                    Cursor = coll.find(query).sort(new BasicDBObject("date", -1)).skip(skip).limit(limit);
                }
                if (behavior==1) {
                    Log.d(TAG,"MongoDB GET SKIP: " + skip + " limit: " + limit);
                    Cursor = coll.find().sort(new BasicDBObject("date", -1)).skip(skip).limit(limit);
                }

                try {
                    while (Cursor.hasNext()) {
                        DBObject theObj = Cursor.next();
                        // Log.i(TAG, "MongoDB Gelesen Datensatz :" + theObj.toString());

                        if (theObj.get("type").toString().equals("sgv")) {
                            // Log.i(TAG, "MongoDB Gelesen sgv :" + theObj.get("sgv").toString());
                            // Log.i(TAG, "MongoDB Gelesen datestring:" + theObj.get("dateString").toString());
                            //Log.i(TAG, "MongoDB Gelesen date :" + theObj.get("date").toString());
                            //Log.i(TAG, "MongoDB Gelesen direction :" + theObj.get("direction").toString());
                            //Log.i(TAG, "MongoDB Gelesen type:" + theObj.get("type").toString());
                            //Log.i(TAG, "MongoDB Gelesen filtered:" + theObj.get("filtered").toString());
                            //Log.i(TAG, "MongoDB Gelesen unfiltered:" + theObj.get("unfiltered").toString());
                            //Log.i(TAG, "MongoDB Gelesen rssi:" + theObj.get("rssi").toString());
                            //Log.i(TAG, "MongoDB Gelesen device :" + theObj.get("device").toString());

                            Bundle bundle = new Bundle();
                            bundle.putString("sgv", theObj.get("sgv").toString());
                            bundle.putString("dateString", theObj.get("dateString").toString());
                            bundle.putString("date", theObj.get("date").toString());
                            bundle.putString("direction", theObj.get("direction").toString());
                            bundle.putString("type", theObj.get("type").toString());
                            bundle.putString("filtered", theObj.get("filtered").toString());
                            bundle.putString("unfiltered", theObj.get("unfiltered").toString());
                            bundle.putString("rssi", theObj.get("rssi").toString());
                            bundle.putString("device", theObj.get("device").toString());


                            // Log.i(TAG, "MongoDB Trage Daten ein SGV: " + Long.parseLong(theObj.get("sgv").toString()) +" dateString: " +  theObj.get("dateString").toString());
                            // Daten.ADDLIST(Long.parseLong(theObj.get("sgv").toString()), Long.parseLong(theObj.get("date").toString()));

                            DatenSQL.insertentries(bundle);



                            // Double.parseDouble((theObj.get("sgv")+ ".0"));
                        }
                    }
                } finally {
                    Cursor.close();
                }

                client.close();

                Log.i(TAG, "MongoDB Liste gelesen fertig");
                firstalldata=1;
                return;

            } catch (Exception e) {
                Log.e(TAG, "MongoDB Fehler !!! Code: ", e);
            }
        }
        return;
    }


}

