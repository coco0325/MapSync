package me.coco0325.mapsync.items;

import io.github.thebusybiscuit.slimefun4.core.handlers.ItemDropHandler;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunPlugin;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import me.coco0325.mapsync.utils.MapUtils;
import me.mrCookieSlime.Slimefun.Lists.RecipeType;
import me.mrCookieSlime.Slimefun.Objects.Category;
import me.mrCookieSlime.Slimefun.api.Slimefun;
import me.mrCookieSlime.Slimefun.api.SlimefunItemStack;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;

import java.util.Collection;
import java.util.Optional;

public class MapRune extends SimpleSlimefunItem<ItemDropHandler> {

    private static final double RANGE = 1.5;

    public MapRune(Category category, SlimefunItemStack item, RecipeType type, ItemStack[] recipe, ItemStack recipeOutput) {
        super(category, item, type, recipe, recipeOutput);
    }

    @Override
    public ItemDropHandler getItemHandler() {
        return (e, p, item) -> {
            if (isItem(item.getItemStack())) {
                SlimefunPlugin.runSync(() -> activate(p, item), 20L);

                return true;
            }
            return false;
        };
    }

    private void activate(Player p, Item rune) {
        if (!rune.isValid()) {
            return;
        }

        Location l = rune.getLocation();
        Collection<Entity> entites = l.getWorld().getNearbyEntities(l, RANGE, RANGE, RANGE, this::findCompatibleItem);
        Optional<Entity> optional = entites.stream().findFirst();

        if (optional.isPresent()) {
            Item item = (Item) optional.get();
            ItemStack itemStack = item.getItemStack();

            if(item.hasMetadata("no_pickup") || item.hasMetadata("PROCOSMETICS_ITEM")) return;

            if (itemStack.getAmount() == 1) {
                // This lightning is just an effect, it deals no damage.
                l.getWorld().strikeLightningEffect(l);

                MapMeta mapMeta = (MapMeta) itemStack.getItemMeta();

                if(!MapUtils.canCopy(itemStack)) {
                    p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aSlimefun 4 &7> &e此地圖已被封印"));
                    return;
                }

                SlimefunPlugin.runSync(() -> {
                    // Being sure entities are still valid and not picked up or whatsoever.
                    if (rune.isValid() && item.isValid() && itemStack.getAmount() == 1) {

                        l.getWorld().createExplosion(l, 0);
                        l.getWorld().playSound(l, Sound.ENTITY_GENERIC_EXPLODE, 0.3F, 1);

                        item.remove();
                        rune.remove();

                        setLocked(itemStack, true);
                        l.getWorld().dropItemNaturally(l, itemStack);

                        SlimefunPlugin.getLocalization().sendMessage(p, "messages.soulbound-rune.success", true);
                    } else {
                        SlimefunPlugin.getLocalization().sendMessage(p, "messages.soulbound-rune.fail", true);
                    }
                }, 10L);
            } else {
                SlimefunPlugin.getLocalization().sendMessage(p, "messages.soulbound-rune.fail", true);
            }
        }
    }

    private boolean findCompatibleItem(Entity entity) {
        if (entity instanceof Item) {
            Item item = (Item) entity;

            return item.getItemStack().getType() == Material.FILLED_MAP;
        }
        return false;
    }

}