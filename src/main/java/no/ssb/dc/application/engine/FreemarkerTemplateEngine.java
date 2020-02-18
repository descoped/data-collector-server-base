package no.ssb.dc.application.engine;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

public class FreemarkerTemplateEngine {

    private final String basePackagePath;
    private final Configuration config;

    public FreemarkerTemplateEngine(String basePackagePath) {
        this.basePackagePath = basePackagePath;
        config = configure();
    }

    static ClassLoader tccl() {
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        if (contextClassLoader == null) {
            return ClassLoader.getSystemClassLoader();
        }
        return contextClassLoader;
    }

    Configuration configure() {
        Configuration config = new Configuration(Configuration.VERSION_2_3_29);
        config.setClassLoaderForTemplateLoading(tccl(), basePackagePath);
        config.setDefaultEncoding("UTF-8");
        config.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        config.setLogTemplateExceptions(false);
        config.setWrapUncheckedExceptions(true);
        config.setFallbackOnNullLoopVariable(false);
        return config;
    }

    public void process(String templateFile, Map<String, Object> dataModel, Writer output) {
        try {
            Template template = config.getTemplate(templateFile);
            template.process(dataModel, output);

        } catch (IOException | TemplateException e) {
            throw new RuntimeException(e);
        }
    }
}
