package me.coco0325.mapsync.utils;

import me.coco0325.mapsync.MapSync;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapPalette;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public class MapUtils {

    static MapSync plugin = MapSync.instance;
    private static final NamespacedKey idkey = new NamespacedKey(plugin, "mapid");
    private static final NamespacedKey copyright = new NamespacedKey(plugin, "copy");
    private static final NamespacedKey author = new NamespacedKey(plugin, "author");
    private static final NamespacedKey server = new NamespacedKey(plugin, "server");
    private static final NamespacedKey rawid = new NamespacedKey(plugin, "rawid");

    public static boolean hasUUID(MapMeta map){
        return map.getPersistentDataContainer().has(idkey, PersistentDataType.LONG);
    }

    public static Long getUUID(MapMeta map){
        return map.getPersistentDataContainer().get(idkey, PersistentDataType.LONG);
    }

    public static void applyUUID(ItemStack itemStack, Long uuid, Player player){
        ItemMeta meta = itemStack.getItemMeta();
        ArrayList<String> lore = meta.hasLore() ? (ArrayList<String>) meta.getLore() : new ArrayList<>();
        for(String text : plugin.MAP_LORE){
            lore.add(ChatColor.translateAlternateColorCodes('&', text.replace("%UUID%", uuid.toString()).replace("%AUTHOR%", player.getName())));
        }
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(idkey, PersistentDataType.LONG, uuid);
        meta.getPersistentDataContainer().set(copyright, PersistentDataType.BYTE, (byte)0);
        meta.getPersistentDataContainer().set(author, PersistentDataType.STRING, player.getUniqueId().toString());
        itemStack.setItemMeta(meta);
    }

    public static long generateUUID(Player player){
        return (System.currentTimeMillis() / 10) * 1000000 + (player.getUniqueId().hashCode() % 1000) * 1000 + ThreadLocalRandom.current().nextInt(0, 1000);
    }

    public static void render(ItemStack itemStack, byte[] bytes){
        if(!(itemStack.getItemMeta() instanceof MapMeta)) return;
        MapMeta mapMeta = (MapMeta) itemStack.getItemMeta();

        for(MapRenderer mapRenderer : mapMeta.getMapView().getRenderers()){
            mapMeta.getMapView().removeRenderer(mapRenderer);
        }

        Objects.requireNonNull(mapMeta.getMapView()).addRenderer(new MapRenderer() {
            @Override
            public void render(MapView map, MapCanvas canvas, Player player) {
                for(int i=0; i<128; i++){
                    for(int j=0; j<128; j++){
                        canvas.setPixel(i, j, bytes[j*128+i]);
                    }
                }
            }
        });
        itemStack.setItemMeta(mapMeta);
    }

    public static void renderMap(ItemStack item) {
        MapMeta mapMeta = (MapMeta) item.getItemMeta();
        if(mapMeta.getMapView() == null) return;
        if(hasUUID(mapMeta)){
            Long uuid = getUUID(mapMeta);
            if(plugin.getMapDataManager().isLocal(uuid)){
                mapMeta.setMapId(plugin.getMapDataManager().getLocalId(uuid));
                mapMeta.getMapView().setLocked(true);
                item.setItemMeta(mapMeta);
                FileUtils.toByteArray(uuid, (bytes) -> Bukkit.getScheduler().runTask(plugin, () -> {
                    try {
                        if(bytes == null) {
                            plugin.getMapDataManager().getMapMap().remove(uuid);
                            renderMap(item);
                        }
                        //plugin.getLogger().log(Level.INFO, "Loaded map from local storage.");
                        render(item, bytes);
                        //setMapPixels(bytes, mapMeta.getMapView());
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                }));
            }else{
                int rawid = Bukkit.createMap(Bukkit.getWorlds().get(0)).getId();
                mapMeta.setMapId(rawid);
                mapMeta.getMapView().setLocked(true);
                item.setItemMeta(mapMeta);
                plugin.getDatabaseManager().fetchMapData(uuid, rawid, (bytes) -> Bukkit.getScheduler().runTask(plugin, () -> {
                    try {
                        //plugin.getLogger().log(Level.INFO, "Downloaded map from database.");
                        render(item, bytes);
                        //setMapPixels(bytes, mapMeta.getMapView());
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                }));
            }
        }else{
            normalMapRender(item);
        }
    }

    public static byte[] getMapPixels(MapView view) throws Exception{
        String s = "map_" + view.getId();
        Object craftworld = getCBTClass().cast(Bukkit.getServer().getWorlds().get(0));
        Object world = craftworld.getClass().getMethod("getHandle").invoke(craftworld);
        Object worldmap = world.getClass().getDeclaredMethod("a", String.class).invoke(world, s);
        return (byte[]) worldmap.getClass().getDeclaredField("colors").get(worldmap);
    }

    private static Class<?> getCBTClass() throws ClassNotFoundException {
        String version = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3] + ".";
        String name = "org.bukkit.craftbukkit." + version + "CraftWorld";
        return Class.forName(name);
    }

    public static boolean canCopy(ItemStack item){
        if(Objects.requireNonNull(item.getItemMeta()).getPersistentDataContainer().has(copyright, PersistentDataType.BYTE)){
            return item.getItemMeta().getPersistentDataContainer().get(copyright, PersistentDataType.BYTE) == (byte)0;
        }
        return true;
    }

    public static void normalMapRender(ItemStack item){
        MapMeta meta = (MapMeta) item.getItemMeta();

        if(meta.getPersistentDataContainer().has(server, PersistentDataType.STRING)){

            if(plugin.getServername().equals(meta.getPersistentDataContainer().get(server, PersistentDataType.STRING))){
                if(meta.getPersistentDataContainer().has(rawid, PersistentDataType.INTEGER)){
                    meta.setMapId(meta.getPersistentDataContainer().get(rawid, PersistentDataType.INTEGER));
                }else{
                    meta.getPersistentDataContainer().set(rawid, PersistentDataType.INTEGER, meta.getMapId());
                }
            }else{
                meta.setMapId(0);
                if(!meta.getPersistentDataContainer().has(rawid, PersistentDataType.INTEGER)){
                    meta.getPersistentDataContainer().set(rawid, PersistentDataType.INTEGER, 0);
                }

                for(MapRenderer mapRenderer : meta.getMapView().getRenderers()){
                    meta.getMapView().removeRenderer(mapRenderer);
                }

                Objects.requireNonNull(meta.getMapView()).addRenderer(new MapRenderer() {
                    @Override
                    public void render(MapView map, MapCanvas canvas, Player player) {
                        for(int i=0; i<128; i++){
                            for(int j=0; j<128; j++){
                                canvas.setPixel(i, j, MapPalette.TRANSPARENT);
                            }
                        }
                    }
                });
            }
        }else{
            meta.getPersistentDataContainer().set(server, PersistentDataType.STRING, plugin.getServername());
            meta.getPersistentDataContainer().set(rawid, PersistentDataType.INTEGER, meta.getMapId());
        }
        item.setItemMeta(meta);
    }
}
