package com.techniccontroller.myRobCon.connectors;

import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;
import geometry_msgs.Twist;


public class Actuator {

	private Publisher<geometry_msgs.Twist> publisher;
	private String topicName;
	private boolean active;

	public Actuator(String topic) {
		this.topicName = topic;
		this.active = false;
	}

	public int createPublisher(final ConnectedNode connectedNode) {
		if(publisher == null && connectedNode != null){
			publisher = connectedNode.newPublisher(topicName, geometry_msgs.Twist._TYPE);
			active = true;
			return 0;
		}
		else{
			active = false;
			return 1;
		}

	}

	public int shutdownPublisher(){
		if(!publisher.hasSubscribers()){
			publisher.shutdown();
			publisher = null;
			active = false;
			return 0;
		}else{
			System.out.println("Publisher has subscribers, cannot shut down.");
			return 1;
		}

	}

	public void move(int distance, int speed, int direction) {

	}

	public void turn(int angle, int speed, int radius) {

	}

	public void twist(double x, double y, double th){
		if (publisher != null) {
			Twist twist = publisher.newMessage();
			twist.getLinear().setX(x);
			twist.getLinear().setY(y);
			twist.getLinear().setZ(0.0);
			twist.getAngular().setX(0.0);
			twist.getAngular().setY(0.0);
			twist.getAngular().setZ(th);
			publisher.publish(twist);
		} else {
			System.out.println("Publisher has not been created yet -> run createPublisher()");
		}
	}

	/**
	 *
	 * @param speed
	 *            moving speed in m/s
	 * @param directionDeg
	 *            moving direction in degrees (0 -> forward)
	 * @param rotspeed
	 *            rotation speed in degrees/s
	 */
	public void speed(double speed, int directionDeg, double rotspeed) {
		if (publisher != null) {
			Twist twist = publisher.newMessage();
			twist.getLinear().setX(speed * Math.cos(Math.toRadians(directionDeg)));
			twist.getLinear().setY(speed * Math.sin(Math.toRadians(directionDeg)));
			twist.getLinear().setZ(0.0);
			twist.getAngular().setX(0.0);
			twist.getAngular().setY(0.0);
			twist.getAngular().setZ(Math.toRadians(rotspeed));
			publisher.publish(twist);
		} else {
			System.out.println("Publisher has not been created yet -> run createPublisher()");
		}

	}

	public void stop() {
		if (publisher != null) {
			Twist twist = publisher.newMessage();
			twist.getLinear().setX(0.0);
			twist.getLinear().setY(0.0);
			twist.getLinear().setZ(0.0);
			twist.getAngular().setX(0.0);
			twist.getAngular().setY(0.0);
			twist.getAngular().setZ(0.0);
			publisher.publish(twist);
		} else {
			System.out.println("Publisher has not been created yet -> run createPublisher()");
		}
	}

	public String getTopicName() {
		return topicName;
	}

	public void setTopicName(String topicName) {
		this.topicName = topicName;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

}
