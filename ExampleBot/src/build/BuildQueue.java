package build;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import bwapi.Unit;
import bwapi.UnitType;

public class BuildQueue implements Comparable<BuildQueue> {

	private List<UnitType> queue;
	private List<UnitType> ableToBuild;
	private Unit building;

	public BuildQueue(Unit building, UnitType... ableToBuild) {
		this.building = building;
		this.ableToBuild = Arrays.asList(ableToBuild);
		this.queue = new ArrayList<UnitType>();
	}

	public boolean isEmpty() {
		return queue.isEmpty();
	}

	public boolean remove(Unit unit) {
		int tolerance = 20;
		int remaining = building.getRemainingTrainTime();
		boolean buildIsDone = false;
		if (remaining != 0) {
			for (UnitType type : ableToBuild) {
				if (type.buildTime() - tolerance <= remaining
						&& remaining <= type.buildTime()) {
					buildIsDone = true;
					break;
				}
			}
		} else {
			buildIsDone = true;
		}

		if (!buildIsDone || queue.isEmpty() || unit.getType() != queue.get(0)) {
			return false;
		}
		queue.remove(0);
		return true;
	}

	public void add(UnitType unit) {
		queue.add(unit);
		// if (queue.size() == 1) {
		// System.out.println("BUILDING " + unit + " NOW (from add)");
		// building.train(unit);
		// }
	}

	public List<UnitType> getQueue() {
		return queue;
	}

	public boolean check() {
		// If there's something in the queue and we aren't building
		if (!queue.isEmpty() && !building.isTraining()) {
			System.out.println("Starting construction of " + queue.get(0));
			return building.train(queue.get(0));
		}
		return true;
	}

	public Unit getBuilding() {
		return building;
	}

	public boolean canBuild(UnitType unit) {
		return ableToBuild.contains(unit);
	}

	@Override
	public int compareTo(BuildQueue other) {
		return this.queue.size() - other.queue.size();
	}
}
