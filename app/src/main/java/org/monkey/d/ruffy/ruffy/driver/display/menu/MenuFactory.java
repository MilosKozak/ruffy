package org.monkey.d.ruffy.ruffy.driver.display.menu;

import org.monkey.d.ruffy.ruffy.driver.display.Menu;
import org.monkey.d.ruffy.ruffy.driver.display.MenuAttribute;
import org.monkey.d.ruffy.ruffy.driver.display.MenuType;
import org.monkey.d.ruffy.ruffy.driver.display.Symbol;
import org.monkey.d.ruffy.ruffy.driver.display.Token;
import org.monkey.d.ruffy.ruffy.driver.display.menu.MenuTime;
import org.monkey.d.ruffy.ruffy.driver.display.parser.CharacterPattern;
import org.monkey.d.ruffy.ruffy.driver.display.parser.NumberPattern;
import org.monkey.d.ruffy.ruffy.driver.display.parser.Pattern;
import org.monkey.d.ruffy.ruffy.driver.display.parser.SymbolPattern;

import java.util.LinkedList;

/**
 * Created by fishermen21 on 22.05.17.
 */

public class MenuFactory {
    public static Menu get(LinkedList<Token>[] tokens) {
        Token first = tokens[0].getFirst();
        if(first != null && first.getPattern() instanceof SymbolPattern && ((SymbolPattern)first.getPattern()).getSymbol()== Symbol.CLOCK)
            return makeMainMenu(tokens);

        return null;
    }

    private static Menu makeMainMenu(LinkedList<Token>[] tokens) {
        Menu m = new Menu(MenuType.MAIN_MENU);
        LinkedList<Integer> time = new LinkedList<>();
        LinkedList<Integer> runtime = new LinkedList<>();
        LinkedList<Character> timeC = new LinkedList<>();
        boolean hasRunning=false;

        int stage = 0;
        while(tokens[0].size()>0)
        {
            Token t = tokens[0].removeFirst();
            Pattern p = t.getPattern();
            switch(stage)
            {
                case 0://clock
                    if(!isSymbol(p,Symbol.CLOCK))
                        return null;//wrong
                    stage++;
                    break;
                case 1://hour10
                case 2://hour1
                case 4://minute10
                case 5://minute1
                    if (p instanceof NumberPattern) {
                        time.add(((NumberPattern) p).getNumber());
                    } else
                        return null;//Wrong
                    stage++;
                    break;
                case 3://: or number (: blinks)
                    if(isSymbol(p,Symbol.SEPERATOR))
                    {
                        stage++;
                    }
                    else if (p instanceof NumberPattern) {
                        time.add(((NumberPattern) p).getNumber());
                        stage += 2;
                    }
                    else
                        return null;//wr
                    break;
                case 6://P(m), A(M), or running
                    if(p instanceof CharacterPattern) {
                        timeC.add(((CharacterPattern) p).getCharacter());
                        stage++;
                    } else if(isSymbol(p,Symbol.ARROW)) {
                        hasRunning = true;
                        stage = 9;
                    } else
                        return null;//wrong
                    break;
                case 7://it should be an M
                    if(p instanceof CharacterPattern) {
                        timeC.add(((CharacterPattern) p).getCharacter());
                        stage++;
                    } else
                        return null;//nothing else matters
                    break;
                case 8://can onbly be running arrow
                    if(isSymbol(p,Symbol.ARROW)) {
                        hasRunning = true;
                        stage++;
                    } else
                        return null;
                    break;
                case 9://h10
                case 10://h1
                case 12://m10
                case 13://m1
                    if (p instanceof NumberPattern) {
                        runtime.add(((NumberPattern) p).getNumber());
                    } else
                        return null;//Wrong
                    stage++;
                    break;
                case 11://: or number (: blinks)
                    if(isSymbol(p,Symbol.SEPERATOR))
                    {
                        stage++;
                    }
                    else
                        return null;//wr
                    break;
                default:
                    return null;//the impossible girl
            }
        }
        //set time
        int minute1 = time.removeLast();
        int minute10 = time.removeLast();
        int hour1 = time.removeLast();
        int hour10 = 0;
        if(time.size()>0)
            hour10 = time.removeLast();

        int tadd = 0;
        if(timeC.size()>0)
        {
            if(timeC.get(0)=='P' && timeC.get(1)=='M')
            {
                tadd += 12;
            }
        }
        m.setAttribute(MenuAttribute.TIME,new MenuTime((hour10*10)+tadd+hour1,(minute10*10)+minute1));

        if(hasRunning) {
            minute1 = runtime.removeLast();
            minute10 = runtime.removeLast();
            hour1 = runtime.removeLast();
            hour10 = runtime.removeLast();
            m.setAttribute(MenuAttribute.RUNTIME, new MenuTime((hour10 * 10) + hour1, (minute10 * 10) + minute1));
        }

        stage = 0;
        BolusType bt = null;
        int tbr = 0;
        LinkedList<Pattern> number = new LinkedList<>();

        while(tokens[1].size()>0) {
            Token t = tokens[1].removeFirst();
            Pattern p = t.getPattern();
            switch (stage) {
                case 0:
                    if (isSymbol(p, Symbol.SEPERATOR.LARGE_EXTENDED_BOLUS)) {
                        bt = BolusType.EXTENDED;
                        stage++;
                    } else if (isSymbol(p, Symbol.SEPERATOR.LARGE_MULTIWAVE)) {
                        bt = BolusType.MULTIWAVE_EXTENDED;
                        stage++;
                    }
                    else if(isSymbol(p,Symbol.SEPERATOR.LARGE_MULTIWAVE_BOLUS))
                    {
                        bt = BolusType.MULTIWAVE_BOLUS;
                        stage++;
                    }
                    else if(isSymbol(p,Symbol.SEPERATOR.LARGE_BOLUS))
                    {
                        bt = BolusType.NORMAL;
                        stage++;
                    }
                    else if (isSymbol(p, Symbol.SEPERATOR.LARGE_BASAL)) {
                        bt = null;
                        stage++;
                    } else {
                        return null;
                    }
                    break;
                case 1:
                    if (isSymbol(p, Symbol.UP)) {
                        tbr = 1;
                        stage++;
                    } else if (isSymbol(p, Symbol.DOWN)) {
                        tbr = 2;
                        stage++;
                    } else if (p instanceof NumberPattern) {
                        number.add(p);
                        stage += 2;
                    } else
                        return null;//
                    break;
                case 2:
                    if (p instanceof NumberPattern) {
                        number.add(p);
                        stage++;
                    } else
                        return null;//
                    break;
                case 3:
                    if (p instanceof NumberPattern) {
                        number.add(p);
                    } else if (p instanceof CharacterPattern && ((CharacterPattern)p).getCharacter()=='u') {
                        number.add(p);
                    } else if (isSymbol(p, Symbol.LARGE_DOT) || isSymbol(p, Symbol.LARGE_PERCENT) || isSymbol(p,Symbol.LARGE_UNITS_PER_HOUR)) {
                        number.add(p);
                    } else
                        return null;//
                    break;
            }
        }
        double doubleNUmber = 0d;
        String d = "";
        for(Pattern p : number)
        {
            if(p instanceof NumberPattern)
            {
                d+=""+((NumberPattern)p).getNumber();
            } else if(isSymbol(p,Symbol.LARGE_DOT)) {
                d += ".";
            } else if(isSymbol(p,Symbol.LARGE_PERCENT) ||
                    isSymbol(p,Symbol.LARGE_UNITS_PER_HOUR) ||
                    (p instanceof CharacterPattern && ((CharacterPattern)p).getCharacter()=='u')){
                //irgnore
            } else {
                return null;//violation!
            }
        }
        try { doubleNUmber = Double.parseDouble(d);}
        catch (Exception e){return null;}//violation, there must something parseable

        if(bt != null)
        {
            //running bolus
            m.setAttribute(MenuAttribute.BOLUS,bt+" bolus running");
            m.setAttribute(MenuAttribute.BOLUS_REMAINING,doubleNUmber+" bolus remaining");
        }
        else
        {
            switch(tbr)
            {
                case 0:
                    m.setAttribute(MenuAttribute.TBR,new Double(100));
                    m.setAttribute(MenuAttribute.BASAL_RATE,doubleNUmber);
                    break;
                case 1:
                case 2:
                    m.setAttribute(MenuAttribute.TBR,new Double(doubleNUmber));
                    break;
            }
        }

        if(tokens[2].size()==1 && tokens[2].get(0).getPattern() instanceof NumberPattern)
            m.setAttribute(MenuAttribute.BASAL_SELECTED,new Integer(((NumberPattern)tokens[2].get(0).getPattern()).getNumber()));
        else
            return null;

        stage = 0;
        number.clear();
        boolean lowInsulin = false;
        boolean lowBattery= false;
        int lockState = 0;
        while(tokens[3].size()>0) {
            Token t = tokens[3].removeFirst();
            Pattern p = t.getPattern();
            switch (stage) {
                case 0:
                    if (hasRunning) {
                        if (p instanceof NumberPattern) {
                            number.add(p);
                        } else if (isSymbol(p, Symbol.DOT)) {
                            number.add(p);
                        } else if (isSymbol(p, Symbol.UNITS_PER_HOUR)) {
                            number.add(p);
                            stage++;
                        } else
                            return null;//
                        break;
                    }
                    break;
                case 1:
                    if (isSymbol(p, Symbol.LOW_BAT)) {
                        lowBattery = true;
                    } else if (isSymbol(p, Symbol.LOW_INSULIN)) {
                        lowInsulin= true;
                    } else if (isSymbol(p, Symbol.LOCK_CLOSED)) {
                        lockState=2;
                    } else if (isSymbol(p, Symbol.LOCK_OPENED)) {
                        lockState=2;
                    } else {
                        return null;
                    }
                    break;
            }
        }
        if(lowBattery)
            m.setAttribute(MenuAttribute.LOW_BATTERY,new Boolean(true));
        else
            m.setAttribute(MenuAttribute.LOW_BATTERY,new Boolean(false));
        if(lowInsulin)
            m.setAttribute(MenuAttribute.LOW_INSULIN,new Boolean(true));
        else
            m.setAttribute(MenuAttribute.LOW_INSULIN,new Boolean(false));

        m.setAttribute(MenuAttribute.LOCK_STATE,new Integer(lockState));

        if(hasRunning)
        {
            if(number.size()>0) {
                doubleNUmber = 0d;
                d = "";
                for (Pattern p : number) {
                    if (p instanceof NumberPattern) {
                        d += "" + ((NumberPattern) p).getNumber();
                    } else if (isSymbol(p, Symbol.DOT)) {
                        d += ".";
                    } else if (isSymbol(p, Symbol.UNITS_PER_HOUR)) {
                        //irgnore
                    } else {
                        return null;//violation!
                    }
                }
                try {
                    doubleNUmber = Double.parseDouble(d);
                } catch (Exception e) {
                    return null;
                }//violation, there must something parseable
                m.setAttribute(MenuAttribute.BASAL_RATE, doubleNUmber);
            }
            else
            {
                return null;
            }
        }
        else if(number.size()>0)
        {
            return null;
        }
        return m;
    }


    private static boolean isSymbol(Pattern p, Symbol symbol) {
        return (p instanceof SymbolPattern) && ((SymbolPattern) p).getSymbol() == symbol;
    }
    }
