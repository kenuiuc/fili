// Copyright 2018 Yahoo Inc.
// Licensed under the terms of the Apache license. Please see LICENSE.md file distributed with this work for terms.
package com.yahoo.wiki.webservice.data.config.metric;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


/**
 * Wiki metric maker config template.
 * <p>
 * An example:
 * <p>
 *      {
 *          "name": "bigThetaSketch",
 *          "class": "com.yahoo.bard.webservice.data.config.metric.makers.ThetaSketchMaker",
 *          "params": {
 *          "sketchSize": "4096"
 *          }
 *      }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class WikiMetricMakerTemplate {

    @JsonProperty("name")
    private final String name;

    @JsonProperty("class")
    private final String classPath;

    @JsonProperty("params")
    private final Map<String, String> parameters;

    /**
     * Constructor used by json parser.
     *
     * @param name       json property name
     * @param classPath  json property classPath
     * @param parameters json property parameters
     */
    public WikiMetricMakerTemplate(
            @NotNull @JsonProperty("name") String name,
            @NotNull @JsonProperty("class") String classPath,
            @JsonProperty("params") Map<String, String> parameters
    ) {
        this.name = name;
        this.classPath = classPath;
        this.parameters = (Objects.isNull(parameters) ? null : new HashMap<>(parameters));
    }

    /**
     * Get maker's name.
     *
     * @return maker's name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Get maker's class.
     *
     * @return maker's class
     */
    public String getClassPath() {
        return this.classPath;
    }

    /**
     * Get maker's parameters.
     *
     * @return maker's parameters
     */
    public Map<String, String> getParams() {
        return this.parameters;
    }
}