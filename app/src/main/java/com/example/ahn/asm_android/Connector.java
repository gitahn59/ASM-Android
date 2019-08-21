package com.example.ahn.asm_android;

import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class Connector {
    private String serverJsonInfo;
    private String ip;
    private int port;
    private String code;
    public boolean isReady;

    public Connector(String code){
        this.isReady=false;
        this.code ="code="+code;
    }

    public void GetServerInfo(){
        String web = "http://~/code.php?"+this.code; // input your web server url
        StringBuilder sb = new StringBuilder();

        try{
            BufferedInputStream bis=null;
            URL url = new URL(web);
            HttpURLConnection huc = (HttpURLConnection)url.openConnection();
            huc.setConnectTimeout(3000);
            huc.setReadTimeout(3000);

            if(huc.getResponseCode() == 200){
                bis = new BufferedInputStream(huc.getInputStream());
                BufferedReader reader = new BufferedReader(new InputStreamReader(bis,"UTF-8"));
                String line = null;
                while(true) {
                    line = reader.readLine();
                    if(line==null)
                        break;
                    sb.append(line);
                }
            }
            Log.e("json",sb.toString());
            serverJsonInfo=sb.toString();
            this.SetServerInfo();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    //json to ip,port
    public void SetServerInfo(){
        try {
            JSONArray ja = new JSONArray(this.serverJsonInfo);

            String [] name = {"ip","port"};

            JSONObject json = ja.getJSONObject(0);
            this.ip=json.getString(name[0]);
            this.port=Integer.parseInt(json.getString(name[1]));
            isReady=true;

        }catch(JSONException je){
            je.printStackTrace();
        }
        Log.e("serverInfo",ip+":"+port);
    }

    public String GetIp(){
        return this.ip;
    }

    public int GetPort(){
        return this.port;
    }

}
