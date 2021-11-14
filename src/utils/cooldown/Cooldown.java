package utils.cooldown;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class Cooldown {

    private int MAX; //max cd store
    private int MAX_PERIOD; // millsec
    public String name; //identificator
    private Instant[] cd_store;

    public Cooldown(String name, int MAX, int MAX_PERIOD){
        this.name = name;
        this.MAX = MAX-1;
        this.MAX_PERIOD = MAX_PERIOD;
        this.cd_store = new Instant[MAX];
    }

    public boolean add(){
        for(int i = 0; i<= this.MAX; i++){
            if(this.cd_store[i] == null){
                this.cd_store[i] = Instant.now();
                return true;
            }
        }
        return false;
    }


    public int avaible_slots(){
        int result = 0;
        this.clear();
        for(int i = 0; i<=this.MAX;i++){
            if(this.cd_store[i] == null) result+=1;
        }
        return result;
    }

    public void clear(){
        for(int i = 0; i<=this.MAX; i++){
            if(this.cd_store[i] != null) {
                if(ChronoUnit.MILLIS.between(this.cd_store[i],Instant.now())>this.MAX_PERIOD){
                    this.cd_store[i] = null;
                }
            }
        }
    }

    public boolean check(){
        if(this.avaible_slots()<=0) return false;
        return true;
    }


}
