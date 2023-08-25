/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package database;

import classinfo.MytvData;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 *
 * @author Lenovo
 */
public class GetDataFromXls {
    public static List<MytvData> readMytvData(String excelFilePath) {
        List<MytvData> excelDataList = new LinkedList<>();

        try (FileInputStream inputStream = new FileInputStream(new File(excelFilePath));
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet =  workbook.getSheetAt(0); 

            Iterator<Row> iterator = sheet.iterator();
            iterator.next(); 

            while (iterator.hasNext()) {
                Row currentRow = iterator.next();
                MytvData excelData = new MytvData();

                excelData.time=(currentRow.getCell(1).getLocalDateTimeCellValue().toString());
                excelData.loadSpeed=(currentRow.getCell(2).getNumericCellValue());
                excelData.maxRtoDown=((int)currentRow.getCell(3).getNumericCellValue());
                excelData.delay2Customer=(currentRow.getCell(4).getNumericCellValue());
                excelData.vidName=(currentRow.getCell(5).getStringCellValue());
                excelData.contentLength=((long) currentRow.getCell(6).getNumericCellValue());
                excelData.loadDuration=(currentRow.getCell(7).getNumericCellValue());
                excelData.vidPath=((long)currentRow.getCell(8).getNumericCellValue());
                excelData.vidQuality=(currentRow.getCell(9).getStringCellValue());
                excelData.bufferSize=((int) currentRow.getCell(10).getNumericCellValue());

                excelDataList.add(excelData);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return excelDataList;
    }
}
