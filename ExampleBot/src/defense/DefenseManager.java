package defense;

import java.util.ArrayList;
import java.util.List;

import bwapi.DefaultBWListener;
import bwapi.Unit;
import bwapi.UnitType;

public class DefenseManager extends DefaultBWListener {
	private List<Unit> bunkers;
	private List<Unit> marines;

	@Override
	public void onStart() {
		bunkers = new ArrayList<Unit>();
		marines = new ArrayList<Unit>();
	}

	@Override
	public void onUnitComplete(Unit unit) {
		if (unit.getType() == UnitType.Terran_Marine) {
			marines.add(unit);
		}
		if (unit.getType() == UnitType.Terran_Bunker) {
			bunkers.add(unit);
		}
	}

	@Override
	public void onFrame() {
		if (marines.isEmpty()) {
			return;
		}
		List<Unit> deadMarines = new ArrayList<Unit>();
		Unit unloadedMarine = null;
		for (Unit marine : marines) {
			if (marine.getType() == UnitType.Unknown) {
				// He's dead, Jim!
				deadMarines.add(marine);
			} else if (!marine.isLoaded()) {
				unloadedMarine = marine;
			}
		}
		for (Unit marine : deadMarines) {
			marines.remove(marine);
		}

		if (bunkers.isEmpty()) {
			return;
		}
		List<Unit> deadBunkers = new ArrayList<Unit>();
		Unit openBunker = null;
		for (Unit bunker : bunkers) {
			if (bunker.getType() == UnitType.Unknown) {
				deadBunkers.add(bunker);
			} else if (bunker.getLoadedUnits().size() < 4) {
				openBunker = bunker;
			}
		}

		if (unloadedMarine != null && unloadedMarine.isIdle()
				&& openBunker != null) {
			unloadedMarine.rightClick(openBunker);
		}
	}
}
