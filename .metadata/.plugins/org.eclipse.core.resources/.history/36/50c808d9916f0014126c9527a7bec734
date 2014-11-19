package driver;

import java.util.ArrayList;
import java.util.List;

import overmind.ControlCenter;
import resources.ResourceManager;
import bwapi.DefaultBWListener;
import bwapi.Game;
import bwapi.Mirror;


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
				
				// Add AI modules here
				listenerModules.add(new ControlCenter(game));
				listenerModules.add(new ResourceManager(game));
				
				for (DefaultBWListener listener : listenerModules) {
					listener.onStart();
				}
			}
			
			@Override
			public void onFrame() {
				// Call all listener onFrame methods
				for (DefaultBWListener listener : listenerModules) {
					listener.onFrame();
				}
			}
			
			// TODO: any other methods that modules want access to should be overridden here in the same way as onFrame()
		});
		
		mirror.startGame();
	}

}
