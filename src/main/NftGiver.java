package main;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldguard.bukkit.RegionContainer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.logging.Logger;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import socket.Server;
import database.Database;

public class NftGiver extends JavaPlugin implements Server.Callback{
    Logger log = getLogger();
    private Database db;
    private static ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();

    @Override
    public void onEnable() {
        Server server = new Server(6666);
        server.registerCallBack(this);
        new Thread(server).start();
        log.info("Socket-Server passed");

        db = new Database();
        log.info("Connecting to MySQL passed");

        this.getCommand("nft").setExecutor(new CommandKit(server,db));
        log.info("CommandKit starting passed");

        getServer().getPluginManager().registerEvents(new EventListener(db), this);
        log.info("EventListener starting passed");

        log.info("NftGiver started");
    }

    @Override
    public void onDisable() {

    }

    @Override
    public void callingBack(String response) throws ParseException {
        if (response != null) {
            System.out.println("Callback response: " + response);
            Object obj = new JSONParser().parse(response);
            JSONObject jo = (JSONObject) obj;

            String type = (String) jo.get("type");
            if (type.equals("transferred_assets")){
                String asset_id = (String) jo.get("asset_id");
                String transaction_id = (String) jo.get("transaction_id");
                String account = (String) jo.get("account");
                String item = (String) jo.get("item");

                //System.out.println(type);
                //System.out.println(asset_ids);
                //System.out.println(transaction_id);
                //System.out.println(account);
                //System.out.println(item);

                Player player = Bukkit.getPlayer(account);
                try {
                    db.stake_nft(account,String.format("{\"%s\":\"%s\"}",asset_id,item));
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
                if(player != null) {
                    //inventory.addItem(new ItemStack(Material.valueOf(item))); //inventory.addItem(new ItemStack(Material.getMaterial(item.toUpperCase())));
                    giveItem(account,item);
                } else {
                    try {
                        db.store_nft(account,String.format("[\"%s\"]",item));
                    } catch (SQLException throwables) {
                        throwables.printStackTrace();
                    }
                }
            } else if(type.equals("transfer_assets_error")){
                String user_name = (String) jo.get("user_name");
                String asset_id = (String) jo.get("asset_id");
                String item_name = (String) jo.get("item");

                Player player = Bukkit.getPlayer(user_name);
                try {
                    db.stake_nft(user_name,String.format("{\"%s\":\"%s\"}",asset_id,item_name));
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
                if(player != null) {
                    giveItem(user_name,item_name);
                } else {
                    try {
                        db.store_nft(user_name,String.format("[\"%s\"]",item_name));
                    } catch (SQLException throwables) {
                        throwables.printStackTrace();
                    }
                }
            } else if(type.equals("successfully_unstaked")){
                String user_name = (String) jo.get("user_name");
                String asset_id = (String) jo.get("asset_id");
                String item_name = (String) jo.get("item");
                String transaction_id = (String) jo.get("transaction_id");

                Player player = Bukkit.getPlayer(user_name);
                player.sendMessage(String.format("You successfully unstaked %s with id: %s.\n Transaction ID: %s",item_name,asset_id,transaction_id));
            }
        } else {
            log.info("No response");
        }
    }

    protected static void giveItem(String account, String item){
        Bukkit.dispatchCommand(console, String.format("give %s %s",account,item));
    }

    public static WorldEditPlugin getWorldEdit(){
        Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("WorldEdit");
        if(plugin==null || !(plugin instanceof  WorldGuardPlugin)){
            return null;
        }
        return (WorldEditPlugin) plugin;
    }

    public static WorldGuardPlugin getWorldGuard(){
        Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("WorldGuard");
        if(plugin==null || !(plugin instanceof  WorldGuardPlugin)){
            return null;
        }
         return (WorldGuardPlugin) plugin;
    }
}