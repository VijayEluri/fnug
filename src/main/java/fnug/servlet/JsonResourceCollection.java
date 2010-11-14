package fnug.servlet;

import java.util.LinkedList;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;

import com.googlecode.jslint4java.JSLintResult;

import fnug.resource.HasJSLintResult;
import fnug.resource.Resource;
import fnug.resource.ResourceCollection;

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

@JsonPropertyOrder({ "name", "compJs", "compCss", "files" })
public class JsonResourceCollection {
    @JsonProperty
    String name;
    @JsonProperty
    String compJs;
    @JsonProperty
    String compCss;
    @JsonProperty
    JsonJSLintResult[] jsLintResult;
    @JsonProperty
    LinkedList<String> files = new LinkedList<String>();

    public JsonResourceCollection(ResourceCollection c) {
        name = c.getBundle().getName();
        if (c.getCompressedJs().getLastModified() > 0) {
            compJs = c.getCompressedJs().getFullPath();
        }
        if (c.getCompressedCss().getLastModified() > 0) {
            compCss = c.getCompressedCss().getFullPath();
        }

        LinkedList<JsonJSLintResult> jsLintResult = new LinkedList<JsonJSLintResult>();

        for (Resource r : c.getAggregates()) {
            if (r instanceof HasJSLintResult) {
                JSLintResult partResult = ((HasJSLintResult) r).getJSLintResult();
                if (partResult != null && !partResult.getReport().isEmpty()) {
                    jsLintResult.add(new JsonJSLintResult(r.getFullPath(), partResult.getReport()));
                }
            }
            files.add(r.getFullPath());
        }
        this.jsLintResult = jsLintResult.toArray(new JsonJSLintResult[jsLintResult.size()]);
    }
}