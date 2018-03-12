package ut.org.chiari.jira.mitelink;

import org.junit.Test;
import org.chiari.jira.mitelink.api.MyPluginComponent;
import org.chiari.jira.mitelink.impl.MyPluginComponentImpl;

import static org.junit.Assert.assertEquals;

public class MyComponentUnitTest
{
    @Test
    public void testMyName()
    {
        MyPluginComponent component = new MyPluginComponentImpl(null);
        assertEquals("names do not match!", "myComponent",component.getName());
    }
}