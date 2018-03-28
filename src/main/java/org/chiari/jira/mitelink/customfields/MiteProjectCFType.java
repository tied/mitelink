package org.chiari.jira.mitelink.customfields;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.customfields.manager.OptionsManager;
import com.atlassian.jira.issue.customfields.manager.GenericConfigManager;
import com.atlassian.jira.issue.customfields.option.Option;
import com.atlassian.jira.issue.customfields.option.Options;
import com.atlassian.jira.issue.customfields.persistence.CustomFieldValuePersister;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.config.FieldConfig;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.fields.rest.json.beans.JiraBaseUrls;
import com.atlassian.jira.issue.customfields.impl.SelectCFType;
import com.atlassian.jira.issue.search.SearchContextImpl;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

import org.json.JSONObject;
import org.json.JSONArray;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;


@Scanned
public class MiteProjectCFType extends SelectCFType {

    @ComponentImport
    private final JiraBaseUrls jiraBaseUrls;
    @ComponentImport
    private final OptionsManager optionsManager;
    @ComponentImport
    private final GenericConfigManager genericConfigManager;
    @ComponentImport
    private final CustomFieldValuePersister customFieldValuePersister;
    @ComponentImport
    private static PluginSettingsFactory pluginSettingsFactory;

    // TODO@FE: add cache for json request instead of hammering the server
    private static final Logger log = LoggerFactory.getLogger( MiteProjectCFType.class );
    private static PluginSettings settings = null;

    @Inject
    public MiteProjectCFType( CustomFieldValuePersister customFieldValuePersister, OptionsManager optionsManager, GenericConfigManager genericConfigManager, JiraBaseUrls jiraBaseUrls, PluginSettingsFactory pluginSettingsFactory ) {
        super( customFieldValuePersister, optionsManager, genericConfigManager, jiraBaseUrls );
        this.jiraBaseUrls = jiraBaseUrls;
        this.optionsManager = optionsManager;
        this.genericConfigManager = genericConfigManager;
        this.customFieldValuePersister = customFieldValuePersister;
        this.pluginSettingsFactory = pluginSettingsFactory;
    }

    /**
     * Allows passing of additional parameters to the velocity context (beyond the getValueFromIssue methods)
     * Values are added to all velocity views, e.g edit, search, view, xml
     *
     * @param issue
     * @param field
     * @param fieldLayoutItem
     * @return
     */
    @Override
    public Map<String, Object> getVelocityParameters( Issue issue, CustomField field, FieldLayoutItem fieldLayoutItem ) {

        log.debug( "Called getVelocityParameters" );

        FieldConfig fieldConfig;
        Map params = super.getVelocityParameters( issue, field, fieldLayoutItem );

        if ( issue == null ) {
            log.debug( "Issue is null" );
            fieldConfig = field.getReleventConfig( new SearchContextImpl() );
        } else {
            log.debug( "Issue is not null" );
            log.debug( "Issue ID: %d", issue.getId() );
            fieldConfig = field.getRelevantConfig( issue );
        }

        // initialize settings, if not already initialized
        // could be done in the constructor
        if ( settings == null ) {
            settings = this.pluginSettingsFactory.createGlobalSettings();
        }

        // fetch a list of all current projects from mite
        ArrayList<JSONObject> projectList = getProjectList(
            ( String ) settings.get( "mitelink.config.account" ),
            ( String ) settings.get( "mitelink.config.apikey" )
        );


        // insert newly added options by comparing new and old option values
        updateFieldOptions( fieldConfig, projectList );

        // refresh newly updated options
        Options currentOptions = this.optionsManager.getOptions( fieldConfig );

        // create a key,value map that can be handed to the frontend
        // to be parsed into the fields options
        Map<Long, String> results = new HashMap<>();
        Long selectedId = ( long ) -1;
        boolean hasValue = false;
        Object value = field.getValue( issue );
        if ( value != null ) {
            hasValue = true;
        }

        for ( Option option : currentOptions ) {

            results.put( option.getOptionId(), option.getValue() );

            if ( hasValue && value.toString().equals( option.getValue() ) ) {
                selectedId = option.getOptionId();
            }

        }

        // hand all options and the currently selected id
        // to the velocity template. this is in addition to
        // other parameters, such as the current field value.
        params.put( "options", results );
        params.put( "selectedId", selectedId );

        return params;
    }

    private void updateFieldOptions( FieldConfig fieldConfig, ArrayList<JSONObject> projectData ) {
        log.debug( "Updating options with new values, if necessary" );

        // fetch current option-items for this particular field
        // initially, it should only be one as JIRA requires at least one entry
        // when registering the custom-field in the backend
        Options currentOptions = this.optionsManager.getOptions( fieldConfig );

        // build a lookup table containing all current options
        HashSet<String> currentOptionValues = new HashSet<>();

        for ( Option option : currentOptions ) {
            currentOptionValues.add( option.getValue() );
        }

        // compare current options with new ones
        // if an option is not already present, add it
        for ( JSONObject project : projectData ) {
            String optionValue = getFullProjectName( project );
            if ( !currentOptionValues.contains( optionValue ) ) {
                log.debug( "Adding new option: %s ", optionValue );
                this.optionsManager.createOption( fieldConfig, null, null, optionValue );
            }
        }
    }

    /**
     * Sends a GET request to a remote yolk Mite API-endpoint
     * Returns the response, which should be a JSON-String
     *
     * @param url The URL of the endpoint
     * @return The JSON-response from the server, or an empty string if the request was not successful
     */
    public Map<String, String> sendGETRequest( String url ) {

        Map<String, String> result = new HashMap<>();
        StringBuffer response = new StringBuffer();

        try {
            URL obj = new URL( url );
            HttpURLConnection con = ( HttpURLConnection ) obj.openConnection();

            con.setRequestProperty( "User-Agent", "Mozilla/5.0" );

            BufferedReader in = new BufferedReader( new InputStreamReader( con.getInputStream() ) );
            String inputLine;

            while ( ( inputLine = in.readLine() ) != null ) {
                response.append( inputLine );
            }
            in.close();

            result.put( "code", Integer.toString( con.getResponseCode() ) );
        } catch ( MalformedURLException e ) {
            log.error( "Method sendGETRequest threw MalformedURLException", e );
        } catch ( IOException e ) {
            log.error( "Method sendGETRequest threw IOException", e );
        }

        result.put( "response", response.toString() );

        return result;
    }

    /**
     * Fetches and returns a list of the current Mite-Projects
     *
     * @return A list of JSONObjects
     */
    public ArrayList<JSONObject> getProjectList( String account, String key ) {

        ArrayList<JSONObject> projects = new ArrayList<>();

        String url = String.format( "https://%s.mite.yo.lk/projects.json?api_key=%s", account, key );

        String JSONResponse = sendGETRequest( url ).get( "response" );
        JSONArray array = new JSONArray( JSONResponse );

        for ( int i = 0; i < array.length(); i++ ) {
            JSONObject jsonobject = array.getJSONObject( i );
            JSONObject project = jsonobject.getJSONObject( "project" );
            projects.add( project );
        }

        log.debug( "Number of fetched Mite-Projects: %d", projects.size() );

        return projects;
    }

    /**
     * Builds and returns an identifying name for a specific project
     *
     * @param project
     * @return The name of the project
     */
    public String getFullProjectName( JSONObject project ) {

        String identifier = "";

        if ( project.has( "customer_name" ) ) {
            identifier = project.getString( "customer_name" ) + " - ";
        }

        identifier += project.getString( "name" );

        return identifier;
    }
}