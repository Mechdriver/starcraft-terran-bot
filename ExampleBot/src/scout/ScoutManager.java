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
	private BuildRequest scoutReq;
	private ArrayList<Unit> scoutList = new ArrayList<Unit>();
	
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
		if (motherBrain != null && !request && enemyBaseLoc == null) {
			scoutReq = new BuildRequest(UnitType.Terran_Marine);
			scoutReq = scoutReq.withUnitOutput(scoutList);
			motherBrain.submitRequest(scoutReq);
			request = true;
		}
		
		if (scoutReq != null && scoutList.size() > 0) {
			
			Unit unit = scoutList.get(0);
			
			scoutList.remove(0);
			
			setScout(unit);
		}
		
		if (myScout != null && !scouting) {
			if (enemyBaseLoc == null) {
				findBase();
			}
			
			else {
				myScout.attack(enemyBaseLoc);
			}
			
			scouting = true;
		}
		
		if (myScout != null && enemyBaseLoc == null && foundEnemy()) {
				primeScout();			
		}
		
		if (myScout != null && enemyBaseLoc != null && !reported) {
			if (distToEnemy(25) || myScout.isUnderAttack()) {
				//reportEnemy();
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
		if (!myGame.enemy().getUnits().isEmpty()) {
			for (Unit enemy : myGame.enemy().getUnits()) {
				if (enemy.getType().isBuilding()) {
					return true;
				}
			}
		}
			
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
		
		if (enemyBaseLoc == null) {
			enemyBaseLoc = bestPos;
		}
		
		if (enemyBaseLoc != null) {
			myScout.attack(enemyBaseLoc);
			//myScout.stop();
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
