package overmind;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

import resources.ResourceManager;
import tech.TechManager;
import build.BuildManager;
import build.BuildRequest;
import bwapi.DefaultBWListener;
import bwapi.Game;
import bwapi.Player;
import bwapi.Unit;
import bwapi.UnitType;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ControlCenter extends DefaultBWListener {
	private Game game;
	private Player self;

	private ResourceManager resourceManager;
	// private ScoutManager scoutManager;
	private BuildManager buildManager;
//	private TechManager techManager;
	// private AttackManager attackManager;
	// Constants for genetic algorithm
	public static final String knowledgeBasePath = "./knowledgebase.txt";
	public static final int NUM_BUILD_ORDERS = 12;
	public static final int NUM_ORDERS_PRESERVED = 3;
	public static final int INITIAL_BUILD_LENGTH = 20;
	public static final Map<String, UnitType> UNIT_MAP = new HashMap<>();

	static {
		UNIT_MAP.put("SCV", UnitType.Terran_SCV);
		UNIT_MAP.put("Barracks", UnitType.Terran_Barracks);
		UNIT_MAP.put("Marine", UnitType.Terran_Marine);
		UNIT_MAP.put("SupplyDepot", UnitType.Terran_Supply_Depot);
		UNIT_MAP.put("Refinery", UnitType.Terran_Refinery);
		UNIT_MAP.put("Bunker", UnitType.Terran_Bunker);

		UNIT_TYPES = UNIT_MAP.keySet().toArray(new String[UNIT_MAP.size()]);
	}

	public static final String[] UNIT_TYPES;
	// Other global vars for genetic algorithm
	BuildOrder currentBuildOrder;
	public int buildOrderIteration = 0;
	public int ITERATION_COUNT = NUM_BUILD_ORDERS;

	
	public class BuildOrder {
		public List<String> order;
		public int score;
		
		public BuildOrder() {
			
		}
		
		public void setScore(int time, int resourceScore, int numBuildFailures, int killScore) {
			this.score = time + resourceScore + killScore;
			this.score -= numBuildFailures;
		}
	}
	
	public class KnowledgeBaseObject {
		public List<BuildOrder> orders;
		public int index;
		public boolean readyToMutate;
		
		public KnowledgeBaseObject () {
			
		}
	}
	

	public ControlCenter(Game game, ResourceManager resourceManager,
			BuildManager buildManager, TechManager techManager) {
		this.game = game;
		this.self = game.self();
		this.resourceManager = resourceManager;

		// No scouting for this demo
		// this.scoutManager = scoutManager;

		this.buildManager = buildManager;
//		this.techManager = techManager;
		// this.attackManager = attackManager;
	}

	@Override
	public void onStart() {

		initializeBuildOrder();

		// Perform the first marine upgrade
		// Uncomment if you want upgrades to happen
		// techManager.performResearch(UnitType.Terran_Marine);
		// buildManager.submitBuildRequest(new
		// BuildRequest(UnitType.Terran_Supply_Depot)
		// .withBuildLocation(self.getStartLocation()));
	}

	@Override
	public void onEnd(boolean arg0) {
		super.onEnd(arg0);

		// arg0 True if winner, else false
		// Also false on early quit
		System.out.println("Game ended with winner: " + arg0);

		// Wtf... don't have access to game.elapsedTime;
		// int gameTime = game.getFrameCount()/game.getFPS();
		int gameTime = game.getFrameCount();
		int resourceScore = self.gatheredMinerals() + self.gatheredGas();
		int numFailures = buildManager.getBuildFailures();
		int killScore = self.getKillScore() * 10000;
		System.out.println("Time Elapsed(Seconds): " + gameTime);
		System.out.println("Resource Score: " + resourceScore);

		// Update the currentBuild order with the stats of the run through
		currentBuildOrder.setScore(gameTime, resourceScore, numFailures, killScore);

		updateBuildOrderInKB(currentBuildOrder);

	}

	// Find the build order in the file and update the data
	public void updateBuildOrderInKB(BuildOrder current) {
		try {
			File knowledgeBase = new File(knowledgeBasePath);
			Scanner in = new Scanner(knowledgeBase);
			Gson gson = new Gson();

			KnowledgeBaseObject kb = gson.fromJson(in.nextLine(), KnowledgeBaseObject.class);
			in.close();

			BufferedWriter writer = new BufferedWriter(new FileWriter(
					knowledgeBase, false));
			
			kb.orders.set(kb.index, current);
			kb.index++;
			if (kb.index >= NUM_BUILD_ORDERS) {
				//kb.readyToMutate = true;
				
				// Store what we did for this first iteration
				File knowledgeBaseIteration = new File("./iteration" + buildOrderIteration++ + ".txt");
				BufferedWriter writer2 = new BufferedWriter(new FileWriter(
						knowledgeBaseIteration, false));
				writer2.write(gson.toJson(kb));
				writer2.newLine();
				Gson gsonPretty = new GsonBuilder().setPrettyPrinting().create();
				writer2.write(gsonPretty.toJson(kb));
				writer2.newLine();
				writer2.close();
				
				// Then naturally select the new orders
				forceNaturalSelection(kb);
			}
			else {
				writer.write(gson.toJson(kb));
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
		} catch (com.google.gson.JsonSyntaxException ex) {
			return false;
		}
	}

	@Override
	public void onFrame() {
		for (Unit unit : self.getUnits()) {
			if (unit.getType().isWorker() && unit.isIdle()
					&& unit.isCompleted()) {
				resourceManager.giveWorker(unit);
			}
		}
	}

	public void initializeBuildOrder() {
		File knowledgeBase = new File(knowledgeBasePath);

		// If the file does not exist, create it and write 12 randomly generated
		// build order to it
		if (!knowledgeBase.isFile()) {
			System.out.println("File Does not exist, creating...");
			try {
				generateInitialBuildOrders(knowledgeBase);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// Read from the file, get the build order index, load that build order
		// and run it
		try {
			Scanner in = new Scanner(knowledgeBase);

			// Get the index of the build order we want to load:
			String line = in.nextLine();
			Gson gson = new Gson();
			KnowledgeBaseObject kb = gson.fromJson(line, KnowledgeBaseObject.class);
			
			if (kb.readyToMutate) {
				// Store what we did for this first iteration
//				File knowledgeBaseIteration = new File(knowledgeBasePath + "_iteration" + buildOrderIteration++);
//				BufferedWriter writer = new BufferedWriter(new FileWriter(
//						knowledgeBaseIteration, false));
//				writer.write(gson.toJson(kb));
//				writer.newLine();
//				Gson gsonPretty = new GsonBuilder().setPrettyPrinting().create();
//				writer.write(gsonPretty.toJson(kb));
//				writer.newLine();
//				writer.close();
//				
//				// Then naturally select the new orders
//				forceNaturalSelection(kb);
			}
			else {
				loadBuildOrderFromFile(kb);
			}
			
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public int findNext12(int n) {
		if (n % 12 == 0) {
			return n;
		}
		
		return (12 - (n % 12)) + n; 
	}
	
	public void forceNaturalSelection(KnowledgeBaseObject kb) {
		ArrayList<BuildOrder> ordersToSave = new ArrayList<BuildOrder>();
		ArrayList<BuildOrder> allOrders = new ArrayList<BuildOrder>(kb.orders);
		
		
		for (int i = 0; i < NUM_ORDERS_PRESERVED; i++) {
			// Find the highest scoring build order, preserve it, remove it from
			// the initial list
			int highest = -100000;
			BuildOrder temp = null;
			for (BuildOrder order : allOrders) {
				if (order.score > highest) {
					highest = order.score;
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
			Gson gson = new Gson();
			KnowledgeBaseObject newKB = gson.fromJson(line, KnowledgeBaseObject.class);
			System.out.println("Loading index: " + newKB.index);

			loadBuildOrderFromFile(newKB);
			newScanner.close();
		} catch (FileNotFoundException e) {
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
			BufferedWriter writer = new BufferedWriter(new FileWriter(
					knowledgeBase, false));
			KnowledgeBaseObject kb = new KnowledgeBaseObject();
			kb.index = 0;
			kb.readyToMutate = false;
			kb.orders = new ArrayList<BuildOrder>();

			// Keep the original 3 winners, but update their index
			for (BuildOrder order : orders) {
				order.score = -1;
				kb.orders.add(order);
			}

			System.out.println("Cross Breeding...");

			for (BuildOrder first : orders) {
				for (BuildOrder second : orders) {
					if (first == second) {
						continue;
					}
					System.out.println(first + " mating " + second);
					BuildOrder newOrder = new BuildOrder();
					newOrder.score = -1;
					newOrder.order = crossBreed(first.order, second.order);
					kb.orders.add(newOrder);
				}
			}

			BuildOrder one = orders.get(0);
			one.order = mutate(one.order);
			one.score = -1;
			kb.orders.add(one);
			BuildOrder two = orders.get(1);
			two.order = mutate(two.order);
			two.score = -1;
			kb.orders.add(two);
			BuildOrder three = orders.get(2);
			three.order = mutate(two.order);
			three.score = -1;
			kb.orders.add(three);

			// Flip 2 random moves in the original 3 to add some mutation to
			// the mix
			writer.write(gson.toJson(kb));

			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public List<String> mutate(List<String> orders) {
		Random rand = new Random();
		int first = rand.nextInt(INITIAL_BUILD_LENGTH);
		int second = rand.nextInt(INITIAL_BUILD_LENGTH);

		orders.set(first, UNIT_TYPES[rand.nextInt(UNIT_TYPES.length)]);
		orders.set(second, UNIT_TYPES[rand.nextInt(UNIT_TYPES.length)]);

		return orders;
	}

	public List<String> crossBreed(List<String> first, List<String> second) {
		Random rand = new Random();
		int index = rand.nextInt(INITIAL_BUILD_LENGTH);

		List<String> newList = new ArrayList<String>(first.subList(0, index));
		// List<String> newList = first.subList(0, index);
		newList.addAll(second.subList(index, INITIAL_BUILD_LENGTH));

		return newList;
	}

	// Generate 12 random build orders and write them to the file
	public void generateInitialBuildOrders(File knowledgeBase)
			throws IOException {

		// Always start index at 0 to load the first randomly generated build
		// order
		BufferedWriter writer = new BufferedWriter(
				new FileWriter(knowledgeBase));
		
		KnowledgeBaseObject kb = new KnowledgeBaseObject();
		kb.index = 0;
		kb.orders = new ArrayList<BuildOrder>();
		kb.readyToMutate = false;
		
		Random random = new Random();
		for (int i = 0; i < NUM_BUILD_ORDERS; i++) {
			BuildOrder buildOrder = new BuildOrder();
			buildOrder.score = -1;
			buildOrder.order = new ArrayList<String>();

			for (int j = 0; j < INITIAL_BUILD_LENGTH; j++) {
				int randIndex = random.nextInt(UNIT_TYPES.length);
				buildOrder.order.add(UNIT_TYPES[randIndex]);
			}
			kb.orders.add(buildOrder);
		}
		
		Gson gson = new Gson();
		String buildOrderJson = gson.toJson(kb);
		writer.write(buildOrderJson);
		writer.newLine();

		writer.close();
	}

	// Loads a build order given a scanner to the file and the index of the
	// build order
	public void loadBuildOrderFromFile(KnowledgeBaseObject kb) {
		currentBuildOrder = kb.orders.get(kb.index);
		
		// Load the build order
		System.out.println("Build Order from file: " + kb.index);
		for (String unit : currentBuildOrder.order) {
			UnitType unitType = UNIT_MAP.get(unit);

			if (unitType != null) {
				buildManager.submitBuildRequest(new BuildRequest(unitType)
						.withBuildLocation(self.getStartLocation()));
			} else {
				System.out
						.println("Something went terribly wrong in the build order: "
								+ unitType);
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
