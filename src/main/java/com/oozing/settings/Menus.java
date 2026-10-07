package com.oozing.settings;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.ItemLore;

import java.util.*;

/** Chest-style GUIs. Vanilla clients see a normal chest; all logic runs on the server. */
public final class Menus {
    private Menus() {}

    interface Screen {
        void render(Container c);
        void click(ServerPlayer p, int slot, int button, ClickType type);
    }

    static final class OzMenu extends ChestMenu {
        private final Screen screen;
        private final Container container;

        OzMenu(int id, Inventory inv, int rows, Container c, Screen screen) {
            super(rows == 3 ? MenuType.GENERIC_9x3 : MenuType.GENERIC_9x6, id, inv, c, rows);
            this.screen = screen;
            this.container = c;
            screen.render(c);
        }

        @Override
        public void clicked(int slot, int button, ClickType type, Player player) {
            if (slot >= 0 && slot < container.getContainerSize()
                && (type == ClickType.PICKUP || type == ClickType.QUICK_MOVE || type == ClickType.THROW)
                && player instanceof ServerPlayer sp) {
                screen.click(sp, slot, button, type);
                if (sp.containerMenu == this) screen.render(container);
            }
            setCarried(ItemStack.EMPTY);
            sendAllDataToRemote(); // undo any client-side prediction
        }

        @Override
        public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }

        @Override
        public boolean stillValid(Player player) { return true; }
    }

    static void open(ServerPlayer p, String title, int rows, Screen screen) {
        p.openMenu(new SimpleMenuProvider(
            (id, inv, pl) -> new OzMenu(id, inv, rows, new SimpleContainer(rows * 9), screen),
            Component.literal(title)));
    }

    // ---------- helpers ----------
    static ItemStack named(Item item, ChatFormatting color, String name, String... lore) {
        ItemStack s = new ItemStack(item);
        s.set(DataComponents.CUSTOM_NAME, Component.literal(name).withStyle(color).withStyle(st -> st.withItalic(false)));
        if (lore.length > 0) {
            List<Component> lines = new ArrayList<>();
            for (String l : lore) lines.add(Component.literal(l).withStyle(ChatFormatting.GRAY).withStyle(st -> st.withItalic(false)));
            s.set(DataComponents.LORE, new ItemLore(lines));
        }
        return s;
    }

    static ItemStack pane() { return named(Items.GRAY_STAINED_GLASS_PANE, ChatFormatting.DARK_GRAY, " "); }

    static void fill(Container c) {
        for (int i = 0; i < c.getContainerSize(); i++) c.setItem(i, pane());
    }

    static String pretty(String id) {
        String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        StringBuilder sb = new StringBuilder();
        for (String w : path.split("_")) {
            if (w.isEmpty()) continue;
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(' ');
        }
        return sb.toString().trim();
    }

    // ---------- main menu ----------
    public static void openMain(ServerPlayer p) {
        open(p, "Oozings Settings", 3, new Screen() {
            public void render(Container c) {
                fill(c);
                c.setItem(11, named(Items.BREWING_STAND, ChatFormatting.GOLD, "Brewing Blocker",
                    "Potions blocked: " + Config.INSTANCE.bannedPotions.size(), "Click to edit"));
                c.setItem(15, named(Items.ENDER_PEARL, ChatFormatting.AQUA, "Item Cooldowns",
                    "Items with cooldowns: " + Config.INSTANCE.cooldowns.values().stream().filter(v -> v > 0).count(),
                    "Click to edit"));
            }
            public void click(ServerPlayer pl, int slot, int button, ClickType type) {
                if (slot == 11) openBrewing(pl, 0);
                if (slot == 15) openCooldowns(pl);
            }
        });
    }

    // ---------- brewing blocker ----------
    static void openBrewing(ServerPlayer p, int startPage) {
        List<Holder.Reference<Potion>> potions = new ArrayList<>();
        BuiltInRegistries.POTION.listElements().forEach(potions::add);
        potions.sort(Comparator.comparing(Holder::getRegisteredName));
        final int PAGE = 45;
        final int pages = Math.max(1, (potions.size() + PAGE - 1) / PAGE);
        int[] page = { Math.min(startPage, pages - 1) };

        open(p, "Brewing Blocker", 6, new Screen() {
            public void render(Container c) {
                fill(c);
                for (int i = 0; i < PAGE; i++) {
                    int idx = page[0] * PAGE + i;
                    if (idx >= potions.size()) break;
                    Holder.Reference<Potion> h = potions.get(idx);
                    String id = h.getRegisteredName();
                    boolean banned = Config.INSTANCE.bannedPotions.contains(id);
                    ItemStack s = PotionContents.createItemStack(Items.POTION, h);
                    s.set(DataComponents.CUSTOM_NAME, Component.literal(pretty(id))
                        .withStyle(banned ? ChatFormatting.RED : ChatFormatting.GREEN)
                        .withStyle(st -> st.withItalic(false)));
                    s.set(DataComponents.LORE, new ItemLore(List.of(
                        Component.literal(banned ? "BLOCKED from brewing" : "Allowed")
                            .withStyle(banned ? ChatFormatting.RED : ChatFormatting.GREEN)
                            .withStyle(st -> st.withItalic(false)),
                        Component.literal("Click to toggle").withStyle(ChatFormatting.GRAY)
                            .withStyle(st -> st.withItalic(false)))));
                    c.setItem(i, s);
                }
                c.setItem(45, named(Items.ARROW, ChatFormatting.YELLOW, "Back"));
                if (page[0] > 0) c.setItem(48, named(Items.SPECTRAL_ARROW, ChatFormatting.YELLOW, "Previous page"));
                c.setItem(49, named(Items.PAPER, ChatFormatting.WHITE, "Page " + (page[0] + 1) + "/" + pages));
                if (page[0] < pages - 1) c.setItem(50, named(Items.SPECTRAL_ARROW, ChatFormatting.YELLOW, "Next page"));
            }
            public void click(ServerPlayer pl, int slot, int button, ClickType type) {
                if (slot == 45) { openMain(pl); return; }
                if (slot == 48 && page[0] > 0) { page[0]--; return; }
                if (slot == 50 && page[0] < pages - 1) { page[0]++; return; }
                if (slot < PAGE) {
                    int idx = page[0] * PAGE + slot;
                    if (idx >= potions.size()) return;
                    String id = potions.get(idx).getRegisteredName();
                    if (!Config.INSTANCE.bannedPotions.remove(id)) Config.INSTANCE.bannedPotions.add(id);
                    Config.save();
                }
            }
        });
    }

    // ---------- cooldowns ----------
    static void openCooldowns(ServerPlayer p) {
        open(p, "Item Cooldowns", 6, new Screen() {
            List<String> keys() {
                List<String> k = new ArrayList<>(Config.INSTANCE.cooldowns.keySet());
                Collections.sort(k);
                return k;
            }
            public void render(Container c) {
                fill(c);
                List<String> keys = keys();
                for (int i = 0; i < 45 && i < keys.size(); i++) {
                    String k = keys.get(i);
                    int secs = Config.INSTANCE.cooldowns.get(k);
                    Item item = Config.item(k);
                    c.setItem(i, named(item == null ? Items.BARRIER : item,
                        secs > 0 ? ChatFormatting.GREEN : ChatFormatting.GRAY, pretty(k),
                        "Cooldown: " + (secs > 0 ? secs + "s" : "Off"),
                        "Left click: +1s   Right click: -1s",
                        "Shift: +/-10s",
                        "Q (drop): remove from list"));
                }
                c.setItem(45, named(Items.ARROW, ChatFormatting.YELLOW, "Back"));
                c.setItem(49, named(Items.HOPPER, ChatFormatting.AQUA, "Add item in your main hand",
                    "Hold an item, then click here"));
            }
            public void click(ServerPlayer pl, int slot, int button, ClickType type) {
                if (slot == 45) { openMain(pl); return; }
                if (slot == 49) {
                    ItemStack held = pl.getMainHandItem();
                    if (held.isEmpty()) {
                        pl.sendSystemMessage(Component.literal("Hold the item you want to add first."));
                        return;
                    }
                    Config.INSTANCE.cooldowns.putIfAbsent(Config.key(held.getItem()), 0);
                    Config.save();
                    return;
                }
                List<String> keys = keys();
                if (slot >= 45 || slot >= keys.size()) return;
                String k = keys.get(slot);
                if (type == ClickType.THROW) {
                    Config.INSTANCE.cooldowns.remove(k);
                } else {
                    int step = type == ClickType.QUICK_MOVE ? 10 : 1;
                    int delta = button == 1 ? -step : step;
                    int v = Math.max(0, Math.min(3600, Config.INSTANCE.cooldowns.get(k) + delta));
                    Config.INSTANCE.cooldowns.put(k, v);
                }
                Config.save();
            }
        });
    }
}
