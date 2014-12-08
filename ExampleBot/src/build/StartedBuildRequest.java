package build;

import bwapi.Unit;

public class StartedBuildRequest {

	private static final int DEFAULT_RESUBMIT_COUNTDOWN = 10;

	private BuildRequest request;
	private Unit worker;
	private int resubmitCountdown;

	public StartedBuildRequest(BuildRequest request, Unit builder) {
		this.request = request;
		this.worker = builder;
		this.resubmitCountdown = DEFAULT_RESUBMIT_COUNTDOWN;
		if (builder.getType().isWorker()) {
			System.out.println("Adding " + (request.getBuildLocation().getDistance(builder.getTilePosition())
					/ builder.getType().topSpeed() * 32) + " to countdown for distance traveled");
			this.resubmitCountdown += 
					request.getBuildLocation().getDistance(builder.getTilePosition())
					/ builder.getType().topSpeed() 
					* 32; // Pixels / Tile
		}
	}
	
	public boolean isTimedOut() {
		// Only buildings will time out
		if (!request.getUnit().isBuilding()) {
			return false;
		}
		if (resubmitCountdown-- <= 0) {
			resubmitCountdown = DEFAULT_RESUBMIT_COUNTDOWN;
			return true;
		}
		return false;
	}

	public BuildRequest getRequest() {
		return request;
	}

	public void setRequest(BuildRequest request) {
		this.request = request;
	}

	public Unit getWorker() {
		return worker;
	}

	public void setWorker(Unit worker) {
		this.worker = worker;
	}

	public int getResubmitCountdown() {
		return resubmitCountdown;
	}

	public void setResubmitCountdown(int resubmitCountdown) {
		this.resubmitCountdown = resubmitCountdown;
	}
}
