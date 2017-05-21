package org.monkey.d.ruffy.ruffy.driver.display;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Created by fishermen21 on 20.05.17.
 */

public abstract class Menu {
    public abstract String getName();

    private Map<String,Object> attributes = new HashMap<>();
    public void setAttribute(String key, Object value)
    {
        attributes.put(key,value);
    }

    public List<String> attributes()
    {
        return new LinkedList<String>(attributes.keySet());
    }

    public Object getAttribute(String key)
    {
        return attributes.get(key);
    }
}
