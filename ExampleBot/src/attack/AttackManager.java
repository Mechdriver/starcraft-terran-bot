package attack;

import java.util.ArrayList;
import java.util.HashMap;

import overmind.ControlCenter;
import scout.ScoutManager;
import build.BuildRequest;
import bwapi.DefaultBWListener;
import bwapi.Game;
import bwapi.Position;
import bwapi.Unit;
import bwapi.UnitType;

public class AttackManager extends DefaultBWListener {
	private ControlCenter motherBrain = null;
	private ScoutManager scoutMan;
	private Game myGame;
	private Position enemyBaseLoc = null;
	private BuildRequest squadReq;
	private boolean attacking = false;
	private boolean depot = false;
	private int reqs = 0;
	private int supRatio = 0;
	private Unit target = null;
	private ArrayList<Unit> squadList = new ArrayList<Unit>();
	
	private static int maxSize = 20;
	
	public AttackManager(Game game, ScoutManager scoutMan) {
		myGame = game;
		this.scoutMan = scoutMan;
	}
	
	@Override
	public void onStart() {
		System.out.println("Attack Manager initialized.");
	}
	
	@Override
	public void onUnitDestroy(Unit unit) {
		if (unit.getType() == UnitType.Terran_Marine) {			
			if (squadList.remove(unit)) {
				reqs--;
			}
		}
		
		if (unit.equals(target)) {
			target = null;
		}
	}
	
	@Override
	public void onFrame() {
		/*if (motherBrain != null && myGame.self().supplyUsed() >= myGame.self().supplyTotal() - 3 && !depot) {
			motherBrain.submitRequest(new BuildRequest(UnitType.Terran_Supply_Depot));
			depot = true;
		}
		
		if (motherBrain != null && myGame.self().supplyUsed() < myGame.self().supplyTotal() - 3) {
			depot = false;
		}*/
		
		if (motherBrain != null && reqs < maxSize) {
			squadReq = new BuildRequest(UnitType.Terran_Marine);
			squadReq = squadReq.withUnitOutput(squadList);
			motherBrain.submitRequest(squadReq);
			reqs++;
			supRatio++;
			
			if (supRatio == 5) {
				motherBrain.submitRequest(new BuildRequest(UnitType.Terran_Supply_Depot));
				supRatio = 0;
			}
		}
		
		if (enemyBaseLoc == null) {
			enemyBaseLoc = scoutMan.getEnemyBaseLoc();
		}
		
		//TODO: Get rid of Marines when they die.
		
		if (enemyBaseLoc != null && squadSize() >= maxSize) {
			for (Unit joe : squadList) {
				joe.attack(enemyBaseLoc);
			}			
			attacking = true;
		}
		
		if (!myGame.enemy().getUnits().isEmpty() && attacking) {
			target = myGame.enemy().getUnits().get(0);
			int min = 1000000000;
			//ArrayList<Unit> tempList = new ArrayList<Unit>(squadList);
			
			for (Unit joe : squadList) {
				Unit unqTarget = null;
				for (Unit en : myGame.enemy().getUnits()) {
					int dist = joe.getDistance(en);
					
					if (dist < min) {
						min = dist;
						unqTarget = en;
					}
				}
				if (unqTarget != null){
					joe.attack(unqTarget);
				}
			}
		}	
	}
	
	private int squadSize() {
		return squadList.size();
	}
	
	public void setControlCenter(ControlCenter control) {
		motherBrain = control;
	}
	
}
