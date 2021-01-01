package com.ayansh.java.common;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

	private static DBConnection instance;
	protected Connection dbConnection;
	
	public static DBConnection getInstance(String dbURL, String user, String pwd) throws SQLException{
		
		if(instance == null){
			instance = new DBConnection(dbURL, user, pwd);
		}
		
		return instance;
	}
	
	public static DBConnection getInstance(){

		return instance;
	}

	private DBConnection(String dbURL, String user, String pwd) throws SQLException{
		dbConnection = DriverManager.getConnection(dbURL, user, pwd);
	}
	
	public Connection getDBConnection(){
		return dbConnection;
	}
	
}