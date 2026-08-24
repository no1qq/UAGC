package io.github.no1qq.uagc.bukkit.gui;

import io.github.no1qq.uagc.bukkit.UagcPlugin;
import io.github.no1qq.uagc.bukkit.UagcRuntime;
import io.github.no1qq.uagc.bukkit.config.SettingsWriter;
import io.github.no1qq.uagc.bukkit.message.Messages;
import io.github.no1qq.uagc.engine.check.CheckCategory;
import io.github.no1qq.uagc.engine.check.RegisteredCheck;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SettingsMenu implements InventoryHolder {

    private static final int SIZE = 54;
    private static final int CHECKS_PER_PAGE = 36;

    private enum Page {
        MAIN,
        CHECKS,
        CHECK
    }

    private enum Kind {
        TOGGLE,
        NUMBER,
        OPEN_CHECKS,
        OPEN_CHECK,
        BACK,
        CLOSE,
        NEXT_PAGE,
        PREVIOUS_PAGE
    }

    private record Entry(Kind kind, String path, double step, double minimum, double maximum, String target) {

        static Entry toggle(String path) {
            return new Entry(Kind.TOGGLE, path, 0.0D, 0.0D, 0.0D, null);
        }

        static Entry number(String path, double step, double minimum, double maximum) {
            return new Entry(Kind.NUMBER, path, step, minimum, maximum, null);
        }

        static Entry action(Kind kind, String target) {
            return new Entry(kind, null, 0.0D, 0.0D, 0.0D, target);
        }
    }

    private final UagcPlugin plugin;
    private final UagcRuntime runtime;
    private final Inventory inventory;
    private final Map<Integer, Entry> entries = new HashMap<>();

    private Page page = Page.MAIN;
    private String checkId;
    private int listPage;
    private String status;

    public SettingsMenu(UagcPlugin plugin, UagcRuntime runtime) {
        this.plugin = plugin;
        this.runtime = runtime;
        this.inventory = Bukkit.createInventory(this, SIZE, Messages.parse("<dark_gray>UAGC settings"));
        render();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    public void click(Player player, int slot, boolean rightClick, boolean shiftClick) {
        Entry entry = entries.get(slot);
        if (entry == null) {
            return;
        }
        switch (entry.kind()) {
            case TOGGLE -> apply(SettingsWriter.toggle(plugin, entry.path()), entry.path());
            case NUMBER -> {
                double current = plugin.getConfig().getDouble(entry.path());
                double step = entry.step() * (shiftClick ? 10.0D : 1.0D);
                double next = rightClick ? current - step : current + step;
                apply(SettingsWriter.setNumber(plugin, entry.path(), next, entry.minimum(), entry.maximum()),
                        entry.path());
            }
            case OPEN_CHECKS -> {
                page = Page.CHECKS;
                status = null;
            }
            case OPEN_CHECK -> {
                page = Page.CHECK;
                checkId = entry.target();
                status = null;
            }
            case BACK -> {
                page = page == Page.CHECK ? Page.CHECKS : Page.MAIN;
                status = null;
            }
            case NEXT_PAGE -> listPage++;
            case PREVIOUS_PAGE -> listPage = Math.max(0, listPage - 1);
            case CLOSE -> {
                player.closeInventory();
                return;
            }
        }
        render();
    }

    private void apply(SettingsWriter.Result result, String path) {
        status = switch (result) {
            case APPLIED -> "<green>saved " + path;
            case UNKNOWN_PATH -> "<red>" + path + " is not in config.yml";
            case WRONG_TYPE -> "<red>" + path + " cannot be edited here";
            case RELOAD_FAILED -> "<red>written but the reload failed, see the console";
        };
    }

    private void render() {
        entries.clear();
        inventory.clear();
        switch (page) {
            case MAIN -> renderMain();
            case CHECKS -> renderChecks();
            case CHECK -> renderCheck();
        }
        if (status != null) {
            put(48, item(Material.NAME_TAG, "<white>last change", List.of(status)), null);
        }
    }

    private void renderMain() {
        label(0, Material.BEACON, "<gold>UAGC settings",
                List.of("<gray>left click raises a number",
                        "<gray>right click lowers it",
                        "<gray>hold shift for ten times the step"));

        label(9, Material.COMPARATOR, "<gold>general", List.of());
        toggle(10, "general.enabled", "anti cheat enabled");
        toggle(11, "general.log-violations-to-console", "log violations to console");
        toggle(12, "general.exempt-on-lag-spike", "exempt on lag spike");

        label(18, Material.BELL, "<gold>alerts", List.of());
        toggle(19, "alerts.enabled", "alerts enabled");
        toggle(20, "alerts.flag-on-alert", "flag the player on alert");
        toggle(21, "alerts.send-to-console", "send alerts to console");
        number(22, "alerts.cooldown-ticks", "alert cooldown ticks", 1.0D, 0.0D, 200.0D);
        number(23, "alerts.flag-setback-interval-ticks", "flag interval ticks", 1.0D, 1.0D, 200.0D);
        number(24, "alerts.default-minimum-confidence", "minimum confidence", 0.05D, 0.0D, 1.0D);

        label(27, Material.IRON_AXE, "<gold>punishments", List.of());
        toggle(28, "punishments.enabled", "punishments enabled");
        toggle(29, "punishments.dry-run", "dry run");

        label(31, Material.SPYGLASS, "<gold>debug", List.of());
        toggle(32, "debug.enabled", "debug enabled");

        int active = 0;
        for (RegisteredCheck registered : runtime.registry().all()) {
            if (registered.config().enabled()) {
                active++;
            }
        }
        put(40, item(Material.CHEST, "<aqua>check settings",
                List.of("<gray>" + active + " of " + runtime.registry().size() + " checks enabled",
                        "<yellow>click to open")), Entry.action(Kind.OPEN_CHECKS, null));
        put(49, item(Material.BARRIER, "<red>close", List.of()), Entry.action(Kind.CLOSE, null));
    }

    private void renderChecks() {
        List<RegisteredCheck> checks = new ArrayList<>(runtime.registry().all());
        int pages = Math.max(1, (checks.size() + CHECKS_PER_PAGE - 1) / CHECKS_PER_PAGE);
        listPage = Math.min(listPage, pages - 1);
        int from = listPage * CHECKS_PER_PAGE;

        label(4, Material.CHEST, "<gold>checks",
                List.of("<gray>page " + (listPage + 1) + " of " + pages, "<gray>click a check to open it"));

        for (int index = 0; index < CHECKS_PER_PAGE && from + index < checks.size(); index++) {
            RegisteredCheck registered = checks.get(from + index);
            boolean enabled = registered.config().enabled();
            CheckCategory category = registered.definition().category();
            List<String> lore = new ArrayList<>();
            lore.add("<gray>" + category.id() + " <dark_gray>" + registered.id());
            lore.add("<gray>state: " + (enabled ? "<green>on" : "<red>off"));
            lore.add("<gray>alert threshold: <white>"
                    + SettingsWriter.format(plugin, path(registered) + ".alert-threshold"));
            lore.add("<gray>flags so far: <white>" + registered.flags());
            lore.add("<yellow>click to open");
            put(9 + index, item(enabled ? Material.LIME_DYE : Material.GRAY_DYE,
                            (enabled ? "<green>" : "<gray>") + registered.definition().displayName(), lore),
                    Entry.action(Kind.OPEN_CHECK, registered.id()));
        }

        if (listPage > 0) {
            put(45, item(Material.ARROW, "<white>previous page", List.of()),
                    Entry.action(Kind.PREVIOUS_PAGE, null));
        }
        if (from + CHECKS_PER_PAGE < checks.size()) {
            put(53, item(Material.ARROW, "<white>next page", List.of()), Entry.action(Kind.NEXT_PAGE, null));
        }
        put(49, item(Material.OAK_DOOR, "<white>back", List.of()), Entry.action(Kind.BACK, null));
    }

    private void renderCheck() {
        RegisteredCheck registered = runtime.registry().byId(checkId);
        if (registered == null) {
            page = Page.CHECKS;
            render();
            return;
        }
        String base = path(registered);
        FileConfiguration config = plugin.getConfig();
        if (!config.contains(base)) {
            status = "<red>" + registered.id() + " has no section in config.yml";
            page = Page.CHECKS;
            render();
            return;
        }

        label(4, Material.WRITABLE_BOOK, "<gold>" + registered.definition().displayName(),
                List.of("<gray>" + registered.definition().category().id() + " <dark_gray>" + registered.id(),
                        "<gray>" + registered.definition().description()));

        toggle(9, base + ".enabled", "enabled");
        number(10, base + ".violation-increment", "violation increment", 0.5D, 0.0D, 100.0D);
        number(11, base + ".decay-per-tick", "decay per tick", 0.01D, 0.0D, 10.0D);
        number(12, base + ".minimum-confidence", "minimum confidence", 0.05D, 0.0D, 1.0D);
        number(13, base + ".alert-threshold", "alert threshold", 1.0D, 0.0D, 500.0D);
        number(14, base + ".max-violation-level", "max violation level", 10.0D, 1.0D, 5000.0D);
        toggle(15, base + ".setback-enabled", "setback enabled");
        number(16, base + ".setback-threshold", "setback threshold", 1.0D, 0.0D, 500.0D);

        ConfigurationSection options = config.getConfigurationSection(base + ".options");
        int slot = 18;
        if (options != null) {
            for (String key : options.getKeys(false)) {
                if (slot >= 45) {
                    break;
                }
                String optionPath = base + ".options." + key;
                Object value = options.get(key);
                if (value instanceof Boolean) {
                    toggle(slot++, optionPath, key.replace('-', ' '));
                } else if (value instanceof Number number) {
                    number(slot++, optionPath, key.replace('-', ' '),
                            SettingsWriter.step(number.doubleValue()), 0.0D, 10_000.0D);
                }
            }
        }
        put(49, item(Material.OAK_DOOR, "<white>back", List.of()), Entry.action(Kind.BACK, null));
    }

    private String path(RegisteredCheck registered) {
        return "checks." + registered.definition().category().id() + "." + registered.id();
    }

    private void toggle(int slot, String path, String label) {
        if (!SettingsWriter.exists(plugin, path) || !SettingsWriter.isBoolean(plugin, path)) {
            return;
        }
        boolean current = plugin.getConfig().getBoolean(path);
        put(slot, item(current ? Material.LIME_DYE : Material.GRAY_DYE,
                (current ? "<green>" : "<red>") + label,
                List.of("<gray>value: " + (current ? "<green>on" : "<red>off"),
                        "<dark_gray>" + path,
                        "<yellow>click to turn " + (current ? "off" : "on"))), Entry.toggle(path));
    }

    private void number(int slot, String path, String label, double step, double minimum, double maximum) {
        if (!SettingsWriter.exists(plugin, path) || !SettingsWriter.isNumber(plugin, path)) {
            return;
        }
        put(slot, item(Material.COMPARATOR, "<aqua>" + label,
                List.of("<gray>value: <white>" + SettingsWriter.format(plugin, path),
                        "<dark_gray>" + path,
                        "<yellow>left click <gray>+" + trim(step) + "  <yellow>right click <gray>-" + trim(step),
                        "<dark_gray>hold shift for ten times that")), Entry.number(path, step, minimum, maximum));
    }

    private void label(int slot, Material material, String name, List<String> lore) {
        put(slot, item(material, name, lore), null);
    }

    private void put(int slot, ItemStack item, Entry entry) {
        if (slot < 0 || slot >= SIZE) {
            return;
        }
        inventory.setItem(slot, item);
        if (entry != null) {
            entries.put(slot, entry);
        }
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(plain(Messages.parse(name)));
            List<Component> lines = new ArrayList<>(lore.size());
            for (String line : lore) {
                lines.add(plain(Messages.parse(line)));
            }
            meta.lore(lines);
            item.setItemMeta(meta);
        }
        return item;
    }

    private Component plain(Component component) {
        return component.decoration(TextDecoration.ITALIC, false);
    }

    private String trim(double value) {
        String text = String.format(java.util.Locale.ROOT, "%.3f", value);
        while (text.contains(".") && (text.endsWith("0") || text.endsWith("."))) {
            text = text.substring(0, text.length() - 1);
        }
        return text;
    }
}
