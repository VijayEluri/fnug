package fnug.resource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import fnug.util.IOUtils;

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
 * Default implementation of {@link ResourceCollection}.
 * 
 * @author Martin Algesten
 * 
 */
public class DefaultResourceCollection extends AbstractAggregatedResource
        implements ResourceCollection, HasBundle {

    private static final String SUPER_NAME = "__CALCULATED__";

    private static final Resource[] EMPTY_RESOURCES = new Resource[] {};

    private Bundle bundle;
    private volatile String path;
    private Resource[] aggregates;
    private Resource[] dependencies;
    private JsCompressor jsCompressor;
    private CssCompressor cssCompressor;

    private byte[] css;
    private volatile Resource compressedJs;
    private volatile Resource compressedCss;

    /**
     * Constructs setting all necessary bits.
     * 
     * @param owner
     *            The bundle that generated this collection.
     * @param bundle
     *            The bundle to which the resources in this collection belongs.
     * @param aggregates
     *            Resources that comprises the aggregates. This is a mix of javascript, css and other dependent
     *            resources.
     * @param dependencies
     *            Resources that are just dependencies, not used for building aggregated bytes, but for
     *            {@link #getLastModified()}.
     */
    public DefaultResourceCollection(Bundle owner, Bundle bundle, Resource[] aggregates, Resource[] dependencies) {
        super(owner, SUPER_NAME);
        this.bundle = bundle;
        this.aggregates = aggregates == null ? EMPTY_RESOURCES : aggregates;
        this.dependencies = dependencies == null ? EMPTY_RESOURCES : dependencies;
        jsCompressor = new JsCompressor();
        cssCompressor = new CssCompressor();
    }

    private static int hash(Resource[] aggregates) {
        int i = DefaultResourceCollection.class.getName().hashCode();
        for (Resource r : aggregates) {
            i = 31 * i + r.getPath().hashCode();
            i = 31 * i + (new Long(r.getLastModified()).hashCode());
        }
        return i;
    }

    /**
     * The path of a resource collection is an md5 hash sum as hexadecimal of all the aggregates file names and last
     * modified dates prepended with the bundle name. I.e. "bundle-ab39283bcd09237576"
     */
    @Override
    public String getPath() {
        String result = path;
        if (result == null) {
            synchronized (this) {
                result = path;
                if (result == null) {
                    path = result = bundle.getName() + "-" + IOUtils.md5("" + hash(getAggregates()));
                }
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Resource[] getAggregates() {
        return aggregates;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Resource[] getDependencies() {
        return dependencies;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Bundle getBundle() {
        return bundle;
    }

    /**
     * Builds two sets of aggregated bytes. One which is {@link #getJs()} and the other {@link #getCss()}. Loops over
     * all {@link #getAggregates()} and picks out {@link Resource#isJs()} and {@link Resource#isCss()}.
     */
    @Override
    protected byte[] buildAggregate() {
        try {
            ByteArrayOutputStream jsbaos = new ByteArrayOutputStream();
            ByteArrayOutputStream cssbaos = new ByteArrayOutputStream();
            for (Resource r : getAggregates()) {

                System.out.println(r +" "+ r.isJs());
                
                if (r.isJs()) {
                    jsbaos.write(r.getBytes());
                } else if (r.isCss()) {
                    cssbaos.write(r.getBytes());
                }
            }
            css = cssbaos.toByteArray();
            return jsbaos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to build aggregate", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] getJs() {
        return getBytes();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] getCss() {
        ensureReadEntry();
        return css;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Resource getCompressedJs() {
        Resource result = compressedJs;
        if (result == null) {
            synchronized (this) {
                result = compressedJs;
                if (result == null) {
                    compressedJs = result = new DefaultCompressedResource(getBundle(), getBasePath(),
                            getPath() + ".js", getJs(), getLastModified(getExistingJsAggregates()), jsCompressor);
                }
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Resource getCompressedCss() {
        Resource result = compressedCss;
        if (result == null) {
            synchronized (this) {
                result = compressedCss;
                if (compressedCss == null) {
                    compressedCss = result = new DefaultCompressedResource(getBundle(), getBasePath(), getPath()
                            + ".css", getCss(), getLastModified(getExistingCssAggregates()), cssCompressor);
                }
            }
        }
        return result;
    }

    /**
     * {@inheritDoc} If any resource is changed, will dropped the compressed javascript and css.
     */
    @Override
    public boolean checkModified() {
        boolean modified = super.checkModified();
        if (modified) {
            synchronized (this) {
                compressedJs = null;
                compressedCss = null;
                path = null;
            }
        }
        return modified;
    }

    private long getLastModified(List<Resource> resources) {
        ensureReadEntry();
        long mostRecent = bundle.getConfig().configResource().getLastModified();
        boolean anyResource = false;
        for (Resource res : resources) {
            long l = res.getLastModified();
            if (l > 0) {
                mostRecent = Math.max(mostRecent, l);
                anyResource = true;
            }
        }
        return anyResource ? mostRecent : -1l;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Resource> getExistingJsAggregates() {
        return getExisting(CONTENT_TYPE_TEXT_JAVASCRIPT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Resource> getExistingCssAggregates() {
        return getExisting(CONTENT_TYPE_TEXT_CSS);
    }

    private List<Resource> getExisting(String contentType) {
        LinkedList<Resource> res = new LinkedList<Resource>();
        for (Resource r : getAggregates()) {
            if (r.getContentType().equals(contentType) &&
                    r.getLastModified() > 0) {
                res.add(r);
            }
        }
        return res;
    }

}
