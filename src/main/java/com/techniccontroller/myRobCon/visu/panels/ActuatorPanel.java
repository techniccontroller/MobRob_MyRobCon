package com.techniccontroller.myRobCon.visu.panels;

import java.util.ArrayList;
import java.util.HashMap;

import com.techniccontroller.myRobCon.core.MyRob;

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

public class ActuatorPanel extends VBox {
	private MyRob robot;
	private GridPane gridPane;
	private TextField txtSpeed;
	private CheckBox chkHolo;

	private HashMap<Character, Integer[]> moveBindings = new HashMap<Character, Integer[]>() {

		private static final long serialVersionUID = 1L;

		{
			put('z', new Integer[] { 1, 0, 0, 0 });
			put('u', new Integer[] { 1, 0, 0, -1 });
			put('g', new Integer[] { 0, 0, 0, 1 });
			put('j', new Integer[] { 0, 0, 0, -1 });
			put('t', new Integer[] { 1, 0, 0, 1 });
			put('n', new Integer[] { -1, 0, 0, 0 });
			put('m', new Integer[] { -1, 0, 0, 1 });
			put('b', new Integer[] { -1, 0, 0, -1 });
			put('U', new Integer[] { 1, -1, 0, 0 });
			put('Z', new Integer[] { 1, 0, 0, 0 });
			put('G', new Integer[] { 0, 1, 0, 0 });
			put('J', new Integer[] { 0, -1, 0, 0 });
			put('T', new Integer[] { 1, 1, 0, 0 });
			put('N', new Integer[] { -1, 0, 0, 0 });
			put('M', new Integer[] { -1, -1, 0, 0 });
			put('B', new Integer[] { -1, 1, 0, 0 });
			put('h', new Integer[] { 0, 0, 0, 0 });
			put('H', new Integer[] { 0, 0, 0, 0 });
		}
	};
	private char[][] cmds = { { 't', 'z', 'u' }, { 'g', 'h', 'j' }, { 'b', 'n', 'm' } };

	private enum ControlMode {
		TRANSLATION, ROTATION
	};

	public ActuatorPanel(MyRob myrob) {
		this.robot = myrob;
		gridPane = new GridPane();

		ArrayList<Button> buttonList = new ArrayList<>();


		for (int r = 0; r < 3; r++) {
			for (int c = 0; c < 3; c++) {
				Button temp = new Button("" + cmds[r][c]);
				temp.setOnMousePressed(this::control);
				temp.setOnMouseReleased(this::controlStop);
				presetButton(temp, c, r);
				buttonList.add(temp);
			}
		}

		gridPane.setHgap(5);
		gridPane.setVgap(5);
		setPadding(new Insets(10));

		GridPane speedPane = new GridPane();
		speedPane.setPadding(new Insets(10, 0, 10, 0));
		speedPane.setVgap(5);
		speedPane.setHgap(5);
		Label lblSpeed = new Label("Speed [m/s]:");
		txtSpeed = new TextField("0.2");
		txtSpeed.setPrefWidth(50);
		Button btnSpdMinus = new Button("-");
		btnSpdMinus.setOnAction(e -> txtSpeed.setText(String.valueOf((Double.valueOf(txtSpeed.getText())-0.01))));
		btnSpdMinus.setPrefWidth(20);
		Button btnSpdPlus = new Button("+");
		btnSpdPlus.setOnAction(e -> txtSpeed.setText(String.valueOf((Double.valueOf(txtSpeed.getText())+0.01))));
		btnSpdPlus.setPrefWidth(20);
		speedPane.add(lblSpeed, 0, 0);
		speedPane.add(txtSpeed, 1, 0);
		speedPane.add(btnSpdMinus, 2, 0);
		speedPane.add(btnSpdPlus, 3, 0);
		chkHolo = new CheckBox("Holonomic mode");
		chkHolo.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				System.out.println(chkHolo.isSelected());
				for(int i = 0; i < buttonList.size(); i++){
					if(chkHolo.isSelected()){
						buttonList.get(i).setText(buttonList.get(i).getText().toUpperCase());
					}else{
						buttonList.get(i).setText(buttonList.get(i).getText().toLowerCase());
					}
				}
			}

		});
		speedPane.add(chkHolo, 0, 1, 4, 1);

		JoystickPanel joystickPane = new JoystickPanel();
		getChildren().add(gridPane);
		getChildren().add(speedPane);
		getChildren().add(joystickPane);
		BorderPane.setMargin(gridPane, new Insets(10));
		BorderPane.setMargin(joystickPane, new Insets(10));
		setVisible(false);
	}

	private void presetButton(Button btn, int col, int row) {
		btn.setPrefHeight(55.0);
		btn.setPrefWidth(65.0);
		btn.setMinHeight(55.0);
		btn.setMinWidth(65.0);
		btn.setTextAlignment(TextAlignment.CENTER);
		btn.setWrapText(true);
		gridPane.add(btn, col, row);
	}

	protected void control(MouseEvent event) {
		Button btn = (Button) event.getSource();
		control(btn.getText());
	}

	public void control(String cmd){
		if(cmd.length() > 0){
			Integer[] dirArray = moveBindings.get(cmd.charAt(0));
			if(dirArray != null){
				double speed = Double.valueOf(txtSpeed.getText());
				double x = dirArray[0] * speed;
				double y = dirArray[1] * speed;
				double th = dirArray[3] * speed;
				robot.getActuator().twist(x, y, th);
			}
		}
	}

	protected void controlStop(Event event) {
		robot.getActuator().stop();
	}

	public CheckBox getChkHolo() {
		return chkHolo;
	}

	class JoystickPanel extends Pane {

		private final int radius = 80;
		private int lastangle = 0;
		private int lastdist = 0;
		private double mouseX, mouseY;

		private ControlMode controlMode = ControlMode.TRANSLATION;

		public JoystickPanel() {
			setPrefHeight(200.0);
			setPrefWidth(200.0);
			setMinHeight(200.0);
			setMinWidth(200.0);
			Circle background = new Circle(100, 100, 100);
			background.setStrokeWidth(0);
			Stop[] stops = new Stop[] { new Stop(0, Color.web("#7a70ff9a")), new Stop(1, Color.web("#002bff99")) };
			RadialGradient radialGradient = new RadialGradient(0, 0, 0.5, 0.5, 0.5, true, CycleMethod.REPEAT, stops);
			background.setFill(radialGradient);
			Circle joystick = new Circle(100, 100, 30);
			Text textLabel = new Text(93, 109, "T");
			textLabel.setFill(Color.web("#48ff00"));
			textLabel.setFont(Font.font(24));
			setPadding(new Insets(10));
			getChildren().add(background);
			getChildren().add(joystick);
			getChildren().add(textLabel);

			background.setOnMouseClicked(event -> {
				if (controlMode == ControlMode.TRANSLATION) {
					controlMode = ControlMode.ROTATION;
					textLabel.setText("R");
				} else {
					controlMode = ControlMode.TRANSLATION;
					textLabel.setText("T");
				}
			});

			joystick.setOnMousePressed(evt -> {
				mouseX = evt.getScreenX();
				mouseY = evt.getScreenY();
			});
			textLabel.setOnMousePressed(evt -> {
				mouseX = evt.getScreenX();
				mouseY = evt.getScreenY();
			});

			EventHandler<MouseEvent> mouseeventHandlerDragged = new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent mouseevent) {
					double maxSpeed = Double.valueOf(txtSpeed.getText());
					double maxRotSpd = 90;
					double dx = mouseevent.getScreenX() - mouseX;
					double dy = mouseevent.getScreenY() - mouseY;
					int angle = (int) (Math.toDegrees(Math.atan2(dx, dy)) / 10) * 10;
					int dist = (int) (Math.min(Math.sqrt(dx * dx + dy * dy), radius));
					joystick.setTranslateX(dist * Math.sin(Math.toRadians(angle)));
					joystick.setTranslateY(dist * Math.cos(Math.toRadians(angle)));
					textLabel.setTranslateX(dist * Math.sin(Math.toRadians(angle)));
					textLabel.setTranslateY(dist * Math.cos(Math.toRadians(angle)));
					switch (controlMode) {
					case TRANSLATION:
						if (angle > 0) {
							angle = angle - 180;
						} else {
							angle = angle + 180;
						}
						if ((dist > 10 && dist != lastdist) || angle != lastangle) {
							robot.getActuator().speed(maxSpeed * dist / radius, -angle, 0);
						} else if (dist <= 10) {
							robot.getActuator().stop();
						}

						lastangle = angle;
						lastdist = dist;
						break;
					case ROTATION:
						double speed = -maxSpeed * dist * Math.cos(Math.toRadians(angle)) / radius;
						double rotspeed = maxRotSpd * dist * Math.sin(Math.toRadians(angle)) / radius;
						if ((dist > 10 && dist != lastdist) || angle != lastangle) {
							robot.getActuator().speed(speed, 0, rotspeed);
						} else if (dist <= 10) {
							robot.getActuator().stop();
						}

						break;
					}
				}
			};
			joystick.setOnMouseDragged(mouseeventHandlerDragged);
			textLabel.setOnMouseDragged(mouseeventHandlerDragged);

			joystick.setOnMouseReleased(evt -> {
				joystick.setTranslateX(0);
				joystick.setTranslateY(0);
				textLabel.setTranslateX(0);
				textLabel.setTranslateY(0);
				robot.getActuator().stop();
			});
			textLabel.setOnMouseReleased(evt -> {
				joystick.setTranslateX(0);
				joystick.setTranslateY(0);
				textLabel.setTranslateX(0);
				textLabel.setTranslateY(0);
				robot.getActuator().stop();
			});
		}
	}
}
