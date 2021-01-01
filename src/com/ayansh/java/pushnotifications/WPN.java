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
import org.json.JSONObject;


public class WPN extends AbstractPNService{

	protected WPN(int id, PNMessage msg, GCMInvoker invoker) {
		
		super(id, msg, invoker);
		
	}

	private String access_token;
	private Future<?> access_token_task;
	
	@Override
	public void run() {
		
		int status = 100;
		List<Future<?>> wpn_task_list = new ArrayList<Future<?>>();
		
		try {
			
			ExecutorService executor = PushNotificationsApp.getInstance().getExecutor();
	        
			access_token_task = executor.submit(new GetAccessToken());
			
			Iterator<String> iterator = pn_message.getRegistrationIDs().iterator();
			
			System.out.println("WPN Starting Task for App ID: " + app_id + " and " + pn_message.getRegistrationIDs().size() + " IDs");
			
			while(iterator.hasNext()){
	        	
	        	String uri = iterator.next();       	
	        	PostWPNMessage post_wpn = new PostWPNMessage(uri);
	        	wpn_task_list.add(executor.submit(post_wpn));
	        	
	        }
	        
			System.out.println("WPN wait for tasks to be finished");
			
			// Now wait till all are finished
			Iterator<Future<?>> i = wpn_task_list.iterator();
			while(i.hasNext()){
				i.next().get();
			}
			
			System.out.println("WPN tasks are finished. Now correct IDs");
			
	        // Correct Canonical IDs
    		correctCanonicalIDs();
			
	        // Remove Un-Registered IDs
    		removeUnRegisteredIDs();
    		
    		System.out.println("WPN Message sent for App ID: " + app_id + " and " + pn_message.getRegistrationIDs().size() + " IDs");
			
		} catch (Exception e) {
			System.out.println("WPN Error for App ID: " + app_id + ". Error: " + e.getMessage());
		}
		
		invoker.finishedGCMProcessing(status,pn_message);
	}

	private class GetAccessToken implements Runnable {

		@Override
		public void run() {
			
			String sid, secret;
			JSONObject wpn_tokens = PushNotificationsApp.getInstance().getConfigurationData().getJSONObject("wpn_credentials");
			sid = wpn_tokens.getJSONObject(String.valueOf(app_id)).getString("sid");
			secret = wpn_tokens.getJSONObject(String.valueOf(app_id)).getString("secret");
			
			HttpClient httpclient = new DefaultHttpClient();  
	        HttpPost httpPost = new HttpPost("https://login.live.com/accesstoken.srf");
	        
	        httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");
	        
	        // Body of the GCM Request
	        String body = "grant_type=client_credentials&" 
	        			+ "client_id=" + sid 
	        			+ "&client_secret=" + secret 
	        			+ "&scope=notify.windows.com";
	        
	        try{
	        	
	        	httpPost.setEntity(new StringEntity(body));
				
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
		    		access_token = response_data.getString("access_token");
		    		
		    		System.out.println("WPN Yes, we have Access Token for App ID: " + app_id);
		    		
		    	}
		    
	        }
	        catch(Exception e){
	        	System.out.println("WPN Cound not get Access Token for App ID: " + app_id + ". Error: " + e.getMessage());
	        	access_token = "";
	        }
	        
			
			
		}
		
	}
	
	private class PostWPNMessage implements Runnable {

		private String uri;
		
		private PostWPNMessage(String uri){
			this.uri = uri;
		}
		
		@Override
		public void run() {
		
			String xml_data =	"<NotificationData>" + 
									"<Task>PerformSync</Task>" +
									"<LatestDataTimeStamp>" + blog_latest_data_timestamp + "</LatestDataTimeStamp>" +
								"</NotificationData>";
			
			HttpClient httpclient = new DefaultHttpClient();
			HttpPost httpPost = new HttpPost(uri);
			
			httpPost.setHeader("Content-Type", "application/octet-stream");
	        //httpPost.setHeader("Content-Length", String.valueOf(xml_data.length()));
	        httpPost.setHeader("X-WNS-Type", "wns/raw");
	        
	        try{
	        	
	        	if(access_token == null){
	        		access_token_task.get();
	        	}
	        			        
		        httpPost.setHeader("Authorization", "Bearer " + access_token);
	        	
	        	httpPost.setEntity(new StringEntity(xml_data));
		        
		        // Execute HTTP Post Request 
		    	HttpResponse response = httpclient.execute(httpPost);
		    	
		    	int response_code = response.getStatusLine().getStatusCode();
		    	
		    	switch(response_code){
		    	
		    	case 200:
		    		pn_message.addSuccessID(uri);
		    		break;
		    		
		    	case 401:
		    		// Wrong Access token. Ignore.
		    		break;
		    		
		    	case 403:
		    		pn_message.addIncorrectID(uri);
		    		break;
		    		
		    	case 404:
		    		pn_message.addIncorrectID(uri);
		    		break;
		    		
		    	case 410:
		    		pn_message.addIncorrectID(uri);
		    		break;
		    		
		    	default:
		    		break;
		    	
		    	}
		    	
	        }
	        catch(Exception e){
	        	System.out.println("WPN Post Message. Error: " + e.getMessage());
	        }
	        			
		}
		
	}
	
}