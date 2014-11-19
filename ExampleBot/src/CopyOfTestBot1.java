import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import resource.ResourceManager;
import scout.ScoutManager;
import bwapi.DefaultBWListener;
import bwapi.Game;
import bwapi.Mirror;
import bwapi.Player;
import bwapi.Position;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;
import bwta.BWTA;

public class CopyOfTestBot1 {

	private Mirror mirror = new Mirror();

	private Game game;

	private Player self;

	private Position EnemyBaseLoc = null;

	private ScoutManager scoutMan = null;
	private ResourceManager resourceManager = null;

	private boolean reported = false;

	private Deque<BuildRequest> pendingRequests = new LinkedList<BuildRequest>();
	private List<BuildRequest> startedRequests = new ArrayList<BuildRequest>();

	public void run() {
		mirror.getModule().setEventListener(new DefaultBWListener() {
			@Override
			public void onUnitCreate(Unit unit) {
				System.out.println("New unit " + unit.getType());

				for (BuildRequest request : startedRequests) {
					if (request.getUnit().equals(unit.getType())) {
						pendingRequests.remove(request);
						startedRequests.remove(request);
						return;
					}
				}
			}

			@Override
			public void onUnitComplete(Unit unit) {
				System.out.println("Completed unit " + unit.getType());
				if (unit.getType() == UnitType.Terran_Marine) {
					scoutMan = new ScoutManager(unit, game);
				}
				if (unit.getType() == UnitType.Terran_SCV) {
					resourceManager.giveWorker(unit);
				}
			}

			@Override
			public void onUnitDiscover(Unit unit) {
				resourceManager.onUnitDiscover(unit);
			}

			@Override
			public void onStart() {
				game = mirror.getGame();
				self = game.self();
				resourceManager = new ResourceManager(mirror, game, self);

				// Use BWTA to analyze map
				// This may take a few minutes if the map is processed first
				// time!
				System.out.println("Analyzing map...");
				BWTA.readMap();
				BWTA.analyze();
				System.out.println("Map data ready");

				submitBuildRequest(new BuildRequest(UnitType.Terran_SCV));
				submitBuildRequest(new BuildRequest(UnitType.Terran_SCV));
				submitBuildRequest(new BuildRequest(UnitType.Terran_Barracks));
				submitBuildRequest(new BuildRequest(
						UnitType.Terran_Supply_Depot));
				submitBuildRequest(new BuildRequest(UnitType.Terran_SCV));
				submitBuildRequest(new BuildRequest(UnitType.Terran_SCV));
				submitBuildRequest(new BuildRequest(UnitType.Terran_Marine));
			}

			@Override
			public void onFrame() {
				resourceManager.onFrame();
				game.setTextSize(10);
				game.drawTextScreen(10, 10, "Playing as " + self.getName()
						+ " - " + self.getRace());

				StringBuilder message = new StringBuilder("Build Queue:\n");
				for (BuildRequest req : pendingRequests) {
					message.append(req.getUnit() + "\n");
				}
				game.drawTextScreen(400, 25, message.toString());

				StringBuilder units = new StringBuilder("My units:\n");

				if (scoutMan != null && EnemyBaseLoc == null) {
					if (scoutMan.foundEnemy()) {
						scoutMan.primeScout();
						EnemyBaseLoc = scoutMan.getEnemyBaseLoc();
					}
				}

				if (EnemyBaseLoc != null && !reported) {
					if (scoutMan.distToEnemy(15)
							|| scoutMan.getScout().isAttacking()
							|| scoutMan.getScout().isUnderAttack()) {
						scoutMan.reportEnemy();
						reported = true;
					}
				}

				fulfillRequests();

				/*
				 * if (!self.getUnits().contains(UnitType.Terran_Refinery) &&
				 * self.minerals() >= 100 &&
				 * !buildings.contains(UnitType.Terran_Refinery)) { for (Unit
				 * unit : self.getUnits()) { if (unit.getType().isWorker()) { if
				 * (build(unit, UnitType.Terran_Refinery)) { break; } } } }
				 */

				/*
				 * if (!haveBarracks && self.minerals() >= 100 &&
				 * !buildings.contains(UnitType.Terran_Barracks)) { for (Unit
				 * unit : self.getUnits()) { if (unit.getType().isWorker()) { if
				 * (unit.isCarryingMinerals()) { if (build(unit,
				 * UnitType.Terran_Barracks)) { break; } } } } }
				 */

				/*
				 * if (!haveFactory && haveBarracks && self.minerals() >= 200 &&
				 * self.gas() >= 100 &&
				 * !buildings.contains(UnitType.Terran_Factory)) { for (Unit
				 * unit : self.getUnits()) { if (unit.getType().isWorker()) { if
				 * (unit.isCarryingMinerals()) { if (build(unit,
				 * UnitType.Terran_Factory)) { break; } } } } }
				 */

				/*
				 * if (self.supplyTotal() - self.supplyUsed() <= 4 &&
				 * haveBarracks && self.minerals() >= 100 &&
				 * !buildings.contains(UnitType.Terran_Supply_Depot)) { for
				 * (Unit unit : self.getUnits()) { if
				 * (unit.getType().isWorker()) { if (!unit.isCarryingMinerals()
				 * && !unit.isCarryingGas()) { if (build(unit,
				 * UnitType.Terran_Supply_Depot)) { break; } } } } }
				 */

				/*
				 * for (Unit myUnit : self.getUnits()) { if (myUnit.getType() ==
				 * UnitType.Terran_Marine) { haveMarine = true; break; } else
				 * haveMarine = false; }
				 */

				// iterate through my units
				for (Unit myUnit : self.getUnits()) {
					units.append(myUnit.getType()).append(" ")
							.append(myUnit.getTilePosition()).append("\n");

				}

				// draw my units on screen
				game.drawTextScreen(10, 25, units.toString());
			}
		});

		mirror.startGame();
	}

	private boolean submitBuildRequest(BuildRequest request) {
		System.out.println("Submitting request for " + request.getUnit());
		return pendingRequests.offer(request);
	}

	private boolean fulfillRequests() {
		// TODO: take into account priority
		BuildRequest request = pendingRequests.peek();
		if (request == null || startedRequests.contains(request)) {
			return false;
		}
		UnitType unitToBuild = request.getUnit();
		if (unitToBuild.gasPrice() > self.gas()
				|| unitToBuild.mineralPrice() > self.minerals()) {
			return false;
		}
		if (!unitToBuild.isBuilding()) {
			// Train a unit
			boolean isTraining = false;
			for (Unit unit : self.getUnits()) {
				// TODO: Don't just build in the first building that'll take the
				// unit, balance it out more
				if (unit.train(unitToBuild)) {
					isTraining = true;
					System.out.println("Success: " + unitToBuild);
					startedRequests.add(request);
					break;
				}
			}
			return isTraining;
		}
		// Build a building
		Unit worker = null;
		for (Unit unit : self.getUnits()) {
			if (unit.getType().isWorker()) {
				// Grab a miner
				if (!unit.isCarryingMinerals()
						&& !unit.isCarryingGas()
						&& (unit.isGatheringGas() || unit.isGatheringMinerals())) {
					worker = unit;
					break;
				}
			}
		}
		if (worker == null) {
			return false;
		}
		/*
		 * buildingAssignments.add(new
		 * BuildingAssignment(pendingRequests.poll(), worker));
		 * 
		 * /
		 */
		TilePosition buildTile = getBuildTile(worker, request.getUnit(),
				request.getBuildLocation());
		if (buildTile != null) {
			// System.out.println("Building " + request.getUnit() + " at " +
			// buildTile);
			if (worker.build(buildTile, request.getUnit())) {
				System.out.println("Success: " + request.getUnit());
				startedRequests.add(request);
				return true;
			} else {
				System.out.println("Fail: " + request.getUnit());
			}
		}
		// */
		return false;
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

	private Queue<UnitType> buildings = new LinkedList<UnitType>();

	private boolean enqueueBuilding(UnitType building) {
		if (!building.isBuilding()) {
			return false;
		}
		return buildings.offer(building);
	}

	private boolean build(Unit worker, UnitType building) {
		// System.out.println(buildings);
		TilePosition buildTile = getBuildTile(worker, building,
				self.getStartLocation());
		// System.out.println(buildTile.toString());
		if (buildTile != null) {
			if (worker.build(buildTile, building)) {
				buildings.add(building);
				return true;
			}
		}
		return false;
	}

	public class BuildRequest {
		public static final int DEFAULT_PRIORITY = 5;

		private UnitType unit;
		private TilePosition buildLocation;
		private int priority;
		private Collection<Unit> requesterUnits;

		public BuildRequest(UnitType unit, Collection<Unit> requesterUnits) {
			this(unit, DEFAULT_PRIORITY, requesterUnits);
		}

		public BuildRequest(UnitType unit) {
			this(unit, DEFAULT_PRIORITY);
		}

		public BuildRequest(UnitType unit, TilePosition buildLocation) {
			this(unit, buildLocation, DEFAULT_PRIORITY);
		}

		public BuildRequest(UnitType unit, int priority) {
			this(unit, priority, null);
		}

		public BuildRequest(UnitType unit, TilePosition buildLocation,
				int priority) {
			this(unit, buildLocation, priority, null);
		}

		public BuildRequest(UnitType unit, int priority,
				Collection<Unit> requesterUnits) {
			this(unit, self.getStartLocation(), priority, requesterUnits);
		}

		public BuildRequest(UnitType unit, TilePosition nearHere,
				Collection<Unit> requesterUnits) {
			this(unit, nearHere, DEFAULT_PRIORITY, requesterUnits);
		}

		public BuildRequest(UnitType unit, TilePosition buildLocation,
				int priority, Collection<Unit> requesterUnits) {
			this.unit = unit;
			this.buildLocation = buildLocation;
			this.priority = priority;
			this.requesterUnits = requesterUnits;
		}

		public Collection<Unit> getRequesterUnits() {
			return requesterUnits;
		}

		public void setRequesterUnits(Collection<Unit> requesterUnits) {
			this.requesterUnits = requesterUnits;
		}

		public UnitType getUnit() {
			return unit;
		}

		public void setUnit(UnitType unit) {
			this.unit = unit;
		}

		public TilePosition getBuildLocation() {
			return buildLocation;
		}

		public void setBuildLocation(TilePosition nearHere) {
			this.buildLocation = nearHere;
		}

		public int getPriority() {
			return priority;
		}

		public void setPriority(int priority) {
			this.priority = priority;
		}

		@Override
		public String toString() {
			return "Request=[unit=" + unit + "]";
			// return "Request=[unit=" + unit + ", priority=" + priority
			// + ", nearHere=" + buildLocation + ", requesterUnits="
			// + requesterUnits + "]";
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result
					+ ((buildLocation == null) ? 0 : buildLocation.hashCode());
			result = prime * result + priority;
			result = prime
					* result
					+ ((requesterUnits == null) ? 0 : requesterUnits.hashCode());
			result = prime * result + ((unit == null) ? 0 : unit.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			BuildRequest other = (BuildRequest) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (buildLocation == null) {
				if (other.buildLocation != null)
					return false;
			} else if (!buildLocation.equals(other.buildLocation))
				return false;
			if (priority != other.priority)
				return false;
			if (requesterUnits == null) {
				if (other.requesterUnits != null)
					return false;
			} else if (!requesterUnits.equals(other.requesterUnits))
				return false;
			if (unit == null) {
				if (other.unit != null)
					return false;
			} else if (!unit.equals(other.unit))
				return false;
			return true;
		}

		private CopyOfTestBot1 getOuterType() {
			return CopyOfTestBot1.this;
		}
	}

	public static void main(String... args) {
		new CopyOfTestBot1().run();
	}
}
