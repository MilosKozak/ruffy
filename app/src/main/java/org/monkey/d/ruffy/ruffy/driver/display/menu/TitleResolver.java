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

        //FIXME add Translations
        return null;
    }
}
