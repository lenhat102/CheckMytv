/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package database;

import classinfo.MytvData;
import classinfo.SummaryData;
import classinfo.YoutubeData;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Lenovo
 */
public class GetDataFromSql {

    public static Connection conn;
    public static Statement st;

    public static HashMap<String, SummaryData> getSummaryHash(String tbl,HashMap<String, List<MytvData>> hashDetail) {

        HashMap< String, SummaryData> hashSummary = new LinkedHashMap<>();
        for (Map.Entry<String, List<MytvData>> entry : hashDetail.entrySet()) {
            String clientMac = entry.getKey();
            List<MytvData> dataList = entry.getValue();
            double totalDelay = 0;
            int totalRetran = 0;
            int totalRequests = dataList.size();
            double totalSpeed = 0;
            Set<String> uniqueUserAgents = new HashSet<>();

            for (MytvData mytvData : dataList) {
                totalDelay += mytvData.delay2Customer;
                if (mytvData.maxRtoDown > 500) {//Số lần rto>0.5s
                    totalRetran++;
                }
                totalSpeed += mytvData.loadSpeed;
                uniqueUserAgents.add(mytvData.userAgent);
            }

            double avgDelay = totalDelay / totalRequests;
            double avgSpeed = totalSpeed / totalRequests;
            String userAgentString = String.join("<->", uniqueUserAgents);

            SummaryData sd=new SummaryData();
            sd.avg_delay=avgDelay;
            sd.total_rto=totalRetran;
            sd.total_rq=totalRequests;
            sd.load_spd=avgSpeed;
            sd.user_agent=userAgentString;
            sd.date=tbl.replace("result_mytv_detail_", "").replace("_", "-");
            
            hashSummary.put(clientMac, sd);

        }
        
        return hashSummary;

    }

    public static HashMap<String, List<MytvData>> getDetailHash(String tbl) {

        HashMap< String, List<MytvData>> hashDelay = new LinkedHashMap<>();

        conn = SqlHelper.connDb();

        if (conn != null) {
            try {
                st = conn.createStatement();

                String cmd = "select client_mac,id,user_agent,FROM_UNIXTIME(timestamp) as time,load_speed/(1000*1000) as spd,max_rto_down*1000 as rto,delay2customer,"
                        + "vid_name,content_length,load_duration,vid_path, vid_quality, buffer_size,video_size"
                        + " from " + tbl + " order by vid_name,time";
                System.out.println("getDetailHash: " + cmd);

                ResultSet rs = null;

                try {
//                    JSONParser parser = new JSONParser();
                    rs = st.executeQuery(cmd);
                    while (rs.next()) {
                        String client_mac = rs.getString("client_mac");

                        if (hashDelay.containsKey(client_mac)) {
                            MytvData md = new MytvData();
                            md.time = rs.getString("time");
                            md.loadSpeed = rs.getDouble("spd");
                            md.maxRtoDown = rs.getInt("rto");
                            md.delay2Customer = rs.getDouble("delay2customer");
                            md.vidName = rs.getString("vid_name");
                            md.contentLength = rs.getInt("content_length");
                            md.loadDuration = rs.getDouble("load_duration");
                            md.vidPath = rs.getLong("vid_path");
                            md.vidQuality = rs.getString("vid_quality");
                            md.bufferSize = rs.getInt("buffer_size");
                            md.userAgent=rs.getString("user_agent");

                            hashDelay.get(client_mac).add(md);

                        } else {
                            List<MytvData> list = new LinkedList<>();
                            MytvData md = new MytvData();
                            md.time = rs.getString("time");
                            md.loadSpeed = rs.getDouble("spd");
                            md.maxRtoDown = rs.getInt("rto");
                            md.delay2Customer = rs.getDouble("delay2customer");
                            md.vidName = rs.getString("vid_name");
                            md.contentLength = rs.getInt("content_length");
                            md.loadDuration = rs.getDouble("load_duration");
                            md.vidPath = rs.getLong("vid_path");
                            md.vidQuality = rs.getString("vid_quality");
                            md.bufferSize = rs.getInt("buffer_size");
                            md.userAgent=rs.getString("user_agent");

                            list.add(md);

                            hashDelay.put(client_mac, list);

                        }

                    }

                    rs.close();
                } catch (SQLException ex) {
                    Logger.getLogger(GetDataFromSql.class.getName()).log(Level.SEVERE, null, ex);
                    System.out.println("Loi lenh getDetailHash: " + cmd);
                }

                st.close();
                conn.close();
            } catch (SQLException e) {
                Logger.getLogger(GetDataFromSql.class.getName()).log(Level.SEVERE, null, e);
                System.err.println("loi ket noi db");

            }

        }
        return hashDelay;

    }

    public static HashMap<String, HashMap<String, List<YoutubeData>>> getDetailYoutube(String tbl) {

        HashMap<String, HashMap<String, List<YoutubeData>>> hashDelay = new LinkedHashMap<>();

        conn = SqlHelper.connDb();

        if (conn != null) {
            try {
                st = conn.createStatement();

                String cmd = "select client_mac,ip_customer_str,ip_server_str,server_name,  value2customer,session_id,flow_id,"
                        + " (down_speed/(1000*1000)) as spd ,byte_total/(1000*1000) as total_byte,num_req_pkt,byte_total_up,isp,FROM_UNIXTIME(start_time) as time,"
                        + "CONVERT(info USING utf8) from " + tbl + " t";
                System.out.println("getDetailYoutub: " + cmd);

                ResultSet rs = null;

                try {
//                    JSONParser parser = new JSONParser();
                    rs = st.executeQuery(cmd);
                    while (rs.next()) {
                        String clientMac = rs.getString("client_mac");
                        String sessionId = rs.getString("session_id");
                        // Extract other columns similarly

                        YoutubeData youtubeData = new YoutubeData();
                        youtubeData.ipCustomerStr=rs.getString("ip_customer_str");
                        youtubeData.ipServerStr=rs.getString("ip_server_str");
                        youtubeData.serverName=rs.getString("server_name");
                        youtubeData.value2Customer=rs.getDouble("value2customer");
                        youtubeData.flowId=rs.getString("flow_id");
                        youtubeData.downSpeed=rs.getDouble("spd");
                        youtubeData.byteTotal=rs.getDouble("total_byte");
                        youtubeData.numReqPkt=rs.getInt("num_req_pkt");
                        youtubeData.byteTotalUp=rs.getDouble("byte_total_up");
                        youtubeData.isp=rs.getString("isp");
                        youtubeData.time=rs.getString("time");
                        

                        hashDelay.computeIfAbsent(clientMac, k -> new LinkedHashMap<>())
                                .computeIfAbsent(sessionId, k -> new LinkedList<>())
                                .add(youtubeData);

                    }

                    rs.close();
                } catch (SQLException ex) {
                    Logger.getLogger(GetDataFromSql.class.getName()).log(Level.SEVERE, null, ex);
                    System.out.println("Loi lenh getDetailHash: " + cmd);
                }

                st.close();
                conn.close();
            } catch (SQLException e) {
                Logger.getLogger(GetDataFromSql.class.getName()).log(Level.SEVERE, null, e);
                System.err.println("loi ket noi db");

            }

        }
        return hashDelay;

    }

}
