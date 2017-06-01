package org.monkey.d.ruffy.ruffy.driver.display.menu;

import org.monkey.d.ruffy.ruffy.driver.display.MenuType;

/**
 * Created by fishermen21 on 22.05.17.
 */

class TitleResolver {
    public static Title resolve(String title) {

        /**english titles**/
        if(title.equalsIgnoreCase("bolus amount"))
            return Title.BOLUS_AMOUNT;
        if(title.equalsIgnoreCase("immediate bolus"))
            return Title.IMMEDIATE_BOLUS;
        if(title.equalsIgnoreCase("bolus duration"))
            return Title.BOLUS_DURATION;
        if(title.equalsIgnoreCase("quick info"))
            return Title.QUICK_INFO;
        if(title.equalsIgnoreCase("bolus data"))
            return Title.BOLUS_DATA;
        if(title.equalsIgnoreCase("error data"))
            return Title.ERROR_DATA;
        if(title.equalsIgnoreCase("daily totals"))
            return Title.DAILY_TOTALS;
        if(title.equalsIgnoreCase("tbr data"))
            return Title.TBR_DATA;
        if(title.equalsIgnoreCase("tbr percentage"))
            return Title.TBR_SET;
        if(title.equalsIgnoreCase("tbr duration"))
            return Title.TBR_DURATION;

        /**german titles**/
        if(title.equalsIgnoreCase("bolus-menge"))
            return Title.BOLUS_AMOUNT;
        if(title.equalsIgnoreCase("sofortige abgabe"))
            return Title.IMMEDIATE_BOLUS;
        if(title.equalsIgnoreCase("abgabedauer"))
            return Title.BOLUS_DURATION;
        if(title.equalsIgnoreCase("quick info"))
            return Title.QUICK_INFO;
        if(title.equalsIgnoreCase("bolusinformation"))
            return Title.BOLUS_DATA;
        if(title.equalsIgnoreCase("fehlermeldungen"))
            return Title.ERROR_DATA;
        if(title.equalsIgnoreCase("tagesgesamtmenge"))
            return Title.DAILY_TOTALS;
        if(title.equalsIgnoreCase("tbr-information"))
            return Title.TBR_DATA;
        if(title.equalsIgnoreCase("tbr wert"))
            return Title.TBR_SET;
        if(title.equalsIgnoreCase("tbr dauer"))
            return Title.TBR_DURATION;

        /**French titles**/
        if(title.equalsIgnoreCase("quantité bolus"))
            return Title.BOLUS_AMOUNT;
        if(title.equalsIgnoreCase("quanti. immédiate"))
            return Title.IMMEDIATE_BOLUS;
        if(title.equalsIgnoreCase("durée du bolus"))
            return Title.BOLUS_DURATION;
        if(title.equalsIgnoreCase("quick info"))
            return Title.QUICK_INFO;
        if(title.equalsIgnoreCase("bolus"))
            return Title.BOLUS_DATA;
        if(title.equalsIgnoreCase("erreurs"))
            return Title.ERROR_DATA;
        if(title.equalsIgnoreCase("quantités journ."))
            return Title.DAILY_TOTALS;
        if(title.equalsIgnoreCase("dbt"))
            return Title.TBR_DATA;
        if(title.equalsIgnoreCase("valeur du dbt"))
            return Title.TBR_SET;
        if(title.equalsIgnoreCase("durée du dbt"))
            return Title.TBR_DURATION;


        /**spanish titles**/
        if(title.equalsIgnoreCase("cantidad de bolo"))
            return Title.BOLUS_AMOUNT;
        if(title.equalsIgnoreCase("bolo inmediato"))
            return Title.IMMEDIATE_BOLUS;
        if(title.equalsIgnoreCase("duración de bolo"))
            return Title.BOLUS_DURATION;
        if(title.equalsIgnoreCase("quick info"))
            return Title.QUICK_INFO;
        if(title.equalsIgnoreCase("datos de bolo"))
            return Title.BOLUS_DATA;
        if(title.equalsIgnoreCase("datos de error"))
            return Title.ERROR_DATA;
        if(title.equalsIgnoreCase("totales diarios"))
            return Title.DAILY_TOTALS;
        if(title.equalsIgnoreCase("datos de dbt"))
            return Title.TBR_DATA;
        if(title.equalsIgnoreCase("porcentaje dbt"))
            return Title.TBR_SET;
        if(title.equalsIgnoreCase("duración de dbt"))
            return Title.TBR_DURATION;


        /**italian titles**/
        if(title.equalsIgnoreCase("quantita bolo")) //multiwave 1
            return Title.BOLUS_AMOUNT;
        if(title.equalsIgnoreCase("bolo immediato")) //multiwave 2
            return Title.IMMEDIATE_BOLUS;
        if(title.equalsIgnoreCase("tempo erogazione")) //multiwave 3
            return Title.BOLUS_DURATION;
        if(title.equalsIgnoreCase("quick info")) //check1
            return Title.QUICK_INFO;
        if(title.equalsIgnoreCase("memoria boli")) //check2, mydata 1
            return Title.BOLUS_DATA;
        if(title.equalsIgnoreCase("memoria allarmi")) //mydata 2
            return Title.ERROR_DATA;
        if(title.equalsIgnoreCase("totali giornata")) //mydata 3
            return Title.DAILY_TOTALS;
        if(title.equalsIgnoreCase("memoria pbt")) //mydata 4
            return Title.TBR_DATA;
        if(title.equalsIgnoreCase("percentuale pbt")) //TBR 1
            return Title.TBR_SET;
        if(title.equalsIgnoreCase("durata pbt")) //TBR 2
            return Title.TBR_DURATION;

        /**dutch titles**/
        if(title.equalsIgnoreCase("bolushoeveelheid")) //multiwave 1
            return Title.BOLUS_AMOUNT;
        if(title.equalsIgnoreCase("directe bolus")) //multiwave 2
            return Title.IMMEDIATE_BOLUS;
        if(title.equalsIgnoreCase("bolusduur")) //multiwave 3
            return Title.BOLUS_DURATION;
        if(title.equalsIgnoreCase("quick info")) //check1
            return Title.QUICK_INFO;
        if(title.equalsIgnoreCase("bolusgegevens")) //check2, mydata 1
            return Title.BOLUS_DATA;
        if(title.equalsIgnoreCase("foutengegevens")) //mydata 2
            return Title.ERROR_DATA;
        if(title.equalsIgnoreCase("dagtotalen")) //mydata 3
            return Title.DAILY_TOTALS;
        if(title.equalsIgnoreCase("tbd-gegevens")) //mydata 4
            return Title.TBR_DATA;
        if(title.equalsIgnoreCase("tbd-percentage")) //TBR 1
            return Title.TBR_SET;
        if(title.equalsIgnoreCase("tbd-duur")) //TBR 2
            return Title.TBR_DURATION;

        /**norwegian titles**/
        if(title.equalsIgnoreCase("bolusmengde")) //multiwave 1
            return Title.BOLUS_AMOUNT;
        if(title.equalsIgnoreCase("umiddelbar bolus")) //multiwave 2
            return Title.IMMEDIATE_BOLUS;
        if(title.equalsIgnoreCase("bolusvarighet")) //multiwave 3
            return Title.BOLUS_DURATION;
        if(title.equalsIgnoreCase("quick info")) //check1
            return Title.QUICK_INFO;
        if(title.equalsIgnoreCase("bolusdata")) //check2, mydata 1
            return Title.BOLUS_DATA;
        if(title.equalsIgnoreCase("feildata")) //mydata 2
            return Title.ERROR_DATA;
        if(title.equalsIgnoreCase("døgnmengde")) //mydata 3
            return Title.DAILY_TOTALS;
        if(title.equalsIgnoreCase("mbd-data")) //mydata 4
            return Title.TBR_DATA;
        if(title.equalsIgnoreCase("mbd-prosent")) //TBR 1
            return Title.TBR_SET;
        if(title.equalsIgnoreCase("mbd-varighet")) //TBR 2
            return Title.TBR_DURATION;

        /**polish titles**/
        if(title.equalsIgnoreCase("wielkość bolusa")) //multiwave 1
            return Title.BOLUS_AMOUNT;
        if(title.equalsIgnoreCase("bolus natychm.")) //multiwave 2
            return Title.IMMEDIATE_BOLUS;
        if(title.equalsIgnoreCase("cz. trw. bolusa")) //multiwave 3
            return Title.BOLUS_DURATION;
        if(title.equalsIgnoreCase("quick info")) //check1
            return Title.QUICK_INFO;
        if(title.equalsIgnoreCase("dane bolusa")) //check2, mydata 1
            return Title.BOLUS_DATA;
        if(title.equalsIgnoreCase("dane błędu")) //mydata 2
            return Title.ERROR_DATA;
        if(title.equalsIgnoreCase("dzien. d. całk.")) //mydata 3
            return Title.DAILY_TOTALS;
        if(title.equalsIgnoreCase("dane tdp")) //mydata 4
            return Title.TBR_DATA;
        if(title.equalsIgnoreCase("procent tdp")) //TBR 1
            return Title.TBR_SET;
        if(title.equalsIgnoreCase("czas trwania tdp")) //TBR 2
            return Title.TBR_DURATION;

        /**cz titles**/
        if(title.equalsIgnoreCase("množství bolusu")) //multiwave 1
            return Title.BOLUS_AMOUNT;
        if(title.equalsIgnoreCase("okamžitý bolus")) //multiwave 2
            return Title.IMMEDIATE_BOLUS;
        if(title.equalsIgnoreCase("trvání bolusu")) //multiwave 3
            return Title.BOLUS_DURATION;
        if(title.equalsIgnoreCase("quick info")) //check1
            return Title.QUICK_INFO;
        if(title.equalsIgnoreCase("údaje bolusů")) //check2, mydata 1
            return Title.BOLUS_DATA;
        if(title.equalsIgnoreCase("údaje chyb")) //mydata 2
            return Title.ERROR_DATA;
        if(title.equalsIgnoreCase("celk. den. dávky")) //mydata 3
            return Title.DAILY_TOTALS;
        if(title.equalsIgnoreCase("údaje dbd")) //mydata 4
            return Title.TBR_DATA;
        if(title.equalsIgnoreCase("procento dbd")) //TBR 1
            return Title.TBR_SET;
        if(title.equalsIgnoreCase("trvání dbd")) //TBR 2
            return Title.TBR_DURATION;

        /**finnish titles**/
        if(title.equalsIgnoreCase("bolus mİktari")) //multiwave 1
            return Title.BOLUS_AMOUNT;
        if(title.equalsIgnoreCase("hemen bolus uygl")) //multiwave 2
            return Title.IMMEDIATE_BOLUS;
        if(title.equalsIgnoreCase("bolus süresİ")) //multiwave 3
            return Title.BOLUS_DURATION;
        if(title.equalsIgnoreCase("quick info")) //check1
            return Title.QUICK_INFO;
        if(title.equalsIgnoreCase("bolus verİlerİ")) //check2, mydata 1
            return Title.BOLUS_DATA;
        if(title.equalsIgnoreCase("hata verİlerİ")) //mydata 2
            return Title.ERROR_DATA;
        if(title.equalsIgnoreCase("günlük toplam")) //mydata 3
            return Title.DAILY_TOTALS;
        if(title.equalsIgnoreCase("gbh verİlerİ")) //mydata 4
            return Title.TBR_DATA;
        if(title.equalsIgnoreCase("gbh yüzdesİ")) //TBR 1
            return Title.TBR_SET;
        if(title.equalsIgnoreCase("gbh süresİ")) //TBR 2
            return Title.TBR_DURATION;

        /**romanian titles**/
        if(title.equalsIgnoreCase("cantitate bolus")) //multiwave 1
            return Title.BOLUS_AMOUNT;
        if(title.equalsIgnoreCase("bolus imediat")) //multiwave 2
            return Title.IMMEDIATE_BOLUS;
        if(title.equalsIgnoreCase("durată bolus")) //multiwave 3
            return Title.BOLUS_DURATION;
        if(title.equalsIgnoreCase("quick info")) //check1
            return Title.QUICK_INFO;
        if(title.equalsIgnoreCase("date bolus")) //check2, mydata 1
            return Title.BOLUS_DATA;
        if(title.equalsIgnoreCase("date eroare")) //mydata 2
            return Title.ERROR_DATA;
        if(title.equalsIgnoreCase("totaluri zilnice")) //mydata 3
            return Title.DAILY_TOTALS;
        if(title.equalsIgnoreCase("date rbt")) //mydata 4
            return Title.TBR_DATA;
        if(title.equalsIgnoreCase("procent rbt")) //TBR 1
            return Title.TBR_SET;
        if(title.equalsIgnoreCase("durata rbt")) //TBR 2
            return Title.TBR_DURATION;

        //FIXME add Translations
        return null;
    }
}
