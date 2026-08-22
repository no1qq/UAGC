package io.github.no1qq.uagc.engine.bypass;

import io.github.no1qq.uagc.engine.check.CheckCategory;
import io.github.no1qq.uagc.engine.check.CheckRegistry;
import io.github.no1qq.uagc.engine.check.RegisteredCheck;
import io.github.no1qq.uagc.engine.evidence.EvidenceEntry;
import io.github.no1qq.uagc.engine.evidence.EvidenceType;
import io.github.no1qq.uagc.engine.platform.ServerContext;
import io.github.no1qq.uagc.engine.player.PlayerData;
import io.github.no1qq.uagc.engine.player.PlayerDataManager;

import java.util.List;

public final class BypassService {

    public static final String PERMISSION_ALL = "uagc.bypass.all";

    private final ServerContext server;
    private final PlayerDataManager players;
    private final CheckRegistry registry;

    private volatile int refreshIntervalTicks;

    public BypassService(ServerContext server, PlayerDataManager players, CheckRegistry registry, int refreshIntervalTicks) {
        this.server = server;
        this.players = players;
        this.registry = registry;
        this.refreshIntervalTicks = Math.max(20, refreshIntervalTicks);
    }

    public void updateInterval(int ticks) {
        this.refreshIntervalTicks = Math.max(20, ticks);
    }

    public void refresh(PlayerData player) {
        long tick = server.clock().currentTick();
        boolean all = server.hasPermission(player.uuid(), PERMISSION_ALL);

        CheckCategory[] categories = CheckCategory.values();
        boolean[] categoryFlags = new boolean[categories.length];
        if (!all) {
            for (CheckCategory category : categories) {
                categoryFlags[category.ordinal()] = server.hasPermission(player.uuid(), category.bypassPermission());
            }
        }

        List<RegisteredCheck> checks = registry.all();
        boolean[] checkFlags = new boolean[checks.size()];
        if (!all) {
            for (RegisteredCheck registered : checks) {
                if (categoryFlags[registered.definition().category().ordinal()]) {
                    continue;
                }
                checkFlags[registered.index()] =
                        server.hasPermission(player.uuid(), registered.definition().bypassPermission());
            }
        }

        boolean previously = player.bypass().hasPermissionBypass();
        player.bypass().applyPermissions(all, categoryFlags, checkFlags, tick);
        boolean now = player.bypass().hasPermissionBypass();

        if (previously != now) {
            player.recordEvidence(EvidenceEntry.of(EvidenceType.BYPASS,
                            now ? "permission bypass became active" : "permission bypass no longer active")
                    .with("all", all));
        }
    }

    public void refreshIfDue(PlayerData player) {
        if (player.bypass().needsRefresh(server.clock().currentTick(), refreshIntervalTicks)) {
            refresh(player);
        }
    }

    public void refreshAll() {
        for (PlayerData player : players.all()) {
            refresh(player);
        }
    }
}
