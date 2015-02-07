package de.schumis_net.olli.nightscout_view;


import android.os.Bundle;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * Created by OLLI on 05.01.2015.
 */




public class DataCollection {
    private static final String TAG = DataCollection.class.getSimpleName();
    private static Bundle daten = new Bundle();
    private static ArrayList<Long> DatenListeDATE = new ArrayList<Long>();
    private static ArrayList<Long> DatenListeSGV = new ArrayList<Long>();
    private static boolean FlaggetSGV=false;
    private static boolean FlaggetDATE=false;
    private static boolean FlagADDLIST=false;

    public Bundle GETDATA(){
        return daten;
    }

    public  int SETDATA(Bundle bundle){
        daten = bundle;
        return 1;
    }

    //
    // muss noch Date prüfen ob schon in der Liste enthalten
    //
    public  int ADDLIST(long SGV,long Date){
        while(FlaggetDATE || FlaggetSGV) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        FlagADDLIST=true;

        DatenListeDATE.add(Date);
        DatenListeSGV.add(SGV);

        FlagADDLIST=false;
        return 1;
    }
    public ArrayList GETLISTDATE(){
        while(FlagADDLIST) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        FlaggetDATE=true;


        ArrayList<Long> DatenListe = new ArrayList<Long>();
        List DListe1= new ArrayList();
        DListe1 = DatenListeDATE;
        Iterator it = DListe1.iterator();

        while (it.hasNext() ) {
            DatenListe.add((long)it.next());

        }
        FlaggetDATE=false;
        return DatenListe;
    }

    public ArrayList GETLISTSGV(){
        while(FlagADDLIST) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        FlaggetSGV=true;

        ArrayList<Long> DatenListe = new ArrayList<Long>();
        List DListe1= new ArrayList();
        DListe1 = DatenListeSGV;
        Iterator it = DListe1.iterator();

        while (it.hasNext() ) {
            DatenListe.add((long)it.next());

        }
        FlaggetSGV=false;
        return DatenListe;
    }
}



//        List DListe1= new ArrayList();
//        DListe1 = Daten.GETLISTSGV();
//        List DListe2= new ArrayList();
//        DListe2 = Daten.GETLISTDATE();
//
//        Iterator it = DListe1.iterator();
//        Iterator it2 = DListe2.iterator();
//        long dummylong;
//
//        while (it.hasNext() & it2.hasNext()) {
//
//            //customCanvas.setChartPoint((long)it2.next(),(long)it.next(),mHeight,mWidth );
//            dummylong = (long)it2.next();
//            dummylong = (long)it.next();
//
//        }