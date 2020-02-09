package com.techniccontroller.myRobCon.visu;

import com.techniccontroller.myRobCon.core.MyRob;

import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class VisuMenuBar extends MenuBar {

	private MyRob robot;
	private VisuGUI visu;
	private MenuItem menuConActuator;
	private MenuItem menuConLaser;
	private MenuItem menuConCamera;
	private MenuItem menuConEGO;
	private MenuItem menuConGripper;
	private MenuItem menuSettings;

	public VisuMenuBar(VisuGUI visu) {
		this.robot = visu.getRobot();
		this.visu = visu;

		Menu menu = new Menu("Connection");
		menuConActuator = new MenuItem("Connect Actuators");
		menuConActuator.setOnAction(this::connectActuator);
		menuConLaser = new MenuItem("Connect Laserscanner");
		menuConLaser.setOnAction(this::startLaser);
		menuConCamera = new MenuItem("Connect Camera");
		menuConCamera.setOnAction(this::startCamera);
		menuConEGO = new MenuItem("Connect EGOPose Sensor");
		menuConEGO.setOnAction(this::startEGOPoseSensor);
		menuConGripper = new MenuItem("Connect Gripper");
		menuConGripper.setOnAction(this::connectGripper);
		menuSettings = new MenuItem("Settings");
		menuSettings.setOnAction(this::openSettings);

		menu.getItems().add(menuConActuator);
		menu.getItems().add(menuConLaser);
		menu.getItems().add(menuConCamera);
		menu.getItems().add(menuConEGO);
		menu.getItems().add(menuConGripper);
		menu.getItems().add(menuSettings);

		getMenus().add(menu);

		disableConnects(true);

		menu.setOnShowing(e -> updateLabels());


	}

	public void updateLabels(){
		if(robot.getActuator().isActive()){
			menuConActuator.setText("Disconnect Actuator");
		}else{
			menuConActuator.setText("Connect Actuator");
		}
	}

	public void disableConnects(boolean val){
		menuConActuator.setDisable(val);
		menuConCamera.setDisable(val);
		menuConEGO.setDisable(val);
		menuConGripper.setDisable(val);
		menuConLaser.setDisable(val);
	}

	protected void startCamera(ActionEvent event) {
		if(robot.getNode() != null){
			int res = robot.getCamera().createSubscriber(robot.getNode());
			if (res == 0) {
				robot.getCamera().startCameraThread();
				menuConCamera.setText("Disconnect Camera");
			} else if (res == 1) {
				if(robot.getCamera().shutdownSubscriber() == 0){
					menuConCamera.setText("Connect Camera");
				}
			}
		}
		updateLabels();
	}

	protected void startLaser(ActionEvent event) {
		int res = robot.getLsscanner().initLaserscannerSocket();
		if (res == 0) {
			robot.getLsscanner().startLaserscannerThread();
			robot.getLsscanner().startDisplayThread();
			menuConLaser.setText("Disconnect Laserscanner");
		} else if (res == 1) {
			robot.getLsscanner().stopDisplayThread();
			robot.getLsscanner().stopLaserscannerThread();
			robot.getLsscanner().closeLaserSocket();
			menuConLaser.setText("Connect Laserscanner");
		}
	}

	protected void connectActuator(ActionEvent event) {
		if(robot.getNode() != null){
			int res = robot.getActuator().createPublisher(robot.getNode());
			if (res == 0) {
				visu.getControlPanel().showActuatorPane(true);
			} else if (res == 1) {
				if(robot.getActuator().shutdownPublisher() == 0){
					visu.getControlPanel().showActuatorPane(false);
				}
			}
		}
		updateLabels();
	}

	protected void startEGOPoseSensor(ActionEvent event) {
		if(robot.getNode() != null){
			int res = robot.getEgoSensor().createSubscriber(robot.getNode());
			if (res == 0) {
				menuConEGO.setText("Disconnect EGOPose Sensor");
			} else if (res == 1) {
				if(robot.getEgoSensor().shutdownSubscriber() == 0){
					menuConEGO.setText("Connect EGOPose Sensor");
				}
			}
		}
	}

	protected void connectGripper(ActionEvent event) {
		int res = robot.getGripper().initGripperSocket();
		if (res == 0) {
			visu.getControlPanel().showGripperPane(true);
			menuConGripper.setText("Disconnect Gripper");
		} else if (res == 1) {
			robot.getGripper().closeGripperSocket();
			visu.getControlPanel().showGripperPane(false);
			menuConGripper.setText("Connect Gripper");
		}
	}

	protected void openSettings(ActionEvent event) {
		Stage popupwindow = new Stage();

		popupwindow.initModality(Modality.APPLICATION_MODAL);
		popupwindow.setTitle("Settings");

		VisuSettings settingsPane = new VisuSettings(popupwindow, robot);

		Scene scene1 = new Scene(settingsPane, 400, 600);

		popupwindow.setScene(scene1);

		popupwindow.showAndWait();
	}
}
