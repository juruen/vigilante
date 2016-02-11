package io.vigilante.site.http;

import io.dropwizard.Application;
import io.dropwizard.setup.Environment;
import io.vigilante.site.api.Manager;
import io.vigilante.site.api.impl.datastore.DatastoreManager;
import io.vigilante.site.http.filters.ResponseFilter;
import io.vigilante.site.http.impl.datastore.GCPProfile;
import io.vigilante.site.http.resources.*;
import io.vigilante.site.http.filters.ResponseFilter;
import io.vigilante.site.http.impl.datastore.GCPProfile;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class SiteApplication extends Application<SiteConfiguration> {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(SiteApplication.class);

    private final Executor executor = Executors.newFixedThreadPool(8);

    private  ResponseAdapter responseAdapter;

    private Manager manager;

    public static void main(String[] args) throws Exception {
        new SiteApplication().run(args);
    }

    @Override
    public String getName() {
        return "site";
    }

    @Override
    public void run(SiteConfiguration siteConfiguration, Environment environment) throws Exception {
        LOG.info("initializing...");

        try {
            manager = new DatastoreManager(GCPProfile.getBackend());
        } catch (Exception e) {
            LOG.error("Failed to initialize backend {}", e);
            System.exit(-1);
        }

        responseAdapter = new ResponseAdapter(manager.getAuthenticationManager(), executor);

        environment.jersey().register(new ResponseFilter());
        environment.jersey().register(new UserResource(manager.getUserManager(), responseAdapter));
        environment.jersey().register(new UsersResource(manager.getUserManager(), responseAdapter));
        environment.jersey().register(new TeamResource(manager.getTeamManager(), manager.getScheduleManager(),
            manager.getIncidentManager(), responseAdapter));
        environment.jersey().register(new TeamsResource(manager.getTeamManager(), responseAdapter));
        environment.jersey().register(new ScheduleResource(manager.getScheduleManager(), responseAdapter));
        environment.jersey().register(new SchedulesResource(manager.getScheduleManager(), responseAdapter));
        environment.jersey().register(new ServiceResource(manager.getServiceManager(), manager.getIncidentManager(),
            responseAdapter));
        environment.jersey().register(new ServicesResource(manager.getServiceManager(), responseAdapter));
        environment.jersey().register(new IncidentsResource(manager.getIncidentManager(), responseAdapter));
        environment.jersey().register(new IncidentResource(manager.getIncidentManager(), responseAdapter));
        environment.jersey().register(new OptionsResource());
        environment.jersey().register(new NowResource(responseAdapter));


        LOG.info("initialized, ready to rock!");
    }
}