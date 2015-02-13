package de.schumis_net.olli.nightscout_view;


import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EventObject;
import java.util.GregorianCalendar;
import java.util.Iterator;

import static java.lang.System.currentTimeMillis;

/**
 * Created by OLLI on 06.01.2015.
 */
public class CanvasView extends View {
    private static final String TAG = CanvasView.class.getSimpleName();
    private SQL_lite DatenSQL;
    private static int Class_w;
    private static int Class_h;

    private static int chart_top=0;
    private static int chart_left=0;
    private static int chart_height=0;
    private static int chart_width=0;

    private static int chart_Limit_data=10;
    private static Date chart_data_Date=new Date();
    private static Date chart_click_time;
    private static Date chart_click_timelu;

    private static Date Alarm_click_time;
    private static Date Val_click_time;
    private static boolean updateview=false;
    private static boolean sizeChanged=false;
    private static boolean simpleupdate=false;

    private static Bitmap mBitmap;
    public static Canvas mCanvas;
    private Path mPath1;
    private Path mPath2;
    private Path mPath3;
    private Path mPath4;
    private static boolean Nachtmodus=false; // Stellt die Uhr funktion um 0=0-24h  1=12-12 (Nacht im zusammenhang)

    Context context;
    private Paint mPaint1;
    private Paint mPaint2;
    private Paint mPaint3;
    private Paint mPaint4;
    private static final float TOLERANCE = 5;
    private float oldX;
    static boolean Alarm_quitt=false;

    static int tagewischen=0;
    static float lastSGVpos;
    static float lasttimepos;

    ArrayList<Path> Pathlist = new ArrayList<Path>();
    ArrayList<Paint> Paintlist = new ArrayList<Paint>();

    public CanvasView(Context c, AttributeSet attrs) {
        super(c, attrs);
        context = c;

        // we set a new Path
        mPath1 = new Path();
        mPath2 = new Path();
        mPath3 = new Path();
        mPath4 = new Path();


        mPaint1 = new Paint();
        mPaint2 = new Paint();
        mPaint3 = new Paint();
        mPaint4 = new Paint();

        mPaint1.setAntiAlias(true);
        mPaint1.setColor(Color.WHITE);
        mPaint1.setStyle(Paint.Style.STROKE);
        mPaint1.setStrokeJoin(Paint.Join.ROUND);
        mPaint1.setStrokeWidth(2);
        mPaint1.setTextSize(12);

        // Punkte
        mPaint2.setAntiAlias(true);
        mPaint2.setColor(Color.GRAY);
        mPaint2.setStyle(Paint.Style.STROKE);
        mPaint2.setStrokeJoin(Paint.Join.ROUND);
        mPaint2.setStrokeWidth(3);
        mPaint2.setStrokeMiter(1);

        // Line die die Werte verbindet
        mPaint3.setAntiAlias(true);
        mPaint3.setColor(Color.GRAY);
        mPaint3.setStyle(Paint.Style.STROKE);
        mPaint3.setStrokeJoin(Paint.Join.ROUND);
        mPaint3.setStrokeWidth(1);

        //  horizontale und vertikale Scalen
        mPaint4.setAntiAlias(true);
        mPaint4.setColor(Color.GRAY);
        mPaint4.setStyle(Paint.Style.STROKE);
        mPaint4.setStrokeJoin(Paint.Join.ROUND);
        mPaint4.setStrokeWidth(0);
        mPaint4.setPathEffect(new DashPathEffect(new float[]{   1, 2}, 0));
        mPaint4.setTextSize(12);



    }
    public interface InterestingEvent
    {
        // This is just a regular method so it can return something or
        // take arguments if you like.
        public void interestingEvent ();
    }

    public class EventNotifier
    {
        private InterestingEvent ie;
        private boolean somethingHappened;
        public EventNotifier (InterestingEvent event)
        {
            // Save the event object for later use.
            ie = event;
            // Nothing to report yet.
            somethingHappened = false;
        }
        //...
        public void doWork ()
        {
            // Check the predicate, which is set elsewhere.
            if (somethingHappened)
            {
                // Signal the even by invoking the interface's method.
                ie.interestingEvent ();
            }
            //...
        }
        // ...
    }

            // override onSizeChanged
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        if (w!=Class_w & h!=Class_h) {
            sizeChanged=true;

            Class_w = w;
            Class_h = h;

            chart_top=15;
            chart_left=0;
            chart_height=h-chart_top-10;
            chart_width=w;


            Log.d(TAG, "onSizeChanged w:" + w + " h:" + h + " oldw: " + oldw + " oldh: " + oldh);
            Log.d(TAG, "onSizeChanged Class_w:" + Class_w + " Class_h:" + Class_h);
        }  else {
            Log.d(TAG, "onSizeChanged w:" + w + " h:" + h + "  falscher Alarm !!!!!!!!!!!!!");
        }


    }




    // override onDraw
    @Override
    protected void onDraw(Canvas canvas) {
        Log.d(TAG, "action onDraw");
        super.onDraw(canvas);
        // draw the mPath with the mPaint on the canvas when onDraw


        mCanvas = canvas;
        mCanvas.save();

        settext(mCanvas);

        // setChart();
        mCanvas.drawPath(mPath1, mPaint1);
        mCanvas.drawPath(mPath2, mPaint2);
        mCanvas.drawPath(mPath3, mPaint3);
        mCanvas.drawPath(mPath4, mPaint4);


    }



    private void dummy() {

        Cursor c;
        Date dateakt = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("EEE dd.MM.yyyy - HH:mm:ss");

        // long dateaktlng = dateakt.getTime() - 60000;

        long dateaktlng = dateakt.getTime();

        Log.d(TAG,"Action DatenSQL.getentries dateakat1.1 " + formatter.format(dateaktlng) + " timeval: " + dateaktlng) ;
        Log.d(TAG,"Action DatenSQL.getentries dateakat1.2: " + formatter.format(dateaktlng-get0hourofday(dateaktlng)) + " timeval: " + (dateaktlng-get0hourofday(dateaktlng)));

        c = DatenSQL.getentries(dateaktlng-get0hourofday(dateaktlng));

        Date dateakt2 = new Date();
        long dateaktlng2 = dateakt2.getTime();
        Log.d(TAG,"Action DatenSQL.getentries dateakat2 " + (dateaktlng2-dateaktlng)) ;

        while (c.moveToNext()) {
            setChartPoint(c.getLong(c.getColumnIndex("date")), c.getLong(c.getColumnIndex("sgv")));
            mCanvas.drawPath(mPath2, mPaint2);
        }
        c.close();

    }



            //override the onTouchEvent
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                oldX=x;
                Motion_click(x,y,event);
                break;
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_UP:
                Motion_click(x,y,event);
                Motion_ActionMove(x,y);
                break;
            case MotionEvent.ACTION_OUTSIDE:
                //Motion_ActionMove(x,y);
                break;
        }
        return true;
    }

    private boolean Motion_click(float X, float Y, MotionEvent e) {
        Date ClickDate = new Date();
        Log.d(TAG,"Motion_click X:" + X + " Y:" + Y );

        // double click event abfragen/ selbst generiert
        if (X>0 & X<(Class_w/3) & Y>0 & Y< (Class_h/7) ) {
            if (e.getAction()==MotionEvent.ACTION_UP) {
                if (!(Alarm_click_time == null)) {
                    if (Alarm_click_time.getTime()+1000 > ClickDate.getTime() ) {
                        Log.d(TAG, "Motion_click Alarm Quit ACTION_DOWN  within 1 sec.");
                        Log.d(TAG, "Motion_click Alarm Quit");
                        Alarm_quitt = true;

                    }
                }
            }
            if (e.getAction()==MotionEvent.ACTION_DOWN) {
                Alarm_click_time=new Date();
            }

        }


        // double click event abfragen/ selbst generiert
        if (X>150 & X<chart_width & Y>0 & Y< 150 ) {
            if (e.getAction()==MotionEvent.ACTION_UP) {
                if (!(Val_click_time == null)) {
                    if (Val_click_time.getTime()+1000 > ClickDate.getTime() ) {
                        Log.d(TAG, "Motion_click Value Double Click within 0,5 sec.");
                        updateview=true;
                   }
                }
            }
            if (e.getAction()==MotionEvent.ACTION_DOWN) {
                Val_click_time=new Date();
            }

        }



        // double click event abfragen/ selbst generiert
        if (Y<chart_height+chart_top & Y>chart_top & X>chart_left & X< chart_left+chart_width & (e.getAction()==MotionEvent.ACTION_UP)) {
            Log.d(TAG, "Motion_click Chart");

            if (!(chart_click_time == null)) {
                if (chart_click_time.getTime()+500 > ClickDate.getTime() ) {
                    Log.d(TAG, "Motion_click Chart Double Click within 0,5 sec.");
                    // TO DO something
                    chart_Limit_data=300;

                }
            }
            chart_click_time=new Date();
        }



        if (Y>(chart_top+(chart_height/4*3)) & X<(chart_left + (chart_width/4)) & (e.getAction()==MotionEvent.ACTION_UP)) {
            Log.d(TAG, "Motion_click Chart Links unten");

            if (!(chart_click_timelu == null)) {
                if (chart_click_timelu.getTime()+500 > ClickDate.getTime() ) {
                    Log.d(TAG, "Motion_click Chart Links unten Double Click within 0,5 sec.");
                    // TO DO something
                    toogleNachtmodus();

                }
            }
            chart_click_timelu=new Date();
        }

        return true;
    }
    private boolean Motion_ActionMove(float X, float Y) {
        // Log.d(TAG,"Motion_ActionMove :X"  + X + " Y:" + Y + " public_w/2 " + (public_w/2));

        if (oldX > (X+(Class_w/3))) {
            Log.d(TAG,"Motion_ActionMove Ein links Wisch" );
            tagewischen--;
            simpleupdate=true;
            if (tagewischen<0) {tagewischen=0;}
        }
        if (oldX < (X-(Class_w/3))) {
            Log.d(TAG,"Motion_ActionMove Ein rechts Wisch" );
            tagewischen++;
            simpleupdate=true;


        }
        Log.d(TAG,"Motion_ActionMove Tagwischen: " + tagewischen );
        return true;
    }

    //
    // chart_Limit_data zurückgeben (Mongo Aufruf !!)
    //
    public int getChart_Limit_data() {
        // Log.d(TAG,"getChart_Limit_data : " + chart_Limit_data);
        return chart_Limit_data;
    }

    public boolean resetAlarmquitt() {
        Alarm_quitt=false;
        return true;
    }

    public boolean getAlarmQuitt(){
        // Log.d(TAG,"getAlarmQuitt val:" + Alarm_quitt );
        return Alarm_quitt;
    }


    public boolean settext(Canvas canvas) {
        Log.d(TAG,"settext");


        long top=chart_top;
        long left=chart_left;
        long width=chart_width;
        long height=chart_height;

        long x1 = left;
        long x2 = left + width;
        long y1 = top+height;
        long y2 = top;
        long ymax = height;
        long xmax = x2-x1;

        float scaleymin = 0;
        float scaleymax = 400;

        float scalexmin = 0;
        float scalexmax= 24;

        float factory = ymax/(scaleymax-scaleymin);

        float factorx = xmax/scalexmax;


        // --------------- erste Horizontale Linie
        float scaleypos1 = y1 - scaleymin - 60 * factory;
        mPath4.moveTo(left+ 10, scaleypos1);
        canvas.drawText("60", 5, scaleypos1 - 5 , mPaint3);
        mPath4.lineTo(width, scaleypos1);

        // --------------- zweite Horizontale Linie
        float scaleypos2 = y1 - scaleymin - 80 * factory;
        canvas.drawText("80", 5, scaleypos2 - 5 , mPaint3);
        mPath4.moveTo(left+ 10, scaleypos2);
        mPath4.lineTo(width, scaleypos2);

        // --------------- dritte Horizontale Linie
        float scaleypos3 = y1 - scaleymin - 120 * factory;
        canvas.drawText("120", 0, scaleypos3 - 5 , mPaint3);
        mPath4.moveTo(left+ 10, scaleypos3);
        mPath4.lineTo(width, scaleypos3);

        // --------------- vierte Horizontale Linie
        float scaleypos4 = y1 - scaleymin - 160 * factory;
        canvas.drawText("160", 0, scaleypos4 - 5 , mPaint3);
        mPath4.moveTo(left+ 10, scaleypos4);
        mPath4.lineTo(width, scaleypos4);

        // --------------- fünfte Horizontale Linie
        float scaleypos5 = y1 - scaleymin - 250 * factory;
        canvas.drawText("250", 0, scaleypos5 - 5 , mPaint3);
        mPath4.moveTo(left+ 10, scaleypos5);
        mPath4.lineTo(width, scaleypos5);

        // --------------- sechste Horizontale Linie
        float scaleypos6 = y1 - scaleymin - 350 * factory;
        canvas.drawText("350", 0, scaleypos6 - 5 , mPaint3);
        mPath4.moveTo(left+ 10, scaleypos6);
        mPath4.lineTo(width, scaleypos6);

        // --------------- erste Vertikale  Linie
        float scalexpos1 = 6 * factorx; // 6:00 h
        mPath4.moveTo(scalexpos1, y2);
        mPath4.lineTo(scalexpos1, y1);


        // --------------- zweite Vertikale  Linie
        float scalexpos2 = 12 * factorx; // 12:00 h
        mPath4.moveTo(scalexpos2, y2);
        mPath4.lineTo(scalexpos2, y1);

        // --------------- dritte Vertikale  Linie
        float scalexpos3 = 18 * factorx; // 18:00 h
        mPath4.moveTo(scalexpos3, y2);
        mPath4.lineTo(scalexpos3, y1);

        if (Nachtmodus==false) {
            canvas.drawText("6:00h",  scalexpos1 + 5, y2 , mPaint3);
            canvas.drawText("12:00h", scalexpos2 + 5, y2 , mPaint3);
            canvas.drawText("18:00h", scalexpos3 + 5, y2 , mPaint3);
        } else {
            canvas.drawText("18:00h", scalexpos1 + 5, y2 , mPaint3);
            canvas.drawText("0:00h",  scalexpos2 + 5, y2 , mPaint3);
            canvas.drawText("6:00h",  scalexpos3 + 5, y2 , mPaint3);
        }

        Date dateakt = new Date();
        long dateaktlng = dateakt.getTime();
        long dummylong = get0hourofday(dateaktlng);
        SimpleDateFormat formatter = new SimpleDateFormat("EEE dd.MM.yyyy");

        Log.d(TAG,"DatenSQL.getentries dummylong " + formatter.format(dateakt.getTime()-dummylong) + " Nachtmodus: " + Nachtmodus) ;

        String modus="";

        if (Nachtmodus==false) {
            modus=" 0-24Uhr ";
        } else {
            modus=" 12-12Uhr ";
        }


        // --------------- unten links Historie eintragen
        if (tagewischen >0) {
            canvas.drawText(formatter.format(dateakt.getTime()-dummylong) + modus + " (-" + tagewischen + ")", 5, y1 -5 , mPaint3);
        } else {
            canvas.drawText(formatter.format(dateakt.getTime()-dummylong) + modus, 5, y1 -5 , mPaint3);
        }


        // --------------- Grundrahemen malen
        mPath1.moveTo(width, top);
        mPath1.lineTo(width, height + top);
        mPath1.lineTo(left, height + top);




        Log.d(TAG,"settext w:" + Class_w +" h:" + Class_h );


        return true;
    }


    //  ^
    //  |
    // y2
    //
    //
    // y1:x1      x2->
    //
    public boolean setChartPoint(long date, long sgv) {


        long aktdate = get0hourofday(date);
        Path mypath = new Path();

        long top=chart_top;
        long left=chart_left;
        long width=chart_width;
        long height=chart_height;

        long x1 = left;
        long x2 = width;
        long y1 = top+height;
        long y2 = top;
        long ymax = height;
        long xmax = x2-x1;

        float scaleymin = 0;  // BZ werte Bereich
        float scaleymax = 400;

        float scalexmin = 0;  // Zeit Werte Bereich
        float scalexmax= 60*60*24*1000;

        float factory = ymax/(scaleymax-scaleymin);

        float factorx = xmax/(scalexmax-scalexmin);

        float svgpos=0;
        float timepos=0;


        if (sgv<=30) {return  true;}
        if (sgv>scaleymax-5) {sgv=(long)(scaleymax-5);};
        if (aktdate<0) {return true;}

        timepos = x1 + aktdate * factorx;
        svgpos = y1 - scaleymin - sgv * factory;

        // Log.d(TAG,"setChartPoint SGV: (" + sgv + ") SGV POS: (" +  (svgpos) + ") timepos: (" + timepos +") date (" + date+")");
        // mPaint2.setStrokeWidth(2);
        mPaint2.setColor(Color.GREEN);
        if (sgv>180)
            mPaint2.setColor(Color.YELLOW);
        if (sgv<80)
            mPaint2.setColor(Color.RED);

        mPaint2.setColor(Color.GREEN);

        mPath2.moveTo(timepos, svgpos);
        mPath2.lineTo(timepos, svgpos +1);
        mPath2.lineTo(timepos + 1, svgpos);
        mPath2.lineTo(timepos , svgpos - 1);
        mPath2.lineTo(timepos - 1, svgpos - 1);
        mPath2.lineTo(timepos, svgpos +1);
        mPath2.close();


        if (lasttimepos>0 && lastSGVpos>0) {
            mPath3.moveTo(lasttimepos,lastSGVpos);
            mPath3.lineTo(timepos, svgpos);
        }

        lastSGVpos = svgpos;
        lasttimepos = timepos;

        return true;
    }

    public boolean getupdateview() {
        if (updateview==true) {
            Log.d(TAG,"getupdateview ausgelöst !!" );
            updateview=false;
            return true;
        } else {
            return false;
        }
    }

    public boolean getSizeChanged() {
        if (sizeChanged==true) {
            Log.d(TAG,"getsizeChanged ausgelöst !!" );
            sizeChanged=false;
            return true;
        } else {
            return false;
        }
    }

    public boolean getsimpleupdate() {
        if (simpleupdate==true) {
            Log.d(TAG,"simpleupdate ausgelöst !!" );
            simpleupdate=false;
            return true;
        } else {
            return false;
        }
    }


    public long get0hourofday(long date) {

        long eintaginmillis=1000*60*60*24;
        long v12hinmillisec=1000*60*60*12;
        long ret=0;


        // Aktuelles Datum
        Date akt = new Date();

        GregorianCalendar calender = new GregorianCalendar();

        // ------------------------   Die Aktuelle Stunde Minute sekunde und Millisekunde errechnen
        int millis = calender.get(GregorianCalendar.MILLISECOND);
        int sec = calender.get(GregorianCalendar.SECOND);
        int min = calender.get(GregorianCalendar.MINUTE);
        int hour = calender.get(GregorianCalendar.HOUR_OF_DAY);

        long timeToPass = millis + (sec * 1000) + (min * 1000 * 60) + (hour * 1000 * 60 * 60);



        // ------ Die übergebene Zeit - Aktuelle Zeit
        //                            - zurück auf heute 0:00.0000 Uhr
        //                            - Zeitkorektur vom Tageweise Blättern



        if (Nachtmodus) {
            if (hour<18) {
                ret = (date - (1 + akt.getTime() - timeToPass - (eintaginmillis * tagewischen)) + v12hinmillisec);
            } else {
                ret = (date - (1 + akt.getTime() - timeToPass - (eintaginmillis * tagewischen)) - v12hinmillisec);
            }
        } else {
            ret= (date - (1+ akt.getTime() - timeToPass  -(eintaginmillis*tagewischen)));
        }

        return  ret;
    }



    public void refresh() {
        // --------------- Damit beim Grafik wechsel kein Strick durch geht
        lastSGVpos=0;
        lasttimepos=0;
        // invalidate();

        Log.d(TAG,"Action refresh");

        if (!(mCanvas==null)) {
            Log.d(TAG,"Action refresh mCanvas isset");
            // settext(mCanvas);
            // setChart();
            // mCanvas.drawPath(mPath1, mPaint1);
            // mCanvas.drawPath(mPath3, mPaint3);
            // mCanvas.drawPath(mPath2, mPaint2);
            // mCanvas.drawPath(mPath4, mPaint4);
        }

    }



    public boolean toogleNachtmodus(){

        if (Nachtmodus==false) {
            Nachtmodus=true;
        } else {
            Nachtmodus=false;
        }
        simpleupdate=true;
        Log.d(TAG,"Motion_click  toggleNachtmodus val: " + Nachtmodus);
        return Nachtmodus;
    }


}

