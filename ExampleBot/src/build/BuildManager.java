package build;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import overmind.ControlCenter;
import bwapi.DefaultBWListener;
import bwapi.Game;
import bwapi.Player;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;

public class BuildManager extends DefaultBWListener {

	private Game game;
	private Player self;
	private ControlCenter control;

	private Deque<BuildRequest> pendingRequests = new LinkedList<BuildRequest>();
	private List<StartedBuildRequest> startedRequests = new ArrayList<StartedBuildRequest>();
	private List<StartedBuildRequest> finishedRequests = new ArrayList<StartedBuildRequest>();
	private List<BuildQueue> buildings = new ArrayList<BuildQueue>();

	private int buildFailures = 0;

	private int startedMinerals = 0;
	private int startedGas = 0;

	public BuildManager(Game game, Player self) {
		this.game = game;
		this.self = self;
	}

	public void setControlCenter(ControlCenter control) {
		this.control = control;
	}

	public int getBuildFailures() {
		return buildFailures;
	}

	@Override
	public void onUnitCreate(Unit unit) {

		// Find the request the unit fulfills.
		StartedBuildRequest started = null;
		for (StartedBuildRequest request : startedRequests) {
			if (request.getRequest().getUnit().equals(unit.getType())) {
				started = request;
				break;
			}
		}

		// Remove the request
		if (started != null) {
			startedRequests.remove(started);

			finishedRequests.add(started);

			startedMinerals -= started.getRequest().getUnit().mineralPrice();
			startedGas -= started.getRequest().getUnit().gasPrice();
		} else if (self.getUnits().contains(unit)) {
			startedMinerals -= unit.getType().mineralPrice();
			startedGas -= unit.getType().gasPrice();
		}
	}

	@Override
	public void onUnitComplete(Unit unit) {
		StartedBuildRequest completed = null;
		for (StartedBuildRequest request : finishedRequests) {
			if (request.getRequest().getUnit().equals(unit.getType())) {
				completed = request;
				break;
			}
		}
		if (completed != null) {
			// Put the unit where it was requested to go.
			if (completed.getRequest().getUnitOutput() != null) {
				completed.getRequest().getUnitOutput().add(unit);
			}
			finishedRequests.remove(completed);
		}
		if (unit.getType() == UnitType.Terran_Barracks) {
			buildings.add(new BuildQueue(unit, UnitType.Terran_Marine));
		}
		if (unit.getType() == UnitType.Terran_Command_Center) {
			buildings.add(new BuildQueue(unit, UnitType.Terran_SCV));
		}

		for (BuildQueue building : buildings) {
			if (building.remove(unit)) {
				break;
			}
		}
	}

	public boolean submitBuildRequest(BuildRequest request) {
		System.out.println("Submitting request for " + request.getUnit());
		if (request.getBuildLocation() == null) {
			request.withBuildLocation(self.getStartLocation());
		}
		return pendingRequests.offer(request);
	}

	public boolean removeBuildRequest(UnitType unitType) {
		// Remove the last added unit of a type
		Iterator<BuildRequest> it = pendingRequests.descendingIterator();
		while (it.hasNext()) {
			BuildRequest request = it.next();
			if (request.getUnit() == unitType) {
				it.remove();
				return true;
			}
		}
		return false;
	}

	@Override
	public void onFrame() {
		StringBuilder message = new StringBuilder("Build Queue:\n");
		for (BuildRequest req : pendingRequests) {
			message.append(req.getUnit() + "\n");
		}
		game.drawTextScreen(100, 25, message.toString());

		message = new StringBuilder("Started Queue:\n");
		for (StartedBuildRequest req : startedRequests) {
			message.append(req.getRequest().getUnit() + "\n");
		}
		game.drawTextScreen(0, 25, message.toString());

		game.drawTextScreen(200, 25, "Started Minerals: " + startedMinerals
				+ "\nStarted Gas: " + startedGas);

		message = new StringBuilder("Building Queues:\n");
		for (BuildQueue queue : buildings) {

			// YO KEEP THIS AROUND
			buildFailures += (queue.check() ? 0 : 1);
			// YO KEEP THAT AROUND

			message.append(queue.getBuilding().getType() + ": ");
			for (UnitType u : queue.getQueue()) {
				message.append(u + ", ");
			}
			message.append("\n");
		}
		game.drawTextScreen(0, 250, message.toString());

		// Check for requests that need to be restarted
		for (StartedBuildRequest request : startedRequests) {
			if (request.isTimedOut()) {
				System.out.println("Request for "
						+ request.getRequest().getUnit() + " timed out.");
				if (request.getRequest().getUnit().isBuilding()) {
					if (!request.getWorker().isCompleted()) {
						return;
					}
					TilePosition location = getBuildTile(request.getWorker(),
							request.getRequest().getUnit(), request
									.getRequest().getBuildLocation());
					if (location != null) {
						request.getWorker().build(location,
								request.getRequest().getUnit());
					}
				}
			}
		}

		BuildRequest request = pendingRequests.peek();
		if (request == null) {
			return;
		}
		for (StartedBuildRequest started : startedRequests) {
			if (started.getRequest().equals(request)) {
				return;
			}
		}
		UnitType unitToBuild = request.getUnit();
		if (unitToBuild.gasPrice() > (self.gas() - startedGas)
				|| unitToBuild.mineralPrice() > (self.minerals() - startedMinerals)) {
			return;
		}
		if (unitToBuild.isBuilding()) {
			buildBuilding(request);
		} else {
			trainUnit(request);
		}
	}

	private void buildBuilding(BuildRequest request) {
		Unit worker = control.requestUnit(UnitType.Terran_SCV);
		if (worker == null) {
			return;
		}
		TilePosition requestLocation = request.getBuildLocation();
		if (requestLocation == null) {
			requestLocation = self.getStartLocation();
		}

		TilePosition buildTile = getBuildTile(worker, request.getUnit(),
				requestLocation);
		System.out.println("Sending " + worker.getType() + " to build "
				+ request.getUnit() + " at " + buildTile);
		if (buildTile == null || worker.build(buildTile, request.getUnit())) {
			System.out.println("Success: " + request.getUnit());

			startedMinerals += request.getUnit().mineralPrice();
			startedGas += request.getUnit().gasPrice();

			startedRequests.add(new StartedBuildRequest(request, worker));
			pendingRequests.remove(request);
		} else {
			System.out.println("Fail: " + request.getUnit());
			control.releaseUnit(worker);
			buildFailures += 1;
		}
	}

	private void trainUnit(BuildRequest request) {
		UnitType unitToBuild = request.getUnit();

		List<BuildQueue> queues = new ArrayList<BuildQueue>();
		for (BuildQueue building : buildings) {
			// System.out.println("Can " + building.getBuilding().getType()
			// + " train " + unitToBuild + "? "
			// + building.canBuild(unitToBuild));
			if (building.canBuild(unitToBuild)) {
				queues.add(building);
			}
		}
		if (!queues.isEmpty()) {
			Collections.sort(queues);
			StringBuilder queueBuildings = new StringBuilder();
			for (BuildQueue queue : queues) {
				queueBuildings.append(queue.getBuilding().getType() + ", ");
			}
			BuildQueue queue = queues.get(0);

			queue.add(unitToBuild);

			startedMinerals += request.getUnit().mineralPrice();
			startedGas += request.getUnit().gasPrice();

			startedRequests.add(new StartedBuildRequest(request, queue
					.getBuilding()));
			pendingRequests.remove(request);
			return;
		}

		// Couldn't find a building that we registered, fall back
		// to search through all units.
		System.out.println("FAILED to find building for " + unitToBuild);
		buildFailures += 1;
		// for (Unit unit : self.getUnits()) {
		// if (unit.train(unitToBuild)) {
		// startedRequests.add(new StartedBuildRequest(request, unit));
		// pendingRequests.remove(request);
		// break;
		// }
		// }
		return;
	}

	// Returns a suitable TilePosition to build a given building type near
	// specified TilePosition aroundTile, or null if not found. (builder
	// parameter is our worker)
	public TilePosition getBuildTile(Unit builder, UnitType buildingType,
			TilePosition aroundTile) {
		TilePosition ret = null;
		int maxDist = 3;
		int stopDist = 40;

		// Refinery, Assimilator, Extractor
		if (buildingType.isRefinery()) {
			for (Unit u : self.getUnits()) {
				if (u.getType().isRefinery()) {
					return null;
				}
			}
			for (Unit n : game.neutral().getUnits()) {
				if ((n.getType() == UnitType.Resource_Vespene_Geyser)
						&& (Math.abs(n.getTilePosition().getX()
								- aroundTile.getX()) < stopDist)
						&& (Math.abs(n.getTilePosition().getY()
								- aroundTile.getY()) < stopDist))
					return n.getTilePosition();
			}
		}

		while ((maxDist < stopDist) && (ret == null)) {
			for (int i = aroundTile.getX() - maxDist; i <= aroundTile.getX()
					+ maxDist; i++) {
				for (int j = aroundTile.getY() - maxDist; j <= aroundTile
						.getY() + maxDist; j++) {
					if (game.canBuildHere(builder, new TilePosition(i, j),
							buildingType, false)) {
						// units that are blocking the tile
						boolean unitsInWay = false;
						for (Unit u : game.getAllUnits()) {
							if (u.getID() == builder.getID())
								continue;
							if ((Math.abs(u.getTilePosition().getX() - i) < 4)
									&& (Math.abs(u.getTilePosition().getY() - j) < 4))
								unitsInWay = true;
						}
						if (!unitsInWay) {
							return new TilePosition(i, j);
						}
					}
				}
			}
			maxDist += 2;
		}

		if (ret == null) {
			game.printf("Unable to find suitable build position for "
					+ buildingType.toString());
			buildFailures += 1;
		}

		return ret;
	}
}
