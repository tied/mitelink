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

import java.util.HashMap;
import java.util.Map;


@Scanned
public class MiteProjectSelector extends SelectCFType {

    @ComponentImport JiraBaseUrls jiraBaseUrls;
    @ComponentImport OptionsManager optionsManager;
    @ComponentImport GenericConfigManager genericConfigManager;
    @ComponentImport CustomFieldValuePersister customFieldValuePersister;

    public MiteProjectSelector(CustomFieldValuePersister customFieldValuePersister, OptionsManager optionsManager, GenericConfigManager genericConfigManager, JiraBaseUrls jiraBaseUrls) {
        super(customFieldValuePersister, optionsManager, genericConfigManager, jiraBaseUrls);
        this.optionsManager = optionsManager;
        this.genericConfigManager = genericConfigManager;
        this.customFieldValuePersister = customFieldValuePersister;

        this.jiraBaseUrls = jiraBaseUrls;

    }

    @Override
    public Map<String, Object> getVelocityParameters(Issue issue, CustomField field, FieldLayoutItem fieldLayoutItem) {
        Map params = super.getVelocityParameters(issue, field, fieldLayoutItem);

        FieldConfig fieldConfig = null;
        if (issue == null) {
            fieldConfig = field.getReleventConfig(new SearchContextImpl());
        } else {
            fieldConfig = field.getRelevantConfig(issue);
        }

        Options options = this.optionsManager.getOptions(fieldConfig);
        if (options.isEmpty()) {
            this.optionsManager.createOption(fieldConfig, null, new Long(1), "Option 1");
            this.optionsManager.createOption(fieldConfig, null, new Long(2), "Option 2");
            this.optionsManager.createOption(fieldConfig, null, new Long(3), "Foo");
        }
        options = this.optionsManager.getOptions(fieldConfig);

        Map < Long, String > results = new HashMap<>();
        Long selectedId = (long) - 1;
        boolean selected = false;
        Object value = field.getValue(issue);
        if (value != null) {
        selected = true;
        }
        for (Option option: options) {
            results.put(option.getOptionId(), option.getValue());

            if (selected && value.toString().equals(option.getValue())) {
                selectedId = option.getOptionId();
            }
        }

        params.put("results", results);
        params.put("selectedId", selectedId);

        return params;
    }
}