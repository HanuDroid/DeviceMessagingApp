package com.ayansh.java.pushnotifications;

public interface GCMInvoker {

	public void finishedGCMProcessing(int status, PNMessage pn_message);

}