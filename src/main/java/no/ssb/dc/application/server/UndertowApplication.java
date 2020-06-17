package no.ssb.dc.application.server;

import io.prometheus.client.hotspot.DefaultExports;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import io.undertow.util.StatusCodes;
import no.ssb.config.DynamicConfiguration;
import no.ssb.dc.api.services.InjectionParameters;
import no.ssb.dc.api.services.ObjectCreator;
import no.ssb.dc.application.controller.CORSHandler;
import no.ssb.dc.application.controller.DispatchController;
import no.ssb.dc.application.health.HealthApplicationMonitor;
import no.ssb.dc.application.health.HealthApplicationResource;
import no.ssb.dc.application.health.HealthConfigResource;
import no.ssb.dc.application.health.HealthContextsResource;
import no.ssb.dc.application.health.HealthResourceFactory;
import no.ssb.dc.application.metrics.MetricsResourceFactory;
import no.ssb.dc.application.spi.Controller;
import no.ssb.dc.application.spi.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UndertowApplication {

    private static final Logger LOG = LoggerFactory.getLogger(UndertowApplication.class);

    private final Undertow server;
    private final String host;
    private final int port;
    private final Map<Class<? extends Service>, Service> services;
    private final HealthApplicationMonitor applicationMonitor;

    private String evaluateToStringOrDefault(DynamicConfiguration configuration, String key, String defaultValue) {
        return configuration.evaluateToString(key) == null ? defaultValue : configuration.evaluateToString(key);
    }

    private int evaluateToIntOrDefault(DynamicConfiguration configuration, String key, Integer defaultValue) {
        return configuration.evaluateToString(key) == null ? defaultValue : configuration.evaluateToInt(key);
    }

    private boolean evaluateToBooleanOrDefault(DynamicConfiguration configuration, String key, Boolean defaultValue) {
        return configuration.evaluateToString(key) == null ? defaultValue : configuration.evaluateToBoolean(key);
    }

    private UndertowApplication(DynamicConfiguration configuration, String host, int port, DispatchController dispatchController, Map<Class<? extends Service>, Service> services, HealthApplicationMonitor applicationMonitor) {
        this.host = host;
        this.port = port;
        this.services = services;
        this.applicationMonitor = applicationMonitor;

        PathHandler pathHandler = Handlers.path();

        pathHandler.addPrefixPath("/", dispatchController);

        CORSHandler corsHandler = configureCORSHandler(configuration, pathHandler);

        this.server = Undertow.builder()
                .addHttpListener(port, host)
                .setHandler(corsHandler)
                .build();
        this.applicationMonitor.setServerStatus(HealthApplicationMonitor.ServerStatus.INITIALIZED);
    }

    private CORSHandler configureCORSHandler(DynamicConfiguration configuration, PathHandler pathHandler) {
        List<Pattern> corsAllowOrigin = Stream.of(evaluateToStringOrDefault(configuration, "http.cors.allow.origin", ".*")
                .split(",")).map(Pattern::compile).collect(Collectors.toUnmodifiableList());
        Set<String> corsAllowMethods = Set.of(evaluateToStringOrDefault(configuration, "http.cors.allow.methods", "POST,GET,PUT,DELETE,HEAD")
                .split(","));
        Set<String> corsAllowHeaders = Set.of(evaluateToStringOrDefault(configuration, "http.cors.allow.header", "Content-Type,Authorization")
                .split(","));
        boolean corsAllowCredentials = evaluateToBooleanOrDefault(configuration, "http.cors.allow.credentials", false);
        int corsMaxAge = evaluateToIntOrDefault(configuration, "http.cors.allow.max-age", 900);

        return new CORSHandler(pathHandler, pathHandler, corsAllowOrigin,
                corsAllowCredentials, StatusCodes.NO_CONTENT, corsMaxAge, corsAllowMethods, corsAllowHeaders
        );
    }

    public static UndertowApplication initializeUndertowApplication(DynamicConfiguration configuration) {
        int port = configuration.evaluateToInt("http.port");
        return initializeUndertowApplication(configuration, port);
    }

    public static UndertowApplication initializeUndertowApplication(DynamicConfiguration configuration, Integer port) {
        if (!configuration.evaluateToBoolean("prometheus.defaultExports.disabled")) {
            DefaultExports.initialize();
        }
        LOG.info("Initializing Data Collector server ...");
        MetricsResourceFactory metricsResourceFactory = MetricsResourceFactory.create();

        HealthResourceFactory healthResourceFactory = HealthResourceFactory.create();
        healthResourceFactory.getHealthResource(HealthConfigResource.class).setConfiguration(configuration.asMap());
        HealthApplicationMonitor applicationMonitor = healthResourceFactory.getHealthResource(HealthApplicationResource.class).getMonitor();
        applicationMonitor.setServerStatus(HealthApplicationMonitor.ServerStatus.INITIALIZING);

        String host = configuration.evaluateToString("http.host") == null ? "localhost" : configuration.evaluateToString("http.host");

        applicationMonitor.setHost(host);
        applicationMonitor.setPort(port);

        InjectionParameters serviceInjectionParameters = new InjectionParameters();
        serviceInjectionParameters.register(DynamicConfiguration.class, configuration);
        serviceInjectionParameters.register(MetricsResourceFactory.class, metricsResourceFactory);
        serviceInjectionParameters.register(HealthResourceFactory.class, healthResourceFactory);

        InjectionParameters controllerInjectionParameters = new InjectionParameters();
        controllerInjectionParameters.putAll(serviceInjectionParameters);

        Map<Class<? extends Service>, Service> services = new LinkedHashMap<>();
        for (Class<Service> serviceClass : ServiceProviderDiscovery.discover(Service.class)) {
            Service service = ObjectCreator.newInstance(serviceClass, serviceInjectionParameters);
            controllerInjectionParameters.register(serviceClass, service);
            services.put(serviceClass, service);
            // TODO this is weak and requires Service Priority to guarantee injection ordering. Was added to let RawdataFileSystemService to get access to WorkerServer
            // TODO refactor and move exporter to WorkerService for direct access
            serviceInjectionParameters.register(serviceClass, service);
            // END TODO
            LOG.info("Registered service: {}", serviceClass.getName());
        }

        HealthContextsResource healthContextsResource = healthResourceFactory.getHealthResource(HealthContextsResource.class);
        NavigableMap<String, Controller> controllers = new TreeMap<>();
        for (Class<Controller> controllerClass : ServiceProviderDiscovery.discover(Controller.class)) {
            Controller controller = ObjectCreator.newInstance(controllerClass, controllerInjectionParameters);
            String contextPath = controller.contextPath();
            controllers.put(contextPath, controller);
            healthContextsResource.add(contextPath, controller.allowedMethods(), controllerClass);
            LOG.info("Registered controller: {} ->  {}", contextPath, controller.getClass().getName());
        }

        DispatchController dispatchController = new DispatchController(controllers);

        return new UndertowApplication(configuration, host, port, dispatchController, services, applicationMonitor);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public void start() {
        server.start();
        enableAllServices();
        applicationMonitor.setServerStatus(HealthApplicationMonitor.ServerStatus.RUNNING);
        LOG.info("Started Data Collector. PID {}", ProcessHandle.current().pid());
        LOG.info("Listening on {}:{}", host, port);
    }

    public void stop() {
        applicationMonitor.setServerStatus(HealthApplicationMonitor.ServerStatus.SHUTTING_DOWN);
        server.stop();
        for (Service service : services.values()) {
            service.stop();
        }
        applicationMonitor.setServerStatus(HealthApplicationMonitor.ServerStatus.SHUTDOWN);
        LOG.info("Leaving.. Bye!");
    }

    public void enableAllServices() {
        for (Service service : services.values()) {
            LOG.info("Starting: {}", service.getClass().getName());
            service.start();
        }
    }

    public void enable(Class<? extends Service> service) {
        services.get(service).start();
    }

    public void disable(Class<? extends Service> service) {
        services.get(service).stop();
    }

    public <R> R unwrap(Class<R> clazz) {
        if (clazz.isAssignableFrom(server.getClass())) {
            return (R) server;
        } else {
            if (services.containsKey(clazz)) {
                return (R) services.get(clazz);
            }
            throw new UnsupportedOperationException("Unable to unwrap class: " + clazz);
        }
    }

}
