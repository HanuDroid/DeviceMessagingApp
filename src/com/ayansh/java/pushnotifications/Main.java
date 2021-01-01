package com.ayansh.java.pushnotifications;

import java.util.Calendar;

public class Main {

	public static void main(String[] args) {
		
		// Create Application Instance
		PushNotificationsApp app = PushNotificationsApp.getInstance();

		try {

			app.initializeApplication();
			
			if(args.length == 1){
				
				String input = args[0];
				String[] time = input.split(":");
				Calendar c = Calendar.getInstance();
				c.set(Calendar.HOUR_OF_DAY, Integer.valueOf(time[0]));
				c.set(Calendar.MINUTE, Integer.valueOf(time[1]));
				app.setTimeOfExecution(c);
			}
			
			app.loadApplicationIDs();
			
			// Send Notifications to Sync Data
			app.sendSyncNotifications();
			
			
		} catch (Exception e) {
			System.out.println("Error in Main: " + e.getMessage());
		}
		
		try {
			
			// Trigger execution shut down and wait.
			app.shutdownExecutor();
			
		} catch (Exception e) {
			System.out.println("Error in Shutdown Executor" + e.getMessage());
		}

		// Finish App.
		app.finish();
	}

}
