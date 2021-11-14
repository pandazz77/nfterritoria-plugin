package main;

import database.Database;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.sql.SQLException;

public class EventListener implements Listener {
    private Database db;

    public EventListener(Database db_obj){
        db = db_obj;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) throws SQLException, ParseException {
        Player player = event.getPlayer();
        String player_name = player.getName();
        if(db.checkExist("stored_nft","user_name",player_name)){

            String json_stored_nft = db.get_stored_nft(player_name);
            Object obj = new JSONParser().parse(json_stored_nft);
            JSONArray stored_nft = (JSONArray) obj;
            for(Object nft: stored_nft) {
                NftGiver.giveItem(player_name,nft.toString());
            }
            db.drop_stored_nft(player_name);

        }
    }
}
