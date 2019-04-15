package com.evolvedbinary.appbundler;

import org.apache.maven.plugins.annotations.Parameter;

public class PlistEntry {

    @Parameter(required = true)
    private String key;

    @Parameter(required = true)
    private String type;

    @Parameter(required = true)
    private String value;

    public String getKey() {
        return key;
    }

    public String getType() {
        return type;
    }

    public String getValue() {
        return value;
    }
}
