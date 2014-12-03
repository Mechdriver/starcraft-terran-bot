package scout;

import java.util.ArrayList;
import java.util.HashMap;

import overmind.ControlCenter;
import build.BuildRequest;
import bwapi.DefaultBWListener;
import bwapi.Game;
import bwapi.Position;
import bwapi.Unit;
import bwapi.UnitType;
import bwta.BWTA;
import bwta.BaseLocation;

public class ScoutManager extends DefaultBWListener {
	private ControlCenter motherBrain = null;
	private Unit myScout = null;
	private Game myGame;
	private boolean request = false;
	private boolean scouting = false; 
	private boolean reported = false;
	private Position enemyBaseLoc = null;
	private HashMap<Integer, Unit> enemyUnitMemory = new HashMap<Integer, Unit>();
	BuildRequest scoutReq;
	
	public ScoutManager(Unit scout, Game game) {
		myScout = scout;
		myGame = game;
		findBase();
		
	}
	
	public ScoutManager(Game game) {
		myGame = game;	
	}
	
	@Override
	public void onStart() {
		System.out.println("Scout Manager initialized.");
	}	
	
	@Override
	public void onFrame() {
		//System.out.println("A");
		
		if (motherBrain != null && !request) {
			scoutReq = new BuildRequest(UnitType.Terran_Marine);
			scoutReq = scoutReq.withUnitOutput(new ArrayList<Unit>());
			motherBrain.submitRequest(scoutReq);
			motherBrain.submitRequest(new BuildRequest(UnitType.Terran_Supply_Depot));
			for (int i = 0; i < 3; i++) {
				motherBrain.submitRequest(new BuildRequest(UnitType.Terran_Marine));
			}
			request = true;
		}
		
		if (scoutReq != null && scoutReq.getUnitOutput().size() > 0) {
			ArrayList<Unit> list = (ArrayList<Unit>)scoutReq.getUnitOutput();
			
			Unit unit = list.get(0);
			
			list.remove(0);
			
			setScout(unit);
		}
		
		if (myScout != null && !scouting) {
			if (enemyBaseLoc == null) {
				findBase();
			}
			
			else {
				myScout.move(enemyBaseLoc);
			}
			
			scouting = true;
		}
		
		if (myScout != null && foundEnemy()) {
				primeScout();			
		}
		
		if (myScout != null && enemyBaseLoc != null && !reported) {
			if (distToEnemy(25) || myScout.isAttacking() || myScout.isUnderAttack()) {
				reportEnemy();
				reported = true;
			}
		}
		
		if (myScout != null && reported && myScout.getHitPoints() <= 0) {
			myScout = null;
			scouting = false;
			reported = false;
			scoutReq = null;
			request = false;			
		}
	}
	/* Control Center Calls
	//public void requestUnits(UnitType unit, Integer amount);
	//public void requestBuilding(UnitType building)
	*/
	
	public void setControlCenter(ControlCenter control) {
		motherBrain = control;
	}
	
	public void setScout(Unit scout) {
		myScout = scout;	
	}
	
	//after base is found, send out another scout and start using gathered info to create an army
	
	private void findBase() {
		for (BaseLocation baseL : BWTA.getBaseLocations()) {
			if(baseL.isStartLocation() && baseL.getTilePosition() != myGame.self().getStartLocation()) {
				myScout.move(baseL.getPosition(), true);
			}
		}
	}
	
	public boolean foundEnemy() {
		if (!myGame.enemy().getUnits().isEmpty())
			return true;
		else
			return false;
	}
	
	public void primeScout() {
		int minDist = 1000000000;
		int curDist;
		Position bestPos = null;
		
		for (BaseLocation baseL : BWTA.getBaseLocations()) {
			if(baseL.isStartLocation() && baseL.getTilePosition() != myGame.self().getStartLocation()) {
				curDist = baseL.getPosition().getApproxDistance(myScout.getPosition());
				if (curDist < minDist) {
					minDist = curDist;
					bestPos = baseL.getPosition();
				}
			}
		}
		
		enemyBaseLoc = bestPos;
		
		if (enemyBaseLoc != null) {
			myScout.move(enemyBaseLoc);
		}
		
		else {
			myScout.stop();
		}
	}
	
	public boolean distToEnemy(int lim) {
		if (myScout.getPosition().getApproxDistance(enemyBaseLoc) <= lim) {
			return true;
		}
		return false;
	}
	
	public Position getEnemyBaseLoc() {
		return enemyBaseLoc;
	}
	
	public void reportEnemy() {
		myScout.stop();
		for (Unit eUnit : myGame.enemy().getUnits()) {
			if (!enemyUnitMemory.containsKey(eUnit.getID())) {
				enemyUnitMemory.put(eUnit.getID(), eUnit);
				myGame.printf("Sir, I found a " + eUnit.getType().toString() + "!");
				System.out.println("Sir, I found a " + eUnit.getType().toString() + "!");
			}
		}
	}
	
	public HashMap<Integer, Unit> getEnemyUnits() {
		return enemyUnitMemory;
	}
	
	public Unit getScout() {
		return myScout;
	}	
}
