package com.jsh.erp.utils;

import com.alibaba.fastjson.JSONObject;
import com.google.common.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

/**
 * Created by jishenghua 2018-5-11 09:48:08
 *
 * @author jishenghua
 */
public class JsonUtils {

    public static JSONObject ok(){
        JSONObject obj = new JSONObject();
        JSONObject tmp = new JSONObject();
        tmp.put("message", "成功");
        obj.put("code", 200);
        obj.put("data", tmp);
        return obj;
    }

    public static List<Long> StrToLongList(String str){
        Type type = new TypeToken<List<Long>>() {}.getType();
        return JSONObject.parseObject(str,type);
    }

    public static List<String> spiltToStringList(String str, String regex){
        String newStr  = str.replace("[", "").replace("]","");
        String[] split = newStr.split(regex);
        return Arrays.asList(split);
    }

}
