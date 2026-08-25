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
import io.github.no1qq.uagc.bukkit.listener.InventoryListener;
import io.github.no1qq.uagc.bukkit.gui.SettingsMenuListener;
import io.github.no1qq.uagc.bukkit.listener.MovementListener;
import io.github.no1qq.uagc.bukkit.listener.StateListener;
import io.github.no1qq.uagc.bukkit.sample.PlayerSampler;
import io.github.no1qq.uagc.engine.alert.AlertService;
import io.github.no1qq.uagc.engine.check.event.MovementEvent;
import io.github.no1qq.uagc.engine.config.UagcConfig;
import io.github.no1qq.uagc.engine.movement.MovementSnapshot;
import io.github.no1qq.uagc.engine.player.PlayerData;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

public final class UagcPlugin extends JavaPlugin {

    private UagcRuntime runtime;
    private UagcApiImpl api;
    private BukkitTask tickTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        migrateConfig();
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
            sweepStandingPlayers();
        } catch (RuntimeException exception) {
            getLogger().warning("tick task failed: " + exception.getMessage());
        }
    }

    private void sweepStandingPlayers() {
        long tick = runtime.server().currentTick();
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerData data = runtime.players().get(player.getUniqueId());
            if (data == null || player.isDead() || player.isInsideVehicle()) {
                continue;
            }
            MovementSnapshot last = data.movement().last();
            if (last == null || last.tick() >= tick) {
                continue;
            }
            if (tick - last.tick() > 1L) {
                continue;
            }
            MovementSnapshot idle;
            try {
                Location at = player.getLocation();
                idle = PlayerSampler.sample(player, at, at, tick, System.currentTimeMillis());
            } catch (RuntimeException exception) {
                data.movement().breakContinuity();
                continue;
            }
            data.movement().update(idle);
            runtime.engine().process(data, new MovementEvent(idle, true));
        }
    }

    private void migrateConfig() {
        File file = new File(getDataFolder(), "config.yml");
        if (!file.exists()) {
            return;
        }
        int shipped = shippedConfigVersion();
        int present = getConfig().getInt("config-version", 0);
        if (present >= shipped) {
            return;
        }
        File backup = new File(getDataFolder(), "config-v" + present + "-"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".yml");
        try {
            Files.copy(file.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
            Files.delete(file.toPath());
        } catch (IOException exception) {
            getLogger().severe("could not replace the outdated config.yml, it is still on version "
                    + present + " and the new check tuning will not apply: " + exception.getMessage());
            return;
        }
        saveDefaultConfig();
        reloadConfig();
        getLogger().warning("config.yml was written by an older UAGC and has been replaced with the defaults "
                + "for version " + shipped + ". Your old file is kept as " + backup.getName()
                + ", copy any settings you had changed back out of it.");
    }

    private int shippedConfigVersion() {
        try (InputStream stream = getResource("config.yml")) {
            if (stream == null) {
                return 0;
            }
            return YamlConfiguration
                    .loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getInt("config-version", 0);
        } catch (IOException exception) {
            return 0;
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
        Bukkit.getPluginManager().registerEvents(new InventoryListener(runtime), this);
        Bukkit.getPluginManager().registerEvents(new SettingsMenuListener(), this);
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
