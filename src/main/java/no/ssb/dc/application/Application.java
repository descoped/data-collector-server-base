package no.ssb.dc.application;

import no.ssb.config.DynamicConfiguration;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public interface Application {

    static String getDefaultConfigurationResourcePath() {
        return "application-defaults.properties";
    }

    static <T, R> Application create(DynamicConfiguration configuration) {
        try {
            Class<?> undertowApplication = Class.forName("no.ssb.dc.application.UndertowApplication");
            Method method = undertowApplication.getDeclaredMethod("initializeUndertowApplication", DynamicConfiguration.class);
            return (Application) method.invoke(null, configuration);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    static <T, R> Application create(DynamicConfiguration configuration, Integer port) {
        try {
            Class<?> undertowApplication = Class.forName("no.ssb.dc.application.UndertowApplication");
            Method method = undertowApplication.getDeclaredMethod("initializeUndertowApplication", DynamicConfiguration.class, Integer.class);
            return (Application) method.invoke(null, configuration, port);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    String getHost();

    int getPort();

    void start();

    void stop();

    void enableAllServices();

    void enable(Class<? extends Service> service);

    void disable(Class<? extends Service> service);

    <R> R unwrap(Class<R> clazz);
}
