package com.techniccontroller.myRobCon.strategies;

import com.techniccontroller.myRobCon.behaviours.BehaviourGroup;

public class RDStrategy extends Strategy {

	protected BehaviourGroup dock;

	public RDStrategy(BehaviourGroup dock) {
		this.dock = dock;
		dock.activateExclusive();
	}

	@Override
	public void plan() {
		if(dock.isSuccess()) {
			stopRunning();
		}
	}

}
