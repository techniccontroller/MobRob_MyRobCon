package com.techniccontroller.myRobCon;


import com.techniccontroller.myRobCon.core.MyRob;

public class MainRos {

	public static void main(String[] args) {

		//MyRob robot = new MyRob("Mob1", "http://ROSKinetic:11311/");
		//MyRob robot = new MyRob("Mob1", "http://MOBROB:11311/");
		MyRob robot = new MyRob("Mob1", "http://EAW-ubuntu:11311/");
		robot.showGUI();

	}
}
