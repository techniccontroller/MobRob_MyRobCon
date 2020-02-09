package com.techniccontroller.myRobCon.connectors;

import java.awt.image.BufferedImage;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.ros.message.MessageListener;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Subscriber;

import com.techniccontroller.myRobCon.utils.Utils;
import com.techniccontroller.myRobCon.visu.VisuGUI;

import cv_bridge.CvImage;
import cv_bridge.ImageEncodings;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

public class CameraROS {

	private String topicname;
	private VisuGUI visu;

	private CvImage cvimage;

	private boolean active = false;

	private Subscriber<sensor_msgs.Image> subscriber;

	private BufferedImage cameraFrame;
	private Object lock = new Object();

	private final int height = 480;
	private final int width = 640;

	public CameraROS(String topic) {
		this.topicname = topic;
	}

	public int createSubscriber(final ConnectedNode connectedNode) {
		if (subscriber == null && connectedNode != null) {
			System.loadLibrary( Core.NATIVE_LIBRARY_NAME );
			subscriber = connectedNode.newSubscriber(topicname, sensor_msgs.Image._TYPE);
			subscriber.addMessageListener(new MessageListener<sensor_msgs.Image>() {
				@Override
				public void onNewMessage(sensor_msgs.Image message) {
					try {
						cvimage = CvImage.toCvCopy(message);
						Mat matimage = CvImage.cvtColor(cvimage, ImageEncodings.BGR8).image;
						Image imageToShow= Utils.mat2Image(matimage);
						visu.updateCameraImageView(imageToShow);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
			active = true;
			visu.log("Camera Subscriber created");
			return 0;
		} else {
			active = false;
			return 1;
		}
	}

	public int shutdownSubscriber() {
		subscriber.shutdown();
		subscriber = null;
		Image imageToShow = SwingFXUtils.toFXImage(Utils.getBlankImage(width,  height), null);
		visu.updateCameraImageView(imageToShow);
		active = false;
		return 0;
	}

	public BufferedImage getCameraFrame() {
		synchronized (lock) {
			return cameraFrame;
		}
	}

	public String getTopicname() {
		return topicname;
	}

	public void setTopicname(String topicname) {
		this.topicname = topicname;
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
}
