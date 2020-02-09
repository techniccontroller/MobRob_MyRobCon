package com.techniccontroller.myRobCon.utils;

import java.util.ArrayList;
import java.util.List;

public class LSCluster {

	private ArrayList<LSScanPoint> scanpoints;
	private LSScanPoint middlePoint;
	private ArrayList<LSLine> lines;

	public LSCluster() {
		this.scanpoints = new ArrayList<LSScanPoint>();
		this.lines = new ArrayList<>();
	}

	public LSCluster(List<LSScanPoint> scanpoints) {
		this.scanpoints = new ArrayList<LSScanPoint>(scanpoints);
		this.lines = new ArrayList<>();
	}

	public void addPoint(LSScanPoint p) {
		scanpoints.add(p);
	}

	public void addLine(LSLine line) {
		this.lines.add(line);
	}

	public LSLine getLine(int i) {
		return lines.get(i);
	}

	public int size() {
		return scanpoints.size();
	}

	public double dimension() {
		double minX = scanpoints.stream().mapToDouble(p -> p.getX()).min().getAsDouble();
		double maxX = scanpoints.stream().mapToDouble(p -> p.getX()).max().getAsDouble();
		double minY = scanpoints.stream().mapToDouble(p -> p.getY()).min().getAsDouble();
		double maxY = scanpoints.stream().mapToDouble(p -> p.getY()).max().getAsDouble();
		return Math.sqrt((maxX-minX)*(maxX-minX) + (maxY-minY)*(maxY-minY));
	}

	public int numLines() {
		return lines.size();
	}

	public void clear() {
		scanpoints.clear();
		middlePoint = null;
	}

	public LSScanPoint getPoint(int index) {
		return scanpoints.get(index);
	}

	public void sortScanpoints() {
		double minAngle = scanpoints.stream().mapToDouble(p -> p.getAngle()).min().getAsDouble();
		double maxAngle = scanpoints.stream().mapToDouble(p -> p.getAngle()).max().getAsDouble();
		if(maxAngle - minAngle > 180) {
			// cluster is on the transition from -180� to 180�
			scanpoints.sort((p1, p2) -> {
				double a1=p1.getAngle(), a2=p2.getAngle();
				if(p1.getAngle() < 0) a1=a1+360;
				if(p2.getAngle() < 0) a2=a2+360;
				return Double.compare(a1, a2);
			});
		}else {
			// regular cluster
			scanpoints.sort((p1, p2) -> Double.compare(p1.getAngle(), p2.getAngle()));
		}
	}

	public LSScanPoint getMiddlePoint() {
		double sumX = scanpoints.stream().mapToDouble(p -> p.getX()).sum();
		double sumY = scanpoints.stream().mapToDouble(p -> p.getY()).sum();
		middlePoint = new LSScanPoint(sumX/scanpoints.size(), sumY/scanpoints.size(), 0, 0);
		return middlePoint;
	}

	public ArrayList<LSScanPoint> getScanpoints() {
		return scanpoints;
	}

	@Override
	public String toString() {
		return scanpoints.toString();
	}
}
