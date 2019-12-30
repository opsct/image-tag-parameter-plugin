package io.jenkins.plugins.luxair;

import java.util.logging.Logger;

import org.kohsuke.stapler.DataBoundSetter;

import hudson.Extension;
import jenkins.model.GlobalConfiguration;

@Extension
public class ImageTagParameterConfiguration extends GlobalConfiguration {

    private static final Logger logger = Logger.getLogger(ImageTagParameterConfiguration.class.getName());

    public static ImageTagParameterConfiguration get() {
        return GlobalConfiguration.all().get(ImageTagParameterConfiguration.class);
    }

    private String defaultRegistry;

    public ImageTagParameterConfiguration() {
        load();
    }

    public String getDefaultRegistry() {
        return defaultRegistry;
    }

    @DataBoundSetter
    public void setDefaultRegistry(String defaultRegistry) {
        logger.info("Changind default registry to: " + defaultRegistry);
        this.defaultRegistry = defaultRegistry;
        save();
    }

}