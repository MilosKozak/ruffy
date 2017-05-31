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

        //FIXME add Translations
        return null;
    }
}
