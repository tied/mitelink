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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


@Scanned
public class MiteProjectCFType extends SelectCFType {

    // TODO@FE: move this to plugin config
    private String APIEndpoint = "https://kompass-chiari.mite.yo.lk/projects.json?api_key=25cfb27ce28cc08d";

    @ComponentImport
    JiraBaseUrls jiraBaseUrls;
    @ComponentImport
    OptionsManager optionsManager;
    @ComponentImport
    GenericConfigManager genericConfigManager;
    @ComponentImport
    CustomFieldValuePersister customFieldValuePersister;

    public MiteProjectCFType(CustomFieldValuePersister customFieldValuePersister, OptionsManager optionsManager, GenericConfigManager genericConfigManager, JiraBaseUrls jiraBaseUrls) {
        super(customFieldValuePersister, optionsManager, genericConfigManager, jiraBaseUrls);
        this.optionsManager = optionsManager;
        this.genericConfigManager = genericConfigManager;
        this.customFieldValuePersister = customFieldValuePersister;
        this.jiraBaseUrls = jiraBaseUrls;
    }

    @Override
    public Map<String, Object> getVelocityParameters(Issue issue, CustomField field, FieldLayoutItem fieldLayoutItem) {

        System.out.println("--- [DEBUG] Called getVelocityParameters() ---");

        Map params = super.getVelocityParameters(issue, field, fieldLayoutItem);

        FieldConfig fieldConfig = null;

        if (issue == null) {
            System.out.println("--- [DEBUG] Issue is null ---");
            fieldConfig = field.getReleventConfig(new SearchContextImpl());
        } else {
            System.out.println("--- [DEBUG] Issue is not null ---");
            System.out.println(String.format("--- [DEBUG] Issue ID: %d ---", issue.getId()));
            fieldConfig = field.getRelevantConfig(issue);
        }

        //this.optionsManager.removeCustomFieldOptions(field);

        Options options = this.optionsManager.getOptions(fieldConfig);
        if (options.isEmpty()) {

            System.out.println("--- [DEBUG] Options not present - fetching from remote ---");

            ArrayList<JSONObject> projectData = GetProjectList();

            for (int i = 0; i < projectData.size(); i++) {

                String identifier = "";

                if (projectData.get(i).has("customer_name")) {
                    identifier = projectData.get(i).getString("customer_name") + " - ";
                }

                identifier += projectData.get(i).getString("name");

                this.optionsManager.createOption(fieldConfig, null, null, identifier);

            }

        } else {
            System.out.println("--- [DEBUG] Options already present ---");
        }
        options = this.optionsManager.getOptions(fieldConfig);


        Map<Long, String> results = new HashMap<>();
        Long selectedId = (long) -1;
        boolean selected = false;
        Object value = field.getValue(issue);
        if (value != null) {
            selected = true;
        }
        for (Option option : options) {

            results.put(option.getOptionId(), option.getValue());

            if (selected && value.toString().equals(option.getValue())) {
                selectedId = option.getOptionId();
            }

        }

        params.put("options", results);
        params.put("selectedId", selectedId);

        return params;
    }

    private String SendGETRequest(String url) {

        StringBuffer response = new StringBuffer();

        try {
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            con.setRequestProperty("User-Agent", "Mozilla/5.0");

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return response.toString();
    }

    private ArrayList<JSONObject> GetProjectList() {

        ArrayList<JSONObject> projects = new ArrayList<>();

        String response = SendGETRequest(this.APIEndpoint);

        JSONArray jsonarray = new JSONArray(response);
        for (int i = 0; i < jsonarray.length(); i++) {
            JSONObject jsonobject = jsonarray.getJSONObject(i);
            JSONObject project = jsonobject.getJSONObject("project");
            projects.add(project);
        }

        return projects;
    }
}