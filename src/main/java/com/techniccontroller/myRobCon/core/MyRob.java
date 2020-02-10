package com.techniccontroller.myRobCon.core;

import java.net.URI;
import java.util.LinkedList;

import org.ros.address.InetAddressFactory;
import org.ros.node.ConnectedNode;
import org.ros.node.DefaultNodeMainExecutor;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import javafx.application.Platform;

import com.techniccontroller.myRobCon.MainNode;
import com.techniccontroller.myRobCon.behaviours.BehaviourGroup;
import com.techniccontroller.myRobCon.connectors.Actuator;
import com.techniccontroller.myRobCon.connectors.CameraROS;
import com.techniccontroller.myRobCon.connectors.Camera;
import com.techniccontroller.myRobCon.connectors.EGOPoseSensor;
import com.techniccontroller.myRobCon.connectors.Gripper;
import com.techniccontroller.myRobCon.connectors.LSScanner;
import com.techniccontroller.myRobCon.strategies.Strategy;
import com.techniccontroller.myRobCon.visu.VisuGUI;

public class MyRob {
	private String id;
	private Resolver resolver;
	private LinkedList<BehaviourGroup> behGroups;
	private Strategy strategy;

	private String rosMasterUri;
	private LSScanner lsscanner;
	private CameraROS cameraROS;
	private Camera camera;
	private Actuator actuator;
	private EGOPoseSensor egoSensor;
	private Gripper gripper;
	private VisuGUI visu;
	private boolean shutdown = false;

	private final NodeMainExecutor runner;
	private MainNode rosNode;



	private Object lock = new Object();

	public MyRob(String id, String myRosMasterUri) {
		runner = DefaultNodeMainExecutor.newDefault();
		this.id = id;
		this.rosMasterUri = myRosMasterUri;
		this.resolver = new Resolver();
		resolver.setRobot(this);
		this.behGroups = new LinkedList<BehaviourGroup>();
		this.strategy = null;

		//https://stackoverflow.com/questions/30335165/handle-on-launched-javafx-application
		// Create VisuGUI
		try {
			this.visu = VisuGUI.getInstance();
			this.visu.setRobot(this);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		addLaserscanner("mobrob", 1234);
		addActuator("cmd_vel");
		//addCameraROS("cameranode/image_raw");
		addCamera("mobrob", 5001);
		addEGOPoseSensor("pose");
		addGripper("mobrob", 5044);
	}

	public void startRosNode(){
		String host = InetAddressFactory.newNonLoopback().getHostAddress().toString();

		NodeConfiguration configuration = NodeConfiguration.newPublic(host, URI.create(getRosMasterUri()));
		rosNode = new MainNode();
		configuration.setNodeName("myNodeName1");
		runner.execute(rosNode, configuration);

		visu.getMenuBar().disableConnects(false);

	}

	public void stopRosNode(){
		runner.shutdownNodeMain(rosNode);
		visu.getMenuBar().disableConnects(true);
	}

	public void setVisu(VisuGUI visu) {
		this.visu = visu;
	}

	public VisuGUI getVisu() {
		return visu;
	}

	public String getId() {
		return id;
	}

	public int addLaserscanner(String ip, int port) {
		lsscanner = new LSScanner(ip, port);
		lsscanner.setVisu(visu);
		return 0;
	}

	public int addCameraROS(String topic) {
		cameraROS = new CameraROS(topic);
		cameraROS.setVisu(visu);
		return 0;
	}
	
	public int addCamera(String ip, int port) {
		camera = new Camera(ip, port);
		camera.setVisu(visu);
		return 0;
	}

	public int addActuator(String topic) {
		actuator = new Actuator(topic);
		return 0;
	}

	public int addEGOPoseSensor(String topic) {
		egoSensor = new EGOPoseSensor(topic);
		egoSensor.setVisu(visu);
		return 0;
	}

	public int addGripper(String ip, int port) {
		gripper = new Gripper(ip, port);
		return 0;
	}

	public String getRosMasterUri() {
		return rosMasterUri;
	}

	public void setRosMasterUri(String myRosMasterUri) {
		this.rosMasterUri = myRosMasterUri;
	}

	public LSScanner getLsscanner() {
		return lsscanner;
	}

	public CameraROS getCameraROS() {
		return cameraROS;
	}
	
	public Camera getCamera() {
		return camera;
	}

	public Actuator getActuator() {
		return actuator;
	}

	public EGOPoseSensor getEgoSensor() {
		return egoSensor;
	}

	public Gripper getGripper() {
		return gripper;
	}

	public ConnectedNode getNode() {
		if(rosNode != null){
			return rosNode.getConnectedNode();
		}else{
			System.out.println("No Node started yet -> please run robot.startRosNode()");
			return null;
		}
	}

	public void add(BehaviourGroup behGroup) {
		behGroup.setResolver(resolver);
		behGroup.setRobot(this);
		this.behGroups.add(behGroup);
	}

	public void add(Strategy strategy) {
		strategy.setRobot(this);
		this.strategy = strategy;
	}

	public void run() {
		if(cameraROS != null) {
			logOnVisu("Connecting Camera ...");
			if(cameraROS.createSubscriber(getNode()) == 0) {
				logOnVisu("connected\n");
			}else {
				logOnVisu("not connected\n");
			}
		}
		if(lsscanner != null) {
			logOnVisu("Connecting Laserscanner ..." );
			if(lsscanner.initLaserscannerSocket() == 0) {
				logOnVisu("connected\n");
				lsscanner.startLaserscannerThread();
			}else {
				logOnVisu("not connected\n");
			}
		}
		if(actuator != null) {
			logOnVisu("Connecting Actuator ..." );
			if(actuator.createPublisher(getNode()) == 0) {
				logOnVisu("connected\n");
			}else {
				logOnVisu("not connected\n");
			}
		}
		resolver.setStrategy(strategy);
		if(strategy == null && behGroups.size() > 0) {
			behGroups.get(0).activateExclusive();
		}else {
			strategy.setFinish(false);
		}
		resolver.startWorking();
	}

	public void stop() {
		resolver.stopWorking();
		behGroups.stream().forEach(bg -> bg.setSuccess(false));
		if(actuator != null) {
			actuator.shutdownPublisher();
			logOnVisu("Actuator Publisher closed!\n");
			System.out.println("Actuator Publisher closed!");
		}
		if(cameraROS != null) {
			cameraROS.shutdownSubscriber();
			logOnVisu("Camera Subscriber closed!\n");
			System.out.println("Camera Subscriber closed!");
		}
		if(lsscanner != null) {
			lsscanner.stopLaserscannerThread();
			System.out.println("Laserscanner Thread canceled!");
			lsscanner.closeLaserSocket();
			logOnVisu("Laserscanner Socket closed!\n");
			System.out.println("Laserscanner Socket closed!");
		}
	}

	public void logOnVisu(String text) {
		visu.log(text);
	}

	public void shutDown() {
		synchronized (lock) {
			shutdown = true;
			lock.notifyAll();
		}
	}

	public void showGUI() {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				visu.getStage().show();
			}
		});
		synchronized (lock) {
			while(!shutdown) {
				try {
					lock.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		System.out.println("Shutdown Main");
		System.exit(0);
	}
}
