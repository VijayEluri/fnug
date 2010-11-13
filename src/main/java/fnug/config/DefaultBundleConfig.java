package fnug.config;

import java.util.regex.Pattern;

import fnug.resource.Bundle;
import fnug.resource.Resource;

/**
 * Default implementation of {@link BundleConfig}
 * 
 * @author Martin Algesten
 * 
 */
public class DefaultBundleConfig implements BundleConfig {

    private Resource configResource;
    private String name;
    private String basePath;
    private Pattern[] matches;
    private boolean jsLint;
    private boolean checkModified;
    private String[] jsCompileArgs;
    private String[] files;

    /**
     * Constructs setting all configurations.
     * 
     * @param configResource
     *            Resource that built this instance.
     * @param name
     *            See {@link #name()}
     * @param basePath
     *            See {@link #basePath()}
     * @param matches
     *            See {@link #matches()}
     * @param jsLint
     *            See {@link #jsLint()}
     * @param checkModified
     *            See {@link #checkModified()}
     * @param jsCompileArgs
     *            See {@link #jsCompileArgs()}
     * @param files
     *            See {@link #files()}
     */
    public DefaultBundleConfig(Resource configResource, String name, String basePath, Pattern[] matches,
            boolean jsLint, boolean checkModified,
            String[] jsCompileArgs, String[] files) {
        this.configResource = configResource;
        if (!Bundle.BUNDLE_ALLOWED_CHARS.matcher(name).matches()) {
            throw new IllegalArgumentException("Bundle name must match: " + Bundle.BUNDLE_ALLOWED_CHARS.toString());
        }
        this.name = name;
        this.basePath = basePath;
        this.matches = matches;
        this.jsLint = jsLint;
        this.checkModified = checkModified;
        this.jsCompileArgs = jsCompileArgs;
        this.files = files;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Resource configResource() {
        return configResource;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String name() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String basePath() {
        return basePath;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Pattern[] matches() {
        return matches;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean jsLint() {
        return jsLint;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean checkModified() {
        return checkModified;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] jsCompileArgs() {
        return jsCompileArgs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] files() {
        return files;
    }

}
