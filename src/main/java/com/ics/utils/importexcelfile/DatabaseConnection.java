package com.ics.utils.importexcelfile;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseConnection {

    private static final Logger logger = Logger.getLogger(DatabaseConnection.class.getName());
    private static Connection connection;

    private DatabaseConnection() {}

    public static void connect() {
        Properties config = new Properties();
        try (FileInputStream fis = new FileInputStream("connect.ini")) {
            config.load(fis);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Cannot read connect.ini", ex);
            return;
        }

        String host     = config.getProperty("server",   "localhost");
        String port     = config.getProperty("port",     "3306");
        String database = config.getProperty("database", "");
        String user     = config.getProperty("user",     "root");
        String pass     = config.getProperty("pass",     "");
        String charset  = config.getProperty("charset",  "UTF-8");

        String url = "jdbc:mysql://" + host + ":" + port + "/" + database
                   + "?characterEncoding=" + charset
                   + "&useSSL=false";

        try {
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection(url, user, pass);
            logger.log(Level.INFO, "Connected to database: {0}", database);
        } catch (ClassNotFoundException ex) {
            logger.log(Level.SEVERE, "MySQL JDBC Driver not found", ex);
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Failed to connect to database: {0}", ex.getMessage());
        }
    }

    public static Connection getConnection() {
        return connection;
    }

    public static boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException ex) {
            return false;
        }
    }

    public static void disconnect() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ex) {
                logger.log(Level.WARNING, "Error closing connection", ex);
            } finally {
                connection = null;
            }
        }
    }
}
