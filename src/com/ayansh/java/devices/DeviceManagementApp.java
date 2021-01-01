package com.ayansh.java.devices;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.ayansh.java.common.Application;

public class DeviceManagementApp extends Application{

	private static DeviceManagementApp app;
	
	public static DeviceManagementApp getInstance(){
		
		if(app == null){
		
			app = new DeviceManagementApp();
			
		}
		
		return app;
		
	}
	
	private DeviceManagementApp(){
		
	}
	
	public void initializeApplication() throws IOException, SQLException{
		
		super.initializeApplication();
		
	}

	public void assessActiveDevices() throws Exception {
		
		String sql = "";
		SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd");
		
		Date now = new Date();
		long time = now.getTime() - 7*24*60*60*1000;
		Date seven_days_old = new Date();
		seven_days_old.setTime(time);
		
		String old_time = sdf.format(seven_days_old);
			
		
		// Mark Device registrations older than 7 days as IN-Active by Default
		sql = "UPDATE hanu_devices SET IsActive = '' WHERE date(CreatedAt) < '" + old_time + "'";
		
		PreparedStatement stmt = dbConnection.prepareStatement(sql);
		stmt.execute();

	
		// Set Active Flag based on Active Device Log of last 7 days
		sql = "UPDATE hanu_devices devices "
				+ "JOIN (SELECT DISTINCT InstanceID FROM hanu_active_devices) active "
				+ "ON devices.InstanceID = active.InstanceID "
				+ "SET devices.IsActive = 'X'";
		
		stmt = dbConnection.prepareStatement(sql);
		stmt.execute();
		
		// Truncate Table
		//sql = "DELETE FROM `hanu_active_devices` WHERE date(CreatedAt) < '" + old_time + "'";
		sql = "TRUNCATE hanu_active_devices";
				
		stmt = dbConnection.prepareStatement(sql);
		stmt.execute();
		
	}
	
}