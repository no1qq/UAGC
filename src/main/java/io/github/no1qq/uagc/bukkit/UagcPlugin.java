package io.github.no1qq.uagc.bukkit;

import io.github.no1qq.uagc.api.UagcApi;
import io.github.no1qq.uagc.bukkit.api.UagcApiImpl;
import io.github.no1qq.uagc.bukkit.command.UagcCommandTree;
import io.github.no1qq.uagc.bukkit.compat.Attributes;
import io.github.no1qq.uagc.bukkit.config.ConfigLoader;
import io.github.no1qq.uagc.bukkit.listener.CombatListener;
import io.github.no1qq.uagc.bukkit.listener.ConnectionListener;
import io.github.no1qq.uagc.bukkit.listener.FreezeListener;
import io.github.no1qq.uagc.bukkit.listener.InteractionListener;
import io.github.no1qq.uagc.bukkit.listener.MovementListener;
import io.github.no1qq.uagc.bukkit.listener.StateListener;
import io.github.no1qq.uagc.engine.alert.AlertService;
import io.github.no1qq.uagc.engine.config.UagcConfig;
import io.github.no1qq.uagc.engine.player.PlayerData;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.UUID;

public final class UagcPlugin extends JavaPlugin {

    private UagcRuntime runtime;
    private UagcApiImpl api;
    private BukkitTask tickTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        UagcConfig config;
        try {
            config = ConfigLoader.load(getConfig(), getLogger());
        } catch (RuntimeException exception) {
            getLogger().severe("configuration could not be read, falling back to built in defaults");
            exception.printStackTrace();
            config = UagcConfig.defaults();
        }

        runtime = new UagcRuntime(this, config, getLogger());
        runtime.freeze().loadPersisted();
        runtime.alerts().loadPersisted();

        registerListeners();
        registerCommands();

        api = new UagcApiImpl(runtime);
        Bukkit.getServicesManager().register(UagcApi.class, api, this, ServicePriority.Normal);

        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerData data = runtime.players().getOrCreate(player.getUniqueId(), player.getName());
            runtime.alerts().applyTo(data, player.hasPermission(AlertService.PERMISSION_VIEW));
            runtime.bypass().refresh(data);
        }

        tickTask = Bukkit.getScheduler().runTaskTimer(this, this::tick, 1L, 1L);
        warnAboutMissingAttributes();
        getLogger().info("UAGC enabled with " + runtime.registry().size() + " checks");
    }

    @Override
    public void onDisable() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        if (runtime != null) {
            runtime.freeze().persist();
            runtime.alerts().persist();
            runtime.players().clear();
            runtime.debug().clear();
        }
        if (api != null) {
            Bukkit.getServicesManager().unregister(UagcApi.class, api);
            api = null;
        }
    }

    private void tick() {
        try {
            runtime.server().refresh();
            runtime.freeze().tick();
        } catch (RuntimeException exception) {
            getLogger().warning("tick task failed: " + exception.getMessage());
        }
    }

    private void warnAboutMissingAttributes() {
        List<String> missing = Attributes.unresolved();
        if (missing.isEmpty()) {
            return;
        }
        getLogger().warning("this server does not expose " + String.join(", ", missing)
                + ", UAGC will fall back to vanilla values for them and may be less accurate");
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new ConnectionListener(runtime), this);
        Bukkit.getPluginManager().registerEvents(new MovementListener(runtime), this);
        Bukkit.getPluginManager().registerEvents(new StateListener(runtime), this);
        Bukkit.getPluginManager().registerEvents(new CombatListener(runtime), this);
        Bukkit.getPluginManager().registerEvents(new InteractionListener(runtime), this);
        Bukkit.getPluginManager().registerEvents(new FreezeListener(runtime), this);
    }

    private void registerCommands() {
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register(UagcCommandTree.build(this, runtime),
                        "UltimateAntiGamingChair administration"));
    }

    public boolean reloadUagc() {
        try {
            reloadConfig();
            UagcConfig updated = ConfigLoader.load(getConfig(), getLogger());
            runtime.applyConfig(updated);
            runtime.bypass().refreshAll();
            return true;
        } catch (RuntimeException exception) {
            getLogger().severe("reload failed: " + exception.getMessage());
            return false;
        }
    }

    public void refreshFreezeState(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) {
            return;
        }
        PlayerData data = runtime.players().get(playerId);
        if (data != null) {
            data.movement().breakContinuity();
        }
    }

    public UagcRuntime runtime() {
        return runtime;
    }
}
