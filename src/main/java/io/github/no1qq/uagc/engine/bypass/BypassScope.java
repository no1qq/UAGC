package io.github.no1qq.uagc.engine.bypass;

import io.github.no1qq.uagc.engine.check.CheckCategory;

import java.util.Objects;

public final class BypassScope {

    private static final BypassScope ALL = new BypassScope(null, null);

    private final CheckCategory category;
    private final String checkId;

    private BypassScope(CheckCategory category, String checkId) {
        this.category = category;
        this.checkId = checkId;
    }

    public static BypassScope all() {
        return ALL;
    }

    public static BypassScope category(CheckCategory category) {
        return new BypassScope(Objects.requireNonNull(category, "category"), null);
    }

    public static BypassScope check(String checkId) {
        return new BypassScope(null, Objects.requireNonNull(checkId, "checkId"));
    }

    public boolean isAll() {
        return category == null && checkId == null;
    }

    public CheckCategory category() {
        return category;
    }

    public String checkId() {
        return checkId;
    }

    public boolean covers(CheckCategory targetCategory, String targetCheckId) {
        if (isAll()) {
            return true;
        }
        if (category != null) {
            return category == targetCategory;
        }
        return checkId.equalsIgnoreCase(targetCheckId);
    }

    public String describe() {
        if (isAll()) {
            return "all";
        }
        return category != null ? "category:" + category.id() : "check:" + checkId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BypassScope scope)) {
            return false;
        }
        return category == scope.category && Objects.equals(checkId, scope.checkId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(category, checkId);
    }

    @Override
    public String toString() {
        return describe();
    }
}
