package utils.cooldown;

import java.util.HashMap;

public class CooldownHandler{
    private HashMap<String, Cooldown> cooldowns = new HashMap<>();
    public String handler_name;
    private int MAX;
    private int MAX_PERIOD;

    public CooldownHandler(String name, int max, int max_period){
        this.handler_name = name;
        this.MAX = max;
        this.MAX_PERIOD = max_period;
    }

    public boolean check(String name){
        if(this.cooldowns.containsKey(name)){
            return this.cooldowns.get(name).check();
        } else{
            this.cooldowns.put(name,new Cooldown(name,this.MAX,this.MAX_PERIOD));
            return true;
        }
    }

    public int avaible_slots(String name){
        if(this.cooldowns.containsKey(name)){
            return this.cooldowns.get(name).avaible_slots();
        } else {
            return this.MAX;
        }
    }

    public boolean add(String name){
        if(this.cooldowns.containsKey(name)){
            return this.cooldowns.get(name).add();
        } else {
            this.cooldowns.put(name,new Cooldown(name,this.MAX,this.MAX_PERIOD));
            return this.cooldowns.get(name).add();
        }
    }
}
