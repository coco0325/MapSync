package me.coco0325.mapsync;

import me.coco0325.mapsync.datastore.DatabaseManager;
import me.coco0325.mapsync.datastore.MapDataManager;
import me.coco0325.mapsync.hook.GriefPreventionHook;
import me.coco0325.mapsync.listeners.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Level;

public final class MapSync extends JavaPlugin{

    public DatabaseManager databaseManager;
    public MapDataManager mapDataManager;
    public FileConfiguration dbconfig, config, mapdata;
    public ArrayList<String> MAP_LORE;
    public String ALREADY_SYNC;
    public String SUCCESS_SYNC;
    public String CANNOT_COPY;
    public String NO_PERMISSION;
    public GriefPreventionHook griefPreventionHook = null;
    public String servername;
    public boolean copyright;
    public boolean isPaperCB = false;
    public boolean needMapId = false;
    public static MapSync instance;
    public String colors_field, getMapFunctionName; // For reflection

    @Override
    public void onEnable() {
        instance = this;
        setup();
        createTable();
    }

    @Override
    public void onDisable() {
        try {
            saveAll();
        } catch (IOException e) {
            this.getLogger().log(Level.SEVERE, "Something went wrong when disabling MapSync.");
            e.printStackTrace();
        }
    }

    public void setup() {
        if(Bukkit.getVersion().contains("1.16")){
            getMapFunctionName = "a";
            colors_field = "colors";
        }else if(Bukkit.getVersion().contains("1.17") || Bukkit.getVersion().contains("1.18") || Bukkit.getVersion().contains("1.19") || Bukkit.getVersion().contains("1.20") || Bukkit.getVersion().contains("1.21")){
            getMapFunctionName = "a";
            colors_field = "g";
            Bukkit.getPluginManager().registerEvents(new MapRenderListener_1_17(this), this);
        }else{
            this.getLogger().log(Level.SEVERE, "Wrong Minecraft Version! Now only support 1.16 to 1.21");
            this.getPluginLoader().disablePlugin(this);
        }

        if(Bukkit.getVersion().contains("1.20.5") || Bukkit.getVersion().contains("1.20.6") || Bukkit.getVersion().contains("1.21")){
            needMapId = true;
            try {
                // Check if using Paper
                Class.forName("com.destroystokyo.paper.ParticleBuilder");
                isPaperCB = true;
            } catch (ClassNotFoundException ignored) {
            }
        }

        try{
            Properties props = new Properties();
            props.load(new FileReader(Paths.get("server.properties").toFile()));
            servername = props.getProperty("server-name");
        }catch (Exception e){
            this.getLogger().log(Level.SEVERE, "Please create a section call \"server-name\" in server.properties and give your server a unique name.");
            this.getPluginLoader().disablePlugin(this);
        }

        if(servername == null) {
            this.getLogger().log(Level.SEVERE, "Please create a section call \"server-name\" in server.properties and give your server a unique name.");
            this.getPluginLoader().disablePlugin(this);
        }

        if(!new File(this.getDataFolder(), "config.yml").exists()){
            saveResource("config.yml", false);
        }
        if(!new File(this.getDataFolder(), "database.yml").exists()){
            saveResource("database.yml", false);
        }
        if(!new File(this.getDataFolder(), "mapdata.yml").exists()){
            saveResource("mapdata.yml", false);
        }

        File datafolder = new File(getDataFolder(), "data");
        if(!datafolder.exists()) datafolder.mkdirs();

        config = YamlConfiguration.loadConfiguration(new File(getDataFolder()+File.separator+"config.yml"));
        dbconfig = YamlConfiguration.loadConfiguration(new File(getDataFolder()+File.separator+"database.yml"));
        mapdata = YamlConfiguration.loadConfiguration(new File(getDataFolder()+File.separator+"mapdata.yml"));
        MAP_LORE = (ArrayList<String>) getConfig().getStringList("mapitem.lore");
        ALREADY_SYNC = ChatColor.translateAlternateColorCodes('&', getConfig().getString("message.already-sync"));
        SUCCESS_SYNC = ChatColor.translateAlternateColorCodes('&', getConfig().getString("message.success-sync"));
        NO_PERMISSION = ChatColor.translateAlternateColorCodes('&', getConfig().getString("message.no-permission"));

        try{
            databaseManager = new DatabaseManager(this);
        }catch (Exception e){
            this.getPluginLoader().disablePlugin(this);
            this.getLogger().log(Level.SEVERE, "Unable to connect to MYSQL database. Please check if database.yml contains the correct database information.");
        }

        mapDataManager = new MapDataManager(this);

        if(Bukkit.getPluginManager().isPluginEnabled("MysqlPlayerDataBridge")){
            Bukkit.getPluginManager().registerEvents(new SyncCompleteListener(), this);
            this.getLogger().log(Level.INFO, "MysqlPlayerDataBridge detected.");
        }else{
            Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(), this);
        }

        Bukkit.getPluginManager().registerEvents(new CartographyTableListener(this), this);

        Bukkit.getPluginManager().registerEvents(new MapRenderListener(this), this);

        if(getConfig().getBoolean("hooks.griefprevention") && getServer().getPluginManager().isPluginEnabled("GriefPrevention")){
            griefPreventionHook = new GriefPreventionHook();
            this.getLogger().log(Level.INFO, "Hooked into GriefPrevention");
            Bukkit.getPluginManager().registerEvents(new MapInitListener(this), this);
        }
    }

    public void saveAll() throws IOException {
        mapDataManager.saveMapSet();
        mapdata.save(new File(getDataFolder()+File.separator+"mapdata.yml"));
    }

    public void createTable(){
        try {
            Connection connection = databaseManager.getConnection();
            Statement statement = connection.createStatement();
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS MapSync (" +
                    "uuid BIGINT, map BLOB, primary key(uuid))");
            connection.close();
        } catch (SQLException e) {
            this.getLogger().log(Level.WARNING, "Unable to create table. Please check if database.yml contains the correct database information.");
            e.printStackTrace();
        }
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public FileConfiguration getMapdata() {
        return mapdata;
    }

    public MapDataManager getMapDataManager(){
        return mapDataManager;
    }

    public DatabaseManager getDatabaseManager(){
        return databaseManager;
    }

    public String getServername(){
        return servername;
    }
}
