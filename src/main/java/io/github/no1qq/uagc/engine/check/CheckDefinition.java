package io.github.no1qq.uagc.engine.check;

import java.util.Locale;
import java.util.Objects;

public record CheckDefinition(
        String id,
        String displayName,
        CheckCategory category,
        String description,
        boolean latencySensitive,
        boolean tickSensitive,
        boolean experimental) {

    public CheckDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(category, "category");
        if (!id.matches("[a-z0-9_]+")) {
            throw new IllegalArgumentException("check id must be lowercase alphanumeric: " + id);
        }
        description = description == null ? "" : description;
    }

    public String bypassPermission() {
        return "uagc.bypass." + id;
    }

    public String configPath() {
        return "checks." + category.id() + "." + id;
    }

    public String qualifiedId() {
        return category.id() + "/" + id;
    }

    public static Builder builder(String id, String displayName, CheckCategory category) {
        return new Builder(id, displayName, category);
    }

    public static final class Builder {

        private final String id;
        private final String displayName;
        private final CheckCategory category;
        private String description = "";
        private boolean latencySensitive;
        private boolean tickSensitive;
        private boolean experimental;

        private Builder(String id, String displayName, CheckCategory category) {
            this.id = id.toLowerCase(Locale.ROOT);
            this.displayName = displayName;
            this.category = category;
        }

        public Builder description(String value) {
            this.description = value;
            return this;
        }

        public Builder latencySensitive() {
            this.latencySensitive = true;
            return this;
        }

        public Builder tickSensitive() {
            this.tickSensitive = true;
            return this;
        }

        public Builder experimental() {
            this.experimental = true;
            return this;
        }

        public CheckDefinition build() {
            return new CheckDefinition(id, displayName, category, description, latencySensitive, tickSensitive, experimental);
        }
    }
}
