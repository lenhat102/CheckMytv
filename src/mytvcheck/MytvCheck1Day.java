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
public class MytvCheck1Day {

    /**
     * @param args the command line arguments
     */
    static String fileTemplatePath = "F:\\checkMytv\\template\\checkMytv.xlsx";
    static String folderExportExcel = "F:\\checkMytv\\output";
    static String tblName = "result_mytv_detail_";
//    static String tblName = "result_mytv_detail_2023_08_18";
    static String mac_user = "";
    static String startTime = "2023-08-16";
    static String stopTime = "2023-08-18";

    public static Connection conn;
    public static Statement st;

    public static void main(String[] args) throws IOException, SQLException {

        File folderExport = new File(folderExportExcel);
        if (!folderExport.exists()) {
            folderExport.mkdir();
        }

        readFileConfig();

        LocalDate start = LocalDate.parse(startTime);
        LocalDate end = LocalDate.parse(stopTime);
        List<String> totalDates = new ArrayList<>();
        while (!start.isAfter(end)) {
            totalDates.add(start.toString().replace("-", "_"));
            start = start.plusDays(1);
        }

        List<String> ListTableHtop = new LinkedList<>();
        conn = SqlHelper.connDb();
        if (conn != null) {
            for (String totalDate : totalDates) {
                DatabaseMetaData dbm = conn.getMetaData();
//                    System.out.println(totalDate);
                String tblHtop = tblName + totalDate;

                ResultSet tables = dbm.getTables(null, null, tblHtop, null);

                if (tables.next()) {

                    ListTableHtop.add(tblHtop);

                }

            }
        }
        conn.close();

        for (String tblHtop : ListTableHtop) {

            ExportFile(tblHtop);

        }

    }
    
    
    public static String get_location_running() {
        String location_running = "";
        CodeSource codeSource = MytvCheck1Day.class.getProtectionDomain().getCodeSource();
        File jarFile;
        try {
            jarFile = new File(codeSource.getLocation().toURI().getPath());
            location_running = jarFile.getParentFile().getPath();
            System.out.println("location_running=" + location_running);
        } catch (URISyntaxException ex) {
            Logger.getLogger(MytvCheck1Day.class.getName()).log(Level.SEVERE, null, ex);
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

                    } else if (line.startsWith("start=")) {
                        startTime = line.split("=")[1];
                        System.out.println("start: " + startTime);
                    } else if (line.startsWith("stop=")) {
                        stopTime = line.split("=")[1];
                        System.out.println("stop: " + stopTime);

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

    public static void ExportFile(String tbl) throws IOException, SQLException {

 
        String dateTime = "";

        String tblmtv = "";
        String tblsmr = "";

        HashMap<String, List<MytvData>> hashDetail = getDetailHash(tbl);

        HashMap<String, SummaryData> hashSummary = getSummaryHash(tbl,hashDetail);

        HashMap<String, List<MytvData>> hashFailSmr = new LinkedHashMap<>();

        FileInputStream templateFileInputStream = new FileInputStream(fileTemplatePath);

        XSSFWorkbook myWorkBook = new XSSFWorkbook(templateFileInputStream);
        
        templateFileInputStream.close();

        CellStyle style = myWorkBook.createCellStyle();

        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setWrapText(true);

        CellStyle style2 = myWorkBook.createCellStyle();

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
                        //Nếu có bất thường trong vid_path và ngưỡng rto,load cao thì Chuyển note sang error
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
            
            hashSummary.get(key).err=list.size();
            row.createCell(0).setCellValue(rowNumber);
            row.createCell(1).setCellValue(key);
            row.createCell(2).setCellValue(list.size());
            
            row.createCell(3).setCellValue(hashSummary.get(key).avg_delay);
            row.createCell(4).setCellValue(hashSummary.get(key).total_rto);
            row.createCell(5).setCellValue(hashSummary.get(key).total_rq);
            row.createCell(6).setCellValue(hashSummary.get(key).load_spd);
            row.createCell(7).setCellValue(hashSummary.get(key).user_agent);
            dateTime = hashSummary.get(key).date;
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

        String fileOutPath = folderExportExcel + "\\" + "reportMytv_" + dateTime + ".xlsx";

        fileOut = new FileOutputStream(fileOutPath);
        myWorkBook.write(fileOut);
        myWorkBook.close();
        fileOut.close();

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

    public static class VidPath {

        Integer count;
        String quality;

    }

}
