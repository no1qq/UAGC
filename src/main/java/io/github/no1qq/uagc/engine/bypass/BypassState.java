package io.github.no1qq.uagc.engine.bypass;

import io.github.no1qq.uagc.engine.check.CheckCategory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class BypassState {

    private final CopyOnWriteArrayList<TemporaryBypass> temporary = new CopyOnWriteArrayList<>();

    private volatile boolean permissionAll;
    private volatile boolean[] permissionCategories = new boolean[CheckCategory.values().length];
    private volatile boolean[] permissionChecks = new boolean[0];
    private volatile long lastRefreshTick = Long.MIN_VALUE;
    private volatile boolean anyPermissionBypass;

    public void applyPermissions(boolean all, boolean[] categories, boolean[] checks, long tick) {
        this.permissionAll = all;
        this.permissionCategories = categories;
        this.permissionChecks = checks;
        this.lastRefreshTick = tick;
        boolean any = all;
        if (!any) {
            for (boolean value : categories) {
                if (value) {
                    any = true;
                    break;
                }
            }
        }
        if (!any) {
            for (boolean value : checks) {
                if (value) {
                    any = true;
                    break;
                }
            }
        }
        this.anyPermissionBypass = any;
    }

    public boolean needsRefresh(long tick, long intervalTicks) {
        return lastRefreshTick == Long.MIN_VALUE || tick - lastRefreshTick >= intervalTicks;
    }

    public boolean isBypassed(CheckCategory category, int checkIndex, String checkId, long tick) {
        if (permissionAll) {
            return true;
        }
        boolean[] categories = permissionCategories;
        if (category != null && category.ordinal() < categories.length && categories[category.ordinal()]) {
            return true;
        }
        boolean[] checks = permissionChecks;
        if (checkIndex >= 0 && checkIndex < checks.length && checks[checkIndex]) {
            return true;
        }
        return isTemporarilyBypassed(category, checkId, tick);
    }

    public boolean isTemporarilyBypassed(CheckCategory category, String checkId, long tick) {
        if (temporary.isEmpty()) {
            return false;
        }
        for (TemporaryBypass bypass : temporary) {
            if (!bypass.isActive(tick)) {
                continue;
            }
            if (bypass.scope().covers(category, checkId)) {
                return true;
            }
        }
        return false;
    }

    public void grantTemporary(TemporaryBypass bypass) {
        temporary.removeIf(existing -> existing.scope().equals(bypass.scope()));
        temporary.add(bypass);
    }

    public boolean revokeTemporary(BypassScope scope) {
        return temporary.removeIf(existing -> existing.scope().equals(scope));
    }

    public int revokeAllTemporary() {
        int size = temporary.size();
        temporary.clear();
        return size;
    }

    public List<TemporaryBypass> activeTemporary(long tick) {
        if (temporary.isEmpty()) {
            return Collections.emptyList();
        }
        List<TemporaryBypass> active = new ArrayList<>();
        List<TemporaryBypass> expired = null;
        for (TemporaryBypass bypass : temporary) {
            if (bypass.isActive(tick)) {
                active.add(bypass);
            } else {
                if (expired == null) {
                    expired = new ArrayList<>(2);
                }
                expired.add(bypass);
            }
        }
        if (expired != null) {
            temporary.removeAll(expired);
        }
        return active;
    }

    public boolean hasAnyBypass(long tick) {
        return anyPermissionBypass || !activeTemporary(tick).isEmpty();
    }

    public boolean hasPermissionBypass() {
        return anyPermissionBypass;
    }

    public boolean permissionAll() {
        return permissionAll;
    }

    public boolean permissionCategory(CheckCategory category) {
        boolean[] categories = permissionCategories;
        return category != null && category.ordinal() < categories.length && categories[category.ordinal()];
    }

    public boolean permissionCheck(int checkIndex) {
        boolean[] checks = permissionChecks;
        return checkIndex >= 0 && checkIndex < checks.length && checks[checkIndex];
    }

    public void clear() {
        temporary.clear();
        permissionAll = false;
        anyPermissionBypass = false;
        permissionCategories = new boolean[CheckCategory.values().length];
        permissionChecks = new boolean[0];
        lastRefreshTick = Long.MIN_VALUE;
    }
}
