package database;

import org.json.simple.parser.ParseException;

import java.sql.SQLException;

public class Testdb {
    private static Database db;
    public static void main(String[] args) throws SQLException, ParseException {
        db = new Database();
        //System.out.println(db.store_nft("qv5ag.wam","[\"COBBLESTONE\",\"DIAMOND\"]"));
        //System.out.println(db.get_stored_nft("qv5ag.wam"));
        System.out.println(db.stake_nft("qv5ag.wam","{\"1099553757987\":\"DIAMOND\"}"));
        System.out.println(db.get_staked_nft("qv5ag.wam"));
        //System.out.println(db.unstake_nft("qv5ag.wam","DIAMOND"));
        //System.out.println(db.get_staked_nft("qv5ag.wam"));
    }
}
