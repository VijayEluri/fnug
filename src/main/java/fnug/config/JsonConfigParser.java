package fnug.config;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonLocation;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonParser.Feature;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * Implementation of {@link ConfigParser} that reads json config files.
 * 
 * @author Martin Algesten
 * 
 */
public class JsonConfigParser implements ConfigParser {

    private static final Logger LOG = LoggerFactory.getLogger(JsonConfigParser.class);

    private static final String KEY_JS_LINT = "jsLint";
    private static final String KEY_CHECK_MODIFIED = "checkModified";
    private static final String KEY_JS_COMPILER_ARGS = "jsCompilerArgs";
    private static final String KEY_FILES = "files";
    private static final String[] EMPTY_STRINGS = new String[] {};
    private ObjectMapper mapper;
    private JsonFactory jsonFactory;

    public JsonConfigParser() {
        configureJsonParser();
    }

    private void configureJsonParser() {
        mapper = new ObjectMapper();

        // relax the default oh so anal json defaults.
        mapper.configure(Feature.ALLOW_COMMENTS, true);
        mapper.configure(Feature.ALLOW_SINGLE_QUOTES, true);
        mapper.configure(Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);

        jsonFactory = mapper.getJsonFactory();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Config parse(Resource res) {

        JsonParser parser = null;

        try {

            parser = jsonFactory.createJsonParser(res.getBytes());

            LinkedList<BundleConfig> bundleConfigs = new LinkedList<BundleConfig>();

            if (parser.nextToken() == null) {
                throw new IllegalArgumentException("Empty config file '" + res.getPath() + "'");
            }

            if (parser.getCurrentToken() != JsonToken.START_OBJECT) {
                throw new IllegalArgumentException("Config file is not a json object");
            }

            JsonNode root = parser.readValueAsTree();

            Iterator<String> iter = root.getFieldNames();
            while (iter.hasNext()) {
                String name = iter.next();
                DefaultBundleConfig config = buildConfig(root.get(name), name, parser.getCurrentLocation(),
                        res);

                LOG.info("Configured bundle: " + config.name());

                bundleConfigs.add(config);
            }

            if (bundleConfigs.isEmpty()) {
                throw new IllegalArgumentException("Failed to read at least " +
                        "one bundle config in '" + res.getPath() + "'");
            }

            return new DefaultConfig(bundleConfigs);

        } catch (Exception e) {
            if (parser != null) {
                throw new JsonConfigParseException(parser.getCurrentLocation(), e);
            } else {
                throw new ConfigParseException("Failed to parse", e);
            }
        }

    }

    private DefaultBundleConfig buildConfig(JsonNode node, String name, JsonLocation loc, Resource configResource)
            throws JsonParseException, IOException {

        boolean jsLint = parseBoolean(node, KEY_JS_LINT, loc, DefaultBundleConfig.DEFAULT_JS_LINT);
        boolean checkModified = parseBoolean(node, KEY_CHECK_MODIFIED, loc, DefaultBundleConfig.DEFAULT_CHECK_MODIFIED);
        String[] jsCompileArgs = parseStringArray(node, KEY_JS_COMPILER_ARGS, loc, EMPTY_STRINGS);
        String[] files = parseStringArray(node, KEY_FILES, loc, EMPTY_STRINGS);

        return new DefaultBundleConfig(configResource, name, configResource.getBasePath(), jsLint,
                checkModified, jsCompileArgs,
                files);
    }

    private String[] parseStringArray(JsonNode node, String key, JsonLocation loc, String[] def) {

        if (!node.has(key)) {
            if (def == null) {
                throw new JsonConfigParseException("Missing key '" + key + "'", loc);
            } else {
                return def;
            }
        }

        JsonNode m = node.get(key);
        if (!m.isArray()) {
            throw new JsonConfigParseException("Key '" + key + "' is not array", loc);
        }
        String[] vals = new String[m.size()];
        int i = 0;
        for (JsonNode j : m) {
            vals[i++] = j.getValueAsText();
        }
        return vals;

    }

    private boolean parseBoolean(JsonNode node, String key, JsonLocation loc, boolean def) {

        if (!node.has(key)) {
            return def;
        }

        JsonNode m = node.get(key);
        if (!m.isBoolean()) {
            throw new JsonConfigParseException("Key '" + key + "' is not a boolean value", loc);
        }

        return m.getBooleanValue();

    }

}
