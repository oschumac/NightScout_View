package de.schumis_net.olli.nightscout_view;

import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.RemoteViews;


import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;

import static java.lang.System.currentTimeMillis;


/**
 * Implementation of App Widget functionality.
 */
public class NewAppWidget2 extends AppWidgetProvider {
    private static final String TAG = MainActivity.class.getSimpleName();



    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        Log.d(TAG," Widget onUpdate durchlaufen !");
        // GetMongodata();


        final int N = appWidgetIds.length;
        for (int i = 0; i < N; i++) {
            int appWidgetId = appWidgetIds[i];

            Intent dataDownloadIntent = new Intent(context, RemoteIntentService.class);
            dataDownloadIntent.putExtra("WIDGET_ID", appWidgetId);
            context.startService(dataDownloadIntent);

            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    public class RemoteIntentService extends IntentService {
        public RemoteIntentService() {
            super("RemoteIntentService");
            Log.d(TAG," Widget RemoteIntentService 1");
        }

        @Override
        protected void onHandleIntent(Intent intent) {
            //ProductData product = new FakeRemoteImpl().getTopProduct();
            //Bitmap productBitmap = getImage(product.imageURL); //lädt ein Bild via URL
            //Bitmap roundedBitmap = null;
            //if (productBitmap != null)
            //    roundedBitmap = ImageHelper.getRoundedCornerBitmap(productBitmap, 30);

            Log.d(TAG," Widget onHandleIntent 1");
            Intent dataAvailable = new Intent("de.schumis_net.olli.nightscout_view.DATA_AVAILABLE");
            // dataAvailable.putExtra("product", "");

            Log.d(TAG," Widget onHandleIntent 2");
            dataAvailable.putExtra("WIDGET_ID", intent.getIntExtra("WIDGET_ID", -1));
            Log.d(TAG," Widget onHandleIntent 3");
            sendBroadcast(dataAvailable);
            Log.d(TAG," Widget onHandleIntent 4");
        }

    }


    // Das ist der GUI Händler für den Timer Thread
    //
    //
    private Handler handler = new Handler() {
        private int myNum;
        public void handleMessage(android.os.Message msg) {


            //RemoteViews views = new RemoteViews(R.id.textView_1, R.layout.new_app_widget2);

            Bundle bundle = msg.getData();
            Log.i(TAG, "Hamdler SGV empfangen:" + bundle.getString("sgv"));
            Log.i(TAG, "Handler datestring: empfangen" + bundle.getString("dateString"));
            Log.i(TAG, "Handler dategmt: empfangen" + bundle.getString("date"));

            //TextView myTextView_1 = (TextView)findViewById(R.id.textView_1);
            //myTextView_1.setText(bundle.getString("sgv")+ " " + getarrows(bundle.getString("direction")));

            myNum = Integer.parseInt(bundle.getString("sgv"));
            //myTextView_1.setTextColor(Color.WHITE);
            if(myNum<80) {
                //myTextView_1.setTextColor(Color.RED);
            }
            if(myNum>160) {
                //myTextView_1.setTextColor(Color.BLUE);
            }

            //TextView myTextViewdate1 = (TextView)findViewById(R.id.textView_Date1);
            //myTextViewdate1.setText(bundle.getString("dateString"));
        }
    };


    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
        Log.d(TAG," Widget onEnabled durchlaufen !");
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
        Log.d(TAG," Widget onDisabled durchlaufen !");
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        CharSequence widgetText = context.getString(R.string.appwidget_text);
        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.new_app_widget2);
        views.setTextViewText(R.id.textView_Date1, widgetText);

        Log.d(TAG, "Widget updateAppWidget durchlaufen !!");
        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);


    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!intent.getAction().equals("de.flavor.android.widgetdemo.DATA_AVAILABLE"))
            super.onReceive(context, intent);
        else
        {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

            int appWidgetId = intent.getIntExtra("WIDGET_ID", -1);

            RemoteViews views = new RemoteViews(context.getPackageName(),
                    R.layout.new_app_widget2);


            //ProductData product = (ProductData)intent.getSerializableExtra("product");
            //views.setImageViewBitmap(R.id.image, (Bitmap)intent.getParcelableExtra("productBitmap"));
            //views.setTextViewText(R.id.text, product.description);
            //views.setTextViewText(R.id.price, product.formattedPrice);

            //Intent to launch flickr.com, just for demo, just for image
            Intent i = new Intent(Intent.ACTION_VIEW);
            //i.setData(Uri.parse("http: //www.flickr.com"));
            //PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, i, 0);
            //views.setOnClickPendingIntent(R.id.image, pendingIntent);

            //update
            appWidgetManager.updateAppWidget(appWidgetId, views);

        }
    }




    // funktion zum Abfragen der Mongodaten
    //
    //
    private long GetMongodata() {

        String dbURI = "mongodb://Lars_2009:Lars65535@ds056727.mongolab.com:56727/larscgm";
        Log.i(TAG, dbURI);
        String collectionName = "entries";
        String dsCollectionName = "devicestatus";
        int sTime = 10000;
        long v_myTimeUTC;

        Log.d(TAG," Widget GetMongoData durchlaufen !!!");
        if (dbURI != null && collectionName != null) {
            try {

                // connect to db
                MongoClientURI uri = new MongoClientURI(dbURI.trim());
                MongoClient client = new MongoClient(uri);

                // get db
                DB db = client.getDB(uri.getDatabase());


// get a list of the collections in this database and print them out
//                Set<String> collectionNames = db.getCollectionNames();
//                for (final String s : collectionNames) {
//                    Log.i (TAG,"MongoDB Collections "+s);
//                }

                DBCollection coll = db.getCollection(collectionName);

                BasicDBObject query = new BasicDBObject("type", "sgv");
                // { "_id" : { "$oid" : "54a3ef7a3777e50db741594c"} , "device" : "dexcom" , "date" : 1420029696000 , "dateString" : "Wed Dec 31 13:41:36 MEZ 2014" , "sgv" : 127 ,
                //  "direction" : "Flat" , "type" : "sgv" , "filtered" : 169280 , "unfiltered" : 166048 , "rssi" : 175}
                DBCursor Cursor = coll.find().sort(new BasicDBObject("date", -1)).limit(1);


                try {
                    while (Cursor.hasNext()) {
                        DBObject theObj = Cursor.next();
                        Log.i(TAG, "MongoDB Gelesen Datensatz :" + theObj.toString());

                        if (theObj.get("type").toString().equals("sgv")) {
                            Log.i(TAG, "MongoDB Gelesen sgv :" + theObj.get("sgv").toString());
                            Log.i(TAG, "MongoDB Gelesen datestring:" + theObj.get("dateString").toString());
                            Log.i(TAG, "MongoDB Gelesen date :" + theObj.get("date").toString());
                            Log.i(TAG, "MongoDB Gelesen direction :" + theObj.get("direction").toString());
                            Log.i(TAG, "MongoDB Gelesen type:" + theObj.get("type").toString());
                            Log.i(TAG, "MongoDB Gelesen filtered:" + theObj.get("filtered").toString());
                            Log.i(TAG, "MongoDB Gelesen unfiltered:" + theObj.get("unfiltered").toString());
                            Log.i(TAG, "MongoDB Gelesen rssi:" + theObj.get("rssi").toString());
                            Log.i(TAG, "MongoDB Gelesen device :" + theObj.get("device").toString());

                            Message message = new Message();
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

                            // alle 5 Minuten und eine sekunde ab der lezten Messung neu anfragen
                            sTime = (int) (301000 - (currentTimeMillis() - Long.valueOf(theObj.get("date").toString()).longValue()));
                            Log.i(TAG, "Zeitdifferenz : " + sTime / 1000 + " Sekunden");
                            if (sTime < 0 || sTime > 301000) {
                                sTime = 10000;
                            }


                            Log.i(TAG, "Neue anfrage in : " + sTime / 1000 + " Sekunden");

                            message.setData(bundle);
                            handler.sendMessage(message);
                        }
                    }
                } finally {
                    Cursor.close();
                }

                client.close();

                Log.i(TAG, "MongoDB gelesen fertig");
                return sTime;

            } catch (Exception e) {
                Log.e(TAG, "MongoDB Fehler !!! Code: ", e);
            }
        }
        return sTime;
    }

}





