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
import java.io.FileOutputStream;
import java.io.IOException;
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
public class MytvCheck {

    /**
     * @param args the command line arguments
     */
    static String fileTemplatePath = "F:\\checkMytv\\template\\checkMytv.xlsx";
    static String folderExportExcel = "F:\\checkMytv\\output";
//    static String tblName = "result_mytv_detail_";
    static String tblName = "result_mytv_detail_2023_08_18";
    static String mac_user = "";
    static String startTime = "2023-08-15";
    static String stopTime = "2023-08-15";

    public static Connection conn;
    public static Statement st;

    public static void main(String[] args) throws IOException, SQLException {

        ExportFile();
    }

    public static void ExportFile() throws IOException, SQLException {

//        String excelFilePath = "E:\\mytv_stall_upd.xlsx";
//        List<MytvData> list = readMytvData(excelFilePath);
//        System.out.println("list size: " + list.size());
        LocalDate start = LocalDate.parse(startTime);
        LocalDate end = LocalDate.parse(stopTime);
        List<String> totalDates = new ArrayList<>();
        while (!start.isAfter(end)) {
            totalDates.add(start.toString().replace("-", "_"));
            start = start.plusDays(1);
        }
        
        String dateTime = "";

        List<String> listTableGrby = new LinkedList<>();

        conn = SqlHelper.connDb();
        if (conn != null) {
            for (String totalDate : totalDates) {
                DatabaseMetaData dbm = conn.getMetaData();
                String tblHtop = tblName + totalDate;

                ResultSet tables = dbm.getTables(null, null, tblHtop, null);

                if (tables.next()) {

                    listTableGrby.add(tblHtop);

                }

            }
        }
        conn.close();

        String tblmtv = "";
        String tblsmr = "";
        if (listTableGrby.size() > 0) {

            for (String tbl : listTableGrby) {
                dateTime = tbl.replace("result_mytv_detail_", "");
                if ("".equals(tblmtv)) {
                    tblmtv += "select client_mac,id,user_agent,timestamp,load_speed,max_rto_down,"
                            + "delay2customer,vid_name,content_length,load_duration,vid_path, vid_quality, buffer_size,video_size"
                            + " from " + tbl + "";
                } else {
                    tblmtv += " union all \n"
                            + "select client_mac,id,user_agent,timestamp,load_speed,max_rto_down,"
                            + "delay2customer,vid_name,content_length,load_duration,vid_path, vid_quality, buffer_size,video_size"
                            + " from " + tbl + "";

                }

                if ("".equals(tblsmr)) {
                    tblsmr += "select client_mac,user_agent,max_rto_down,delay2customer,load_speed"
                            + ""
                            + " from " + tbl + "";
                } else {
                    tblsmr += " union all \n"
                            + "select client_mac,user_agent,max_rto_down,delay2customer,load_speed"
                            + " from " + tbl + "";

                }

            }
            tblmtv = "(" + tblmtv + ")";
            tblsmr = "(" + tblsmr + ")";

        }

        HashMap<String, List<MytvData>> hashDetail = getDetailHash(tblmtv);

        HashMap<String, SummaryData> hashSummary = getSummaryHash(tblsmr,hashDetail);

        HashMap<String, List<MytvData>> hashFailSmr = new LinkedHashMap<>();

        XSSFWorkbook myWorkBook = new XSSFWorkbook(fileTemplatePath);

        CellStyle style = myWorkBook.createCellStyle();

        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setWrapText(true);
        
        
        CellStyle style2= myWorkBook.createCellStyle();

        style2.setAlignment(HorizontalAlignment.CENTER);
        style2.setVerticalAlignment(VerticalAlignment.CENTER);
        style2.setBorderBottom(BorderStyle.THIN);
        style2.setBorderTop(BorderStyle.THIN);
        style2.setBorderRight(BorderStyle.THIN);
        style2.setBorderLeft(BorderStyle.THIN);
        style2.setWrapText(false);
        

        XSSFSheet sheetBW1 = myWorkBook.getSheetAt(0);
        XSSFSheet sheetBW2 = myWorkBook.getSheetAt(1);
        XSSFSheet sheetBW3 = myWorkBook.getSheetAt(2);

        int rowNumber = 0;
        Row row, rowDetail;
        Cell cell;

        for (String key : hashDetail.keySet()) {
            List<MytvData> list = hashDetail.get(key);
//            System.out.println("key: " + key + " size: " + list.size());
            MytvData previousData = null;
            for (MytvData currentData : list) {
                if (previousData != null) {
                    long currentNumber = currentData.vidPath;
                    long previousNumber = previousData.vidPath;

                    if (currentNumber == previousNumber + 1) {

                    } else {

                        if (previousData.maxRtoDown > 100 && previousData.loadDuration > 2) {

                            previousData.note = "Error";

                        }
                    }
                }
                previousData = currentData;
            }

            List<MytvData> listWithSameNote = mergeDuplicateNotesAndVidPaths(list);

            if (listWithSameNote.size() > 0) {
                hashFailSmr.put(key, listWithSameNote);
            }
            //Sheet3
            for (MytvData data : listWithSameNote) {
                rowNumber = rowNumber + 1;
                if (sheetBW3.getRow(rowNumber) == null) {
                    row = sheetBW3.createRow(rowNumber);
                } else {
                    row = sheetBW3.getRow(rowNumber);
                }
                row.createCell(0).setCellValue(key);
                row.createCell(1).setCellValue(data.time);
                row.createCell(2).setCellValue(data.loadSpeed);
                row.createCell(3).setCellValue(data.maxRtoDown);
                row.createCell(4).setCellValue(data.delay2Customer);
                row.createCell(5).setCellValue(data.vidName);
                row.createCell(6).setCellValue(data.contentLength);
                row.createCell(7).setCellValue(data.loadDuration);
                row.createCell(8).setCellValue(data.vidPath);
                row.createCell(9).setCellValue(data.vidQuality);
                row.createCell(10).setCellValue(data.bufferSize);

                for (int i = 0; i < 11; i++) {

                    cell = row.getCell(i);
                    cell.setCellStyle(style);
                }
            }

        }

//        System.out.println("size: " + hashFailSmr.size());
        rowNumber = 0;
        for (String key : hashFailSmr.keySet()) {

            List<MytvData> list = hashFailSmr.get(key);

            //Sheet1
            rowNumber = rowNumber + 1;
            if (sheetBW1.getRow(rowNumber) == null) {
                row = sheetBW1.createRow(rowNumber);
            } else {
                row = sheetBW1.getRow(rowNumber);
            }
            row.createCell(0).setCellValue(rowNumber);
            row.createCell(1).setCellValue(key);
            row.createCell(2).setCellValue(list.size());
            row.createCell(3).setCellValue(hashSummary.get(key).avg_delay);
            row.createCell(4).setCellValue(hashSummary.get(key).total_rto);
            row.createCell(5).setCellValue(hashSummary.get(key).total_rq);
            row.createCell(6).setCellValue(hashSummary.get(key).load_spd);
            row.createCell(7).setCellValue(hashSummary.get(key).user_agent);

            for (int i = 0; i < 7; i++) {

                cell = row.getCell(i);
                cell.setCellStyle(style);
            }
            cell = row.getCell(7);
            cell.setCellStyle(style2);

        }

        //Sheet2
        rowNumber = 0;
        for (String key : hashFailSmr.keySet()) {

            List<MytvData> list = hashDetail.get(key);
            for (MytvData data : list) {
                rowNumber = rowNumber + 1;
                if (sheetBW2.getRow(rowNumber) == null) {
                    row = sheetBW2.createRow(rowNumber);
                } else {
                    row = sheetBW2.getRow(rowNumber);
                }
                row.createCell(0).setCellValue(key);
                row.createCell(1).setCellValue(data.time);
                row.createCell(2).setCellValue(data.loadSpeed);
                row.createCell(3).setCellValue(data.maxRtoDown);
                row.createCell(4).setCellValue(data.delay2Customer);
                row.createCell(5).setCellValue(data.vidName);
                row.createCell(6).setCellValue(data.contentLength);
                row.createCell(7).setCellValue(data.loadDuration);
                row.createCell(8).setCellValue(data.vidPath);
                row.createCell(9).setCellValue(data.vidQuality);
                row.createCell(10).setCellValue(data.bufferSize);
                row.createCell(11).setCellValue(data.note);

                for (int i = 0; i < 12; i++) {

                    cell = row.getCell(i);
                    cell.setCellStyle(style);
                }
            }

        }

        FileOutputStream fileOut;

        String fileOutPath = folderExportExcel + "\\" + "reportMytv_" +dateTime+ ".xlsx";

        fileOut = new FileOutputStream(fileOutPath);
        myWorkBook.write(fileOut);
        
        fileOut.close();
        myWorkBook.close();
        System.out.println("Completed: ");
    }

    public static List<MytvData> mergeDuplicateNotesAndVidPaths(List<MytvData> dataList) {
        List<MytvData> mergedList = new LinkedList<>();
        List<MytvData> objectsWithSameNote = new LinkedList<>();
        Set<MytvData> orderedSet = new LinkedHashSet<>();


        List<MytvData> consecutiveObjectsWithSameNote = new LinkedList<>();
        List<MytvData> consecutiveObjectsWithCase2 = new LinkedList<>();
        MytvData previousObject = null;
        
        //Đếm số lần xuất hiện của các vid_path với các quality khác nhau
        HashMap<Long,VidPath>  hashVidPath=new LinkedHashMap<>();
        for (MytvData md : dataList) {
            
            long vidPath=md.vidPath;
            if (hashVidPath.containsKey(vidPath)) {
                VidPath oldPath=hashVidPath.get(vidPath);
                if (!md.vidQuality.equals(oldPath.quality)) {
                    oldPath.count++;
                }
                hashVidPath.put(vidPath, oldPath);
                
                
            }else{
                VidPath newPath=new VidPath();
                newPath.count=1;
                newPath.quality=md.vidQuality;
                hashVidPath.put(vidPath, newPath);
                
            
            }
            
        }
        

        for (MytvData currentObject : dataList) {
            if (previousObject != null) {
                long currentNumber = currentObject.vidPath;
                long previousNumber = previousObject.vidPath;

                String current_vidq = currentObject.vidQuality;
                String prev_vidq = previousObject.vidQuality;
                
                int count_CurrentPath=hashVidPath.get(currentObject.vidPath).count;
                int count_PrePath=hashVidPath.get(previousObject.vidPath).count;

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
                        && count_PrePath==1 && count_CurrentPath==1) {
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
                        && secondsDifference >= 5 && secondsDifference <= 60) {
                    
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
//                if (previousObject.note.equals(currentObject.note) &&currentNumber!=previousNumber) {
//                    if (consecutiveObjectsWithSameNote.isEmpty()) {
//                        consecutiveObjectsWithSameNote.add(previousObject);
//                    }
//                    consecutiveObjectsWithSameNote.add(currentObject);
//                } else {
//                    if (consecutiveObjectsWithSameNote.size() >= 2) {
//                        objectsWithSameNote.addAll(consecutiveObjectsWithSameNote);
//                    }
//                    consecutiveObjectsWithSameNote.clear();
//                }
            }
            previousObject = currentObject;
        }
        objectsWithSameNote.addAll(orderedSet);

        return objectsWithSameNote;
    }
    
    public static class VidPath{
        Integer count;
        String quality;
        
    }

}
