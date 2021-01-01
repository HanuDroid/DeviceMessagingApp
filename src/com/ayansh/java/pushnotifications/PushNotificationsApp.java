/**
 * 
 */
package com.ayansh.java.pushnotifications;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Future;

import org.json.JSONObject;

import com.ayansh.java.common.Application;

/**
 * @author I041474
 *
 */
public class PushNotificationsApp extends Application implements GCMInvoker{

	private static PushNotificationsApp app;
	
	private int interval_size, interval_rate;
	
	private HashMap<String,HashMap<Integer,PNMessage>> regIDs;
	
	private Calendar now;
	
	public static PushNotificationsApp getInstance(){
		
		if(app == null){
		
			app = new PushNotificationsApp();
			
		}
		
		return app;
		
	}
	
	private PushNotificationsApp(){
		
		regIDs = new HashMap<String,HashMap<Integer,PNMessage>>();
		
		regIDs.put("Android", new HashMap<Integer,PNMessage>());
		regIDs.put("Windows", new HashMap<Integer,PNMessage>());
		
	}
	
	public void initializeApplication() throws IOException, SQLException{
		
		super.initializeApplication();
			
		interval_size = Integer.valueOf(properties.getProperty("interval_size"));
		interval_rate = Integer.valueOf(properties.getProperty("interval_rate"));
		
		initializeExecutorService();
		
		now = Calendar.getInstance();
	}
	
	public void setTimeOfExecution(Calendar time){
		
		now.setTimeInMillis(time.getTimeInMillis());
	}

	public void sendSyncNotifications() throws Exception {
		
		/*
		 * 1. Select Reg. IDs to be notified
		 * 2. Send notifications to Sync
		 */
		
		String platform, reg_id;
		int app_id;
		String sql = "";
		int am_offset_time, pm_offset_time;
		
		String device_status = properties.getProperty("device_status");
		
		// List of Future Tasks
		List<Future<?>> task_list = new ArrayList<Future<?>>();
		Future<?> task;

		HashMap<Integer,Future<JSONObject>> task_map = new HashMap<Integer,Future<JSONObject>>();
		
		if(device_status.contentEquals("active")){
			
			// Get Offset
			am_offset_time = Integer.valueOf(properties.getProperty("active_am_offset_time"));
			pm_offset_time = Integer.valueOf(properties.getProperty("active_pm_offset_time"));	
			
			// 24 hour base
			int hour = now.get(Calendar.HOUR_OF_DAY);
			int minute = now.get(Calendar.MINUTE);
			System.out.println("Time of Execution is = " + hour + ":" + minute);
			
			// Offset the hour.
			if(hour <= 12){
				hour = hour - am_offset_time;
			}
			else{
				hour = hour - 12 - pm_offset_time;
			}
			
			if(hour < 0){
				hour = 0;
			}
			
			minute = minute/interval_size;
			
			int interval_count = 60 / interval_size;
			
			int offset = interval_rate * interval_count * hour + interval_rate * minute;
			int limit = interval_rate;
						
			sql = "SELECT RegId, AppID, Platform FROM hanu_devices WHERE IsActive = 'X' order by ID Limit " + limit + " OFFSET " + offset;
						
		}
		else{
			
			// Get Offset
			am_offset_time = Integer.valueOf(properties.getProperty("dormant_am_offset_time"));
			pm_offset_time = Integer.valueOf(properties.getProperty("dormant_pm_offset_time"));
			
			// 24 hour base
			int hour = now.get(Calendar.HOUR_OF_DAY);
			int minute = now.get(Calendar.MINUTE);
			System.out.println("Time of Execution is = " + hour + ":" + minute);
			
			// Offset the hour.
			if(hour <= 12){
				hour = hour - am_offset_time;
			}
			else{
				hour = hour - 2*pm_offset_time;
			}
			
			if(hour < 0){
				hour = 0;
			}
			
			minute = minute/interval_size;
			
			int interval_count = 60 / interval_size;
			
			int offset = interval_rate * interval_count * hour + interval_rate * minute;
			int limit = interval_rate;
			
			
			sql = "SELECT RegId, AppID, Platform FROM hanu_devices WHERE IsActive = '' order by ID desc Limit " + limit + " OFFSET " + offset;
			
		}
		
		System.out.println(sql);
		
		Statement stmt = dbConnection.createStatement();

		ResultSet result = stmt.executeQuery(sql);
			
		if(result.next()){
			
			do{
				platform = result.getString(3);
				app_id = result.getInt(2);
				reg_id = result.getString(1);
				
				if(platform.contentEquals("WindowsPhone")){
					platform = "Windows";
				}
				
				HashMap<Integer,PNMessage> reg_id_bucket = regIDs.get(platform);
				
				if(reg_id_bucket.get(app_id) == null){
					reg_id_bucket.put(app_id, new PNMessage());
					task_map.put(app_id, executor.submit(new GetBlogLatestDataTimeStamp(app_id)));
				}
				
				PNMessage pn_message = reg_id_bucket.get(app_id);
				
				pn_message.addRegistrationID(reg_id);
				
			}while(result.next());
			
		}
		
		result.close();
		stmt.close();
		
		HashMap<Integer,PNMessage> reg_id_bucket;
		Iterator<Integer> iterator;
		
		// For Windows
		reg_id_bucket = regIDs.get("Windows");
		iterator = reg_id_bucket.keySet().iterator();

		while(iterator.hasNext()){
			
			app_id = iterator.next();
			
			// Get the result of Latest Data TimeStamp
			JSONObject timestamp = task_map.get(app_id).get();
			
			PNMessage pn_message = reg_id_bucket.get(app_id);
			WPN wpn = new WPN(app_id,pn_message,this);
			wpn.setBlogLatestDataTimeStamp(timestamp.getString("timestamp"));
			
			task = executor.submit(wpn);
			task_list.add(task);
			
		}
		
		// For Google
		reg_id_bucket = regIDs.get("Android");
		iterator = reg_id_bucket.keySet().iterator();
				
		while(iterator.hasNext()){
			
			app_id = iterator.next();
			
			// Get the result of Latest Data TimeStamp
			JSONObject timestamp = task_map.get(app_id).get();
			
			PNMessage pn_message = reg_id_bucket.get(app_id);
			GCM gcm = new GCM(app_id,pn_message,this);
			gcm.setBlogLatestDataTimeStamp(timestamp.getString("timestamp"));
			
			task = executor.submit(gcm);
			task_list.add(task);
			
		}
		
		Iterator<Future<?>> i = task_list.iterator();
		while(i.hasNext()){
			task = i.next();
			task.get();
		}
		
	}



	@Override
	public void finishedGCMProcessing(int status, PNMessage pn_message) {		
	}
	
}