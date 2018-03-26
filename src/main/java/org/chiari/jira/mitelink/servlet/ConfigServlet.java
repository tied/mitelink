package org.chiari.jira.mitelink.servlet;

import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.jira.permission.GlobalPermissionKey;
import com.atlassian.jira.security.GlobalPermissionManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.message.I18nResolver;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.templaterenderer.TemplateRenderer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;


@Scanned
public class ConfigServlet extends HttpServlet {

    @ComponentImport
    private final TemplateRenderer renderer;
    @ComponentImport
    private final JiraAuthenticationContext jiraAuthenticationContext;
    @ComponentImport
    private final GlobalPermissionManager globalPermissionManager;
    @ComponentImport
    private final LoginUriProvider loginUriProvider;
    @ComponentImport
    private static PluginSettingsFactory pluginSettingsFactory;
    @ComponentImport
    private static I18nResolver i18n;

    private static final Logger log = LoggerFactory.getLogger( ConfigServlet.class );
    private static PluginSettings settings;

    @Inject
    public ConfigServlet( TemplateRenderer renderer, JiraAuthenticationContext jiraAuthenticationContext, GlobalPermissionManager globalPermissionManager, LoginUriProvider loginUriProvider, PluginSettingsFactory pluginSettingsFactory, I18nResolver i18nResolver) {
        this.renderer = renderer;
        this.jiraAuthenticationContext = jiraAuthenticationContext;
        this.globalPermissionManager = globalPermissionManager;
        this.loginUriProvider = loginUriProvider;
        this.pluginSettingsFactory = pluginSettingsFactory;
        this.settings = pluginSettingsFactory.createGlobalSettings();
        this.i18n = i18nResolver;
    }

    @Override
    protected void doGet( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {

        // check if a user is logged in and if so, if they have the necessary rights to change settings
        ApplicationUser user = jiraAuthenticationContext.getLoggedInUser();
        if ( user == null || !globalPermissionManager.hasPermission( GlobalPermissionKey.SYSTEM_ADMIN, user ) ) {

            URI loginUri = loginUriProvider.getLoginUri( URI.create( request.getServletPath() ) );
            response.sendRedirect( loginUri.toString() );

            return;
        }

        Map params = new HashMap();
        params.put( "apikey", settings.get( "mitelink.config.apikey" ) );
        params.put( "account", settings.get( "mitelink.config.account" ) );

        response.setContentType( "text/html;charset=utf-8" );
        renderer.render( "/templates/servlets/configservlet/config.vm", params, response.getWriter() );
    }

    @Override
    protected void doPost( HttpServletRequest req, HttpServletResponse resp ) throws ServletException, IOException {

        System.out.println( "--- [DEBUG] doPost Called ---" );

        settings.put( "mitelink.config.apikey", req.getParameter( "apikey" ) );
        settings.put( "mitelink.config.account", req.getParameter( "account" ) );

        resp.sendRedirect( req.getRequestURI() );
    }

}