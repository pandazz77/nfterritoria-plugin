package main;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldguard.bukkit.RegionContainer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import constants.Claims;
import constants.Cooldowns;
import constants.Tokens;
import database.Database;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.json.simple.parser.ParseException;
import socket.Server;
import utils.Utils;
import utils.cooldown.CooldownHandler;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandKit implements CommandExecutor {

    private CooldownHandler nft_cd = new CooldownHandler("nft cooldown", Cooldowns.UNSTAKE_NFTS_MAX_SLOTS, Cooldowns.UNSTAKE_NFTS_PERIOD);
    private CooldownHandler tokens_cd = new CooldownHandler("tokens cooldown",Cooldowns.UNSTAKE_TOKENS_MAX_SLOTS,Cooldowns.UNSTAKE_TOKENS_PERIOD);

    private Server server;
    private Database db;

    private WorldGuardPlugin worldguard = NftGiver.getWorldGuard();
    private WorldEditPlugin worldedit = NftGiver.getWorldEdit();

    private RegionContainer container = worldguard.getRegionContainer();

    public CommandKit(Server server_object, Database db_object){
        server = server_object;
        db = db_object;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            String player_name = player.getName();

            if(args[0].equals("unstake")){
                ItemStack item_in_hand = player.getInventory().getItemInMainHand();
                ItemStack item_in_hand_copy = item_in_hand.clone();
                PlayerInventory inventory = player.getInventory();
                String item_name = item_in_hand.getData().getItemType().name().toLowerCase();
                int item_amount = item_in_hand.getAmount();


                if(item_name.contains("nft_")){
                    item_name = "nft:"+item_name.split("nft_")[1];
                }
                item_in_hand.setAmount(0); // removing item in hand

                if(Arrays.asList(Tokens.tokens).contains(item_name)){ // if item in hand is token(coin)
                    item_name = item_name.split("nft:")[1];
                    try {
                        if(tokens_cd.check(player_name)) {
                            db.store_tokens(player_name, item_name, item_amount);

                            tokens_cd.add(player_name);
                            int cd_avaible_slots = tokens_cd.avaible_slots(player_name);
                            player.sendMessage(String.format("You can unstake tokens %d more times.", cd_avaible_slots));
                        } else {
                            player.sendMessage("You have spent your limit, please wait.");
                            inventory.addItem(item_in_hand_copy);
                        }


                    } catch (Exception e) {
                        e.printStackTrace();
                        inventory.addItem(item_in_hand_copy);
                    }
                } else if(Arrays.asList(Tokens.nfts).contains(item_name)){ // if item in hand is NFT
                    try {
                        if(nft_cd.check(player_name)) {
                            String asset_id = db.unstake_nft(player_name, item_name);

                            nft_cd.add(player_name);
                            int cd_avaible_slots = nft_cd.avaible_slots(player_name);
                            player.sendMessage(String.format("You can unstake NFT`s %d more times.", cd_avaible_slots));

                            System.out.println(asset_id);
                            if (asset_id == null) inventory.addItem(item_in_hand_copy); // returns item
                            else {
                                server.send(String.format("{\"type\":\"unstake_nft\",\"user_name\":\"%s\",\"asset_id\":\"%s\",\"item\":\"%s\"}", player_name, asset_id, item_name));
                            }
                        } else {
                            player.sendMessage("You have spent your limit, please wait.");
                            inventory.addItem(item_in_hand_copy);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        inventory.addItem(item_in_hand_copy); // returns item
                    }
                }


            } else if(args[0].equals("claim")){
                String type;
                boolean claim_result;
                if(args.length!=2){
                    player.sendMessage("Not enough arguments.");
                    return true;
                }
                if(args[1].equals("16")) type = "16";
                else if(args[1].equals("8")) type = "8";
                else {
                    player.sendMessage("Invalid argument.");
                    return true;
                }

                try {
                    claim_result = db.claim(player_name,type);
                    if(claim_result) {
                        int bound = Integer.parseInt(type)/2;
                        if(!checkRegionAvailable(bound,player)){
                            player.sendMessage("This area is not suitable for private. Choose another one.");
                            db.unclaim(player_name,type);
                            return true;
                        }
                        Location player_location = player.getLocation();
                        RegionManager regions = container.get(player.getWorld());
                        BlockVector pt1 = new BlockVector(player_location.getBlockX() - bound, 0, player_location.getBlockZ() - bound);
                        BlockVector pt2 = new BlockVector(player_location.getBlockX() + bound, 256, player_location.getBlockZ() + bound);
                        String claim_name = String.format("%s-%d", player_name.split(".wam")[0], Utils.randint(0,99999));
                        ProtectedCuboidRegion region = new ProtectedCuboidRegion(claim_name, pt1, pt2);
                        DefaultDomain owners = new DefaultDomain();
                        owners.addPlayer(player_name);
                        region.setOwners(owners);
                        region.setPriority(Claims.default_priority);
                        regions.addRegion(region);
                        player.sendMessage("Successfully claim. Territory id: " + claim_name);
                    } else player.sendMessage("You have no accessible territory.");

                } catch (SQLException e) {
                    e.printStackTrace();
                } catch (ParseException e) {
                    e.printStackTrace();
                } catch (Exception e){
                    e.printStackTrace();
                }
            } else if(args[0].equals("claim-add")){
                String claim_name;
                String new_member;
                if(args.length!=3){
                    player.sendMessage("Not enough arguments.");
                    return true;
                }
                claim_name = args[1];
                new_member = args[2];

                RegionManager regions = container.get(player.getWorld());
                ProtectedRegion region = regions.getRegion(claim_name);
                if(region.equals(null)){
                    player.sendMessage(String.format("The territory with the ID \"%s\" does not exist. ",claim_name));
                    return true;
                }

                if(!region.getOwners().contains(player_name)){
                    player.sendMessage(String.format("You are not the owner of the \"%s\" territory.",claim_name));
                    return true;
                }

                if(region.getMembers().size()>=Claims.max_members){
                    player.sendMessage(String.format("The territory with the ID \"%s\" has the maximum number of participants.",claim_name));
                    return true;
                }

                DefaultDomain members = region.getMembers();
                members.addPlayer(new_member);
                player.sendMessage(String.format("You have successfully added \"%s\" to territories with ID \"%s\".",new_member,claim_name));
                return true;

            } else if(args[0].equals("claim-remove")){
                String claim_name;
                String rem_member;
                if(args.length!=3){
                    player.sendMessage("Not enough arguments.");
                    return true;
                }
                claim_name = args[1];
                rem_member = args[2];

                RegionManager regions = container.get(player.getWorld());
                ProtectedRegion region = regions.getRegion(claim_name);
                if(region.equals(null)){
                    player.sendMessage(String.format("The territory with the ID \"%s\" does not exist. ",claim_name));
                    return true;
                }

                if(!region.getOwners().contains(player_name)){
                    player.sendMessage(String.format("You are not the owner of the \"%s\" territory.",claim_name));
                    return true;
                }

                if(!region.getMembers().contains(rem_member)){
                    player.sendMessage(String.format("There is no \"%s\" member in the territory with the ID \"%s\"",rem_member,claim_name));
                    return true;
                }

                DefaultDomain members = region.getMembers();
                members.removePlayer(rem_member);
                player.sendMessage(String.format("You have successfully excluded the \"%s\" from your territory with the ID \"%s\".",rem_member,claim_name));
                return true;

            }
        }

        // If the player (or console) uses our command correct, we can return true
        return true;
    }

    private boolean checkRegionAvailable(int bound, Player player){
        Location player_loc = player.getLocation();

        Location min_loc = new Location(player.getWorld(),player_loc.getBlockX()-bound,0,player_loc.getBlockZ()-bound);
        Location max_loc = new Location(player.getWorld(),player_loc.getBlockX()+bound,256,player_loc.getBlockZ()+bound);

        for(Location location: getLocations(min_loc,max_loc)){
            ApplicableRegionSet set = container.get(player.getWorld()).getApplicableRegions(location);
            for (ProtectedRegion region : set) {
                if(!region.getId().equals(Claims.main_claim_id) || region.getId().equals(Claims.wilderness_claim_id) || region.getId().equals(Claims.private_claim_id)) return false;
            }
        }
        return true;
    }

    private List<Location> getLocations(Location l1, Location l2) {
        List<Location> locations = new ArrayList<Location>();
        int topBlockX = (l1.getBlockX() < l2.getBlockX() ? l2.getBlockX() : l1.getBlockX());
        int bottomBlockX = (l1.getBlockX() > l2.getBlockX() ? l2.getBlockX() : l1.getBlockX());

        int topBlockY = (l1.getBlockY() < l2.getBlockY() ? l2.getBlockY() : l1.getBlockY());
        int bottomBlockY = (l1.getBlockY() > l2.getBlockY() ? l2.getBlockY() : l1.getBlockY());

        int topBlockZ = (l1.getBlockZ() < l2.getBlockZ() ? l2.getBlockZ() : l1.getBlockZ());
        int bottomBlockZ = (l1.getBlockZ() > l2.getBlockZ() ? l2.getBlockZ() : l1.getBlockZ());

        for (int x = bottomBlockX; x <= topBlockX; x++) {
            for (int z = bottomBlockZ; z <= topBlockZ; z++) {
                for (int y = bottomBlockY; y <= topBlockY; y++) {
                    Location location = new Location(l1.getWorld(), x, y, z);
                    locations.add(location);
                }
            }
        }
        return locations;
    }
}
