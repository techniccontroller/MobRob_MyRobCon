package com.techniccontroller.myRobCon.connectors;

import org.apache.commons.logging.Log;
import org.ros.message.MessageListener;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Subscriber;
import com.techniccontroller.myRobCon.utils.GeometryUtils;

import com.techniccontroller.myRobCon.visu.VisuGUI;

import geometry_msgs.Quaternion;
import javafx.application.Platform;

public class EGOPoseSensor {
	private String topicname;
	private VisuGUI visu;

	private boolean active = false;

	//private Subscriber<turtlesim.Pose> subscriber;
	private Subscriber<nav_msgs.Odometry> subscriber;

	public EGOPoseSensor(String topic) {
		this.topicname = topic;
	}

	public int createSubscriber(final ConnectedNode connectedNode) {
		if (subscriber == null && connectedNode != null) {
			final Log log = connectedNode.getLog();
			subscriber = connectedNode.newSubscriber(topicname, nav_msgs.Odometry._TYPE);
			subscriber.addMessageListener(new MessageListener<nav_msgs.Odometry>() {
				@Override
				public void onNewMessage(nav_msgs.Odometry message) {
					double x = message.getPose().getPose().getPosition().getX();
					double y = message.getPose().getPose().getPosition().getY();
					double th = GeometryUtils.getYawRad(message.getPose().getPose().getOrientation());
					
					log.info("I heard: \"" + x + ", " + y + ", " + th + "\"");

					Platform.runLater(new Runnable() {

						@Override
						public void run() {
							visu.getKoosCanvas().clear();
							visu.getKoosCanvas().drawRobot(x*1000, y*1000, th);
						}
					});
				}
			});
			
			/*subscriber = connectedNode.newSubscriber(topicname, turtlesim.Pose._TYPE);
			subscriber.addMessageListener(new MessageListener<turtlesim.Pose>() {
				@Override
				public void onNewMessage(turtlesim.Pose message) {
					log.info("I heard: \"" + message.getX() + ", " + message.getY() + ", " + message.getTheta() + "\"");

					Platform.runLater(new Runnable() {

						@Override
						public void run() {
							visu.getKoosCanvas().clear();
							visu.getKoosCanvas().drawRobot(message.getX()*1000, message.getY()*1000, message.getTheta());
						}
					});
				}
			});*/
			active = true;
			return 0;
		} else {
			active = false;
			return 1;
		}
	}

	public int shutdownSubscriber() {
		subscriber.shutdown();
		subscriber = null;
		active = false;
		return 0;
	}

	public void setVisu(VisuGUI visu) {
		this.visu = visu;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public String getTopicname() {
		return topicname;
	}

	public void setTopicname(String topicname) {
		this.topicname = topicname;
	}
}
