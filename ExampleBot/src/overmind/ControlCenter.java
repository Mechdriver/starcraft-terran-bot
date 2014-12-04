package overmind;

import resources.ResourceManager;
import scout.ScoutManager;
import tech.TechManager;
import attack.AttackManager;
import build.BuildManager;
import build.BuildRequest;
import bwapi.DefaultBWListener;
import bwapi.Game;
import bwapi.Player;
import bwapi.Unit;
import bwapi.UnitType;

public class ControlCenter extends DefaultBWListener {
	private Game game;
	private Player self;

	private ResourceManager resourceManager;
	private ScoutManager scoutManager;
	private BuildManager buildManager;
	private TechManager techManager;
	private AttackManager attackManager;
	
	public ControlCenter(Game game, ResourceManager resourceManager,
			ScoutManager scoutManager, BuildManager buildManager, TechManager techManager, AttackManager attackManager) {
		this.game = game;
		this.self = game.self();
		this.resourceManager = resourceManager;
		this.scoutManager = scoutManager;
		this.buildManager = buildManager;
		this.techManager = techManager;
		this.attackManager = attackManager;
	}

	@Override
	public void onStart() {
		// game.setTextSize(10);
		// game.drawTextScreen(10, 10, "Control Center initialized.");
		// System.out.println("Control Center initialized.");
		
		// TODO: lol. Don't just hardcode everything
		
		buildManager.submitBuildRequest(new BuildRequest(UnitType.Terran_SCV)
				.withBuildLocation(self.getStartLocation()));
		buildManager.submitBuildRequest(new BuildRequest(UnitType.Terran_SCV)
				.withBuildLocation(self.getStartLocation()));
		buildManager.submitBuildRequest(new BuildRequest(UnitType.Terran_SCV)
				.withBuildLocation(self.getStartLocation()));
		buildManager.submitBuildRequest(new BuildRequest(UnitType.Terran_SCV)
				.withBuildLocation(self.getStartLocation()));
		buildManager.submitBuildRequest(new BuildRequest(UnitType.Terran_SCV)
				.withBuildLocation(self.getStartLocation()));
		buildManager.submitBuildRequest(new BuildRequest(UnitType.Terran_Barracks)
				.withBuildLocation(self.getStartLocation()));
		buildManager.submitBuildRequest(new BuildRequest(UnitType.Terran_Barracks)
				.withBuildLocation(self.getStartLocation()));
		buildManager.submitBuildRequest(new BuildRequest(UnitType.Terran_Supply_Depot)
				.withBuildLocation(self.getStartLocation()));
		buildManager.submitBuildRequest(new BuildRequest(UnitType.Terran_Refinery)
				.withBuildLocation(self.getStartLocation()));
		buildManager.submitBuildRequest(new BuildRequest(UnitType.Terran_SCV)
				.withBuildLocation(self.getStartLocation()));
		buildManager.submitBuildRequest(new BuildRequest(UnitType.Terran_SCV)
				.withBuildLocation(self.getStartLocation()));
		buildManager.submitBuildRequest(new BuildRequest(UnitType.Terran_SCV)
				.withBuildLocation(self.getStartLocation()));
		buildManager.submitBuildRequest(new BuildRequest(UnitType.Terran_Supply_Depot)
				.withBuildLocation(self.getStartLocation()));
		buildManager.submitBuildRequest(new BuildRequest(UnitType.Terran_Supply_Depot)
				.withBuildLocation(self.getStartLocation()));
		/*buildManager.submitBuildRequest(new BuildRequest(UnitType.Terran_Marine)
				.withBuildLocation(self.getStartLocation()));
		buildManager.submitBuildRequest(new BuildRequest(UnitType.Terran_Marine)
				.withBuildLocation(self.getStartLocation()));
		buildManager.submitBuildRequest(new BuildRequest(UnitType.Terran_Marine)
				.withBuildLocation(self.getStartLocation()));
		buildManager.submitBuildRequest(new BuildRequest(UnitType.Terran_Barracks)
				.withBuildLocation(self.getStartLocation()));
		buildManager.submitBuildRequest(new BuildRequest(UnitType.Terran_Marine)
				.withBuildLocation(self.getStartLocation()));
		buildManager.submitBuildRequest(new BuildRequest(UnitType.Terran_Marine)
				.withBuildLocation(self.getStartLocation()));
		buildManager.submitBuildRequest(new BuildRequest(UnitType.Terran_Marine)
				.withBuildLocation(self.getStartLocation()));
		buildManager.submitBuildRequest(new BuildRequest(UnitType.Terran_Supply_Depot)
				.withBuildLocation(self.getStartLocation()));
		buildManager.submitBuildRequest(new BuildRequest(UnitType.Terran_Marine)
				.withBuildLocation(self.getStartLocation()));
		buildManager.submitBuildRequest(new BuildRequest(UnitType.Terran_Marine)
				.withBuildLocation(self.getStartLocation()));
		buildManager.submitBuildRequest(new BuildRequest(UnitType.Terran_Marine)
				.withBuildLocation(self.getStartLocation()));
		buildManager.submitBuildRequest(new BuildRequest(UnitType.Terran_Marine)
				.withBuildLocation(self.getStartLocation()));
		buildManager.submitBuildRequest(new BuildRequest(UnitType.Terran_Marine)
				.withBuildLocation(self.getStartLocation()));
		buildManager.submitBuildRequest(new BuildRequest(UnitType.Terran_Marine)
				.withBuildLocation(self.getStartLocation()));
		buildManager.submitBuildRequest(new BuildRequest(UnitType.Terran_Supply_Depot)
				.withBuildLocation(self.getStartLocation()));
		buildManager.submitBuildRequest(new BuildRequest(UnitType.Terran_Marine)
				.withBuildLocation(self.getStartLocation()));
		buildManager.submitBuildRequest(new BuildRequest(UnitType.Terran_Marine)
				.withBuildLocation(self.getStartLocation()));
		buildManager.submitBuildRequest(new BuildRequest(UnitType.Terran_Marine)
				.withBuildLocation(self.getStartLocation()));
		buildManager.submitBuildRequest(new BuildRequest(UnitType.Terran_Marine)
				.withBuildLocation(self.getStartLocation()));
		buildManager.submitBuildRequest(new BuildRequest(UnitType.Terran_Marine)
				.withBuildLocation(self.getStartLocation()));
		buildManager.submitBuildRequest(new BuildRequest(UnitType.Terran_Marine)
				.withBuildLocation(self.getStartLocation()));
		buildManager.submitBuildRequest(new BuildRequest(UnitType.Terran_Supply_Depot)
				.withBuildLocation(self.getStartLocation()));
		/*buildManager.submitBuildRequest(new BuildRequest(UnitType.Terran_Marine)
				.withBuildLocation(self.getStartLocation()));
		buildManager.submitBuildRequest(new BuildRequest(UnitType.Terran_Marine)
				.withBuildLocation(self.getStartLocation()));
		buildManager.submitBuildRequest(new BuildRequest(UnitType.Terran_Marine)
				.withBuildLocation(self.getStartLocation()));
		buildManager.submitBuildRequest(new BuildRequest(UnitType.Terran_Marine)
				.withBuildLocation(self.getStartLocation()));
		buildManager.submitBuildRequest(new BuildRequest(UnitType.Terran_Marine)
				.withBuildLocation(self.getStartLocation()));
		buildManager.submitBuildRequest(new BuildRequest(UnitType.Terran_Marine)
				.withBuildLocation(self.getStartLocation()));
		buildManager.submitBuildRequest(new BuildRequest(UnitType.Terran_Supply_Depot)
				.withBuildLocation(self.getStartLocation()));
		buildManager.submitBuildRequest(new BuildRequest(UnitType.Terran_Marine)
				.withBuildLocation(self.getStartLocation()));
		buildManager.submitBuildRequest(new BuildRequest(UnitType.Terran_Marine)
				.withBuildLocation(self.getStartLocation()));
		buildManager.submitBuildRequest(new BuildRequest(UnitType.Terran_Marine)
				.withBuildLocation(self.getStartLocation()));
		buildManager.submitBuildRequest(new BuildRequest(UnitType.Terran_Marine)
				.withBuildLocation(self.getStartLocation()));
		buildManager.submitBuildRequest(new BuildRequest(UnitType.Terran_Marine)
				.withBuildLocation(self.getStartLocation()));
		buildManager.submitBuildRequest(new BuildRequest(UnitType.Terran_Marine)
				.withBuildLocation(self.getStartLocation()));
		buildManager.submitBuildRequest(new BuildRequest(UnitType.Terran_Marine)
				.withBuildLocation(self.getStartLocation()));*/
		
		// Perform the first marine upgrade
		// Uncomment if you want upgrades to happen
//		techManager.performResearch(UnitType.Terran_Marine);
//		buildManager.submitBuildRequest(new BuildRequest(UnitType.Terran_Supply_Depot)
//				.withBuildLocation(self.getStartLocation()));
	}

	@Override
	public void onFrame() {
		for (Unit unit : self.getUnits()) {
			if (unit.getType().isWorker() 
					&& unit.isIdle() 
					&& unit.isCompleted()) {
				resourceManager.giveWorker(unit);
			}
		}
	}

	public Unit requestUnit(UnitType unitType) {
		Unit requestedUnit = null;
		requestedUnit = resourceManager.takeUnit(unitType);
		return requestedUnit;
	}
	
	public boolean submitRequest(BuildRequest request) {
		// TODO: checks if we can actually build the thing, after the 
		// current build queue has been done.
		return buildManager.submitBuildRequest(request);
	}
	
	public boolean releaseUnit(Unit unit) {
		if (unit.getType() == UnitType.Terran_SCV) {
			resourceManager.giveWorker(unit);
			return true;
		}
		return false;
	}
}
