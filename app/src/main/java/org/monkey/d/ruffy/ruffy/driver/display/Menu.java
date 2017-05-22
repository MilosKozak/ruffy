package org.monkey.d.ruffy.ruffy.driver.display;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Created by fishermen21 on 20.05.17.
 */

public class Menu {
    private MenuType type;
    private Map<MenuAttribute,Object> attributes = new HashMap<>();

    public Menu(MenuType type)
    {
        this.type = type;
    }

    public void setAttribute(MenuAttribute key, Object value)
    {
        attributes.put(key,value);
    }

    public List<MenuAttribute> attributes()
    {
        return new LinkedList<MenuAttribute>(attributes.keySet());
    }

    public Object getAttribute(MenuAttribute key)
    {
        return attributes.get(key);
    }

    public MenuType getType() {
        return type;
    }
}
