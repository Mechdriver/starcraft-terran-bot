package build;

import java.util.ArrayList;
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

	private int startedMinerals = 0;
	private int startedGas = 0;

	public BuildManager(Game game, Player self) {
		this.game = game;
		this.self = self;
	}
	
	public void setControlCenter(ControlCenter control) {
		this.control = control;
	}

	@Override
	public void onUnitCreate(Unit unit) {
		System.out.println("New unit " + unit.getType());
		
		// WTF, refinery, why don't you show up???
		if (unit.getType() == UnitType.Terran_Refinery) {
			System.out.println("HOLY COW IT'S A REFINERY");
		}

		for (StartedBuildRequest request : startedRequests) {
			if (request.getRequest().getUnit().equals(unit.getType())) {
				pendingRequests.remove(request.getRequest());
				startedRequests.remove(request);

				// Put the unit where it was requested to go.
				if (request.getRequest().getUnitOutput() != null) {
					request.getRequest().getUnitOutput().add(unit);
				}

				if (unit.getType().isBuilding()) {
					startedMinerals -= request.getRequest().getUnit()
							.mineralPrice();
					startedGas -= request.getRequest().getUnit().gasPrice();
				}
				return;
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
		while(it.hasNext()) {
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
		
		// Check for requests that need to be restarted
		for (StartedBuildRequest request : startedRequests) {
			if (request.isTimedOut()) {
				System.out.println("Request for " + request.getRequest().getUnit() + " timed out.");
				if (request.getRequest().getUnit().isBuilding()) {
					TilePosition location = getBuildTile(request.getWorker(), 
							request.getRequest().getUnit(), request.getRequest().getBuildLocation());
					if (location != null) {
						request.getWorker().build(location, request.getRequest().getUnit());
					}
				} else {
					request.getWorker().train(request.getRequest().getUnit());
				}
			}
		}

		// TODO: take into account priority
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
		System.out.println("Trying to build " + request.getUnit());
		if (!unitToBuild.isBuilding()) {
			// Train a unit
			for (Unit unit : self.getUnits()) {
				// TODO: Don't just build in the first building that'll take the
				// unit, balance it out more
				if (unit.train(unitToBuild)) {
					System.out.println("Started: " + unitToBuild);
					
					// Do not update startedMinerals and startedGas, it's deducted
					// upon enqueueing
					startedRequests.add(new StartedBuildRequest(request, unit));
					pendingRequests.remove(request);
					break;
				}
			}
			return;
		}
		// Build a building
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
		if (buildTile != null) {
			System.out.println("Sending " + worker.getType() + " to build "
					+ request.getUnit() + " at " + buildTile);
			if (worker.build(buildTile, request.getUnit())) {
				System.out.println("Success: " + request.getUnit());
				
				startedMinerals += request.getUnit().mineralPrice();
				startedGas += request.getUnit().gasPrice();
				
				startedRequests.add(new StartedBuildRequest(request, worker));
				pendingRequests.remove(request);

				return;
			} else {
				System.out.println("Fail: " + request.getUnit());
			}
		}
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

		if (ret == null)
			game.printf("Unable to find suitable build position for "
					+ buildingType.toString());
		return ret;
	}
}
