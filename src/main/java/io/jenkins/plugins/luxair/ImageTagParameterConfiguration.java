package io.jenkins.plugins.luxair;

import hudson.Extension;
import io.jenkins.plugins.luxair.util.StringUtil;
import jenkins.model.GlobalConfiguration;
import org.kohsuke.stapler.DataBoundSetter;

import java.util.logging.Logger;

@Extension
public class ImageTagParameterConfiguration extends GlobalConfiguration {

    private static final Logger logger = Logger.getLogger(ImageTagParameterConfiguration.class.getName());
    private static final String DEFAULT_REGISTRY = "https://registry-1.docker.io";

    public static ImageTagParameterConfiguration get() {
        return GlobalConfiguration.all().get(ImageTagParameterConfiguration.class);
    }

    private String defaultRegistry = DEFAULT_REGISTRY;

    public ImageTagParameterConfiguration() {
        load();
    }

    public String getDefaultRegistry() {
        return StringUtil.isNotNullOrEmpty(defaultRegistry) ? defaultRegistry : DEFAULT_REGISTRY;
    }

    @DataBoundSetter
    public void setDefaultRegistry(String defaultRegistry) {
        logger.info("Changing default registry to: " + defaultRegistry);
        this.defaultRegistry = defaultRegistry;
        save();
    }

}