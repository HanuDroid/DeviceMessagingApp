package com.ayansh.java.pushnotifications;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;


public class GCM extends AbstractPNService{

	private static final String FCM_SEND_ENDPOINT = "https://fcm.googleapis.com/fcm/send";
	
	public GCM(int id, PNMessage msg, GCMInvoker invoker){
		
		super(id,msg,invoker);
		
	}
	

	@Override
	public void run() {
		
		int status = 100;
		List<Future<?>> gcm_task_list = new ArrayList<Future<?>>();
		
		try {
			
			PushNotificationsApp pn_app = PushNotificationsApp.getInstance();
			ExecutorService executor = pn_app.getExecutor();
			
			int size = pn_message.getRegistrationIDs().size();
			int packet_size = 950;
			int end;
			
			for(int start=0; start<=size; start+=packet_size){
				
				end = start + packet_size;
				
				if(end > size){
					end = size;
				}
				
				String api_key = pn_app.getConfigurationData().getJSONObject("gcm_tokens").getString(String.valueOf(app_id));
				System.out.println("GCM starting a sub task for index: " + start + " to " + end + " For App ID: " + app_id);
				PostGCMMessage post_gcm = new PostGCMMessage(start,end,api_key);
				gcm_task_list.add(executor.submit(post_gcm));
				
			}
			
			System.out.println("GCM waiting for sub-tasks to be finished For App ID: " + app_id);
			
			// Now wait till all are finished
			Iterator<Future<?>> i = gcm_task_list.iterator();
			while(i.hasNext()){
				i.next().get();
			}
			
			System.out.println("GCM tasks are finished. Now correct IDs For App ID: " + app_id);
			
			// Correct Canonical IDs
    		correctCanonicalIDs();
    		
    		// Remove Un-Registered IDs
    		removeUnRegisteredIDs();
			
			System.out.println("GCM Message sent for App ID: " + app_id + " and " + pn_message.getRegistrationIDs().size() + " IDs");
			
		} catch (Exception e) {
			System.out.println("GCM Error for App ID: " + app_id + ". Error: " + e.getMessage());
		}
		
		invoker.finishedGCMProcessing(status,pn_message);
	}
	
	private class PostGCMMessage implements Runnable{

		private int from, to;
		private String api_key;
		
		private PostGCMMessage(int f, int t, String ak){
			
			from = f;
			to = t;
			api_key = ak;
		}
		
		@Override
		public void run() {
			
			HttpClient httpclient = new DefaultHttpClient();  
	        HttpPost httpPost = new HttpPost(FCM_SEND_ENDPOINT);
	        
	        httpPost.setHeader("Content-Type", "application/json");
	        httpPost.setHeader("Authorization", "key=" + api_key);
	        
	        // Body of the GCM Request
	        JSONObject request_body = new JSONObject();
	        
	        // Collapse Key
	        request_body.put("collapse_key", pn_message.getCollapse_key());
	        
	        // Time to live
	        request_body.put("time_to_live", pn_message.getTime_to_live());
	        
	        // Recipients
	        JSONArray reg_ids = new JSONArray();
	        
	        for(int i=from; i<to; i++){
	        	reg_ids.put(pn_message.getRegistrationIDs().get(i));
	        }

	        
	        request_body.put("registration_ids", reg_ids);
	                
	        // Payload Data
	        JSONObject payload_data = new JSONObject();
	        payload_data.put("message", "PerformSync");
	        payload_data.put("latest_data_timestamp", blog_latest_data_timestamp);
	        
	        // Temporarily send Hanu so that its Sync is done all times.
	        //payload_data.put("latest_data_timestamp", "hanu");
	        
	        request_body.put("data", payload_data);
	        
	        try{
	        	
	        	httpPost.setEntity(new StringEntity(request_body.toString()));
				
		        // Execute HTTP Post Request 
		    	HttpResponse response = httpclient.execute(httpPost);
		    	
		    	int response_code = response.getStatusLine().getStatusCode();
		    	
		    	if (response_code == 200){
		    		
		    		InputStream in = response.getEntity().getContent();
		    		InputStreamReader isr = new InputStreamReader(in);
		    		BufferedReader reader = new BufferedReader(isr);
		    		
		    		StringBuilder sbuilder = new StringBuilder();
		    		String line;
		    		while ((line = reader.readLine()) != null) {
		    			sbuilder.append(line);
		    		}
		    		
		    		JSONObject response_data = new JSONObject(sbuilder.toString());
		    		JSONArray results = response_data.getJSONArray("results");
		    		
		    		for(int i=0; i<results.length(); i++){
		    			
		    			String id = reg_ids.getString(i);
						
		    			JSONObject result = results.getJSONObject(i);
		    			
		    			if(result.has("error")){
		    				// Error !
		    				if(	result.getString("error").contentEquals("InvalidRegistration") ||
		    					result.getString("error").contentEquals("NotRegistered")){
		    					pn_message.addIncorrectID(id);
		    				}
		    			}
		    			else{
		    				// Success
		    				pn_message.addSuccessID(id);
		    				
		    				if(result.has("registration_id")){
		    					// This is case of canonical id
		    					pn_message.addCanonocalID(id, result.getString("registration_id"));
		    				}
		    			}
		    			
		    		}
		    	}
		    	else{
		    		System.out.println("GCM Sub-Task for index: " + from + " - " + to + ". HTTP Status: " + response_code);
		    		System.out.println("Some error: " + response.toString());
		    	}
		    		
	        }catch(Exception e){
	        	System.out.println("GCM Sub-Task for index: " + from + " - " + to + " Post Message. Error: " + e.getMessage());
	        }
					
		}
		
	}
	
}