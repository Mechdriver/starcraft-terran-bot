package scout;

import java.util.HashMap;

import bwapi.DefaultBWListener;
import bwapi.Game;
import bwapi.Position;
import bwapi.Unit;
import bwta.BWTA;
import bwta.BaseLocation;

public class ScoutManager extends DefaultBWListener {
	private Unit myScout;
	private Game myGame;
	private Position enemyBaseLoc;
	private HashMap<Position, Unit> enemyUnitMemory = new HashMap<Position, Unit>();
	
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
		//request scout
		//make scout scout
	}
	
	@Override
	public void onFrame() {
		System.out.println("Looking for the enemy.");				
	}
	/* Control Center Calls
	//public void requestUnits(UnitType unit, Integer amount);
	//public void requestBuilding(UnitType building)
	*/
	
	public void setScout (Unit scout) {
		myScout = scout;
		findBase();
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
			if (!enemyUnitMemory.containsKey(eUnit.getPosition())) {
				enemyUnitMemory.put(eUnit.getPosition(), eUnit);
				myGame.printf("Sir, I found a " + eUnit.getType().toString() + "!");
				System.out.println("Sir, I found a " + eUnit.getType().toString() + "!");
			}
		}
	}
	
	public Unit getScout() {
		return myScout;
	}	
}
