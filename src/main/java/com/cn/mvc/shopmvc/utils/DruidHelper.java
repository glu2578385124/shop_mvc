package com.cn.mvc.shopmvc.utils;

import java.sql.*;
import java.util.Properties;

/**
 * Druid JDBC Helper
 */
public class DruidHelper {

    private String url = "jdbc:avatica:remote:url=http://hadoop-13:8888/druid/v2/sql/avatica/";
    private Properties conf = new Properties();
    private Connection connection;

    static {
        try {
            Class.forName("org.apache.calcite.avatica.remote.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get Druid SQL broker:8888/
     * @return
     */
    public Connection getConnection() {

        try {

            if (null == connection) {
                connection = DriverManager.getConnection(url, conf);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return connection;
    }

    /**
     * clean
     * @param connection
     * @param st
     * @param rs
     */
    public void close(Connection connection, Statement st, ResultSet rs) {
        try {
            if (rs!=null) {
                rs.close();
            }
            if (st!=null) {
                st.close();
            }
            if (connection!=null) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}