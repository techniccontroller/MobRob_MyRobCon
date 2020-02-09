package com.techniccontroller.myRobCon;

import com.techniccontroller.myRobCon.behaviours.BehAlign;
import com.techniccontroller.myRobCon.behaviours.BehConstTransVel;
import com.techniccontroller.myRobCon.behaviours.BehLimFor;
import com.techniccontroller.myRobCon.behaviours.BehStop;
import com.techniccontroller.myRobCon.behaviours.BehaviourGroup;
import com.techniccontroller.myRobCon.core.MyRob;
import com.techniccontroller.myRobCon.strategies.RDStrategy;

public class TestMain {

	public static void main(String[] args) {

		MyRob robot = new MyRob("Mob1", "192.168.0.116");

		BehaviourGroup dock = new BehaviourGroup("Dock");
		BehConstTransVel cv = new BehConstTransVel("ConstVel", 40);
		BehLimFor lv = new BehLimFor("limit", 300, 600, 20);
		BehAlign al = new BehAlign("align", 20, 30);
		BehStop st = new BehStop("stop", 400);
		dock.add(al, 50);
		dock.add(cv, 50);
		dock.add(lv, 90);
		dock.add(st, 50);
		robot.add(dock);

		robot.add(new RDStrategy(dock));

		//robot.run();
		robot.showGUI();

	}

}
