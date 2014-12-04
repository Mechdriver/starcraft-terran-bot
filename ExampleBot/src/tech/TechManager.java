package tech;

import java.util.ArrayList;
import java.util.LinkedList;

import overmind.ControlCenter;
import build.BuildRequest;
import bwapi.DefaultBWListener;
import bwapi.Game;
import bwapi.Player;
import bwapi.Unit;
import bwapi.UnitType;
import bwapi.UpgradeType;

public class TechManager extends DefaultBWListener {
	private UnitType researchFocus = null;
	private boolean researchInProgress;
	private Game game;
	private ControlCenter control;
	private Player self;
	private UpgradeRequest currentUpgrade;
	
	// Used to hold preset tech build orders
	private LinkedList<UpgradeRequest> marineUpgradeOrder;
	private ArrayList<Unit> upgradeBuildings;
	
	public class UpgradeRequest {
		UnitType building;
		UpgradeType upgrade;
		
		public UpgradeRequest(UnitType building, UpgradeType upgrade) {
			this.building = building;
			this.upgrade = upgrade;
		}
	}
	
	public TechManager(Game game) {
		this.game = game;
		researchInProgress = false;
		this.self = game.self();
		
		// Set the hardcoded marine upgrade order
		upgradeBuildings = new ArrayList<Unit>();
		marineUpgradeOrder = new LinkedList<UpgradeRequest>();
		marineUpgradeOrder.add(new UpgradeRequest(UnitType.Terran_Engineering_Bay, UpgradeType.Terran_Infantry_Weapons));
		marineUpgradeOrder.add(new UpgradeRequest(UnitType.Terran_Engineering_Bay, UpgradeType.Terran_Infantry_Armor));
	}
	
	public void setControlCenter(ControlCenter control) {
		this.control = control;
	}
	
	// Method called by ControlCenter to research the next upgrade for the given unit
	public void performResearch(UnitType unitType) {
		System.out.println("Starting research for " + unitType.toString());
		this.researchFocus = unitType;
	}
	
	private void performUpgradeRequest(UpgradeRequest request) {		
		for (Unit unit : self.getUnits()) {
			if (unit.getType() == request.building) {
				// If we don't have enough resources, wait
				if (self.minerals() > request.upgrade.mineralPrice() && self.gas() > request.upgrade.gasPrice()) {
					// We have the building that can perform the research, so do it
					unit.upgrade(request.upgrade);
					//System.out.println("Performing Upgrade: " + request.upgrade.toString());
				}
				return;
			}
		}
		
		// We didnt find the building, so submit a buildRequest for it
		BuildRequest buildRequest = new BuildRequest(request.building);
		buildRequest.withUnitOutput(upgradeBuildings);
		control.submitRequest(buildRequest);
		System.out.println("Requested Building: " + request.building.toString());
	}
	
	@Override
	public void onFrame() {
		// If we reach this state, we're waiting on a building before we can perform the upgrade
		if (researchInProgress && currentUpgrade != null && upgradeBuildings.size() > 0) {
			// The building is complete so finish the upgrade and reset vars
			performUpgradeRequest(currentUpgrade);
		}
		if (researchFocus == null || researchInProgress) {
			return;
		}
		if (researchFocus == UnitType.Terran_Marine) {
			if (marineUpgradeOrder.size() > 0) {
				researchInProgress = true;
				currentUpgrade = marineUpgradeOrder.remove();
				performUpgradeRequest(currentUpgrade);
			}
		}
		else {
			System.out.println("No Upgrades queued up for " + researchFocus.toString());
		}
	}

	@Override
	public void onUnitComplete(Unit unit) {
		// If the completed unit is the upgrade we were waiting for, get ready for the next upgrade
		if (researchInProgress && currentUpgrade != null) {
			if (unit.getUpgrade() == currentUpgrade.upgrade) {
				System.out.println("Research completed! -- " + unit.getUpgrade().toString());
				researchInProgress = false;
				currentUpgrade = null;
				upgradeBuildings.clear();
			}
		}
		
		for (int i = 0; i < 3; i ++) {
			control.submitRequest(new BuildRequest(UnitType.Terran_Marine));
		}
	}
	
	
}
