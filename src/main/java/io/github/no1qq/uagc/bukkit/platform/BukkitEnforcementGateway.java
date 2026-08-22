package io.github.no1qq.uagc.bukkit.platform;

import io.github.no1qq.uagc.bukkit.message.Messages;
import io.github.no1qq.uagc.engine.movement.Vec3;
import io.github.no1qq.uagc.engine.platform.EnforcementGateway;
import io.github.no1qq.uagc.engine.punishment.PunishmentConfig;
import io.github.no1qq.uagc.engine.punishment.PunishmentRecord;
import io.github.no1qq.uagc.engine.util.DurationParser;
import com.destroystokyo.paper.profile.PlayerProfile;
import io.papermc.paper.ban.BanListType;
import org.bukkit.OfflinePlayer;
import org.bukkit.ban.ProfileBanList;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public final class BukkitEnforcementGateway implements EnforcementGateway {

    private final Plugin plugin;
    private final Consumer<UUID> freezeCallback;

    private volatile PunishmentConfig config;

    public BukkitEnforcementGateway(Plugin plugin, PunishmentConfig config, Consumer<UUID> freezeCallback) {
        this.plugin = plugin;
        this.config = config;
        this.freezeCallback = freezeCallback;
    }

    public void updateConfig(PunishmentConfig updated) {
        this.config = updated;
    }

    @Override
    public void kick(UUID playerId, PunishmentRecord record) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) {
            return;
        }
        player.kick(Messages.parse(config.defaultKickMessage(), placeholders(record, null)));
    }

    @Override
    public void ban(UUID playerId, PunishmentRecord record, Duration duration) {
        String name = record.playerName() != null ? record.playerName() : Bukkit.getOfflinePlayer(playerId).getName();
        if (name == null) {
            return;
        }
        String template = duration == null ? config.defaultBanMessage() : config.defaultTempBanMessage();
        Map<String, String> placeholders = placeholders(record, duration);
        String plainReason = Messages.plain(Messages.parse(template, placeholders));

        java.util.Date expiry = duration == null
                ? null
                : new java.util.Date(System.currentTimeMillis() + duration.toMillis());
        PlayerProfile profile = Bukkit.createProfile(playerId, name);
        Bukkit.getBanList(BanListType.PROFILE).addBan(profile, plainReason, expiry, config.banSource());

        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            player.kick(Messages.parse(template, placeholders));
        }
    }

    @Override
    public boolean unban(String playerName) {
        OfflinePlayer offline = Bukkit.getOfflinePlayer(playerName);
        UUID id = offline.getUniqueId();
        String name = offline.getName() == null ? playerName : offline.getName();
        ProfileBanList list = Bukkit.getBanList(BanListType.PROFILE);
        PlayerProfile profile = Bukkit.createProfile(id, name);
        if (!list.isBanned(profile)) {
            return false;
        }
        list.pardon(profile);
        return true;
    }

    @Override
    public void runConsoleCommand(String command) {
        if (command == null || command.isBlank()) {
            return;
        }
        String sanitized = command.startsWith("/") ? command.substring(1) : command;
        Bukkit.getScheduler().runTask(plugin,
                () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), sanitized));
    }

    @Override
    public void setback(UUID playerId, Vec3 position) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || position == null || !position.isFinite()) {
            return;
        }
        Location target = new Location(player.getWorld(), position.x(), position.y(), position.z(),
                player.getLocation().getYaw(), player.getLocation().getPitch());
        player.teleport(target, PlayerTeleportEvent.TeleportCause.PLUGIN);
    }

    @Override
    public void applyFreeze(UUID playerId, boolean frozen) {
        if (freezeCallback != null) {
            freezeCallback.accept(playerId);
        }
    }

    private Map<String, String> placeholders(PunishmentRecord record, Duration duration) {
        Map<String, String> map = new LinkedHashMap<>(8);
        map.put("player", record.playerName() == null ? "unknown" : record.playerName());
        map.put("check", record.checkDisplayName() == null ? "manual" : record.checkDisplayName());
        map.put("check_id", record.checkId() == null ? "manual" : record.checkId());
        map.put("reference", record.reference());
        map.put("reason", record.reason());
        map.put("expiry", DurationParser.format(duration));
        return map;
    }
}
