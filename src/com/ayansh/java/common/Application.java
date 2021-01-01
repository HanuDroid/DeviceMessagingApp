/**
 * 
 */
package com.ayansh.java.common;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

/**
 * @author Varun Verma
 *
 */
public abstract class Application {

	protected Properties properties;
	protected Connection dbConnection;
	
	protected ExecutorService executor;
	protected int thread_limit;
	protected int await_termination_time;
	protected String filePath;
	protected JSONObject configData;
	
	protected HashMap<Integer,String> application_ids;
	
	protected Application(){
		
		properties = new Properties();
		application_ids = new HashMap<Integer,String>();
		
	}
	
	public void initializeApplication() throws IOException, SQLException{
		
		String user, pwd, dbURL, db_name;
		
		properties.load(getClass().getClassLoader().getResourceAsStream("config.properties"));
		
		// File Path
		filePath = properties.getProperty("file_location");
		configData = readJSONFile("config.json");

		// Take from properties file
		db_name = configData.getJSONObject("db").getString("gcm_dbname");
		user = configData.getJSONObject("db").getString("user");
		pwd = configData.getJSONObject("db").getString("password");
		
		dbURL = "jdbc:mysql://" + db_name + "?noAccessToProcedureBodies=true";
		dbConnection = DBConnection.getInstance(dbURL, user, pwd).getDBConnection();
		
		// Thread Limit
		thread_limit = Integer.valueOf(properties.getProperty("thread_limit"));
		
		await_termination_time = Integer.valueOf(properties.getProperty("await_termination_time"));
						
	}
	
	public void initializeExecutorService(){
		
		// Initialize Executor Service
		executor = Executors.newFixedThreadPool(thread_limit);

	}
	
	public void loadApplicationIDs() throws Exception{
		
		String sql = "SELECT ID, BlogURL from hanu_applications";
		
		Statement stmt = dbConnection.createStatement();

		ResultSet result = stmt.executeQuery(sql);
		
		if(result.next()){
			
			do{
				
				application_ids.put(result.getInt(1), result.getString(2));
				
			}while(result.next());
			
		}
		
	}
	
	public String getBlogURL(int app_id){
		return application_ids.get(app_id);
	}
	
	protected void closeApplication(){
		
		try {
			dbConnection.close();
		} catch (SQLException e) {
			System.out.println("SQL Connection Close error: " + e.getMessage());
		}
		
	}
	
	public void finish(){
		
		closeApplication();

		System.out.println("I'm Done!");
		System.exit(0);
		
	}
	
	public ExecutorService getExecutor(){
		return executor;
	}

	public JSONObject getConfigurationData(){
		return configData;
	}
	
	public void shutdownExecutor() throws InterruptedException{
		
		// After all tasks are executed, shutdown the executer.
		executor.shutdown();
				
		// But wait. Wait until 3 minutes
		executor.awaitTermination(await_termination_time, TimeUnit.SECONDS);
		
	}

	private JSONObject readJSONFile(String fileName) throws IOException {
		
		JSONObject file_contents = new JSONObject();
		
		BufferedReader br = new BufferedReader(new FileReader(filePath + fileName));
		try {
		    StringBuilder sb = new StringBuilder();
		    String line = br.readLine();

		    while (line != null) {
		        sb.append(line);
		        sb.append(System.lineSeparator());
		        line = br.readLine();
		    }
		    
		    file_contents = new JSONObject(sb.toString());
		    
		} finally {
		    br.close();
		}
		
		return file_contents;
		
	}
		
}