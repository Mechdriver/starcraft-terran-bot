package resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

	private List<ResourceInfo> resources;

	public ResourceManager(Mirror mirror, Game game, Player self) {
		this.mirror = mirror;
		this.game = game;
		this.self = self;
	}

	@Override
	public void onFrame() {
		for (ResourceInfo resource : resources) {
			for (Unit worker : resource.workers) {
				if (worker.isIdle()) {
					worker.gather(resource.resource);
				}
			}
		}
	}

	public void giveWorker(Unit worker) {
		if (!worker.getType().isWorker()) {
			return;
		}

		ResourceInfo bestResource = null;
		for (ResourceInfo resource : resources) {
			if (bestResource == null
					|| (!resource.isSaturated() && resource.distanceToBase() < bestResource
							.distanceToBase())) {
				bestResource = resource;
			}
		}
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
			return 0;
		}

		private boolean isSaturated() {
			// SUPER arbitrary. Do better in the future
			return workers.size() > 3;
		}
	}
}
