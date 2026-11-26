package com.raj.blueprint.model;

import java.util.Map;

public class Blueprint {
    private String raw;
    private Map<String, String> sections;
    private Map<String, String> metadata;

    public Blueprint() {}

    public Blueprint(String raw, Map<String, String> sections, Map<String, String> metadata) {
        this.raw = raw;
        this.sections = sections;
        this.metadata = metadata;
    }

    public String getRaw() {
        return raw;
    }

    public void setRaw(String raw) {
        this.raw = raw;
    }

    public Map<String, String> getSections() {
        return sections;
    }

    public void setSections(Map<String, String> sections) {
        this.sections = sections;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }
}
