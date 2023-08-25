/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package youtubecheck;

import classinfo.YoutubeData;
import static database.GetDataFromSql.getDetailYoutube;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author Lenovo
 */
public class YoutubeCheck {
    static String tblName = "result_youtube_detail_2023_08_17";
    
    public static void main(String[] args) {
        
        HashMap<String, HashMap<String, List<YoutubeData>>> hashMap=getDetailYoutube(tblName);
        
        int count=0;
        for (String client_mac : hashMap.keySet()) {
            
            HashMap<String, List<YoutubeData>> hashSession=hashMap.get(client_mac);
            
            
            for (String session_id : hashSession.keySet()) {
                
                List<YoutubeData> list=hashSession.get(session_id);
                
                for (YoutubeData ytb : list) {
                    count++;
                    System.out.println(count+" mac: "+client_mac+" session: "+session_id+" down_spd: "+ytb.downSpeed);
                    
                }
                
            }
            
        }
        
    }
}
