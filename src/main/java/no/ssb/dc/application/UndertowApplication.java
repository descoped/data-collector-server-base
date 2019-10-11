package no.ssb.dc.application;

import io.undertow.Undertow;
import no.ssb.config.DynamicConfiguration;
import no.ssb.dc.api.services.InjectionParameters;
import no.ssb.dc.api.services.ObjectCreator;
import no.ssb.dc.application.controller.DispatchController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class UndertowApplication {

    private static final Logger LOG = LoggerFactory.getLogger(UndertowApplication.class);

    public static <T, R> UndertowApplication initializeUndertowApplication(DynamicConfiguration configuration) {
        int port = configuration.evaluateToInt("http.port");
        return initializeUndertowApplication(configuration, port);
    }

    public static <T, R> UndertowApplication initializeUndertowApplication(DynamicConfiguration configuration, Integer port) {
        LOG.info("Initializing Data Collector server ...");

        String host = configuration.evaluateToString("http.host");

        InjectionParameters serviceInjectionParameters = new InjectionParameters();
        serviceInjectionParameters.register(DynamicConfiguration.class, configuration);

        InjectionParameters controllerInjectionParameters = new InjectionParameters();
        controllerInjectionParameters.putAll(serviceInjectionParameters);

        Map<Class<? extends Service>, Service> services = new LinkedHashMap<>();
        for (Class<Service> serviceClass : ServiceProviderDiscovery.discover(Service.class)) {
            Service service = ObjectCreator.newInstance(serviceClass, serviceInjectionParameters);
            controllerInjectionParameters.register(serviceClass, service);
            services.put(serviceClass, service);
        }

        NavigableMap<String, Controller> controllers = new TreeMap<>();
        for (Class<Controller> controllerClass : ServiceProviderDiscovery.discover(Controller.class)) {
            Controller controller = ObjectCreator.newInstance(controllerClass, controllerInjectionParameters);
            controllers.put(controller.contextPath(), controller);
        }

        DispatchController dispatchController = new DispatchController(
                configuration.evaluateToString("http.cors.allow.origin"),
                configuration.evaluateToString("http.cors.allow.header"),
                configuration.evaluateToBoolean("http.cors.allow.origin.test"),
                port,
                controllers
        );

        return new UndertowApplication(configuration, host, port, dispatchController, services);
    }

    private final Undertow server;
    private final DynamicConfiguration configuration;
    private final String host;
    private final int port;
    private final DispatchController dispatchController;
    private final Map<Class<? extends Service>, Service> services;

    <T, R> UndertowApplication(DynamicConfiguration configuration, String host, int port, DispatchController dispatchController, Map<Class<? extends Service>, Service> services) {
        this.configuration = configuration;
        this.host = host;
        this.port = port;
        this.dispatchController = dispatchController;
        this.services = services;
        DispatchController handler = dispatchController;
        Undertow server = Undertow.builder()
                .addHttpListener(port, host)
                .setHandler(handler)
                .build();
        this.server = server;
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
        LOG.info("Started Data Collector. PID {}", ProcessHandle.current().pid());
        LOG.info("Listening on {}:{}", host, port);
    }

    public void stop() {
        server.stop();
        for (Service service : services.values()) {
            service.stop();
        }
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
