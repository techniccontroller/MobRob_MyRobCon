package com.techniccontroller.myRobCon.utils;

import geometry_msgs.Quaternion;

public class GeometryUtils {
	
	public static double clamp (double value, double min, double max) {
		if (value < min) return min;
		if (value > max) return max;
		return value;
	}
	
	public static int getGimbalPole (Quaternion q) {
		final double t = q.getY() * q.getX() + q.getZ() * q.getW();
		return t > 0.499f ? 1 : (t < -0.499f ? -1 : 0);
	}
	
	/** Get the roll euler angle in radians
	 * @return the rotation around the x axis in radians (between -PI and +PI) */
	public static double getRollRad (Quaternion q) {
		double x = q.getX();
		double y = q.getY();
		double z = q.getZ();
		double w = q.getW();
		double sinr_cosp = 2 * (w * x + y * z);
	    double cosr_cosp = 1 - 2 * (x * x + y * y);
	    double roll = Math.atan2(sinr_cosp, cosr_cosp);
		return roll;
	}
	
	/** Get the pitch euler angle in radians
	 * @return the rotation around the y axis in radians (between -(PI/2) and +(PI/2)) */
	public static double getPitchRad (Quaternion q) {
		final int pole = getGimbalPole(q);
		double x = q.getX();
		double y = q.getY();
		double z = q.getZ();
		double w = q.getW();
		double sinp = 2 * (w * y - z * x);
	    double pitch = 0;
		if (Math.abs(sinp) >= 1)
	        pitch = Math.copySign(Math.PI / 2, sinp); // use 90 degrees if out of range
	    else
	        pitch = Math.asin(sinp);
		return pitch;
	}
	
	/** Get the yaw euler angle in radians
	 * @return the rotation around the z axis in radians (between -PI and +PI) */
	public static double getYawRad (Quaternion q) {
		double x = q.getX();
		double y = q.getY();
		double z = q.getZ();
		double w = q.getW();
		double siny_cosp = 2 * (w * z + x * y);
	    double cosy_cosp = 1 - 2 * (y * y + z * z);
	    double yaw = Math.atan2(siny_cosp, cosy_cosp);
		return yaw;
	}
}
