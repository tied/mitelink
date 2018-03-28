package ut.org.chiari.jira.mitelink.customfields;

import com.atlassian.jira.issue.customfields.manager.GenericConfigManager;
import com.atlassian.jira.issue.customfields.manager.OptionsManager;
import com.atlassian.jira.issue.customfields.option.Option;
import com.atlassian.jira.issue.customfields.option.Options;
import com.atlassian.jira.issue.customfields.option.OptionsImpl;
import com.atlassian.jira.issue.customfields.persistence.CustomFieldValuePersister;
import com.atlassian.jira.issue.fields.rest.json.beans.JiraBaseUrls;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

import org.chiari.jira.mitelink.customfields.MiteProjectCFType;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import org.json.JSONObject;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Map;


/**
 * @since 3.5
 */
public class MiteProjectCFTypeTest {

    @Mock
    private CustomFieldValuePersister customFieldValuePersister;
    @Mock
    private GenericConfigManager genericConfigManager;
    @Mock
    private OptionsManager optionsManager;
    @Mock
    private JiraBaseUrls jiraBaseUrls;
    @Mock
    private PluginSettingsFactory pluginSettingsFactory;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks( this );

        /*
        Options optionsCollection = new OptionsImpl( new ArrayList<>(), null, optionsManager );

        Mockito.when( optionsManager.getOptions( org.mockito.Matchers.anyObject() ) ).thenReturn( optionsCollection );
        Mockito.when( optionsManager.createOption( org.mockito.Matchers.anyObject(), null, null, org.mockito.Matchers.anyString() ) ).thenAnswer();
        */
    }

    @After
    public void tearDown() {

    }

    @Test
    public void testSendGetRequest() {
        MiteProjectCFType testClass = Mockito.spy( new MiteProjectCFType( customFieldValuePersister, optionsManager, genericConfigManager, jiraBaseUrls, pluginSettingsFactory ) );
        Map<String, String> result = testClass.sendGETRequest( "https://jsonplaceholder.typicode.com/posts/1" );
        assertEquals( "200", result.get( "code" ) );
        assertTrue( !result.get( "response" ).isEmpty() );
    }

    @Test
    public void testGetFullProjectName() {
        MiteProjectCFType testClass = Mockito.spy( new MiteProjectCFType( customFieldValuePersister, optionsManager, genericConfigManager, jiraBaseUrls, pluginSettingsFactory ) );

        JSONObject project = new JSONObject()

                .put( "budget", 0 )
                .put( "budget_type", "minutes" )
                .put( "created_at", "2018-01-02T17:37:00+01:00" )
                .put( "customer_id", 101757 )
                .put( "hourly_rate", 0 )
                .put( "id", 2343934 )
                .put( "name", "TestProjekt" )
                .put( "note", "" )
                .put( "updated_at", "2018-01-02T17:37:00+01:00" )
                .put( "archived", false )
                .put( "customer_name", "TestCustomer" )
                .put( "active_hourly_rate", "" )
                .put( "hourly_rates_per_service", new ArrayList<>() );

        String name = testClass.getFullProjectName( project );

        assertEquals( "TestCustomer - TestProjekt", name );
    }

    @Test
    public void testGetProjectList() {
        MiteProjectCFType testClass = Mockito.spy( new MiteProjectCFType( customFieldValuePersister, optionsManager, genericConfigManager, jiraBaseUrls, pluginSettingsFactory ) );
        ArrayList<JSONObject> projects = testClass.getProjectList( "none", "none" );
        assertTrue( !projects.isEmpty() );
    }

    /*
    @Test
    public void testGetVelocityParameters() {
        MiteProjectCFType testClass = Mockito.spy( new MiteProjectCFType( customFieldValuePersister, optionsManager, genericConfigManager, jiraBaseUrls, pluginSettingsFactory ) );
    }
    */

}
