package com.ayansh.java.pushnotifications;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PNMessage {

	private String collapse_key;
	private int time_to_live;
	
	private List<String> registration_ids;
	private HashMap<String,String> canonical_ids;
	private ArrayList<String> incorrect_ids;
	private ArrayList<String> success_ids;
	
	public PNMessage(){
		
		registration_ids = new ArrayList<String>();
		collapse_key = "sync_all";			//	Default collapse key
		setTime_to_live(1*7*24*60*40);		//	Default to 1 week
			
		canonical_ids = new HashMap<String,String>();
		incorrect_ids = new ArrayList<String>();
		success_ids = new ArrayList<String>();
	}

	public void addRegistrationID(String registration_id){
		registration_ids.add(registration_id);	
	}
	
	public List<String> getRegistrationIDs(){
		return registration_ids;
	}

	public String getCollapse_key() {
		return collapse_key;
	}

	public void setCollapse_key(String collapse_key) {
		this.collapse_key = collapse_key;
	}

	public int getTime_to_live() {
		return time_to_live;
	}

	public void setTime_to_live(int time_to_live) {
		this.time_to_live = time_to_live;
	}

	public HashMap<String,String> getCanonical_ids() {
		return canonical_ids;
	}

	public void addCanonocalID(String old_key, String new_key) {
		canonical_ids.put(old_key, new_key);
	}

	public ArrayList<String> getIncorrect_ids() {
		return incorrect_ids;
	}

	public void addIncorrectID(String id) {
		incorrect_ids.add(id);
	}

	public ArrayList<String> getSuccess_ids() {
		return success_ids;
	}

	public void addSuccessID(String id) {
		success_ids.add(id);
	}
	
}