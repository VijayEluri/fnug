package fnug.resource;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.jslint4java.JSLint;
import com.googlecode.jslint4java.JSLintBuilder;
import com.googlecode.jslint4java.Option;

import fnug.config.BundleConfig;

/*
 Copyright 2010 Martin Algesten

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

/**
 * Default implementation of {@link Bundle}.
 * 
 * @author Martin Algesten
 * 
 */
public class DefaultBundle implements Bundle {

    private final static Logger LOG = LoggerFactory.getLogger(DefaultBundle.class);

    private static final String SUFFIX_CSS = "css";
    private static final String SUFFIX_JS = "js";

    /**
     * Arbitrary max size for cached resources. We want to avoid filling the
     * heap space with resources pointing to non-existing files. If we hit this
     * limit, something is probably wrong.
     */
    private static final int MAX_CACHE = 10000;

    private BundleConfig config;

    private volatile HashMap<String, Resource> cache = new HashMap<String, Resource>();

    private volatile ResourceCollection[] resourceCollections;
    private HashMap<String, ResourceCollection> previousResourceCollections = new HashMap<String, ResourceCollection>();

    private Pattern bundlePattern;

    /**
     * Constructs a bundle from the given config object.
     * 
     * @param config
     *            config to construct from.
     */
    public DefaultBundle(BundleConfig config) {
        this.config = config;
        bundlePattern = Pattern.compile(getName() + "/[a-f0-9]+\\.(js|css)");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BundleConfig getConfig() {
        return config;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return config.name();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Resource resolve(String path) {

        if (bundlePattern.matcher(path).matches()) {

            String collFile = path.substring(path.indexOf("/") + 1);
            int indx = collFile.indexOf(".");
            String collPath = collFile.substring(0, indx);
            String suffix = collFile.substring(indx + 1);

            ResourceCollection c = getResourceCollection(collPath);

            if (c != null) {
                return getCompressedBySuffix(c, suffix);
            }

            // do not return null here, but proceed to return "normal" resource.

        }

        Resource res = cache.get(path);
        if (res == null) {
            synchronized (this) {
                res = cache.get(path);
                if (res == null) {
                    if (cache.size() > MAX_CACHE) {
                        throw new IllegalStateException("Cache is larger than " + MAX_CACHE);
                    }
                    res = makeResource(path);
                    cache.put(path, res);
                }
            }
        }
        return res;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceCollection[] getResourceCollections() {
        if (resourceCollections == null) {
            synchronized (this) {
                if (resourceCollections == null) {
                    resourceCollections = buildResourceCollections();
                }
            }
        }
        return resourceCollections;
    }

    private ResourceCollection getResourceCollection(String collPath) {
        ResourceCollection[] colls = getResourceCollections();
        for (ResourceCollection coll : colls) {
            if (coll.getPath().equals(collPath)) {
                return coll;
            }
        }
        return null;
    }

    private Resource getCompressedBySuffix(ResourceCollection c, String suffix) {
        if (suffix.equals(SUFFIX_JS)) {
            return c.getCompressedJs();
        } else if (suffix.equals(SUFFIX_CSS)) {
            return c.getCompressedCss();
        }
        return null;
    }

    /**
     * Can be overridden to provide other implementations of {@link Resource}
     * than the {@link DefaultBundleResource}.
     * 
     * @param path
     *            the path to construct a resource around.
     * @return the constructed resource, which is an instance of
     *         {@link DefaultBundleResource}.
     */
    protected Resource makeResource(String path) {
        return new DefaultBundleResource(this, path);
    }

    private ResourceCollection[] buildResourceCollections() {

        LinkedList<Resource> l = new LinkedList<Resource>();
        for (String file : config.files()) {
            Resource r = ResourceResolver.getInstance().resolve(file);
            if (r == null) {
                LOG.warn("No bundle configured to resolve '" + file + "'. Ignoring file.");
                continue;
            }
            l.add(r);
        }

        Tarjan tarjan = new Tarjan(l);

        List<List<Resource>> order = tarjan.getResult();

        LinkedHashMap<Bundle, List<Resource>> bundleResources = new LinkedHashMap<Bundle, List<Resource>>();

        for (List<Resource> cur : order) {
            if (cur.size() > 1) {
                StringBuilder bld = new StringBuilder();
                for (Resource r : cur) {
                    bld.append(r.getPath() + " -> ");
                }
                bld.append(cur.get(0));
                throw new IllegalStateException("Found cyclic dependency: " + bld.toString());
            }
            Resource r = cur.get(0);
            if (!(r instanceof HasBundle)) {
                throw new IllegalStateException("Can only resolve dependencies resources implementing HasBundle");
            }
            Bundle b = ((HasBundle) r).getBundle();
            List<Resource> lr = bundleResources.get(b);
            if (lr == null) {
                lr = new LinkedList<Resource>();
                bundleResources.put(b, lr);
            }
            lr.add(r);
        }

        ResourceCollection[] result = new ResourceCollection[bundleResources.size()];

        int i = 0;
        for (Bundle b : bundleResources.keySet()) {
            List<Resource> lr = bundleResources.get(b);
            Resource[] alr = lr.toArray(new Resource[lr.size()]);

            ResourceCollection newColl = new DefaultResourceCollection(b, "/" + config.name() + "/", alr, null);

            // now we double check the newly built resource collection against
            // ones that were built previously. if we find a previous, we prefer
            // that one, since it may have already compiled javascript/css.
            if (previousResourceCollections.containsKey(newColl.getPath())) {
                newColl = previousResourceCollections.get(newColl.getPath());
            }

            result[i++] = newColl;
        }

        previousResourceCollections.clear();

        return result;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getLastModified() {
        long mostRecent = config.configResource().getLastModified();
        for (ResourceCollection rc : getResourceCollections()) {
            mostRecent = Math.max(mostRecent, rc.getLastModified());
        }
        return mostRecent;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean checkModified() {
        boolean modified = false;
        for (ResourceCollection rc : getResourceCollections()) {
            modified = rc.checkModified() || modified;
        }
        if (modified) {
            synchronized (this) {
                if (resourceCollections != null) {
                    // save resource collections to perhaps be reused when
                    // rebuilding.
                    for (ResourceCollection rc : resourceCollections) {
                        previousResourceCollections.put(rc.getPath(), rc);
                    }
                    resourceCollections = null;
                }
            }
        }
        return modified;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSLint getJsLinter() {
        // the JSLint instance cannot be kept between invocations since it uses
        // a thread local (mozilla rhino) Context that is set up in the
        // JSLintBuilder and used for the current executing thread.
        // We make sure to cache the result instead.
        if (config.jsLintArgs() != null && config.jsLintArgs().length > 0) {
            return initJsLinter();
        } else {
            return null;
        }
    }

    private JSLint initJsLinter() {

        JSLint jsLinter;
        try {
            jsLinter = new JSLintBuilder().fromDefault();
        } catch (IOException e1) {
            throw new IllegalStateException("Failed to init JSLint", e1);
        }

        for (String arg : config.jsLintArgs()) {

            String[] split = arg.split(":");

            Option opt;
            try {
                opt = Option.valueOf(split[0].toUpperCase());
            } catch (Exception e) {
                LOG.warn("Ignoring unknown JSLint option: " + arg);
                continue;
            }

            if (split.length == 1 || split[1].equalsIgnoreCase("true")) {
                jsLinter.addOption(opt);
            } else {
                jsLinter.addOption(opt, split[1]);
            }

        }

        return jsLinter;

    }

}
