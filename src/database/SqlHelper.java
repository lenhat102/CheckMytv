/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package database;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author mypc
 */
public class SqlHelper {
//    public static String dbIp="localhost";
    public static String dbIp="192.168.100.102";
    public static String dbName="cem_network_probe";
    public static String dbPassword="123456";
    public static String dbUser="root";
    
    public static String dbNameG="cem_network_probe";
    
    public static Connection connDb() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection("jdbc:mysql://" + dbIp + ":3306/" + dbName, dbUser, dbPassword);
        } catch (SQLException ex) {
            Logger.getLogger(SqlHelper.class.getName()).log(Level.SEVERE, null, ex);
        }
        return conn;
    }
    
    public static Connection connDbGeneral() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection("jdbc:mysql://" + dbIp + ":3306/" + dbNameG, dbUser, dbPassword);
        } catch (SQLException ex) {
            Logger.getLogger(SqlHelper.class.getName()).log(Level.SEVERE, null, ex);
        }
        return conn;
    }
    
     
    
}
