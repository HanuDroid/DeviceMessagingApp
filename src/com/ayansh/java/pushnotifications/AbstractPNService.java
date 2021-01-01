package com.ayansh.java.pushnotifications;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map.Entry;

import com.ayansh.java.common.DBConnection;

public abstract class AbstractPNService implements Runnable{

	protected PNMessage pn_message;
	protected GCMInvoker invoker;
	protected String blog_latest_data_timestamp;
	protected int app_id;

	protected AbstractPNService(int id, PNMessage msg, GCMInvoker invoker){
		
		pn_message = msg;
		this.invoker = invoker;
		app_id =  id;
	}
	
	protected void correctCanonicalIDs() {
		
		Connection dbConn = DBConnection.getInstance().getDBConnection();
		
		String sql = "UPDATE hanu_devices SET RegId = ? WHERE RegId = ?";
		
		try {
			
			PreparedStatement statement = dbConn.prepareStatement(sql);
			
			Iterator<Entry<String,String>> i = pn_message.getCanonical_ids().entrySet().iterator();
			
			while(i.hasNext()){
				
				Entry<String,String> set = i.next();
				
				statement.setString(1, set.getValue());
				statement.setString(2, set.getKey());
				
				statement.addBatch();
				
			}
			
			statement.executeBatch();
			
			statement.close();
			
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		
	}
	
	protected void removeUnRegisteredIDs() {
		
		Connection dbConn = DBConnection.getInstance().getDBConnection();
		
		String sql = "DELETE FROM hanu_devices WHERE RegId = ?";
		
		try {
			
			PreparedStatement statement = dbConn.prepareStatement(sql);
			
			Iterator<String> i = pn_message.getIncorrect_ids().iterator();
			
			while(i.hasNext()){
				
				statement.setString(1, i.next());			
				statement.addBatch();
				
			}
			
			statement.executeBatch();
			
			statement.close();
			
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		
	}
	
	protected void setBlogLatestDataTimeStamp(String timestamp) {
		blog_latest_data_timestamp = timestamp;
	}
	
}