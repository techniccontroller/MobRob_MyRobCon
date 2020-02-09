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
	
	/** Get the roll euler angle in radians, which is the rotation around the z axis. Requires that this quaternion is normalized.
	 * @return the rotation around the z axis in radians (between -PI and +PI) */
	public static double getRollRad (Quaternion q) {
		final int pole = getGimbalPole(q);
		double x = q.getX();
		double y = q.getY();
		double z = q.getZ();
		double w = q.getW();
		return pole == 0 ? Math.atan2(2f * (w * z + y * x), 1f - 2f * (x * x + z * z)) : (double)pole * 2f
			* Math.atan2(y, w);
	}
	
	/** Get the pitch euler angle in radians, which is the rotation around the x axis. Requires that this quaternion is normalized.
	 * @return the rotation around the x axis in radians (between -(PI/2) and +(PI/2)) */
	public static double getPitchRad (Quaternion q) {
		final int pole = getGimbalPole(q);
		double x = q.getX();
		double y = q.getY();
		double z = q.getZ();
		double w = q.getW();
		return pole == 0 ? (float)Math.asin(clamp(2f * (w * x - z * y), -1f, 1f)) : (float)pole * Math.PI * 0.5f;
	}
	
	/** Get the yaw euler angle in radians, which is the rotation around the y axis. Requires that this quaternion is normalized.
	 * @return the rotation around the y axis in radians (between -PI and +PI) */
	public static double getYawRad (Quaternion q) {
		final int pole = getGimbalPole(q);
		double x = q.getX();
		double y = q.getY();
		double z = q.getZ();
		double w = q.getW();
		return pole == 0 ? Math.atan2(2f * (y * w + x * z), 1f - 2f * (y * y + x * x)) : 0f;
	}
}
