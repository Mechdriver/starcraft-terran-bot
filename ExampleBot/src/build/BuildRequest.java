package build;

import java.util.Collection;

import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;

public class BuildRequest {
	public static final int DEFAULT_PRIORITY = 5;

	private UnitType unit;
	private TilePosition buildLocation;
	private int priority;
	private Collection<Unit> unitOutput;

	public BuildRequest(UnitType unit) {
		this.unit = unit;
		this.priority = DEFAULT_PRIORITY;
	}

	public Collection<Unit> getUnitOutput() {
		return unitOutput;
	}

	public BuildRequest withUnitOutput(Collection<Unit> requesterUnits) {
		this.unitOutput = requesterUnits;
		return this;
	}

	public UnitType getUnit() {
		return unit;
	}

	public BuildRequest withUnit(UnitType unit) {
		this.unit = unit;
		return this;
	}

	public TilePosition getBuildLocation() {
		return buildLocation;
	}

	public BuildRequest withBuildLocation(TilePosition nearHere) {
		this.buildLocation = nearHere;
		return this;
	}

	public int getPriority() {
		return priority;
	}

	public BuildRequest withPriority(int priority) {
		this.priority = priority;
		return this;
	}

	@Override
	public String toString() {
		return "Request={unit=" + unit + "}";
		// return "Request=[unit=" + unit + ", priority=" + priority
		// + ", nearHere=" + buildLocation + ", requesterUnits="
		// + requesterUnits + "]";
	}

/*	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((buildLocation == null) ? 0 : buildLocation.hashCode());
		result = prime * result + priority;
		result = prime
				* result
				+ ((unitOutput == null) ? 0 : unitOutput.hashCode());
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
		if (buildLocation == null) {
			if (other.buildLocation != null)
				return false;
		} else if (!buildLocation.equals(other.buildLocation))
			return false;
		if (priority != other.priority)
			return false;
		if (unitOutput == null) {
			if (other.unitOutput != null)
				return false;
		} else if (!unitOutput.equals(other.unitOutput))
			return false;
		if (unit == null) {
			if (other.unit != null)
				return false;
		} else if (!unit.equals(other.unit))
			return false;
		return true;
	}
*/
}
