package driver;

import java.util.ArrayList;
import java.util.List;

import overmind.ControlCenter;
import resources.ResourceManager;
import scout.ScoutManager;
import build.BuildManager;
import bwapi.DefaultBWListener;
import bwapi.Game;
import bwapi.Mirror;
import bwapi.Unit;
import bwta.BWTA;


public class TerranBotDriver {
	List<DefaultBWListener> listenerModules;
	private Game game;
	private Mirror mirror;
	
	public static void main(String[] args) {
		new TerranBotDriver().run();
	}
	
	public TerranBotDriver() {
		mirror = new Mirror();
	}
	
	public void run() {
		mirror.getModule().setEventListener(new DefaultBWListener() {
			@Override
			public void onStart() {
				game = mirror.getGame();
				listenerModules = new ArrayList<DefaultBWListener>();
				game.setLocalSpeed(5);
				
				// Use BWTA to analyze map
				// This may take a few minutes if the map is processed first
				// time!
				System.out.println("Analyzing map...");
				BWTA.readMap();
				BWTA.analyze();
				System.out.println("Map data ready");
				
				// Add AI modules here
				listenerModules.add(new ControlCenter(game));
				listenerModules.add(new ResourceManager(mirror, game, game.self()));
				listenerModules.add(new ScoutManager(game));
				listenerModules.add(new BuildManager(game, game.self()));
				
				for (DefaultBWListener listener : listenerModules) {
					listener.onStart();
				}
			}
			
			@Override
			public void onFrame() {
				for (DefaultBWListener listener : listenerModules) {
					listener.onFrame();
				}
			}
			
			@Override
			public void onUnitCreate(Unit unit) {
				for (DefaultBWListener listener : listenerModules) {
					listener.onUnitCreate(unit);
				}
			}
			
			@Override
			public void onUnitComplete(Unit unit) {
				for (DefaultBWListener listener : listenerModules) {
					listener.onUnitComplete(unit);
				}
			}

			@Override
			public void onUnitDestroy(Unit unit) {
				for (DefaultBWListener listener : listenerModules) {
					listener.onUnitDestroy(unit);
				}
			}

			@Override
			public void onUnitDiscover(Unit unit) {
				for (DefaultBWListener listener : listenerModules) {
					listener.onUnitDiscover(unit);
				}
			}

			@Override
			public void onEnd(boolean isWinner) {
				for (DefaultBWListener listener : listenerModules) {
					listener.onEnd(isWinner);
				}
			}

			@Override
			public void onUnitEvade(Unit unit) {
				for (DefaultBWListener listener : listenerModules) {
					listener.onUnitEvade(unit);
				}
			}

			@Override
			public void onUnitHide(Unit unit) {
				for (DefaultBWListener listener : listenerModules) {
					listener.onUnitHide(unit);
				}
			}

			@Override
			public void onUnitShow(Unit unit) {
				for (DefaultBWListener listener : listenerModules) {
					listener.onUnitShow(unit);
				}
			}
			
			// TODO: any other methods that modules want access to should be overridden here in the same way as onFrame()
		});
		
		mirror.startGame();
	}

}
