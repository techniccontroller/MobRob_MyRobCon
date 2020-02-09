package com.techniccontroller.myRobCon.connectors;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Base64;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.apache.commons.logging.Log;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.ros.message.MessageListener;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Subscriber;

import com.techniccontroller.myRobCon.utils.Utils;
import com.techniccontroller.myRobCon.visu.VisuGUI;

import cv_bridge.CvImage;
import cv_bridge.ImageEncodings;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import java.awt.Font;

public class CameraROS {

	private String topicname;
	private VisuGUI visu;

	private ScheduledExecutorService pool = Executors.newScheduledThreadPool(3);
	private ScheduledFuture<?> timerCam;

	private CvImage cvimage;
	//private Image imageToShow;

	private boolean active = false;

	private Subscriber<sensor_msgs.Image> subscriber;

	private BufferedImage cameraFrame;
	private Object lock = new Object();

	private final int height = 480;
	private final int width = 640;

	public CameraROS(String topic) {
		this.topicname = topic;
	}

	public BufferedImage getBlankImage(int width, int height) {
		BufferedImage bImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

		Graphics2D g = (Graphics2D) bImage.getGraphics();

		// Clear the background with white
		g.setBackground(Color.BLACK);
		g.clearRect(0, 0, width, height);

		// Write some text
		g.setColor(Color.WHITE);
		Font font = new Font("Tahoma", Font.PLAIN, 28);
		g.setFont(font);
		g.drawString("Camera is OFF", width / 2 - 100, height / 2 - 20);

		g.dispose();
		return bImage;
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
						//System.out.println("Received image ...");
						visu.updateCameraImageView(imageToShow);
					} catch (Exception e) {
						// TODO Auto-generated catch block
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
		Image imageToShow = SwingFXUtils.toFXImage(getBlankImage(width,  height), null);
		visu.updateCameraImageView(imageToShow);
		active = false;
		return 0;
	}

	public int startCameraThread() {
		if (timerCam == null || timerCam.isDone()) {
			//initCameraSocket();

			// grab a frame every 33 ms (30 frames/sec)
			Runnable frameGrabber = new Runnable() {

				@Override
				public void run() {

					String message = "getNewFrame";

					/*try {
						if (cameraActive) {
							long starttime = System.currentTimeMillis();
							outToServerCam.write(message);
							outToServerCam.flush();
							char[] sizeAr = new char[16];
							inFromServerCam.read(sizeAr);
							System.out.println(System.currentTimeMillis() - starttime);
							int size = Integer.valueOf(new String(sizeAr).trim());
							char[] data = new char[size];
							int pos = 0;
							do {
								int read = inFromServerCam.read(data, pos, size - pos);
								// check for end of file or error
								if (read == -1) {
									break;
								} else {
									pos += read;
								}
							} while (pos < size);
							String encoded = new String(data);
							byte[] decoded = Base64.getDecoder().decode(encoded);
							synchronized (lock) {
								cameraFrame = ImageIO.read(new ByteArrayInputStream(decoded));
								//cameraFrame = ImageProcessing.process(cameraFrame);

								Image imageToShow = SwingFXUtils.toFXImage(cameraFrame, null);
								visu.updateCameraImageView(imageToShow);
								System.out.println(cameraFrame.getHeight() + " " + cameraFrame.getWidth());
							}
						}
					} catch (IOException e) {
						System.err.println("Error while reading camera stream: " + e.getMessage());
					}*/
				}

			};
			this.timerCam = this.pool.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);
			return 0;
		} else {
			return 1;
		}
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
}
