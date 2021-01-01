package com.ayansh.java.pushnotifications;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.Callable;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

public class GetBlogLatestDataTimeStamp implements Callable<JSONObject>{

	private int app_id;
	
	public GetBlogLatestDataTimeStamp(int id){
		app_id = id;
	}
	
	@Override
	public JSONObject call() throws Exception {

		JSONObject result = new JSONObject();
		
		String blog_url = PushNotificationsApp.getInstance().getBlogURL(app_id);
		String url = blog_url + "/wp-content/plugins/hanu-droid/GetLatestDataTimeStamp.php";
		
		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httpPost = new HttpPost(url);
		
		try{
			
			// Execute HTTP Post Request 
	    	HttpResponse response = httpclient.execute(httpPost);
	    	
	    	InputStream in = response.getEntity().getContent();
			InputStreamReader isr = new InputStreamReader(in);
			BufferedReader reader = new BufferedReader(isr);
			
			StringBuilder sbuilder = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				sbuilder.append(line);
			}
			
			JSONObject response_data = new JSONObject(sbuilder.toString());
			String timestamp = response_data.getString("latest_data_timestamp");
			
			result.put("app_id", app_id);
			result.put("timestamp", timestamp);
			
		}
		catch(Exception e){
			result.put("app_id", app_id);
			result.put("timestamp", "hanu");
			System.out.println("Error in getting latest timestamp for: " + app_id + ". Error: " + e.getMessage() + ". Empty timestamp will be sent.");
		}
		
		return result;
		
	}

}