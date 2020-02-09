package com.techniccontroller.myRobCon.utils;

import java.awt.geom.Line2D;

public class LSLine extends Line2D.Double {

	private static final long serialVersionUID = 1L;
	private double slope; // slope
	private double intercept; // Y Intercept
	private double variance;
	private int existance;
	private LSScanPoint supportPoint;
	private Vector2D directionVector;

	public LSLine(double m, double b) {
		super(0, b, 1, m*1+b);
		this.slope = m;
		this.intercept = b;
		this.supportPoint = new LSScanPoint(0, b, 0, 0);
		this.directionVector = new Vector2D(1, m);
		this.variance = 0;
		this.existance = 0;
	}

	public LSLine(double m, double b, double variance) {
		super(0, b, 1, m*1+b);
		this.slope = m;
		this.intercept = b;
		this.supportPoint = new LSScanPoint(0, b, 0, 0);
		this.directionVector = new Vector2D(1, m);
		this.variance = variance;
		this.existance = 0;
	}

	public LSLine(LSScanPoint p1, LSScanPoint p2) {
		super(p1, p2);
		this.supportPoint = p1;
		this.directionVector = new Vector2D(p2.getX()-p1.getX(), p2.getY()-p1.getY());
		calcSlope();
		this.variance = 0;
		this.existance = 0;
	}

	public LSLine(double x1, double y1, double x2, double y2) {
		super(x1, y1, x2, y2);
		this.supportPoint = new LSScanPoint(x1, y1, 0, 0);
		this.directionVector = new Vector2D(x2-x1, y2-y1);
		calcSlope();
		this.variance = 0;
		this.existance = 0;
	}

	public LSLine(LSScanPoint support, Vector2D direction) {
		super(support.getX(), support.getY(), support.getX()+direction.getX(), support.getY()+direction.getY());
		this.supportPoint = support;
		this.directionVector = direction;
		calcSlope();
		this.variance = 0;
		this.existance = 0;
	}

	private void calcSlope() {
		this.slope = (getP1().getY()-getP2().getY()) / (getP1().getX()-getP2().getX());
		this.intercept = getP1().getY() - this.slope*getP1().getX();
	}

	public double getSlope() {
		return slope;
	}

	public void setSlope(double m) {
		this.slope = m;
		setLine(0, intercept, 1, intercept+m);
	}

	public double getIntercept() {
		return intercept;
	}

	public void setIntercept(double b) {
		this.intercept = b;
		setLine(0, b, 1, b+slope);
	}

	public LSScanPoint getSupportPoint() {
		return supportPoint;
	}

	public void setSupportPoint(LSScanPoint supportPoint) {
		this.supportPoint = supportPoint;
	}

	public Vector2D getDirectionVector() {
		return directionVector;
	}

	public void setDirectionVector(Vector2D directionVector) {
		this.directionVector = directionVector;
	}

	public double getVariance() {
		return variance;
	}

	public void setVariance(double variance) {
		this.variance = variance;
	}

	public int getExistance() {
		return existance;
	}

	public void setExistance(int existance) {
		this.existance = existance;
		if(this.existance >= 10) this.existance = 9;
	}

}
