package com.techniccontroller.myRobCon.visu;

import com.techniccontroller.myRobCon.core.MyRob;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class VisuSettings extends BorderPane{

	public VisuSettings(Stage stage, MyRob robot) {

		GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(10);
		grid.setPadding(new Insets(10, 10, 10, 10));

		Font titleFont = Font.font("Tahoma", FontWeight.NORMAL, 15);

		int row = 0;
		
		Text titleRobot = new Text("Robot");
		titleRobot.setFont(titleFont);
		grid.add(titleRobot, 0, row++, 2, 1);

		Label lblRosMasterUri = new Label("ROS_MASTER_URI:");
		grid.add(lblRosMasterUri, 0, row);

		TextField txtRosMasterUri = new TextField();
		txtRosMasterUri.setPrefWidth(250);
		txtRosMasterUri.setText(robot.getRosMasterUri());
		grid.add(txtRosMasterUri, 1, row++, 2, 1);

		
		
		TextField txtTopicCamera = new TextField();
		TextField txtPortCamera = new TextField();
		TextField txtIPCamera = new TextField();
		if(robot.getCameraROS() != null) {
			Text titleCamera = new Text("CameraROS");
			titleCamera.setFont(titleFont);
			grid.add(titleCamera, 0, row++, 2, 1);

			Label lblTopicCamera = new Label("Topic:");
			grid.add(lblTopicCamera, 0, row);

			txtTopicCamera.setPrefWidth(150);
			txtTopicCamera.setText("" + robot.getCameraROS().getTopicname());
			grid.add(txtTopicCamera, 1, row++);
		}
		else if(robot.getCamera() != null) {
			Text titleCamera = new Text("Camera");
			titleCamera.setFont(titleFont);
			grid.add(titleCamera, 0, row++, 2, 1);
			
			Label lblIPCamera = new Label("IP:");
			grid.add(lblIPCamera, 0, row);

			txtIPCamera.setPrefWidth(150);
			txtIPCamera.setText("" + robot.getCamera().getIpaddress());
			grid.add(txtIPCamera, 1, row++);

			Label lblPortCamera = new Label("Port:");
			grid.add(lblPortCamera, 0, row);

			txtPortCamera.setPrefWidth(150);
			txtPortCamera.setText("" + robot.getCamera().getPort());
			grid.add(txtPortCamera, 1, row++);
		}
		


		Text titleLaser = new Text("Laserscanner");
		titleLaser.setFont(titleFont);
		grid.add(titleLaser, 0, row++, 2, 1);
		
		Label lblIPLaser = new Label("IP:");
		grid.add(lblIPLaser, 0, row);

		TextField txtIPLaser = new TextField();
		txtIPLaser.setPrefWidth(150);
		txtIPLaser.setText("" + robot.getLsscanner().getIpaddress());
		grid.add(txtIPLaser, 1, row++);

		Label lblPortLaser = new Label("Port:");
		grid.add(lblPortLaser, 0, row);

		TextField txtPortLaser = new TextField();
		txtPortLaser.setPrefWidth(150);
		txtPortLaser.setText("" + robot.getLsscanner().getPort());
		grid.add(txtPortLaser, 1, row++);
		


		
		Text titleActuator = new Text("Actuator");
		titleActuator.setFont(titleFont);
		grid.add(titleActuator, 0, row++, 2, 1);

		Label lblTopicActuator = new Label("Topic:");
		grid.add(lblTopicActuator, 0, row);

		TextField txtTopicActuator = new TextField();
		txtTopicActuator.setPrefWidth(150);
		txtTopicActuator.setText("" + robot.getActuator().getTopicName());
		grid.add(txtTopicActuator, 1, row++);

		Text titleEGO = new Text("EGOPose");
		titleEGO.setFont(titleFont);
		grid.add(titleEGO, 0, row++, 2, 1);

		Label lblTopicEGO = new Label("Topic:");
		grid.add(lblTopicEGO, 0, row);

		TextField txtTopicEGOPose = new TextField();
		txtTopicEGOPose.setPrefWidth(150);
		txtTopicEGOPose.setText("" + robot.getEgoSensor().getTopicname());
		grid.add(txtTopicEGOPose, 1, row++);

		Text titleGripper = new Text("Gripper");
		titleGripper.setFont(titleFont);
		grid.add(titleGripper, 0, row++, 2, 1);

		Label lblTopicGripper = new Label("Topic:");
		grid.add(lblTopicGripper, 0, row);

		TextField txtTopicGripper = new TextField();
		txtTopicGripper.setPrefWidth(150);
		txtTopicGripper.setText("" + robot.getGripper().getPort());
		grid.add(txtTopicGripper, 1, row++);

		setTop(grid);

		BorderPane bottomPane = new BorderPane();
		bottomPane.setPadding(new Insets(10, 10, 10, 10));

		Button btnOk = new Button("OK");
		btnOk.setPrefWidth(100);
		btnOk.setOnAction(e -> {
			robot.setRosMasterUri(txtRosMasterUri.getText());
			if(robot.getCameraROS() != null)
				robot.getCameraROS().setTopicname(txtTopicCamera.getText());
			else if(robot.getCamera() != null) {
				robot.getCamera().setPort(Integer.valueOf(txtPortCamera.getText()));
				robot.getCamera().setIpaddress(txtIPCamera.getText());
			}
			robot.getLsscanner().setPort(Integer.valueOf(txtPortLaser.getText()));
			robot.getLsscanner().setIpaddress(txtIPLaser.getText());
			robot.getActuator().setTopicName(txtTopicActuator.getText());
			robot.getEgoSensor().setTopicname(txtTopicEGOPose.getText());
			robot.getGripper().setPort(Integer.valueOf(txtTopicGripper.getText()));
			System.out.println("Settings saved");
			stage.close();
		});
		bottomPane.setRight(btnOk);

		Button btnCancel = new Button("Cancel");
		btnCancel.setPrefWidth(100);
		btnCancel.setOnAction(e -> stage.close());
		bottomPane.setLeft(btnCancel);

		setBottom(bottomPane);
	}
}
