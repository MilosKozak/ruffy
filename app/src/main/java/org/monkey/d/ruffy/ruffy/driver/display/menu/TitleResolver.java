package org.monkey.d.ruffy.ruffy.driver.display.menu;

import org.monkey.d.ruffy.ruffy.driver.display.MenuType;

/**
 * Created by fishermen21 on 22.05.17.
 */

class TitleResolver {
    public static Title resolve(String title) {
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
        if(title.equalsIgnoreCase("basal rate total"))
            return Title.BASAL_TOTAL;

        //FIXME add Translations
        return null;
    }
}
