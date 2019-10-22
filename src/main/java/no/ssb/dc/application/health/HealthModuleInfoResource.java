package no.ssb.dc.application.health;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.Resource;
import io.github.classgraph.ScanResult;
import no.ssb.dc.api.health.HealthRenderPriority;
import no.ssb.dc.api.health.HealthResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.lang.module.ModuleDescriptor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

@HealthRenderPriority(priority = 20)
public class HealthModuleInfoResource implements HealthResource {

    private static final Logger LOG = LoggerFactory.getLogger(HealthModuleInfoResource.class);

    private final List<ModuleInfo> modules;

    public HealthModuleInfoResource() {
        modules = scanModules("no.ssb");
    }

    @Override
    public String name() {
        return "modules";
    }

    @Override
    public boolean isList() {
        return true;
    }

    @Override
    public boolean canRender(Map<String, Deque<String>> queryParams) {
        return true;
    }

    @Override
    public Object resource() {
        return modules;
    }

    private List<ModuleInfo> scanModules(String packageName) {
        List<Module> modules = ModuleLayer
                .boot()
                .modules()
                .stream()
                .sorted(Comparator.comparing(md -> md.getDescriptor().name()))
                .filter(md -> md.getDescriptor().name().startsWith(packageName))
                .collect(Collectors.toList());

        List<ModuleInfo> moduleInfoList = new ArrayList<>();
        for (Module module : modules) {
            ModuleInfo.Builder moduleInfoBuilder = new ModuleInfo.Builder();

            ModuleDescriptor moduleDescriptor = module.getDescriptor();
            moduleInfoBuilder.moduleName(moduleDescriptor.name());

            try (ScanResult scanResult = new ClassGraph()
                    .ignoreParentClassLoaders()
                    .addClassLoader(module.getClassLoader())
                    .addModuleLayer(module.getLayer())
                    .enableClassInfo()
                    .whitelistPaths("META-INF/maven")
                    .scan()
            ) {
                scanResult.getResourcesWithLeafName("pom.properties")
                        .nonClassFilesOnly()
                        .forEachByteArray((Resource resource, byte[] content) -> {
                            if (isModuleNameEqualToResourceModuleRefName(moduleDescriptor, resource)) {
                                buildMavenInfo(moduleInfoBuilder, content);
                            }
                        });
            }

            moduleInfoList.add(moduleInfoBuilder.build());
        }

        return moduleInfoList;
    }

    private boolean isModuleNameEqualToResourceModuleRefName(ModuleDescriptor moduleDescriptor, Resource resource) {
        return moduleDescriptor.name().equals(resource.getModuleRef().getName());
    }

    private void buildMavenInfo(ModuleInfo.Builder moduleInfoBuilder, byte[] content) {
        try {
            Properties mavenProperties = new Properties();
            try (BufferedReader reader = new BufferedReader(new StringReader(new String(content)))) {
                mavenProperties.load(reader);
                moduleInfoBuilder
                        .groupId(mavenProperties.getProperty("groupId"))
                        .artifactId(mavenProperties.getProperty("artifactId"))
                        .version(mavenProperties.getProperty("version"));
            }
        } catch (IOException e) {
            LOG.warn("Problem reading version resource from classpath: ", e);
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class ModuleInfo {
        @JsonProperty public final String moduleName;
        @JsonProperty public final String groupId;
        @JsonProperty public final String artifactId;
        @JsonProperty public final String version;

        ModuleInfo(String moduleName, String groupId, String artifactId, String version) {
            this.moduleName = moduleName;
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ModuleInfo that = (ModuleInfo) o;
            return moduleName.equals(that.moduleName) &&
                    Objects.equals(groupId, that.groupId) &&
                    Objects.equals(artifactId, that.artifactId) &&
                    Objects.equals(version, that.version);
        }

        @Override
        public int hashCode() {
            return Objects.hash(moduleName, groupId, artifactId, version);
        }

        @Override
        public String toString() {
            return "ModuleInfo{" +
                    "moduleName='" + moduleName + '\'' +
                    ", groupId='" + groupId + '\'' +
                    ", artifactId='" + artifactId + '\'' +
                    ", version='" + version + '\'' +
                    '}';
        }

        static class Builder {
            private String moduleName;
            private String groupId;
            private String artifactId;
            private String version;

            Builder moduleName(String moduleName) {
                this.moduleName = moduleName;
                return this;
            }

            Builder groupId(String groupId) {
                this.groupId = groupId;
                return this;
            }

            Builder artifactId(String artifactId) {
                this.artifactId = artifactId;
                return this;
            }

            Builder version(String version) {
                this.version = version;
                return this;
            }

            public ModuleInfo build() {
                return new ModuleInfo(
                        moduleName,
                        ofNullable(groupId).orElse("unknown"),
                        ofNullable(artifactId).orElse("unknown"),
                        ofNullable(version).orElse("(DEV VERSION)")
                );
            }
        }
    }
}
