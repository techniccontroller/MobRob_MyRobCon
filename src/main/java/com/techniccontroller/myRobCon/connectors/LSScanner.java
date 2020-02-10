package com.techniccontroller.myRobCon.connectors;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.scene.paint.Color;

import org.jblas.*;

import com.techniccontroller.myRobCon.utils.LSCluster;
import com.techniccontroller.myRobCon.utils.LSLine;
import com.techniccontroller.myRobCon.utils.LSScanPoint;
import com.techniccontroller.myRobCon.utils.Vector2D;
import com.techniccontroller.myRobCon.visu.VisuGUI;

public class LSScanner {

	private String ipaddress;
	private int port;
	private VisuGUI visu;

	private ArrayList<LSScanPoint> scanpoints; // current scanpoints from laser scanner (median filtered)
	private ArrayList<LSCluster> clusters; // cluster in current scan
	private ArrayList<LSLine> lines; // lines in current scan
	private ArrayList<Vector2D> featurePoints; // feature points in current scan
	private ArrayList<LSLine> lastLines; // lines in previous scan
	private ArrayList<Vector2D> lastFeaturePoints;// feature points in previous scan
	private DoubleMatrix egoPose;
	private ArrayList<DoubleMatrix> egoPoseHistory;
	private double theta;

	private final double DBSCAN_EPS = 100; // Maximum gap distance in cluster
	private final int MIN_PTS = 10; // Minimum number if points in cluster
	private final double MIN_DIMENSION = 250; // Minimum dimension of cluster to fit line into
	private final double MAX_DIST_CORRES = 200; // Maximum distance to count as corresponded points

	private ScheduledExecutorService pool = Executors.newScheduledThreadPool(3);
	private ScheduledFuture<?> timerLS;
	private ScheduledFuture<?> displayLS;

	private Socket clientSocketLS;
	private OutputStreamWriter outToServerLS;
	private BufferedReader inFromServerLS;

	private boolean laserActive = false;
	private boolean dataReady = false;
	private Object lock = new Object();

	private int skipScansNum = 2;

	public LSScanner(String ip, int port) {
		this.ipaddress = ip;
		this.port = port;
		scanpoints = new ArrayList<LSScanPoint>();
		clusters = new ArrayList<LSCluster>();
		lines = new ArrayList<LSLine>();
		featurePoints = new ArrayList<Vector2D>();
		lastLines = new ArrayList<LSLine>();
		lastFeaturePoints = new ArrayList<Vector2D>();
		egoPose = DoubleMatrix.zeros(3);
		egoPoseHistory = new ArrayList<>();
		theta = 0;
	}

	public int initLaserscannerSocket() {
		if (!laserActive) {
			try {
				if (clientSocketLS == null || clientSocketLS.isClosed()) {
					clientSocketLS = new Socket(ipaddress, port);
					System.out.println("Create LS socket...");
					outToServerLS = new OutputStreamWriter(clientSocketLS.getOutputStream());
					inFromServerLS = new BufferedReader(new InputStreamReader(clientSocketLS.getInputStream()));
				}
				laserActive = true;
				return 0;
			} catch (IOException e) {
				System.err.println("Not able to open the laser connection... (ip: " + ipaddress + ":" + port + ")" );
				laserActive = false;
				return -1;
			}
		} else {
			return 1;
		}
	}

	public int startLaserscannerThread() {
		if (timerLS == null || timerLS.isDone()) {
			initLaserscannerSocket();
			scanpoints.clear();
			clusters.clear();
			skipScansNum = 2;
			Runnable scanGrabber = new Runnable() {

				@Override
				public void run() {
					String message = "getData";
					try {
						if (laserActive) {
							outToServerLS.write(message);
							outToServerLS.flush();
							String data = inFromServerLS.readLine();
							if (skipScansNum == 0) {
								synchronized (lock) {
									addRawData(data);
									lock.notifyAll();
								}
							} else {
								skipScansNum--;
							}

						}
					} catch (IOException e) {
						System.err.println("Error while reading laser stream: " + e.getMessage());
					}
				}

			};
			this.timerLS = this.pool.scheduleAtFixedRate(scanGrabber, 0, 200, TimeUnit.MILLISECONDS);

			System.out.println("Data is ready!");
			return 0;
		} else {
			return 1;
		}
	}

	public void startDisplayThread() {
		synchronized (lock) {
			dataReady = false;
			while (!dataReady) {
				try {
					lock.wait(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		if (displayLS == null || displayLS.isDone()) {
			displayLS = pool.scheduleAtFixedRate(() -> {
				drawRawScanPoints();
			}, 0, 200, TimeUnit.MILLISECONDS);
		}
	}

	public void stopDisplayThread() {
		if (displayLS != null || !displayLS.isDone()) {
			displayLS.cancel(true);
		}
	}

	public void closeLaserSocket() {
		try {
			if (timerLS != null) {
				System.out.println("Wait until Laser Thread isDone ...");
				timerLS.cancel(true);
				while (!timerLS.isDone())
					;
				System.out.println("Thread canceled!");
			}
			if (clientSocketLS != null && !clientSocketLS.isClosed()) {
				if (visu.showServerConfirmation("Laser") == 1) {
					outToServerLS.write("closeDriver");
					outToServerLS.flush();
				}
				System.out.println("Close LS Socket...");
				clientSocketLS.shutdownOutput();
				clientSocketLS.close();
				laserActive = false;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void stopLaserscannerThread()

	{
		if (timerLS != null && !timerLS.isDone()) {
			// stop the timer
			timerLS.cancel(true);
		}
		laserActive = false;
	}

	public void addRawData(String data) {
		System.out.println("Add new Scan to ArrayList ...");
		scanpoints.clear();
		String koordinates[] = data.split(";");
		for (int i = 0; i < koordinates.length; i++) {
			if (koordinates[i].length() > 0) {
				LSScanPoint k = new LSScanPoint(koordinates[i]);
				if (k.getDist() > 0) {
					scanpoints.add(k);
				}
			}
		}
		scanpoints.sort((p1, p2) -> Double.compare(p1.getAngle(), p2.getAngle()));

		filterMedianScanPoints(scanpoints, 9);
		findClustersCartesian();
		System.out.println("Find Clusters ...");
		findClustersPolar();
		System.out.println("Find Lines ...");
		findLinesInClusters();
		System.out.println("Calculate feature points ...");
		calcFeaturePoints();
		System.out.println("Calc ICP");
		calcICP();
		if(egoPoseHistory.size() == 0 || checkDistance(egoPose, egoPoseHistory.get(egoPoseHistory.size()-1)) > 10) {
			egoPoseHistory.add(egoPose);
		}
		if (dataReady == false) {
			dataReady = true;
			System.out.println("ready!");
		}
	}

	public double checkDistance(DoubleMatrix e1, DoubleMatrix e2) {
		return Math.sqrt((e1.get(0)-e2.get(0))*(e1.get(0)-e2.get(0)) + (e1.get(1)-e2.get(1))*(e1.get(1)-e2.get(1)));
	}

	@SuppressWarnings("unused")
	private void findClustersCartesian() {
		clusters.clear();
		LSCluster cluster = new LSCluster();
		for (int i = 0; i < scanpoints.size() - 1; i++) {
			LSScanPoint currentPoint = scanpoints.get(i);
			LSScanPoint nextPoint = scanpoints.get(i + 1);
			double distBtwTwoPoints = LSScanPoint.distance(currentPoint, nextPoint);
			if (distBtwTwoPoints < DBSCAN_EPS) {
				cluster.addPoint(nextPoint);
			} else {
				if (cluster.size() >= MIN_PTS) {
					clusters.add(cluster);
					cluster = new LSCluster();
				} else {
					cluster.clear();
				}
			}
		}
	}

	private void findClustersPolar() {
		clusters.clear();
		LSCluster cluster = new LSCluster();
		for (int i = 0; i < scanpoints.size() - 1; i++) {
			LSScanPoint currentPoint = scanpoints.get(i);
			LSScanPoint nextPoint = scanpoints.get(i + 1);
			double distBtwTwoPoints = LSScanPoint.distanceSpecial(currentPoint, nextPoint);
			if (distBtwTwoPoints < DBSCAN_EPS) {
				cluster.addPoint(nextPoint);
			} else {
				if (cluster.size() >= 2) {
					clusters.add(cluster);
					cluster = new LSCluster();
				} else {
					cluster.clear();
				}
			}
		}
		if (cluster.size() >= 2) {
			clusters.add(cluster);
		}
		if (clusters.size() >= 2) {
			clusters.get(0).getScanpoints().sort((p1, p2) -> Double.compare(p2.getAngle(), p1.getAngle()));
			LSScanPoint firstPoint = clusters.get(0).getScanpoints().get(0);
			clusters.get(clusters.size() - 1).getScanpoints()
					.sort((p1, p2) -> Double.compare(p1.getAngle(), p2.getAngle()));
			LSScanPoint lastPoint = clusters.get(clusters.size() - 1).getScanpoints().get(0);
			double dist = LSScanPoint.distanceSpecial(firstPoint, lastPoint);
			System.out.println(firstPoint.getAngle() + "/ " + firstPoint.getDist() + ", " + lastPoint.getAngle() + "/ "
					+ lastPoint.getDist() + " -> " + dist);
			if (dist < DBSCAN_EPS) {
				clusters.get(clusters.size() - 1).getScanpoints().stream().forEach(p -> {
					clusters.get(0).addPoint(p);
				});
				clusters.remove(clusters.size() - 1);
			}
		}

		clusters = new ArrayList<>(clusters.stream().filter(c -> c.size() > MIN_PTS).collect(Collectors.toList()));
	}

	public void drawRawScanPoints() {
		Platform.runLater(() -> {
			// Clear Display and draw axis again
			visu.getKoosCanvas().clear();

			// draw all canpoints
			getScanpoints().stream()
					.forEach(sp -> visu.getKoosCanvas().drawDataPoint(sp.getX(), sp.getY(), 20, 20, Color.WHITE));

			/*
			 * getClusters().stream().forEach(c -> { LSScanPoint s = c.getMiddlePoint();
			 * visu.getKoosCanvas().drawDataPoint(s.getX(), s.getY(), 50, 50, Color.RED);
			 * });
			 */

			Color[] colors = new Color[10];
			colors[0] = Color.BLUE;
			colors[1] = Color.BEIGE;
			colors[2] = Color.PINK;
			colors[3] = Color.GRAY;
			colors[4] = Color.ORANGE;
			colors[5] = Color.YELLOW;
			colors[6] = Color.LIGHTGRAY;
			colors[7] = Color.PURPLE;
			colors[8] = Color.BROWN;
			colors[9] = Color.GREEN;

			for (int i = 0; i < clusters.size(); i++) {
				final int c = i;
				// Draw all Scanpoints from clusters in different colors
				// clusters.get(i).getScanpoints().stream().forEach(sp ->
				// visu.getKoosCanvas().drawDataPoint(sp.getX(), sp.getY(), 30, 30, colors[c %
				// 10]));

				// Draw middlepoint of cluster
				LSScanPoint s = clusters.get(i).getMiddlePoint();
				visu.getKoosCanvas().drawDataPoint(s.getX(), s.getY(), 50, 50, Color.RED);

				// Draw some text to cluster
				// visu.getKoosCanvas().drawText("" + i, s.getX(), s.getY(), Color.WHITE);

				// Draw lines of cluster
				for (int k = 0; k < clusters.get(i).numLines(); k++) {
					// Draw lines depending on existance in previous scans
					if (clusters.get(i).getLine(k).getExistance() >= 0) {
						Color linecolor = colors[clusters.get(i).getLine(k).getExistance() % 10];// colors[(int)(clusters.get(i).getLine(k).getVariance()/50)
																									// % 10];
						visu.getKoosCanvas().drawStraight(clusters.get(i).getLine(k).getSlope(),
								clusters.get(i).getLine(k).getIntercept(), 2, linecolor);
						visu.getKoosCanvas().drawText("" + (int) (clusters.get(i).getLine(k).getVariance()),
								s.getX() + 100 + 100 * k, s.getY(), colors[c % 10]);
					}
				}

			}

			// draw feature points
			for (int i = 0; i < featurePoints.size(); i++) {
				visu.getKoosCanvas().drawDataPoint(featurePoints.get(i).getX(), featurePoints.get(i).getY(), 20, 20,
						Color.YELLOW);
			}

			visu.getKoosCanvas().drawRobot(egoPose.get(0), egoPose.get(1), egoPose.get(2));

			// Draw EgoPose Points
			egoPoseHistory.stream().forEach(ep -> visu.getKoosCanvas().drawDataPoint(ep.get(0), ep.get(1), 30, 30, Color.RED));

		});
	}

	private LSLine fitLine(List<LSScanPoint> points) {
		double meanX = points.stream().mapToDouble(p -> p.getX()).average().getAsDouble();
		double meanY = points.stream().mapToDouble(p -> p.getY()).average().getAsDouble();
		// Regular fit for y = slope * x + yintersect
		double slope = points.stream().mapToDouble(p -> (p.getX() - meanX) * (p.getY() - meanY)).sum()
				/ points.stream().mapToDouble(p -> (p.getX() - meanX) * (p.getX() - meanX)).sum();
		// double yintercept = meanY - slope * meanX;
		// fit for x = slope2 * y + xintersect
		double slope2 = points.stream().mapToDouble(p -> (p.getX() - meanX) * (p.getY() - meanY)).sum()
				/ points.stream().mapToDouble(p -> (p.getY() - meanY) * (p.getY() - meanY)).sum();
		//double xintercept = meanX - slope2 * meanY;
		/*
		 * double variance = points.stream().mapToDouble(p -> { double pointOnLine =
		 * slope*p.getX()+yintercept; return (pointOnLine -
		 * p.getY())*(pointOnLine-p.getY())/points.size(); }).sum();
		 */
		// LSLine line = new LSLine(slope, yintercept);
		LSLine line = new LSLine(new LSScanPoint(meanX, meanY, 0, 0), new Vector2D(1, slope));
		LSLine line2 = new LSLine(new LSScanPoint(meanX, meanY, 0, 0), new Vector2D(slope2, 1));
		double variance = points.stream().mapToDouble(p -> {
			return line.ptLineDistSq(p) / points.size();
		}).sum();
		line.setVariance(variance);

		double variance2 = points.stream().mapToDouble(p -> {
			return line2.ptLineDistSq(p) / points.size();
		}).sum();
		line2.setVariance(variance2);
		if(line.getVariance() < line2.getVariance())
			return line;
		else {
			return line2;
		}
	}

	private void findLinesInClusters() {
		// Copy lines to lastLines
		lastLines.clear();
		lastLines.addAll(lines);
		lines.clear();

		// Find lines in all clusters
		System.out.println("find lines in clusters");
		for (int i = 0; i < clusters.size(); i++) {
			findLineInClusterV2(lines, clusters.get(i));
			// findLineInCluster(lines, clusters.get(i));
		}

		// Sort lines depending on slope
		// lines.sort((l1, l2) -> Double.compare(l1.getSlope(), l2.getSlope()));

		// Check if found lines is also in previous scan
		ArrayList<Double> deltaThetas = new ArrayList<>();
		for (int i = 0; i < lines.size(); i++) {
			boolean similarFound = false;
			for (int k = 0; k < lastLines.size(); k++) {
				double distSupPoint = lines.get(i).getSupportPoint().distance(lastLines.get(k).getSupportPoint());
				double deltaDistToOrigin = Math.abs(lines.get(i).ptLineDist(0, 0) - lastLines.get(k).ptLineDist(0, 0));
				// double distIntercepts = Math.abs(lines.get(i).getIntercept() -
				// lastLines.get(k).getIntercept());
				double deltaTheta = Math.toDegrees(lines.get(i).getDirectionVector().getTheta()
						- lastLines.get(k).getDirectionVector().getTheta());
				if (deltaDistToOrigin < 100 && Math.abs(deltaTheta) < 10 && lastLines.get(k).getIntercept()*lastLines.get(k).getIntercept()>0) {
					lines.get(i).setExistance(lines.get(i).getExistance() + 1 + lastLines.get(k).getExistance());
					similarFound = true;
					System.out.print(".");
					if(lines.get(i).getExistance() > 2) deltaThetas.add(deltaTheta);
				}
			}
			if (!similarFound)
				lines.get(i).setExistance(0);
		}
		double meanDeltaTheta = deltaThetas.stream().mapToDouble(p -> p.doubleValue()).sum() / deltaThetas.size();
		double varianceDeltaTheta = deltaThetas.stream().mapToDouble(p -> Math.pow(p.doubleValue()-meanDeltaTheta, 2)).sum() / deltaThetas.size();
		System.out.println("Mean DeltaTheta: " + meanDeltaTheta + ", Variance: " +  varianceDeltaTheta);
		if(deltaThetas.size() > 0) {
			theta = theta + meanDeltaTheta;
		}
		System.out.println("Theta: " + theta);
	}

	private void calcFeaturePoints() {
		// Copy featurePoints to lastFeaturePoints
		lastFeaturePoints.clear();
		lastFeaturePoints.addAll(featurePoints);
		featurePoints.clear();

		// Find Intersections between lines
		for (int i = 0; i < lines.size(); i++) {
			if (lines.get(i).getExistance() > 3) {
				for (int k = 0; k < lines.size(); k++) {
					if (lines.get(k).getExistance() > 3 && k != i && Math
							.abs(Math.atan2(lines.get(i).getSlope(), 1) - Math.atan2(lines.get(k).getSlope(), 1)) > Math
									.toRadians(40)) {
						double xIntersection = (lines.get(i).getIntercept() - lines.get(k).getIntercept())
								/ (lines.get(k).getSlope() - lines.get(i).getSlope());
						double yIntersection = lines.get(i).getSlope() * xIntersection + lines.get(i).getIntercept();
						featurePoints.add(new Vector2D(xIntersection, yIntersection));
					}
				}
			}
		}
	}

	private void calcICP() {

		// Find corresponded feature points (closest neighbor)
		ArrayList<Vector2D> m = new ArrayList<>();
		ArrayList<Vector2D> d = new ArrayList<>();
		for(int i = 0; i < lastFeaturePoints.size(); i++) {
			double distance = -1;
			int index = -1;
			for(int k = 0; k < featurePoints.size(); k++) {
				double tempDistance = lastFeaturePoints.get(i).distance(featurePoints.get(k));
				if(k == 0 || tempDistance < distance) {
					index = k;
					distance = tempDistance;
				}
			}
			if(index != -1 && distance < MAX_DIST_CORRES) {
				d.add(lastFeaturePoints.get(i));
				m.add(featurePoints.get(index));
			}
		}

		int N = m.size();
		if(N > 1) {
			// set both point sets to same centroid
			DoubleMatrix meanM = m.stream().map(v -> new DoubleMatrix(new double[]{v.getX(), v.getY()})).reduce((v1, v2) -> v1.add(v2)).get().mul(1.0/m.size());
			DoubleMatrix meanD = d.stream().map(v -> new DoubleMatrix(new double[]{v.getX(), v.getY()})).reduce((v1, v2) -> v1.add(v2)).get().mul(1.0/d.size());
			//Vector2D meanM = m.stream().reduce((v1, v2) -> v1.plus(v2)).get().scalarMult(1.0/m.size());
			//Vector2D meanD = d.stream().reduce((v1, v2) -> v1.plus(v2)).get().scalarMult(1.0/d.size());

			ArrayList<DoubleMatrix> m_c = new ArrayList<>(m.stream().map(v -> new DoubleMatrix(new double[]{v.getX()-meanM.get(0), v.getY()-meanM.get(1)} )).collect(Collectors.toList()));
			ArrayList<DoubleMatrix> d_c = new ArrayList<>(d.stream().map(v -> new DoubleMatrix(new double[]{v.getX()-meanD.get(0), v.getY()-meanD.get(1)} )).collect(Collectors.toList()));

			// Calculate correlation matrix
			DoubleMatrix H = DoubleMatrix.zeros(2, 2);
			for(int i = 0; i < N; i++) {
				DoubleMatrix res = m_c.get(i).mmul(d_c.get(i).transpose());
				H = H.add(res);
			}
			System.out.println("Correlation Matrix H: " +  H);

			// Calculate SVD
			DoubleMatrix[] svdResult = Singular.fullSVD(H);
			DoubleMatrix U = svdResult[0];
			DoubleMatrix S = svdResult[1];
			DoubleMatrix V = svdResult[2];
			System.out.println("SVD Result U: " + U);
			System.out.println("SVD Result S: " + S);
			System.out.println("SVD Result V: " + V);

			// Calculate Rotation Matrix
			DoubleMatrix R = V.mmul(U.transpose());
			System.out.println("Rotation Matrix R: " + R);
			System.out.println("Determinate R: " +  R);
			double deltaTheta = Math.acos(R.get(0));

			// Calculate Translation
			DoubleMatrix T = meanD.sub(R.mmul(meanM));
			System.out.println("Translation Vector T: " + T);
			T = DoubleMatrix.concatVertically(T, new DoubleMatrix(new double[] {deltaTheta}));
			egoPose = egoPose.add(T);
			System.out.println("EGO-Pose: " + egoPose);
		}
	}

	/**
	 * Applies Median filter on list of Double values with specific window size
	 *
	 * @param dataRaw    the list of values to be filtered
	 * @param windowsize the windowsize of median filter
	 * @return the list of filtered values
	 */
	@SuppressWarnings("unused")
	private ArrayList<Double> medianFilter(ArrayList<Double> dataRaw, int windowsize) {
		ArrayList<Double> dataMedian = new ArrayList<>();
		for (int i = 0; i < dataRaw.size(); i++) {
			double[] buffer = new double[windowsize];
			for (int k = 0; k < windowsize; k++) {
				if ((i - windowsize / 2 + k) >= 0 && (i - windowsize / 2 + k) < dataRaw.size()) {
					buffer[k] = dataRaw.get(i - windowsize / 2 + k);
				} else {
					buffer[k] = 0;
				}
			}
			Arrays.sort(buffer);
			dataMedian.add(buffer[windowsize / 2]);
		}
		return dataMedian;
	}

	/**
	 * Applies median filter on list of scanpoints depending on distance with
	 * specific windowsize
	 *
	 * @param dataRaw    the list of scanpoints to be filtered
	 * @param windowsize the windowsize of median filter
	 */
	private static void filterMedianScanPoints(ArrayList<LSScanPoint> dataRaw, int windowsize) {
		ArrayList<LSScanPoint> dataMedian = new ArrayList<>();
		for (int i = 0; i < dataRaw.size(); i++) {
			LSScanPoint[] buffer = new LSScanPoint[windowsize];
			for (int k = 0; k < windowsize; k++) {
				/*
				 * if ((i - windowsize / 2 + k) >= 0 && (i - windowsize / 2 + k) <
				 * dataRaw.size()) { buffer[k] = dataRaw.get(i - windowsize / 2 + k); } else {
				 * buffer[k] = new LSScanPoint(0, 0, 0, 0); }
				 */
				buffer[k] = dataRaw.get((i - windowsize / 2 + k + dataRaw.size()) % dataRaw.size());
			}
			Arrays.sort(buffer, (p1, p2) -> Double.compare(p1.getDist(), p2.getDist()));
			dataMedian.add(new LSScanPoint(0, 0, dataRaw.get(i).getAngle(), buffer[windowsize / 2].getDist()));
		}
		Collections.copy(dataRaw, dataMedian);
	}

	public boolean isReliable(LSLine line, LSCluster cluster) {
		double angleMiddlePoint = cluster.getMiddlePoint().getAngle();
		if(angleMiddlePoint > 90) angleMiddlePoint -= 180;
		else if(angleMiddlePoint < -90) angleMiddlePoint += 180;
		double deltaAngle = Math.toDegrees(Math.atan2(line.getSlope(), 1)) - angleMiddlePoint;
		if(deltaAngle > 90) deltaAngle += 180;
		else if(deltaAngle < 90) deltaAngle -= 180;
		if(Math.abs(deltaAngle) > 40) {
			return true;
		}
		return false;
	}

	/**
	 * Find lines in cluster with iterative approach
	 *
	 * @param lines   the list where the lines will be stored in
	 * @param cluster the cluster into which the line is to be fitted
	 */
	public void findLineInClusterV2(List<LSLine> lines, LSCluster cluster) {
		if (cluster.size() < MIN_PTS)
			return;
		double MAX_VARIANCE = 50;
		// final double MIN_DIMENSION = 200;
		LSCluster subcluster = new LSCluster();

		// sort points in cluster
		cluster.sortScanpoints();

		// add first 2 points to subcluster
		subcluster.addPoint(cluster.getPoint(0));
		subcluster.addPoint(cluster.getPoint(1));

		// Loop over all points in cluster
		for (int i = 2; i < cluster.size(); i++) {
			// Add current Point to subcluster
			subcluster.addPoint(cluster.getPoint(i));
			// fit line
			LSLine line = fitLine(subcluster.getScanpoints());
			MAX_VARIANCE = 50 + (subcluster.dimension() / subcluster.size()) / 3;
			// If variance < Threshhold && not end
			if (line.getVariance() < MAX_VARIANCE && i != cluster.size() - 1) {
				// do nothing
			}
			// If variance < Threshhold && subcluster large enough && isReliable && end
			else if (line.getVariance() < MAX_VARIANCE && subcluster.dimension() > MIN_DIMENSION && isReliable(line, subcluster) && i == cluster.size() - 1) {
				// add line to list
				lines.add(line);
				cluster.addLine(line);
			}
			// else if variance > threshhold
			else if (line.getVariance() >= MAX_VARIANCE) {
				// if dimension of subcluster > Threshhold2
				if (subcluster.dimension() > MIN_DIMENSION && isReliable(line, subcluster)) {
					// remove last point from subcluster
					subcluster.getScanpoints().remove(subcluster.size() - 1);
					// fit line to subcluster
					line = fitLine(subcluster.getScanpoints());
					// add line to list
					lines.add(line);
					cluster.addLine(line);
					// if not (end - 2)
					if (i < cluster.size() - 2) {
						// clear subcluster
						subcluster.clear();
						// add current point to subcluster
						subcluster.addPoint(cluster.getPoint(i));
						// add next point to subcluster
						i++;
						subcluster.addPoint(cluster.getPoint(i));
					} else {
						break;
					}
				}
				// else if dimension of subcluster < threshhold2
				else {
					// remove first point from subcluster
					subcluster.getScanpoints().remove(0);
				}
			}
		}
	}

	/**
	 * Find lines in cluster with recursive approach Divide cluster on farthest
	 * point after fitting line into it
	 *
	 * @param lines   the list where the lines will be stored in
	 * @param cluster the cluster into which the line is to be fitted
	 */
	@SuppressWarnings("unused")
	private void findLineInCluster(List<LSLine> lines, LSCluster cluster) {
		if (cluster.dimension() < MIN_DIMENSION) {
			return;
		}

		LSLine line = fitLine(cluster.getScanpoints());
		double farthestDist = 0;
		int farthestIndex = 0;
		ArrayList<Double> distancesToLine = new ArrayList<>();
		for (int p = 0; p < cluster.getScanpoints().size(); p++) {
			distancesToLine.add(line.ptLineDist(cluster.getScanpoints().get(p)));
		}

		for (int p = 1; p < distancesToLine.size() - 1; p++) {
			if (distancesToLine.get(p) > distancesToLine.get(p - 1)
					&& distancesToLine.get(p) > distancesToLine.get(p + 1) && distancesToLine.get(p) > farthestDist) {
				farthestDist = distancesToLine.get(p);
				farthestIndex = p;
			}
		}

		if (farthestIndex == 0) {
			System.out.println("farthestPoint not found! " + distancesToLine);
			System.out.println("line:" + line.getSlope() + ", " + line.getIntercept());
			System.out.println("cluster: " + cluster);
		} else if (line.getVariance() < 200) {
			lines.add(line);
			cluster.addLine(line);
		} else {
			LSCluster c1 = new LSCluster(cluster.getScanpoints().subList(0, farthestIndex));
			LSCluster c2 = new LSCluster(
					cluster.getScanpoints().subList(farthestIndex, cluster.getScanpoints().size()));
			findLineInCluster(lines, c1);
			findLineInCluster(lines, c2);
			for (int i = 0; i < c1.numLines(); i++) {
				cluster.addLine(c1.getLine(i));
			}
			for (int i = 0; i < c2.numLines(); i++) {
				cluster.addLine(c2.getLine(i));
			}
		}
	}

	public double checkBox(double x1, double y1, double x2, double y2) {
		final double boundXLow = x1 > x2 ? x2 : x1;
		final double boundXHigh = x1 > x2 ? x1 : x2;
		final double boundYLow = y1 > y2 ? y2 : y1;
		final double boundYHigh = y1 > y2 ? y1 : y2;
		LSScanPoint nearestPoint = getScanpoints().stream().filter(scanpoint -> {
			return (scanpoint.getX() > boundXLow && scanpoint.getX() < boundXHigh)
					&& (scanpoint.getY() > boundYLow && scanpoint.getY() < boundYHigh);
		}).min(Comparator.comparingDouble(LSScanPoint::getDist)).orElse(new LSScanPoint(0, 0, 0, 0));

		Platform.runLater(() -> {
			visu.getKoosCanvas().drawLine(x1, y1, x1, y2, 1, Color.YELLOW);
			visu.getKoosCanvas().drawLine(x1, y2, x2, y2, 1, Color.YELLOW);
			visu.getKoosCanvas().drawLine(x2, y2, x2, y1, 1, Color.YELLOW);
			visu.getKoosCanvas().drawLine(x2, y1, x1, y1, 1, Color.YELLOW);
			if (nearestPoint.getDist() > 0)
				visu.getKoosCanvas().drawDataPoint(nearestPoint.getX(), nearestPoint.getY(), 50, 50, Color.ORANGE);
		});
		return nearestPoint.getDist();
	}

	public double checkPolar(double angleStart, double angleEnd, double distance) {
		final double boundLow = angleStart < angleEnd ? angleStart : angleEnd;
		final double boundHigh = angleStart < angleEnd ? angleEnd : angleStart;
		LSScanPoint nearestPoint = getScanpoints().stream().filter(scanpoint -> {
			return (scanpoint.getAngle() > boundLow && scanpoint.getAngle() < boundHigh
					&& scanpoint.getDist() < distance && scanpoint.getDist() > 0);
		}).min(Comparator.comparingDouble(LSScanPoint::getDist)).orElse(new LSScanPoint(0, 0, 0, 0));

		Platform.runLater(() -> {
			visu.getKoosCanvas().drawLine(0, 0, distance * Math.cos(Math.toRadians(angleStart)),
					distance * Math.sin(Math.toRadians(angleStart)), 1, Color.YELLOW);
			visu.getKoosCanvas().drawLine(0, 0, distance * Math.cos(Math.toRadians(angleEnd)),
					distance * Math.sin(Math.toRadians(angleEnd)), 1, Color.YELLOW);
			visu.getKoosCanvas().drawArc(0, 0, distance, angleStart, angleEnd, 1, Color.YELLOW);
			if (nearestPoint.getDist() > 0)
				visu.getKoosCanvas().drawDataPoint(nearestPoint.getX(), nearestPoint.getY(), 50, 50, Color.ORANGE);
		});
		return nearestPoint.getDist();
	}

	public ArrayList<LSScanPoint> getScanpoints() {
		synchronized (lock) {
			return scanpoints;
		}
	}

	public ArrayList<LSCluster> getClusters() {
		return clusters;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getIpaddress() {
		return ipaddress;
	}

	public void setIpaddress(String ipaddress) {
		this.ipaddress = ipaddress;
	}

	public void setVisu(VisuGUI visu) {
		this.visu = visu;
	}

	public boolean isDataReady() {
		return dataReady;
	}
}
