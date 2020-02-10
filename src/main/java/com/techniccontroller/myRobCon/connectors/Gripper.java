package com.techniccontroller.myRobCon.connectors;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

import org.ros.exception.RemoteException;
import org.ros.exception.RosRuntimeException;
import org.ros.exception.ServiceNotFoundException;
import org.ros.node.ConnectedNode;
import org.ros.node.service.ServiceClient;
import org.ros.node.service.ServiceResponseListener;

import myrobot_model.*;


public class Gripper {

	private int port;

	
	ServiceClient<AttinyCommandRequest, AttinyCommandResponse> serviceClient;
	String output = "";
	
	Object SPERRE;

	private boolean gripperActive = false;

	public Gripper(String ip, int port) {
		this.port = port;
		SPERRE = new Object();
	}
	
	public int createClient(final ConnectedNode connectedNode) {
		
	    try {
	      serviceClient = connectedNode.newServiceClient("attiny_command", AttinyCommand._TYPE);
	    } catch (ServiceNotFoundException e) {
	      throw new RosRuntimeException(e);
	    }
	    return 0;
	    
	}
	
	public void closeGripperClient() {
		if(!serviceClient.isConnected()) {
			serviceClient.shutdown();
		}
	}
	
	public void sendCommand(String command) {
		final AttinyCommandRequest request = serviceClient.newMessage();
		
	    request.setInput(command);

	    serviceClient.call(request, new ServiceResponseListener<AttinyCommandResponse>() {
	      @Override
	      public void onSuccess(AttinyCommandResponse response) {
	        output = response.getOutput();
	      }

	      @Override
	      public void onFailure(RemoteException e) {
	        throw new RosRuntimeException(e);
	      }
	    });
	}
	
	public String sendGetCommand(String command) {
		final AttinyCommandRequest request = serviceClient.newMessage();
		
	    request.setInput(command);

	    serviceClient.call(request, new ServiceResponseListener<AttinyCommandResponse>() {
	      @Override
	      public void onSuccess(AttinyCommandResponse response) {
	        output = response.getOutput();
	        synchronized (SPERRE) {
	        	SPERRE.notify();
			}
	      }

	      @Override
	      public void onFailure(RemoteException e) {
	        throw new RosRuntimeException(e);
	      }
	    });
	    
	    try {
	    	synchronized (SPERRE) {
	        	SPERRE.wait(500);
			}
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
	    return output;
	}

	public void setActivServo(boolean value) {
		System.out.println(sendGetCommand("sv_ac(" + (value ? 1:0) + ")"));
	}

	public void writeServo(int angle) {
		System.out.println(sendGetCommand("sv_wr(" + angle + ")\n"));
	}

	public void refreshServo() {
		System.out.println(sendGetCommand("sv_rf(1)\n"));
	}

	public void initGRIP() {
		System.out.println(sendGetCommand("gr_it(1)\n"));
	}

	public void initVERT() {
		System.out.println(sendGetCommand("vt_it(1)\n"));
	}

	public void setSpeedGRIP(int value) {
		System.out.println(sendGetCommand("gr_sp(" + Math.abs(value) + ")\n"));
	}

	public void setSpeedVERT(int value) {
		System.out.println(sendGetCommand("vt_sp(" + Math.abs(value) + ")\n"));
	}

	public void moveAbsGRIP(int value) {
		System.out.println(sendGetCommand("gr_ma(" + value + ")\n"));
	}

	public void moveAbsVERT(int value) {
		System.out.println(sendGetCommand("vt_ma(" + value + ")\n"));
	}

	public void moveRelGRIP(int value) {
		System.out.println(sendGetCommand("gr_mr(" + value + ")\n"));
	}

	public void moveRelVERT(int value) {
		System.out.println(sendGetCommand("vt_mr(" + value + ")\n"));
	}

	public void stopGRIP() {
		System.out.println(sendGetCommand("gr_st(1)\n"));
	}

	public void stopVERT() {
		System.out.println(sendGetCommand("vt_st(1)\n"));
	}

	public void stopAll() {
		System.out.println(sendGetCommand("st_st(1)\n"));
	}

	public void setStayActivStepper(boolean value) {
		System.out.println(sendGetCommand("st_ac(" + (value?1:0) + ")\n"));
	}

	public void getPosVERT() {
		System.out.println(sendGetCommand("vt_gp(1)\n"));
	}

	public void getPosGRIP() {
		System.out.println(sendGetCommand("gr_gp(1)\n"));
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}
}
