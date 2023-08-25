/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mytvcheck;

import classinfo.MytvData;
import classinfo.SummaryData;
import database.GetDataFromSql;
import static database.GetDataFromSql.getDetailHash;
import static database.GetDataFromSql.getSummaryHash;
import static database.GetDataFromXls.readMytvData;
import database.SqlHelper;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.CodeSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 *
 * @author Lenovo
 */
public class MytvCheckError {

    /**
     * @param args the command line arguments
     */
    static String fileTemplatePath = "F:\\checkMytv\\template\\checkMytv.xlsx";
    static String folderExportExcel = "F:\\checkMytv\\output";
//    static String tblName = "result_mytv_detail_";
    static String tblName = "result_mytv_detail_2023_08_16";
    static String mac_user = "";
//    static String startTime = "2023-08-16";
//    static String stopTime = "2023-08-18";

    public static Connection conn;
    public static Statement st;

    public static void main(String[] args) throws IOException, SQLException {

        File folderExport = new File(folderExportExcel);
        if (!folderExport.exists()) {
            folderExport.mkdir();
        }

        readFileConfig();

        HashMap<String, SummaryData> hashErrorUser=getErrorUserHashMap(tblName);
        System.out.println("Ds cac khách hàng mytv lỗi: ");
        for (String client_mac : hashErrorUser.keySet()) {
            System.out.println("client_mac: "+client_mac);
            System.out.println("avg_delay : "+hashErrorUser.get(client_mac).avg_delay);
            System.out.println("total_rto : "+hashErrorUser.get(client_mac).total_rto);
            System.out.println("total_rq  : "+hashErrorUser.get(client_mac).total_rq);
            System.out.println("total_err : "+hashErrorUser.get(client_mac).err);
            System.out.println("avg_speed : "+hashErrorUser.get(client_mac).load_spd);
            System.out.println("user_agent : "+hashErrorUser.get(client_mac).user_agent);
            System.out.println("");
           
        }

    }

    public static String get_location_running() {
        String location_running = "";
        CodeSource codeSource = MytvCheckError.class.getProtectionDomain().getCodeSource();
        File jarFile;
        try {
            jarFile = new File(codeSource.getLocation().toURI().getPath());
            location_running = jarFile.getParentFile().getPath();
            System.out.println("location_running=" + location_running);
        } catch (URISyntaxException ex) {
            Logger.getLogger(MytvCheckError.class.getName()).log(Level.SEVERE, null, ex);
        }
        return location_running;
    }

    public static void readFileConfig() {

        String s = get_location_running();
        String path_file = s + "\\config.txt";

        File file = new File(path_file);
        if (file.isFile()) {
            try {
                FileReader fr = new FileReader(file);
                BufferedReader br = new BufferedReader(fr);

                String line = "";

                while ((line = br.readLine()) != null) {

                    if (line.startsWith("dbIP=")) {
                        SqlHelper.dbIp = line.split("=")[1];
                        System.out.println("dbIp: " + SqlHelper.dbIp);
                    } else if (line.startsWith("dbName=")) {
                        SqlHelper.dbName = line.split("=")[1];
                        System.out.println("dbName: " + SqlHelper.dbName);
                    } else if (line.startsWith("dbPassword=")) {
                        if (line.split("=").length == 1) {
                            SqlHelper.dbPassword = "";
                            System.out.println("dbpass: " + SqlHelper.dbPassword);
                        } else {
                            SqlHelper.dbPassword = line.split("=")[1];
                            System.out.println("dbpass: " + SqlHelper.dbPassword);
                        }

                    } else if (line.startsWith("dbUser=")) {
                        SqlHelper.dbUser = line.split("=")[1];
                        System.out.println("dbUser: " + SqlHelper.dbUser);
                    } else if (line.startsWith("tblName=")) {
                        tblName = line.split("=")[1];
                        System.out.println("tblName: " + tblName);

                    } 

                }
                br.close();
                fr.close();
            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } else {
            System.out.println("File config k ton tai");
        }
    }

    public static HashMap<String, SummaryData> getErrorUserHashMap(String tbl) throws IOException, SQLException {

        String dateTime = "";

        //Lấy danh sách chi tiết bảng
        HashMap<String, List<MytvData>> hashDetail = getDetailHash(tbl);

        //Tính các giá trị Delay2Cust Avg,Total Retran,Total Request,Load_speed Avg
        HashMap<String, SummaryData> hashSummary = getSummaryHash(tbl, hashDetail);

        HashMap<String, List<MytvData>> hashFailSmr = new LinkedHashMap<>();
        
        HashMap<String, SummaryData> hashErrorUser=new LinkedHashMap<>();
 

        for (String key : hashDetail.keySet()) {
            List<MytvData> list = hashDetail.get(key);
            MytvData previousData = null;
            //Lọc ra các vid_path
            for (MytvData currentData : list) {
                if (previousData != null) {
                    long currentNumber = currentData.vidPath;
                    long previousNumber = previousData.vidPath;
                    
                    

                    if (currentNumber == previousNumber + 1) {

                    } else {
                        //Nếu có bất thường trong vid_path và ngưỡng rto,load cao thì Chuyển note sang error
                        if (previousData.maxRtoDown > 100 && previousData.loadDuration > 2 && currentData.vidName.equals(previousData.vidName)) {

                            previousData.note = "Error";

                        }
                    }
                }
                previousData = currentData;
            }

            //lọc lại các trường hợp đặc biệt
            List<MytvData> listWithSameNote = mergeDuplicateNotesAndVidPaths(list);

            if (listWithSameNote.size() > 0) {
                hashFailSmr.put(key, listWithSameNote);
            }
            //Sheet3
            
        }

        //Truyền các khách hàng lỗi ra hash
        for (String key : hashFailSmr.keySet()) {

            List<MytvData> list = hashFailSmr.get(key);
           
            hashSummary.get(key).err = list.size();
                       
            SummaryData sd=hashSummary.get(key);
            hashErrorUser.put(key, sd);
            
        }
    
        return hashErrorUser;
    }

    public static List<MytvData> mergeDuplicateNotesAndVidPaths(List<MytvData> dataList) {
        List<MytvData> mergedList = new LinkedList<>();
        List<MytvData> objectsWithSameNote = new LinkedList<>();
        Set<MytvData> orderedSet = new LinkedHashSet<>();

        List<MytvData> consecutiveObjectsWithSameNote = new LinkedList<>();
        List<MytvData> consecutiveObjectsWithCase2 = new LinkedList<>();
        MytvData previousObject = null;

        //Đếm số lần xuất hiện của các vid_path với các quality khác nhau
        HashMap<Long, VidPath> hashVidPath = new LinkedHashMap<>();
        for (MytvData md : dataList) {

            long vidPath = md.vidPath;
            if (hashVidPath.containsKey(vidPath)) {
                VidPath oldPath = hashVidPath.get(vidPath);
                if (!md.vidQuality.equals(oldPath.quality)) {
                    oldPath.count++;
                }
                hashVidPath.put(vidPath, oldPath);

            } else {
                VidPath newPath = new VidPath();
                newPath.count = 1;
                newPath.quality = md.vidQuality;
                hashVidPath.put(vidPath, newPath);

            }

        }

        for (MytvData currentObject : dataList) {
            if (previousObject != null) {
                long currentNumber = currentObject.vidPath;
                long previousNumber = previousObject.vidPath;

                String current_vidq = currentObject.vidQuality;
                String prev_vidq = previousObject.vidQuality;

                int count_CurrentPath = hashVidPath.get(currentObject.vidPath).count;
                int count_PrePath = hashVidPath.get(previousObject.vidPath).count;

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S");
                LocalDateTime dateTime1 = LocalDateTime.parse(currentObject.time, formatter);
                LocalDateTime dateTime2 = LocalDateTime.parse(previousObject.time, formatter);

//                Duration duration = Duration.between(dateTime1, dateTime2);
                // Chuyển đổi thời gian thành timestamp (số giây kể từ mốc thời gian bắt đầu)
                long timestamp1 = dateTime1.toEpochSecond(java.time.ZoneOffset.UTC);
                long timestamp2 = dateTime2.toEpochSecond(java.time.ZoneOffset.UTC);

                // Chuyển đổi khoảng thời gian sang giây
                long secondsDifference = timestamp1 - timestamp2;

//                System.out.println("Thoi gian chenhkey : " + secondsDifference);
                //Nếu giống note và khác vidPath
                if (previousObject.note != null && currentObject.note != null && currentObject.note.equals(previousObject.note) && currentNumber != previousNumber
                        && count_PrePath == 1 && count_CurrentPath == 1) {
                    if (consecutiveObjectsWithSameNote.isEmpty()) {
                        consecutiveObjectsWithSameNote.add(previousObject);
                    }
                    consecutiveObjectsWithSameNote.add(currentObject);
                } else {
                    if (consecutiveObjectsWithSameNote.size() >= 2) {
                        orderedSet.addAll(consecutiveObjectsWithSameNote);
                    }
                    consecutiveObjectsWithSameNote.clear();
                }

                if (previousObject.note != null && currentObject.note != null && currentNumber == previousNumber && current_vidq.equals(prev_vidq)
                        && secondsDifference >= 5 && secondsDifference <= 60 ) {

                    if (consecutiveObjectsWithCase2.isEmpty()) {
                        consecutiveObjectsWithCase2.add(previousObject);
                    }
                    consecutiveObjectsWithCase2.add(currentObject);

//                    consecutiveObjectsWithCase2.add(previousObject);
                } else {
                    if (consecutiveObjectsWithCase2.size() >= 2) {
                        orderedSet.addAll(consecutiveObjectsWithCase2);
                    }
                    consecutiveObjectsWithCase2.clear();

                }

            }
            previousObject = currentObject;
        }
        objectsWithSameNote.addAll(orderedSet);

        return objectsWithSameNote;
    }

    public static class VidPath {

        Integer count;
        String quality;

    }

}
