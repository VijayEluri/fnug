package fnug.config;

import fnug.resource.Bundle;
import fnug.resource.JsCompressor;
import fnug.resource.Resource;

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
 * Config for a {@link Bundle}.
 * 
 * @author Martin Algesten
 * 
 */
public interface BundleConfig {

    /**
     * Default value for {@link #checkModifiedInterval()}. Set to {@value} .
     */
    static final boolean DEFAULT_CHECK_MODIFIED = true;

    /**
     * Default value for {@link #jsLint()}. Set to {@value}
     */
    static final boolean DEFAULT_JS_LINT = true;

    /**
     * Returns the resource responsible for this configuration.
     * 
     * @return associated resource.
     */
    Resource configResource();

    /**
     * Name of the bundle.
     * 
     * @return Bundle name. Must match {@link Bundle#BUNDLE_ALLOWED_CHARS}.
     */
    String name();

    /**
     * Base path of the bundle. See {@link Resource#getBasePath()}.
     * 
     * @return bundle base path
     */
    String basePath();

    /**
     * Tells if jslint is turned on for this bundle.
     * 
     * @return True if jslint is turned on.
     */
    boolean jsLint();

    /**
     * The interval in milliseconds that we are to check the modified dates of
     * resources resolved by this bundle. A value of 0 disables modified checks.
     * 
     * @return number of milliseconds between checks.
     */
    int checkModifiedInterval();

    /**
     * Additional compilation arguments passed to {@link JsCompressor}.
     * 
     * @return arguments passed to the javascript compressor.
     */
    String[] jsCompileArgs();

    /**
     * Files that comprises this bundle. Additional dependencies will be
     * discovered using {@link Resource#findRequiresTags()}.
     * 
     * @return Starting point files of this bundle.
     */
    String[] files();

}
