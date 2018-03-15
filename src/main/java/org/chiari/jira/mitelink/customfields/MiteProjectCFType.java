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
import org.json.JSONObject;
import org.json.JSONArray;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;


@Scanned
public class MiteProjectCFType extends SelectCFType {

    // TODO@FE: add cache for json request instead of hammering the server
    // TODO@FE: move this to plugin config
    private static final String endpointURL = "https://kompass-chiari.mite.yo.lk/projects.json?api_key=25cfb27ce28cc08d";
    private static final Logger log = LoggerFactory.getLogger( MiteProjectCFType.class );

    @ComponentImport
    JiraBaseUrls jiraBaseUrls;
    @ComponentImport
    OptionsManager optionsManager;
    @ComponentImport
    GenericConfigManager genericConfigManager;
    @ComponentImport
    CustomFieldValuePersister customFieldValuePersister;

    public MiteProjectCFType( CustomFieldValuePersister customFieldValuePersister, OptionsManager optionsManager, GenericConfigManager genericConfigManager, JiraBaseUrls jiraBaseUrls ) {
        super( customFieldValuePersister, optionsManager, genericConfigManager, jiraBaseUrls );
        this.optionsManager = optionsManager;
        this.genericConfigManager = genericConfigManager;
        this.customFieldValuePersister = customFieldValuePersister;
        this.jiraBaseUrls = jiraBaseUrls;
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

        System.out.println( "--- [DEBUG] Called getVelocityParameters() ---" );

        Map params = super.getVelocityParameters( issue, field, fieldLayoutItem );

        FieldConfig fieldConfig;

        if ( issue == null ) {
            System.out.println( "--- [DEBUG] Issue is null ---" );
            fieldConfig = field.getReleventConfig( new SearchContextImpl() );
        } else {
            System.out.println( "--- [DEBUG] Issue is not null ---" );
            System.out.println( String.format( "--- [DEBUG] Issue ID: %d ---", issue.getId() ) );
            fieldConfig = field.getRelevantConfig( issue );
        }

        // fetch current options
        Options currentOptions = this.optionsManager.getOptions( fieldConfig );

        // fetch current projects from mite
        ArrayList<JSONObject> projectData = GetProjectList();

        System.out.println( String.format( "--- [DEBUG] Number of fetched Mite-Projects: %d ---", projectData.size() ) );

        if ( currentOptions.isEmpty() ) {

            System.out.println( "--- [DEBUG] Options not present - fetching from remote ---" );

            // simply add all projects as options
            for ( int i = 0; i < projectData.size(); i++ ) {
                String optionValue = GetFullProjectName( projectData.get( i ) );
                this.optionsManager.createOption( fieldConfig, null, null, optionValue );
            }

        } else {

            System.out.println( "--- [DEBUG] Options already present - updating with new values, if necessary ---" );

            // only insert newly added options by comparing new and old option values

            // build a lookup table containing all current options
            HashSet<String> currentOptionValues = new HashSet<>();

            for ( Option option : currentOptions ) {
                currentOptionValues.add( option.getValue() );
            }

            // compare current options with new ones
            // if a value is not already present, add it
            for ( JSONObject project : projectData ) {
                String optionValue = GetFullProjectName( project );
                if ( !currentOptionValues.contains( optionValue ) ) {
                    System.out.println( String.format( "--- [DEBUG] Adding new option: %s ---", optionValue ) );
                    this.optionsManager.createOption( fieldConfig, null, null, optionValue );
                }
            }

        }

        // refresh newly updated options
        currentOptions = this.optionsManager.getOptions( fieldConfig );

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

    /**
     * Sends a GET request to a remote yolk Mite API-endpoint
     * Returns the response, which should be a JSON-String
     *
     * @param url The URL of the endpoint
     * @return The JSON-response from the server, or an empty string if the request was not successful
     */
    private String SendGETRequest( String url ) {

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

        } catch ( Exception e ) {
            e.printStackTrace();
        }

        return response.toString();
    }

    /**
     * Fetches and returns a list of the current Mite-Projects
     *
     * @return A list of JSONObjects
     */
    private ArrayList<JSONObject> GetProjectList() {

        ArrayList<JSONObject> projects = new ArrayList<>();

        String response = SendGETRequest( this.endpointURL );

        JSONArray jsonarray = new JSONArray( response );
        for ( int i = 0; i < jsonarray.length(); i++ ) {
            JSONObject jsonobject = jsonarray.getJSONObject( i );
            JSONObject project = jsonobject.getJSONObject( "project" );
            projects.add( project );
        }

        return projects;
    }

    /**
     * Builds and returns an identifying name for a specific project
     *
     * @param project
     * @return The name of the project
     */
    private String GetFullProjectName( JSONObject project ) {

        String identifier = "";

        if ( project.has( "customer_name" ) ) {
            identifier = project.getString( "customer_name" ) + " - ";
        }

        identifier += project.getString( "name" );

        return identifier;
    }

}