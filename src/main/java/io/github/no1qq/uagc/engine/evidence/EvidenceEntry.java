package io.github.no1qq.uagc.engine.evidence;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public record EvidenceEntry(
        long tick,
        long timeMillis,
        EvidenceType type,
        String summary,
        Map<String, String> data) {

    public static Builder of(EvidenceType type, String summary) {
        return new Builder(type, summary);
    }

    public static final class Builder {

        private final EvidenceType type;
        private final String summary;
        private final Map<String, String> data = new LinkedHashMap<>(6);

        private Builder(EvidenceType type, String summary) {
            this.type = type;
            this.summary = summary == null ? "" : summary;
        }

        public Builder with(String key, String value) {
            data.put(key, value);
            return this;
        }

        public Builder with(String key, double value) {
            data.put(key, String.format(Locale.ROOT, "%.4f", value));
            return this;
        }

        public Builder with(String key, long value) {
            data.put(key, Long.toString(value));
            return this;
        }

        public Builder with(String key, boolean value) {
            data.put(key, Boolean.toString(value));
            return this;
        }

        public EvidenceEntry build(long tick, long timeMillis) {
            return new EvidenceEntry(tick, timeMillis, type, summary, Collections.unmodifiableMap(data));
        }
    }
}
