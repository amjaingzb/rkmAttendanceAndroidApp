package com.rkm.attendance.importer;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Holds mapping of CSV headers to target fields (full_name, mobile, address, etc.).
 */
public class ImportMapping {
    private final Map<String, String> mapping = new LinkedHashMap<>();

    public ImportMapping() {}

    public ImportMapping(Map<String,String> m) {
        if (m != null) mapping.putAll(m);
    }

    /** Add or update mapping for one header. */
    public void put(String header, String target) {
        mapping.put(header, target);
    }

    /** Get the mapped target field for a header (may be null). */
    public String targetFor(String header) {
        return mapping.get(header);
    }

    /** All headers this mapping knows about. */
    public Set<String> headers() {
        return Collections.unmodifiableSet(mapping.keySet());
    }

    /** Raw view of header → target. */
    public Map<String,String> asMap() {
        return Collections.unmodifiableMap(mapping);
    }
}
