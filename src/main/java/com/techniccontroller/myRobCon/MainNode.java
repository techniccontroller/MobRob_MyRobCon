/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.techniccontroller.myRobCon;

import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;


public class MainNode extends AbstractNodeMain {

	private ConnectedNode connectedNode;

	@Override
	public GraphName getDefaultNodeName() {
		return GraphName.of("rosjava_myRobCon/MainNode");
	}

	@Override
	public void onStart(final ConnectedNode connectedNode) {
		this.connectedNode = connectedNode;
		System.out.println("Node is created!");
	}

	public ConnectedNode getConnectedNode() {
		return connectedNode;
	}

	@Override
	public void onShutdown(Node node){
		System.out.println("Node shutdown started...");
	}

}
