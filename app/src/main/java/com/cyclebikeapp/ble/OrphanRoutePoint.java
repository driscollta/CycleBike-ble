package com.cyclebikeapp.ble;

class OrphanRoutePoint {
	final GPXRoutePoint thePoint;
	/**
	 * presumed beginning location in merged array to place an orphan route
	 * point
	 */
    final int startIndex;
	/**
	 * presumed ending location in merged array to place an orphan route point
	 */
	int endIndex;

	OrphanRoutePoint(GPXRoutePoint point, int start, int end) {

		this.thePoint = point;
		this.startIndex = start;
		this.endIndex = end;
	}

}
