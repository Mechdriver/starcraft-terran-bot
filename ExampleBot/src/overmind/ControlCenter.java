package overmind;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

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

import com.google.gson.Gson;

public class ControlCenter extends DefaultBWListener {
	private Game game;
	private Player self;

	private ResourceManager resourceManager;
//	private ScoutManager scoutManager;
	private BuildManager buildManager;
	private TechManager techManager;
	private AttackManager attackManager;
	
	// Constants for genetic algorithm
	public static final String knowledgeBasePath = "./knowledgebase.txt";
	public static final int NUM_BUILD_ORDERS = 12;
	public static final int NUM_ORDERS_PRESERVED = 3;
	public static final int INITIAL_BUILD_LENGTH = 10;
	public static final String[] UNIT_TYPES = {"SCV", "Barracks", "Marine", "SupplyDepot", "Refinery"};
	
	// Other global vars for genetic algorithm
	BuildOrder currentBuildOrder;
	public int buildOrderIteration = 0;
	
	public class BuildOrder {
		public int id;
		public ArrayList<String> order;
		public int time;
		public int resourceScore;
		
		public BuildOrder() {
			
		}
	}
	
	public ControlCenter(Game game, ResourceManager resourceManager,
		ScoutManager scoutManager, BuildManager buildManager, TechManager techManager, AttackManager attackManager) {
		this.game = game;
		this.self = game.self();
		this.resourceManager = resourceManager;
		
		// No scouting for this demo
//		this.scoutManager = scoutManager;
		
		this.buildManager = buildManager;
		this.techManager = techManager;
		this.attackManager = attackManager;
	}

	@Override
	public void onStart() {
		
		initializeBuildOrder();
		
		// Perform the first marine upgrade
		// Uncomment if you want upgrades to happen
//		techManager.performResearch(UnitType.Terran_Marine);
//		buildManager.submitBuildRequest(new BuildRequest(UnitType.Terran_Supply_Depot)
//				.withBuildLocation(self.getStartLocation()));
	}
	
	@Override
	public void onEnd(boolean arg0) {
		super.onEnd(arg0);
		
		// arg0 True if winner, else false
		// Also false on early quit
		System.out.println("Game ended with winner: " + arg0);
		
		System.out.println("Ran Build order #: " + currentBuildOrder.id);
		// Wtf... don't have access to game.elapsedTime;
		//int gameTime = game.getFrameCount()/game.getFPS();
		int gameTime = game.getFrameCount();
		int resourceScore = self.gatheredMinerals() + self.gatheredGas();
		System.out.println("Time Elapsed(Seconds): " + gameTime);
		System.out.println("Resource Score: " + resourceScore);
		
		// Update the currentBuild order with the stats of the run through
		currentBuildOrder.time = gameTime;
		currentBuildOrder.resourceScore = resourceScore;
		
		updateBuildOrderInKB(currentBuildOrder);
		
	}

	// Find the build order in the file and update the data
	public void updateBuildOrderInKB(BuildOrder current) {
		try {
			File knowledgeBase = new File(knowledgeBasePath);
			Scanner in = new Scanner(knowledgeBase);
			Gson gson = new Gson();
			
			ArrayList<String> kbLines = new ArrayList<String>();
			while(in.hasNextLine()) {
				String line = in.nextLine();
				System.out.println("Adding line to file: " + line);
				kbLines.add(line);
			}
			in.close();
			
			BufferedWriter writer = new BufferedWriter(new FileWriter(knowledgeBase, false));
			// TODO: Could make more efficient and stop serializing objects once the correct build order has been updated
			for (String line : kbLines) {
				if (line.substring(0, line.indexOf(":")).equals("Index")) {
					int index = Integer.parseInt(line.substring(line.indexOf(":")+1));
					index++;
					writer.write("Index:"+index);
				}
				else if (isJsonBuildOrder(line)){
					BuildOrder temp = gson.fromJson(line, BuildOrder.class);
					if(temp.id == current.id) {
						writer.write(gson.toJson(current));
					}
					else {
						writer.write(gson.toJson(temp));
					}
				}
				else {
					writer.write(line);
				}
				writer.newLine();
			}
			writer.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public boolean isJsonBuildOrder(String s) {
	      try {
	    	  Gson gson = new Gson();
	          gson.fromJson(s, BuildOrder.class);
	          return true;
	      } catch(com.google.gson.JsonSyntaxException ex) { 
	          return false;
	      }
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
	
	public void initializeBuildOrder() {
		File knowledgeBase = new File(knowledgeBasePath);
		
		// If the file does not exist, create it and write 12 randomly generated build order to it
		if (!knowledgeBase.isFile()) {
			System.out.println("File Does not exist, creating...");
			try {
				generateInitialBuildOrders(knowledgeBase);				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		// Read from the file, get the build order index, load that build order and run it
		try {
			Scanner in = new Scanner(knowledgeBase);
			
			// Get the index of the build order we want to load:
			String line = in.nextLine();
			int index = Integer.parseInt(line.substring(line.indexOf(":")+1));
			System.out.println("Loading index: " + index);
			
			// Find the correct build order to load
			if (index < NUM_BUILD_ORDERS) {
				loadBuildOrderFromFile(in, index);
			}
			else {
				buildOrderIteration = index;
				forceNaturalSelection(in);
			}
			
			in.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	// TODO: Write function to take the top build orders to survive natural selection
	// Input: scanner coming in pointed at the first buildOrder line in the file
	public void forceNaturalSelection(Scanner in) {
		Gson gson = new Gson();
		ArrayList<BuildOrder> allOrders = new ArrayList<BuildOrder>();
		ArrayList<BuildOrder> ordersToSave = new ArrayList<BuildOrder>();
		
		while (in.hasNextLine()) {
			BuildOrder temp = gson.fromJson(in.nextLine(), BuildOrder.class);
			allOrders.add(temp);
		}
		
		for (int i = 0 ; i < NUM_ORDERS_PRESERVED; i++) {
			// Find the highest scoring build order, preserve it, remove it from the initial list
			int highest = -1;
			BuildOrder temp = null;
			for (BuildOrder order : allOrders) {
				int score = order.time + order.resourceScore;
				if (score > highest) {
					highest = score;
					temp = order;
				}
			}
			ordersToSave.add(temp);
			allOrders.remove(temp);
		}
		
		createNextGeneration(ordersToSave);
		
		try {
			File knowledgeBase = new File(knowledgeBasePath);
			Scanner newScanner = new Scanner(knowledgeBase);
			// Get the index of the build order we want to load:
			String line = newScanner.nextLine();
			int index = Integer.parseInt(line.substring(line.indexOf(":")+1));
			System.out.println("Loading index: " + index);
			
			loadBuildOrderFromFile(newScanner, index);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void createNextGeneration(ArrayList<BuildOrder> orders) {
		Gson gson = new Gson();
		
		System.out.println("The following build orders will survive in the gene pool: ");
		for (BuildOrder order : orders) {
			System.out.println(gson.toJson(order));
		}
		
		try {
			File knowledgeBase = new File(knowledgeBasePath);
			BufferedWriter writer = new BufferedWriter(new FileWriter(knowledgeBase, false));
			writer.write("Index:" + buildOrderIteration);
			writer.newLine();
			
			// TODO: Write all the new build orders to the file with an id of buildOrderIteration++
			
			// Keep the original 3 winners, but update their index
			for (BuildOrder order : orders) {
				order.id = buildOrderIteration++;
				order.time = -1;
				order.resourceScore = -1;
				writer.write(gson.toJson(order));
				writer.newLine();
			}
			
			// Frankenstein the original 3 to come up with 6 monster children
			
			// Flip 2 random moves in the original 3 to add some mutation to the mix
			
			
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
	}

	// Generate 12 random build orders and write them to the file
	public void generateInitialBuildOrders(File knowledgeBase) throws IOException {
		
		// Always start index at 0 to load the first randomly generated build order
		BufferedWriter writer = new BufferedWriter(new FileWriter(knowledgeBase));
		writer.write("Index:0");
		writer.newLine();
		
		Random random = new Random();
		for (int i = 0; i < NUM_BUILD_ORDERS; i++) {
			BuildOrder buildOrder = new BuildOrder();
			buildOrder.id = buildOrderIteration++;
			buildOrder.time = -1;
			buildOrder.resourceScore = -1;
			buildOrder.order = new ArrayList<String>();
			
			for (int j = 0; j < INITIAL_BUILD_LENGTH; j++){
				int randIndex = random.nextInt(UNIT_TYPES.length);
				buildOrder.order.add(UNIT_TYPES[randIndex]);
			}
			
			Gson gson = new Gson();
			String buildOrderJson = gson.toJson(buildOrder);
			writer.write(buildOrderJson);
			writer.newLine();
		}
		
		writer.close();
	}

	// Loads a build order given a scanner to the file and the index of the build order
	public void loadBuildOrderFromFile(Scanner in, int index){
		Gson gson = new Gson();
		currentBuildOrder = gson.fromJson(in.nextLine(), BuildOrder.class);
		while (currentBuildOrder.id != index) {
			currentBuildOrder = gson.fromJson(in.nextLine(), BuildOrder.class);
		}
						
		
		// Load the build order
		System.out.println("Build Order from file: " + currentBuildOrder.id);
		for (String unit : currentBuildOrder.order) {
			UnitType unitType = null;
			System.out.println(unit);
			if (unit.equals("SCV")){
				unitType = UnitType.Terran_SCV;
			}
			else if (unit.equals("Marine")){
				unitType = UnitType.Terran_Marine;
			}
			else if (unit.equals("Barracks")){
				unitType = UnitType.Terran_Barracks;
			}
			else if (unit.equals("SupplyDepot")){
				unitType = UnitType.Terran_Supply_Depot;
			}
			else if (unit.equals("Refinery")){
				unitType = UnitType.Terran_Refinery;
			}
			else {
				System.out.println("Something went terribly wrong in the build order: " + unitType);
			}
			
			if (unitType != null) {
				buildManager.submitBuildRequest(new BuildRequest(unitType)
					.withBuildLocation(self.getStartLocation()));
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
