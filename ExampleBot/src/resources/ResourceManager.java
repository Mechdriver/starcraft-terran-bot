package resources;

import java.util.ArrayList;
import java.util.List;

import overmind.ControlCenter;
import bwapi.DefaultBWListener;
import bwapi.Game;
import bwapi.Mirror;
import bwapi.Player;
import bwapi.Unit;
import bwapi.UnitType;

public class ResourceManager extends DefaultBWListener {

	private Mirror mirror;
	private Game game;
	private Player self;

	private List<ResourceInfo> minerals;
	private List<ResourceInfo> gasses;
	private ControlCenter control;
	
	private List<Unit> availableWorkers;

	public void onUnitDiscover(Unit resource) {
		if (resource.getType().isMineralField()) {
			minerals.add(new ResourceInfo(resource));
		} else if (resource.getType() == UnitType.Terran_Refinery) {
			gasses.add(new ResourceInfo(resource));
		}
	}

	public ResourceManager(Mirror mirror, Game game, Player self) {
		this.mirror = mirror;
		this.game = game;
		this.self = self;
		this.minerals = new ArrayList<ResourceInfo>();
		this.gasses = new ArrayList<ResourceInfo>();
		this.availableWorkers = new ArrayList<Unit>();
	}
	
	public void setControlCenter(ControlCenter control) {
		this.control = control;
	}

	@Override
	public void onFrame() {
		List<ResourceInfo> toRemove = new ArrayList<ResourceInfo>();
		for (ResourceInfo resource : gasses) {
			if (resource.resource.getType() == UnitType.Unknown) {
				toRemove.add(resource);
				continue;
			}
			while (!resource.isSaturated()) {
				Unit worker = takeMineralsWorker();
				if (worker == null) {
					break;
				}
				resource.workers.add(worker);
				worker.gather(resource.resource);
			}
			for (Unit worker : resource.workers) {
				if (worker.isIdle()) {
					System.out.println("Sending " + worker.getType() + " to gather " + resource.resource.getType());
					worker.gather(resource.resource);
				}
			}
		}
		for (ResourceInfo resource : toRemove) {
			List<Unit> freeBuilders = resource.workers;
			gasses.remove(resource);
			for (Unit unit : freeBuilders) {
				giveWorker(unit);
			}
		}
		toRemove.clear();
		for (ResourceInfo resource : minerals) {
			if (resource.resource.getType() == UnitType.Unknown) {
				toRemove.add(resource);
				continue;
			}
			for (Unit worker : resource.workers) {
				if (worker.isIdle()) {
					System.out.println("Sending " + worker.getType() + " to gather " + resource.resource.getType());
					worker.gather(resource.resource);
				}
			}
		}
		for (ResourceInfo resource : toRemove) {
			List<Unit> freeBuilders = resource.workers;
			minerals.remove(resource);
			for (Unit unit : freeBuilders) {
				giveWorker(unit);
			}
		}

		if (!availableWorkers.isEmpty()) {
			for (Unit worker : availableWorkers) {
				giveWorker(worker);
			}
			availableWorkers.clear();
		}
	}

	public void giveWorker(Unit worker) {
		System.out.println("Giving worker: " + worker.getType());
		if (!worker.getType().isWorker()) {
			System.out.println("Not a worker: " + worker.getType());
			return;
		}

		ResourceInfo bestResource = null;
		for (ResourceInfo resource : minerals) {
			if (resource.workers.contains(worker)) {
				return;
			}
			if (bestResource == null
					|| (!resource.isSaturated() && resource.distanceToBase() < bestResource
							.distanceToBase())) {
				bestResource = resource;
			}
		}
		for (ResourceInfo resource : gasses) {
			if (resource.workers.contains(worker)) {
				return;
			}
			if (bestResource == null
					|| (!resource.isSaturated() && resource.distanceToBase() < bestResource
							.distanceToBase())) {
				bestResource = resource;
			}
		}
		System.out.println("Worker given to resource at: "
				+ bestResource.resource.getPosition());
		bestResource.giveWorker(worker);
	}

	private class ResourceInfo {
		private Unit resource;
		private List<Unit> workers;

		private ResourceInfo(Unit resource) {
			this.resource = resource;
			this.workers = new ArrayList<Unit>();
		}

		private void giveWorker(Unit worker) {
			workers.add(worker);
		}

		private int distanceToBase() {
			Integer distance = null;
			for (Unit base : self.getUnits()) {
				if (base.getType() == UnitType.Terran_Command_Center) {
					int dist = resource.getDistance(base);
					if (distance == null || dist < distance) {
						distance = dist;
					}
				}
			}
			if (distance != null) {
				return distance;
			}
			// We don't have any bases. Uh oh.
			System.out.println("I SURE HOPE OUR BASES ARE ALL GONE");
			return 0;
		}

		private boolean isSaturated() {
			// TODO: SUPER arbitrary. Do better in the future
			return workers.size() > 4;
		}
	}
	
	private Unit takeMineralsWorker() {
		Unit toGive = null;
		for (ResourceInfo resource : minerals) {
			for (Unit worker : resource.workers) {
				if (!worker.isCarryingMinerals() && !worker.isConstructing()) {
					toGive = worker;
					break;
				}
			}
			if (toGive != null) {
				resource.workers.remove(toGive);
				return toGive;
			}
		}
		return null;
	}

	public Unit takeUnit(UnitType unitType) {
		if (unitType != UnitType.Terran_SCV) {
			return null;
		}
		Unit toGive = takeMineralsWorker();
		if (toGive != null) {
			return toGive;
		}
		for (ResourceInfo resource : gasses) {
			for (Unit worker : resource.workers) {
				if (!worker.isCarryingGas() && !worker.isConstructing()) {
					toGive = worker;
					break;
				}
			}
			if (toGive != null) {
				resource.workers.remove(toGive);
				return toGive;
			}
		}
		return null;
	}
}
