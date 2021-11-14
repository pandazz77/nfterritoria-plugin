package utils;

import org.json.simple.JSONArray;

import java.util.ArrayList;

public class Utils {

    public static boolean toBoolean(int x){
        if(x!=0) return true;
        return false;
    }

    public static String ArrayListToJson(ArrayList<Object> arraylist){
        JSONArray jsarray = new JSONArray();
        for(Object obj: arraylist){
            jsarray.add(obj);
        }
        return jsarray.toJSONString();
    }

    public static String uniteJsonArrays(String[] js_array){
        String result = "[";
        for(int i=0;i<js_array.length;i++){
            result+=js_array[i].substring(js_array[i].indexOf('[')+1,js_array[i].lastIndexOf(']'));
            if(i!=js_array.length-1) result+=",";
        }
        result+=']';
        return result;
    }
}
