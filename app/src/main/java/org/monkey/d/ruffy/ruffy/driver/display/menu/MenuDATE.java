package org.monkey.d.ruffy.ruffy.driver.display.menu;

/**
 * Created by fishermen21 on 24.05.17.
 */

public class MenuDATE {
    private final int day;
    private final int month;


    public MenuDATE(int day, int month) {
        this.day = day;
        this.month = month;
    }

    @Override
    public String toString() {
        return day+"."+String.format("%02d",month)+".";
    }
}
