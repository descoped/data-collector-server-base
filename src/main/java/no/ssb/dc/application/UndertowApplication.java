package no.ssb.dc.application;

import io.undertow.Undertow;
import no.ssb.config.DynamicConfiguration;
import no.ssb.dc.api.services.InjectionParameters;
import no.ssb.dc.api.services.ObjectCreator;
import no.ssb.dc.application.controller.DispatchController;
import no.ssb.dc.application.controller.HealthResourceFactory;
import no.ssb.dc.application.health.HealthApplicationMonitor;
import no.ssb.dc.application.health.HealthApplicationResource;
import no.ssb.dc.application.health.HealthConfigResource;
import no.ssb.dc.application.health.HealthContextsResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class UndertowApplication {

    private static final Logger LOG = LoggerFactory.getLogger(UndertowApplication.class);

    private final Undertow server;
    private final String host;
    private final int port;
    private final Map<Class<? extends Service>, Service> services;
    private final HealthApplicationMonitor applicationMonitor;

    private UndertowApplication(String host, int port, DispatchController dispatchController, Map<Class<? extends Service>, Service> services, HealthApplicationMonitor applicationMonitor) {
        this.host = host;
        this.port = port;
        this.services = services;
        this.applicationMonitor = applicationMonitor;
        this.server = Undertow.builder()
                .addHttpListener(port, host)
                .setHandler(dispatchController)
                .build();
        this.applicationMonitor.setServerStatus(HealthApplicationMonitor.ServerStatus.INITIALIZED);
    }

    public static UndertowApplication initializeUndertowApplication(DynamicConfiguration configuration) {
        int port = configuration.evaluateToInt("http.port");
        return initializeUndertowApplication(configuration, port);
    }

    public static UndertowApplication initializeUndertowApplication(DynamicConfiguration configuration, Integer port) {
        LOG.info("Initializing Data Collector server ...");
        HealthResourceFactory healthResourceFactory = HealthResourceFactory.create();
        healthResourceFactory.getHealthResource(HealthConfigResource.class).setConfiguration(configuration.asMap());
        HealthApplicationMonitor applicationMonitor = healthResourceFactory.getHealthResource(HealthApplicationResource.class).getMonitor();
        applicationMonitor.setServerStatus(HealthApplicationMonitor.ServerStatus.INITIALIZING);

        String host = configuration.evaluateToString("http.host");
        applicationMonitor.setHost(host);
        applicationMonitor.setPort(port);

        InjectionParameters serviceInjectionParameters = new InjectionParameters();
        serviceInjectionParameters.register(DynamicConfiguration.class, configuration);
        serviceInjectionParameters.register(HealthResourceFactory.class, healthResourceFactory);

        InjectionParameters controllerInjectionParameters = new InjectionParameters();
        controllerInjectionParameters.putAll(serviceInjectionParameters);

        Map<Class<? extends Service>, Service> services = new LinkedHashMap<>();
        for (Class<Service> serviceClass : ServiceProviderDiscovery.discover(Service.class)) {
            Service service = ObjectCreator.newInstance(serviceClass, serviceInjectionParameters);
            controllerInjectionParameters.register(serviceClass, service);
            services.put(serviceClass, service);
            LOG.info("Registered service: {}", serviceClass.getName());
        }

        HealthContextsResource healthContextsResource = healthResourceFactory.getHealthResource(HealthContextsResource.class);
        NavigableMap<String, Controller> controllers = new TreeMap<>();
        for (Class<Controller> controllerClass : ServiceProviderDiscovery.discover(Controller.class)) {
            Controller controller = ObjectCreator.newInstance(controllerClass, controllerInjectionParameters);
            String conextPath = controller.contextPath();
            controllers.put(conextPath, controller);
            healthContextsResource.add(conextPath, controller.allowedMethods(), controllerClass);
            LOG.info("Registered controller: {} ->  {}", conextPath, controller.getClass().getName());
        }

        DispatchController dispatchController = new DispatchController(
                configuration.evaluateToString("http.cors.allow.origin"),
                configuration.evaluateToString("http.cors.allow.header"),
                configuration.evaluateToBoolean("http.cors.allow.origin.test"),
                port,
                controllers
        );

        return new UndertowApplication(host, port, dispatchController, services, applicationMonitor);
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
