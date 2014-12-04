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
		System.out.println("Remaining train time for " + building.getType() 
				+ ": " + building.getRemainingTrainTime());
		System.out.println("Total train time for " + unit.getType() 
				+ ": " + unit.getType().buildTime());
		if (building.getRemainingTrainTime() != 0
				|| queue.isEmpty()
				|| unit.getType() != queue.get(0)) {
			return false;
		}
		queue.remove(0);
		return true;
	}

	public void add(UnitType unit) {
		queue.add(unit);
//		if (queue.size() == 1) {
//			System.out.println("BUILDING " + unit + " NOW (from add)");
//			building.train(unit);
//		}
	}
	
	public List<UnitType> getQueue() {
		return queue;
	}
	
	public void check() {
		// If there's something in the queue and we aren't building
		if (!queue.isEmpty() && !building.isTraining()) {
			System.out.println("Starting construction of " + queue.get(0));
			building.train(queue.get(0));
		}
	}
	
	public Unit getBuilding() {
		return building;
	}
	
	public boolean canBuild(UnitType unit) {
		return ableToBuild.contains(unit);
	}

	@Override
	public int compareTo(BuildQueue other) {
		System.out.println("This: " + this.queue.size()
				+ ", Other: " + other.queue.size());
		return this.queue.size() - other.queue.size();
	}
}
