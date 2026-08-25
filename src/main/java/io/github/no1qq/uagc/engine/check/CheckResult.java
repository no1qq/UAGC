package io.github.no1qq.uagc.engine.check;

import io.github.no1qq.uagc.engine.util.MathUtil;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class CheckResult {

    private static final CheckResult PASSED = new CheckResult(false, 0.0D, "", Collections.emptyMap(), false, false);

    private final boolean flagged;
    private final double severity;
    private final String summary;
    private final Map<String, String> details;
    private final boolean requestSetback;
    private final boolean requestDeny;

    private CheckResult(boolean flagged, double severity, String summary, Map<String, String> details,
                        boolean requestSetback, boolean requestDeny) {
        this.flagged = flagged;
        this.severity = severity;
        this.summary = summary;
        this.details = details;
        this.requestSetback = requestSetback;
        this.requestDeny = requestDeny;
    }

    public static CheckResult passed() {
        return PASSED;
    }

    public static Builder flag(double severity, String summary) {
        return new Builder(severity, summary);
    }

    public boolean flagged() {
        return flagged;
    }

    public double severity() {
        return severity;
    }

    public String summary() {
        return summary;
    }

    public Map<String, String> details() {
        return details;
    }

    public boolean requestSetback() {
        return requestSetback;
    }

    public boolean requestDeny() {
        return requestDeny;
    }

    public static final class Builder {

        private final double severity;
        private final String summary;
        private final Map<String, String> details = new LinkedHashMap<>(8);
        private boolean requestSetback;
        private boolean requestDeny;

        private Builder(double severity, String summary) {
            this.severity = MathUtil.clamp01(severity);
            this.summary = summary == null ? "" : summary;
        }

        public Builder with(String key, String value) {
            details.put(key, value);
            return this;
        }

        public Builder with(String key, double value) {
            details.put(key, String.format(Locale.ROOT, "%.4f", value));
            return this;
        }

        public Builder with(String key, long value) {
            details.put(key, Long.toString(value));
            return this;
        }

        public Builder with(String key, boolean value) {
            details.put(key, Boolean.toString(value));
            return this;
        }

        public Builder setback() {
            this.requestSetback = true;
            return this;
        }

        public Builder deny() {
            this.requestDeny = true;
            return this;
        }

        public CheckResult build() {
            return new CheckResult(true, severity, summary, Collections.unmodifiableMap(details), requestSetback, requestDeny);
        }
    }
}
