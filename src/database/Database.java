package database;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import utils.Utils;

import java.util.Iterator;
import java.util.Map;

import java.sql.*;
import java.util.Set;

import static utils.Utils.*;

public class Database{
    private static final String HOST = "localhost";
    private static final String PORT = "3306";
    private static final String DB_NAME = "mc_server";
    private static final String USER = "server";
    private static final String PASSWD = "passwd;


    private static Connection connection;

    {
        try {
            connection = DriverManager.getConnection("jdbc:mysql://" + HOST + ":" + PORT + "/" + DB_NAME, USER, PASSWD);
            System.out.println("MYSQL Connection established.");
        } catch (SQLException throwables) {
            System.out.println("MYSQL Connection error!");
            throwables.printStackTrace();
        }
    }

    private static ResultSet sqlQuery(String query) throws SQLException {
        Statement statement = connection.createStatement();
        return statement.executeQuery(query);
    }

    private static int sqlUpdate(String task) throws  SQLException{
        Statement statement = connection.createStatement();
        return statement.executeUpdate(task);
    }

    public static boolean checkExist(String table, String column, String value) throws SQLException {
        String query = String.format("SELECT COUNT(*) FROM %s WHERE %s = '%s'",table,column,value);
        ResultSet response = sqlQuery(query);
        if(response.next()) return toBoolean(response.getInt(1));
        return false;
    }

    public static String get_stored_nft(String user_name) throws SQLException {
        ResultSet response = sqlQuery(String.format("SELECT json_data FROM stored_nft WHERE user_name = '%s'",user_name));
        if(response.next()) return response.getString(1);
        return null;
    }

    public static String get_staked_nft(String user_name) throws  SQLException{
        ResultSet response = sqlQuery(String.format("SELECT json_data FROM staked_nft WHERE user_name = '%s'",user_name));
        if(response.next()) return response.getString(1);
        return null;
    }

    public static String get_stored_tokens(String user_name) throws SQLException{
        ResultSet response = sqlQuery(String.format("SELECT json_data FROM stored_tokens WHERE user_name = '%s'",user_name));
        if(response.next()) return response.getString(1);
        return null;
    }

    public static String get_claims(String user_name) throws SQLException {
        ResultSet response = sqlQuery(String.format("SELECT json_data FROM claims WHERE user_name = '%s'",user_name));
        if(response.next()) return response.getString(1);
        return null;
    }

    public static boolean claim(String user_name, String type) throws SQLException, ParseException { // type="16"
        if(checkExist("claims","user_name",user_name)){
            String previous_data = get_claims(user_name);
            JSONObject jo = (JSONObject) new JSONParser().parse(previous_data);
            if(jo.containsKey(type)){
                long claim_count = (long) jo.get(type);
                if(claim_count>0){
                    jo.put(type,claim_count-1);
                    return Utils.toBoolean(sqlUpdate(String.format("UPDATE claims SET json_data = '%s' WHERE user_name = '%s'",jo.toJSONString(),user_name)));
                }
            } else return false;
        }
        return false;
    }

    public static boolean unclaim(String user_name, String type) throws  SQLException, ParseException{
        if(checkExist("claims","user_name",user_name)){
            String previous_data = get_claims(user_name);
            JSONObject jo = (JSONObject) new JSONParser().parse(previous_data);
            if(jo.containsKey(type)){
                long claim_count = (long) jo.get(type);
                jo.put(type,claim_count+1);
                return Utils.toBoolean(sqlUpdate(String.format("UPDATE claims SET json_data = '%s' WHERE user_name = '%s'",jo.toJSONString(),user_name)));
            }

        } return false;
    }

    public static int store_nft(String user_name, String json_data) throws SQLException, ParseException {
        if (!checkExist("stored_nft", "user_name", user_name)) {
            return sqlUpdate(String.format("INSERT INTO stored_nft VALUES ('%s', '%s')", user_name, json_data));
        } else {
            String previous_data = get_stored_nft(user_name);
            json_data = uniteJsonArrays(new String[]{previous_data, json_data});
            return sqlUpdate(String.format("UPDATE stored_nft SET json_data = '%s' WHERE user_name = '%s'",json_data,user_name));
        }
    }

    public static int stake_nft(String user_name, String json_data) throws SQLException, ParseException {
        if(!checkExist("staked_nft","user_name",user_name)){
            return sqlUpdate(String.format("INSERT INTO staked_nft VALUES ('%s','%s')",user_name,json_data));
        } else {
            String previous_data = get_staked_nft(user_name);
            JSONObject jo_previous_data = (JSONObject) new JSONParser().parse(previous_data);
            JSONObject jo_new_data = (JSONObject) new JSONParser().parse(json_data);
            jo_new_data.putAll(jo_previous_data);
            return sqlUpdate(String.format("UPDATE staked_nft SET json_data = '%s' WHERE user_name = '%s'",jo_new_data.toJSONString(),user_name));
        }
    }

    public static int store_tokens(String user_name, String token_name, double amount) throws SQLException, ParseException {
        JSONObject jo_new_data = new JSONObject();
        jo_new_data.put(token_name,amount);
        if(!checkExist("stored_tokens","user_name",user_name)){
            return sqlUpdate(String.format("INSERT INTO stored_tokens VALUES ('%s','%s')",user_name,jo_new_data.toJSONString()));
        } else{
            String previous_data = get_stored_tokens(user_name);
            JSONObject jo_previous_data = (JSONObject) new JSONParser().parse(previous_data);
            if(jo_previous_data.containsKey(token_name)){
                jo_new_data.putAll(jo_previous_data);
                double previous_amount_token = (double) jo_previous_data.get(token_name);
                amount = previous_amount_token+amount;
                jo_new_data.put(token_name,amount);
            } else{
                jo_new_data.putAll(jo_previous_data);
            }
            return sqlUpdate(String.format("UPDATE stored_tokens SET json_data = '%s' WHERE user_name = '%s'",jo_new_data.toJSONString(),user_name));
        }
    }

    public static int drop_stored_nft(String user_name) throws  SQLException{
        if(checkExist("stored_nft","user_name",user_name)){
            return sqlUpdate(String.format("DELETE FROM stored_nft WHERE user_name='%s'",user_name));
        } return 0; // else
    }

    public static int drop_stored_tokens(String user_name) throws SQLException{
        if(checkExist("stored_tokens","user_name",user_name)){
            return sqlUpdate(String.format("DELETE FROM stored_tokens WHERE user_name = '%s'",user_name));
        } return 0;
    }

    public static String unstake_nft(String user_name, String nft_name) throws SQLException, ParseException { // returns asset id
        if(checkExist("staked_nft","user_name",user_name)){
            String previous_data = get_staked_nft(user_name);
            JSONObject jo = (JSONObject) new JSONParser().parse(previous_data);
            Iterator<String> keys = jo.keySet().iterator();
            while(keys.hasNext()){
                String key = keys.next();
                if(jo.get(key).equals(nft_name)){
                    jo.remove(key);
                    sqlUpdate(String.format("UPDATE staked_nft SET json_data = '%s' WHERE user_name = '%s'",jo.toJSONString(),user_name));
                    return key;
                }
            }
        }
        return null;
    }
/*
    public static void main(String[] args) throws SQLException, ParseException {
        System.out.println(get_stored_nft("qv5ag.wam"));
        String json_stored_nft = get_stored_nft("qv5ag.wam");
        Object obj = new JSONParser().parse(json_stored_nft);
        JSONArray stored_nft = (JSONArray) obj;
        for(Object nft: stored_nft){
            System.out.println(nft);
        }
    }

 */
}
