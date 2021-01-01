package com.ayansh.java.devices;

public class Main {

	public static void main(String[] args) {

		DeviceManagementApp app = DeviceManagementApp.getInstance();
		
		try{
			
			app.initializeApplication();
			
			app.assessActiveDevices();
			
		}
		catch (Exception e) {
			System.out.println("Error in Main: " + e.getMessage());
		}
		
		// Finish App.
		app.finish();
		
	}

}
