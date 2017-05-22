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
        if(tokens[0].getFirst() != null && tokens[0].getFirst().getPattern() instanceof SymbolPattern && ((SymbolPattern)tokens[0].getFirst().getPattern()).getSymbol()== Symbol.CLOCK)
            return makeMainMenu(tokens);

        if(tokens[2].size()==1)
        {
            Pattern p = tokens[2].get(0).getPattern();

            if(isSymbol(p,Symbol.LARGE_STOP))
                return new Menu(MenuType.STOP_MENU);

            if(isSymbol(p,Symbol.LARGE_BOLUS))
                return new Menu(MenuType.BOLUS_MENU);

            if(isSymbol(p,Symbol.LARGE_EXTENDED_BOLUS))
                return new Menu(MenuType.EXTENDED_BOLUS_MENU);

            if(isSymbol(p,Symbol.LARGE_MULTIWAVE))
                return new Menu(MenuType.MULTIWAVE_BOLUS_MENU);

            if(isSymbol(p,Symbol.LARGE_TBR))
                return new Menu(MenuType.TBR_MENU);

            if(isSymbol(p,Symbol.LARGE_MY_DATA))
                return new Menu(MenuType.MY_DATA_MENU);

            if(isSymbol(p,Symbol.LARGE_BASAL))
                return new Menu(MenuType.BASAL_MENU);

            if(isSymbol(p,Symbol.LARGE_ALARM_SETTINGS))
                return new Menu(MenuType.ALARM_MENU);

            if(isSymbol(p,Symbol.LARGE_CALENDAR))
                return new Menu(MenuType.DATE_AND_TIME_MENU);

            if(isSymbol(p,Symbol.LARGE_PUMP_SETTINGS))
                return new Menu(MenuType.PUMP_MENU);

            if(isSymbol(p,Symbol.LARGE_THERAPIE_SETTINGS))
                return new Menu(MenuType.THERAPIE_MENU);

            if(isSymbol(p,Symbol.LARGE_BLUETOOTH_SETTINGS))
                return new Menu(MenuType.BLUETOOTH_MENU);

            if(isSymbol(p,Symbol.LARGE_MENU_SETTINGS))
                return new Menu(MenuType.MENU_SETTINGS_MENU);
        }
        else if(tokens[2].size()==2)
        {
            Pattern p1 = tokens[2].get(0).getPattern();
            Pattern p2 = tokens[2].get(1).getPattern();

            if(isSymbol(p1,Symbol.LARGE_BASAL))
            {
                if(p2 instanceof NumberPattern)
                {
                    int num = ((NumberPattern)p2).getNumber();
                    switch(num)
                    {
                        case 1:
                            return new Menu(MenuType.BASAL_1_MENU);
                        case 2:
                            return new Menu(MenuType.BASAL_2_MENU);
                        case 3:
                            return new Menu(MenuType.BASAL_3_MENU);
                        case 4:
                            return new Menu(MenuType.BASAL_4_MENU);
                        case 5:
                            return new Menu(MenuType.BASAL_5_MENU);
                    }
                }
            }
            return null;
        }
        else if(tokens[0].size()>1)
        {
            String title = parseString(tokens[0]);

            Title t = TitleResolver.resolve(title);
            if(t!=null) {
                switch (t) {
                    case BOLUS_AMOUNT:
                        return makeBolusEnter(tokens);
                    case BOLUS_DURATION:
                        return makeBolusDuration(tokens);
                    case IMMEDIATE_BOLUS:
                        return makeImmediateBolus(tokens);
                    case QUICK_INFO:
                        return makeQuickInfo(tokens);
                }
                Pattern p = tokens[1].get(0).getPattern();
            }
        }
        return null;
    }

    private static Menu makeQuickInfo(LinkedList<Token>[] tokens) {
        Menu m = new Menu(MenuType.QUICK_INFO);
        LinkedList<Pattern> number = new LinkedList<>();

        int stage = 0;
        while (tokens[1].size()>0) {
            Token t = tokens[1].removeFirst();
            Pattern p = t.getPattern();
            switch (stage) {
                case 0:
                    if (isSymbol(p, Symbol.LARGE_AMPULE_FULL)) {
                        stage++;
                    } else
                        return null;
                    break;
                case 1:
                    if(p instanceof NumberPattern || isSymbol(p,Symbol.LARGE_DOT))
                    {
                        number.add(p);
                    }
                    else if(p instanceof CharacterPattern && ((CharacterPattern)p).getCharacter()=='u')
                    {
                        stage++;
                    }
                    else
                        return null;
                    break;
                case 3:
                    return null;
            }
        }
        double doubleNumber = 0d;
        String d = "";
        for(Pattern p : number)
        {
            if(p instanceof NumberPattern)
            {
                d+=""+((NumberPattern)p).getNumber();
            } else if(isSymbol(p,Symbol.LARGE_DOT)) {
                d += ".";
            } else {
                return null;//violation!
            }
        }
        try { doubleNumber = Double.parseDouble(d);}
        catch (Exception e){return null;}//violation, there must something parseable

        m.setAttribute(MenuAttribute.REMAINING_INSULIN,new Double(doubleNumber));
        //FIXME 4th line

        return m;
    }

    private static String parseString(LinkedList<Token> tokens) {
        String s = "";
        Token last =null;
        for(Token t : tokens)
        {
            Pattern p = t.getPattern();

            if(last!=null)
            {
                int x = last.getColumn()+last.getWidth()+1+3;
                if(x < t.getColumn())
                {
                    s+=" ";
                }
            }
            if(p instanceof CharacterPattern)
            {
                s += ((CharacterPattern)p).getCharacter();
            }
            else if(isSymbol(p,Symbol.DOT))
            {
                s+=".";
            }
            else if(isSymbol(p,Symbol.SEPERATOR))
            {
                s+=":";
            }
            else if(isSymbol(p,Symbol.DIVIDE))
            {
                s+="/";
            }
            else if(isSymbol(p,Symbol.PARANTHESIS_LEFT))
            {
                s+="(";
            }
            else if(isSymbol(p,Symbol.PARANTHESIS_RIGHT))
            {
                s+=")";
            }
            else
            {
                return null;
            }
            last = t;
        }
        return s;
    }

    private static Menu makeBolusDuration(LinkedList<Token>[] tokens) {
        Menu m = new Menu(MenuType.BOLUS_DURATION);
        LinkedList<Integer> time = new LinkedList<>();

        int stage = 0;
        while (tokens[1].size()>0) {
            Token t = tokens[1].removeFirst();
            Pattern p = t.getPattern();
            switch (stage) {
                case 0:
                    if (isSymbol(p, Symbol.LARGE_ARROW)) {
                        stage++;
                    } else
                        return null;
                    break;
                case 1:
                case 2:
                case 4:
                case 5:
                    if (p instanceof NumberPattern) {
                        time.add(((NumberPattern) p).getNumber());
                        stage++;
                    } else
                        return null;
                    break;
                case 3:
                    if (isSymbol(p, Symbol.LARGE_SEPERATOR))
                        stage++;
                    else
                        return null;
                    break;
            }
        }
        if(time.size()==4)
        {
            int minute1 = time.removeLast();
            int minute10 = time.removeLast();
            int hour1 = time.removeLast();
            int hour10 = time.removeLast();
            m.setAttribute(MenuAttribute.RUNTIME,new MenuTime((hour10*10)+hour1,(minute10*10)+minute1));
        }
        else if(time.size()==0)
        {
            m.setAttribute(MenuAttribute.RUNTIME,new MenuBlink());
        }
        else
            return null;

        LinkedList<Pattern> number = new LinkedList<>();
        LinkedList<Pattern> number2 = new LinkedList<>();
        Symbol sym1 = null;
        Symbol sym2 = null;
        stage = 0;
        while (tokens[3].size()>0) {
            Token t = tokens[3].removeFirst();
            Pattern p = t.getPattern();
            switch (stage) {
                case 0:
                    if (isSymbol(p, Symbol.EXTENDED_BOLUS)) {
                        sym1 = Symbol.EXTENDED_BOLUS;
                        stage++;
                    } else if (isSymbol(p, Symbol.MULTIWAVE)) {
                        sym1 = Symbol.MULTIWAVE;
                        stage++;
                    } else
                        return null;
                    break;
                case 1:
                    if (p instanceof NumberPattern || isSymbol(p, Symbol.DOT)) {
                        number.add(p);
                    } else if (p instanceof CharacterPattern && ((CharacterPattern) p).getCharacter() == 'U') {
                        stage++;
                    } else
                        return null;
                    break;
                case 2:
                    if (isSymbol(p, Symbol.BOLUS)) {
                        sym2 = Symbol.BOLUS;
                        stage++;
                    } else
                        return null;
                    break;
                case 3:
                    if (p instanceof NumberPattern || isSymbol(p,Symbol.DOT)) {
                        number2.add(p);
                    } else if(p instanceof CharacterPattern && ((CharacterPattern)p).getCharacter()=='U') {
                        stage++;
                    } else
                        return null;
                    break;
            }
        }
        double doubleNumber = 0d;
        String d = "";
        for(Pattern p : number)
        {
            if(p instanceof NumberPattern)
            {
                d+=""+((NumberPattern)p).getNumber();
            } else if(isSymbol(p,Symbol.DOT)) {
                d += ".";
            } else {
                return null;//violation!
            }
        }
        try { doubleNumber = Double.parseDouble(d);}
        catch (Exception e){return null;}//violation, there must something parseable

        if(sym1 == Symbol.EXTENDED_BOLUS)
            m.setAttribute(MenuAttribute.BOLUS,new Double(doubleNumber));
        else if(sym1 == Symbol.MULTIWAVE) {
            m.setAttribute(MenuAttribute.BOLUS, new Double(doubleNumber));
            doubleNumber = 0d;
            d = "";
            for (Pattern p : number2) {
                if (p instanceof NumberPattern) {
                    d += "" + ((NumberPattern) p).getNumber();
                } else if (isSymbol(p, Symbol.DOT)) {
                    d += ".";
                } else {
                    return null;//violation!
                }
            }
            try {
                doubleNumber = Double.parseDouble(d);
            } catch (Exception e) {
                return null;
            }//violation, there must something parseable

            m.setAttribute(MenuAttribute.MULTIWAVE_BOLUS, new Double(doubleNumber));
        }
        return m;
    }

    private static Menu makeImmediateBolus(LinkedList<Token>[] tokens) {
        Menu m = new Menu(MenuType.IMMEDIATE_BOLUS);
        LinkedList<Pattern> number = new LinkedList<>();

        int stage = 0;
        while (tokens[1].size()>0) {
            Token t = tokens[1].removeFirst();
            Pattern p = t.getPattern();
            switch (stage) {
                case 0:
                    if (isSymbol(p, Symbol.LARGE_MULTIWAVE_BOLUS)) {
                        stage++;
                    } else
                        return null;
                    break;
                case 1:
                    if (p instanceof NumberPattern) {
                        number.add(p);
                    } else if(isSymbol(p,Symbol.LARGE_DOT)) {
                        number.add(p);
                    } else if(p instanceof CharacterPattern && ((CharacterPattern)p).getCharacter()=='u') {
                        stage++;
                    } else
                        return null;
                    break;
                case 3:
                    return null;
            }
        }

        double doubleNumber = 0d;
        String d = "";

        if(number.size()==0)
        {
            m.setAttribute(MenuAttribute.MULTIWAVE_BOLUS,new MenuBlink());
        }
        else {
            for (Pattern p : number) {
                if (p instanceof NumberPattern) {
                    d += "" + ((NumberPattern) p).getNumber();
                } else if (isSymbol(p, Symbol.LARGE_DOT)) {
                    d += ".";
                } else {
                    return null;//violation!
                }
            }
            try {
                doubleNumber = Double.parseDouble(d);
            } catch (Exception e) {
                return null;
            }//violation, there must something parseable

            m.setAttribute(MenuAttribute.MULTIWAVE_BOLUS, new Double(doubleNumber));
        }

        LinkedList<Integer> time = new LinkedList<>();
        number.clear();
        stage = 0;
        while (tokens[3].size() > 0) {
            Token t = tokens[3].removeFirst();
            Pattern p = t.getPattern();
            switch (stage) {
                case 0:
                    if (isSymbol(p, Symbol.ARROW)) {
                        stage++;
                    } else
                        return null;
                    break;
                case 1:
                case 2:
                case 4:
                case 5:
                    if (p instanceof NumberPattern) {
                        time.add(((NumberPattern) p).getNumber());
                        stage++;
                    } else
                        return null;
                    break;
                case 3:
                    if (isSymbol(p, Symbol.SEPERATOR))
                        stage++;
                    else
                        return null;
                    break;
                case 6:
                    if (isSymbol(p, Symbol.MULTIWAVE)) {
                        stage++;
                    } else
                        return null;
                    break;
                case 7:
                    if (p instanceof NumberPattern || isSymbol(p,Symbol.DOT)) {
                        number.add(p);
                    } else if(p instanceof CharacterPattern && ((CharacterPattern)p).getCharacter()=='U') {
                        stage++;
                    } else
                        return null;
                    break;
                case 8:
                    return null;
            }
        }
        if (time.size() == 4) {
            int minute1 = time.removeLast();
            int minute10 = time.removeLast();
            int hour1 = time.removeLast();
            int hour10 = time.removeLast();
            m.setAttribute(MenuAttribute.RUNTIME, new MenuTime((hour10 * 10) + hour1, (minute10 * 10) + minute1));
        }
        else
            return null;

        if(number.size()>0)
        {
            d="";
            for (Pattern p : number) {
                if (p instanceof NumberPattern) {
                    d += "" + ((NumberPattern) p).getNumber();
                } else if (isSymbol(p, Symbol.DOT)) {
                    d += ".";
                } else {
                    return null;//violation!
                }
            }
            try {
                doubleNumber = Double.parseDouble(d);
            } catch (Exception e) {
                return null;
            }//violation, there must something parseable

            m.setAttribute(MenuAttribute.BOLUS, new Double(doubleNumber));
        }
        else
            return null;
        return m;
    }

    private static Menu makeBolusEnter(LinkedList<Token>[] tokens) {
        Menu m = new Menu(MenuType.BOLUS_ENTER);
        LinkedList<Pattern> number = new LinkedList<>();

        int stage = 0;
        Symbol bolus = null;

        //main part
        while (tokens[1].size()>0) {
            Token t = tokens[1].removeFirst();
            Pattern p = t.getPattern();
            switch (stage) {
                case 0:
                    if (isSymbol(p, Symbol.LARGE_BOLUS)) {
                        bolus = Symbol.LARGE_BOLUS;
                        stage++;
                    } else if (isSymbol(p, Symbol.LARGE_MULTIWAVE)) {
                        bolus = Symbol.LARGE_MULTIWAVE;
                        stage++;
                    } else if (isSymbol(p, Symbol.LARGE_EXTENDED_BOLUS)) {
                        bolus = Symbol.LARGE_EXTENDED_BOLUS;
                        stage++;
                    } else if(p instanceof NumberPattern) {
                        number.add(p);
                        stage++;
                    } else if(p instanceof CharacterPattern && ((CharacterPattern)p).getCharacter()=='u') {
                        stage=2;
                    }else
                        return null;
                    break;
                case 1:
                    if (p instanceof NumberPattern) {
                        number.add(p);
                    } else if(isSymbol(p,Symbol.LARGE_DOT)) {
                        number.add(p);
                    } else if(p instanceof CharacterPattern && ((CharacterPattern)p).getCharacter()=='u') {
                        stage++;
                    } else
                        return null;
                    break;
                case 2:
                    return null;
            }
        }

        if(bolus!=null)
            m.setAttribute(MenuAttribute.BOLUS_TYPE,bolus.toString().replace("LARGE_",""));
        else
            m.setAttribute(MenuAttribute.BOLUS_TYPE,new MenuBlink());

        double doubleNumber = 0d;
        String d = "";
        if(number.size()>0) {
            for (Pattern p : number) {
                if (p instanceof NumberPattern) {
                    d += "" + ((NumberPattern) p).getNumber();
                } else if (isSymbol(p, Symbol.LARGE_DOT)) {
                    d += ".";
                } else {
                    return null;//violation!
                }
            }
            try {
                doubleNumber = Double.parseDouble(d);
            } catch (Exception e) {
                return null;
            }//violation, there must something parseable

            m.setAttribute(MenuAttribute.BOLUS, new Double(doubleNumber));
        } else
            m.setAttribute(MenuAttribute.BOLUS,new MenuBlink());

        //4th line
        LinkedList<Integer> time = new LinkedList<>();
        number.clear();
        stage = 0;
        while (tokens[3].size() > 0) {
            Token t = tokens[3].removeFirst();
            Pattern p = t.getPattern();
            switch (stage) {
                case 0:
                    if (isSymbol(p, Symbol.ARROW)) {
                        stage++;
                    } else
                        return null;
                    break;
                case 1:
                case 2:
                case 4:
                case 5:
                    if (p instanceof NumberPattern) {
                        time.add(((NumberPattern) p).getNumber());
                        stage++;
                    } else
                        return null;
                    break;
                case 3:
                    if (isSymbol(p, Symbol.SEPERATOR))
                        stage++;
                    else
                        return null;
                    break;
                case 6:
                    if (isSymbol(p, Symbol.BOLUS)) {
                        stage++;
                    } else
                        return null;
                    break;
                case 7:
                    if (p instanceof NumberPattern) {
                        number.add(p);
                    } else if(isSymbol(p,Symbol.DOT)) {
                        number.add(p);
                    } else if(p instanceof CharacterPattern && ((CharacterPattern)p).getCharacter()=='U') {
                        stage++;
                    } else
                        return null;
                    break;
                case 8:
                    return null;
            }
        }
        if(time.size()>0)
        {
            int minute1 = time.removeLast();
            int minute10 = time.removeLast();
            int hour1 = time.removeLast();
            int hour10 = time.removeLast();
            m.setAttribute(MenuAttribute.RUNTIME, new MenuTime((hour10 * 10) + hour1, (minute10 * 10) + minute1));

            if(number.size() > 0)
            {
                doubleNumber = 0d;
                d = "";
                for(Pattern p : number)
                {
                    if(p instanceof NumberPattern)
                    {
                        d+=""+((NumberPattern)p).getNumber();
                    } else if(isSymbol(p,Symbol.DOT)) {
                        d += ".";
                    } else if(p instanceof CharacterPattern && ((CharacterPattern)p).getCharacter()=='U'){
                        //irgnore
                    } else {
                        return null;//violation!
                    }
                }
                try { doubleNumber = Double.parseDouble(d);}
                catch (Exception e){return null;}//violation, there must something parseable

                m.setAttribute(MenuAttribute.MULTIWAVE_BOLUS, new Double(doubleNumber));
            }
        }
        else
        {
            m.setAttribute(MenuAttribute.RUNTIME, new MenuTime(0,0));
            m.setAttribute(MenuAttribute.MULTIWAVE_BOLUS, new Double(0));
        }
        return m;
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
            else if(timeC.get(0)=='A' && timeC.get(1)=='M' && hour10 == 1 && hour1 == 2)
            {
                tadd -= 12;
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
                    if (p instanceof NumberPattern) {
                        number.add(p);
                    } else if (isSymbol(p, Symbol.DOT)) {
                        number.add(p);
                    } else if (isSymbol(p, Symbol.UNITS_PER_HOUR)) {
                        number.add(p);
                        stage++;
                    } else if (isSymbol(p, Symbol.LOW_BAT)) {
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

        return m;
    }


    private static boolean isSymbol(Pattern p, Symbol symbol) {
        return (p instanceof SymbolPattern) && ((SymbolPattern) p).getSymbol() == symbol;
    }
    }
