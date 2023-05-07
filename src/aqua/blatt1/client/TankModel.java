package aqua.blatt1.client;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Iterator;
import java.util.Observable;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishModel;

public class TankModel extends Observable implements Iterable<FishModel> {

	public static final int WIDTH = 600;
	public static final int HEIGHT = 350;
	protected static final int MAX_FISHIES = 5;
	protected static final Random rand = new Random();
	protected volatile String id;
	protected final Set<FishModel> fishies;
	protected int fishCounter = 0;
	protected final ClientCommunicator.ClientForwarder forwarder;
	public InetSocketAddress right  = null;
	public InetSocketAddress left = null;
	private boolean token = true;
	private Record record = Record.IDLE;
	private int snapshot = -1;
	private boolean initiator = false;
	private boolean snapshotToken = false;

	public synchronized void initiateSnapshot() {
		snapshot = this.getFishCounter();
		record = Record.BOTH;
		forwarder.sendMarker(left);
		forwarder.sendMarker(right);
		initiator = true;
		snapshotToken = true;
	}
	public synchronized void collectSnapshot(int snapshot) {
		if(initiator) System.out.println("Initiator:" + snapshot + "Fishies in all Tanks");
		else if(snapshot != -1 && record == Record.IDLE) forwarder.collectToken(snapshot + this.snapshot, left);
		else {
			snapshotToken = true;
			this.snapshot += snapshot;
		}
	}
	public boolean getToken() { return this.token;}
	public void takeToken() {
		try {
			TimeUnit.SECONDS.sleep(2);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		this.token = false;
		forwarder.handoffToken(left);
	}
	public void giveToken() {
		this.token = true;
		Thread take = new Thread(this::takeToken);
		take.start();
	}

	public TankModel(ClientCommunicator.ClientForwarder forwarder) {
		this.fishies = Collections.newSetFromMap(new ConcurrentHashMap<FishModel, Boolean>());
		this.forwarder = forwarder;
	}

	public void lease(int lease) {
		try {
			TimeUnit.SECONDS.sleep(lease);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		forwarder.deregister(this.id);
	}

	synchronized void onRegistration(String id, int lease) {
		this.id = id;
		newFish(WIDTH - FishModel.getXSize(), rand.nextInt(HEIGHT - FishModel.getYSize()));
		Thread deregister = new Thread(() -> lease(lease));
		deregister.start();
	}

	synchronized void newNeighbor(InetSocketAddress rightInput, InetSocketAddress leftInput){
		if(rightInput != null){
			right = rightInput;
			System.out.println("new right Neighbor: " + right.getPort());
		}
		if(leftInput != null) {
			left = leftInput;
			System.out.println("new left Neighbor: " + left.getPort());
		}
	}

	public synchronized void newFish(int x, int y) {
		if (fishies.size() < MAX_FISHIES) {
			x = x > WIDTH - FishModel.getXSize() - 1 ? WIDTH - FishModel.getXSize() - 1 : x;
			y = y > HEIGHT - FishModel.getYSize() ? HEIGHT - FishModel.getYSize() : y;

			FishModel fish = new FishModel("fish" + (++fishCounter) + "@" + getId(), x, y,
					rand.nextBoolean() ? Direction.LEFT : Direction.RIGHT);

			fishies.add(fish);
		}
	}
	synchronized void receiveFish(FishModel fish) {
		fish.setToStart();
		fishies.add(fish);
		if(record == Record.BOTH) {
			snapshot++;
		}
		//Fisch kommt von rechts (schwimmt nach links)
		else if(fish.getDirection().getVector() < 0 && record == Record.RIGHT) {
			snapshot++;
		}
		//Fisch kommt von links (schwimmt nach rechts)
		else if(fish.getDirection().getVector() > 0 && record == Record.LEFT) {
			snapshot++;
		}


	}

	public String getId() {
		return id;
	}

	public synchronized int getFishCounter() {
		return fishCounter;
	}

	public synchronized Iterator<FishModel> iterator() {
		return fishies.iterator();
	}

	private synchronized void updateFishies() {
		for (Iterator<FishModel> it = iterator(); it.hasNext();) {
			FishModel fish = it.next();

			fish.update();
			if (this.token && fish.hitsEdge() && fish.getDirection().getVector() < 0)
				if(left != null) forwarder.handOff(fish,left);
				else fish.reverse();
			if(this.token && fish.hitsEdge() && fish.getDirection().getVector() > 0)
				if(right != null) forwarder.handOff(fish,right);
				else fish.reverse();
			if(!this.token && fish.hitsEdge()) fish.reverse();
			if (fish.disappears())
				it.remove();
		}
	}

	private synchronized void update() {
		updateFishies();
		setChanged();
		notifyObservers();
	}

	protected void run() {
		forwarder.register();

		try {
			while (!Thread.currentThread().isInterrupted()) {
				update();
				TimeUnit.MILLISECONDS.sleep(10);
			}
		} catch (InterruptedException consumed) {
			// allow method to terminate
		}
	}

	public synchronized void finish() {
		forwarder.deregister(id);
	}
	private enum Record {IDLE, LEFT, RIGHT,	BOTH}
	public synchronized void setEnum(int n) {
		switch (record) {
			case BOTH -> {
				if(n < 0) record = Record.RIGHT;
				else if(n > 0) record = Record.LEFT;
			}
			case LEFT -> {
				if(n < 0) {
					record = Record.IDLE;
					System.out.println("Tank:" + snapshot + "Fishies");
					if(snapshotToken) forwarder.collectToken(this.snapshot, left);;}
				else if(n > 0) forwarder.sendMarker(left);
			}
			case RIGHT -> {
				if(n > 0) {
					record = Record.IDLE;
					System.out.println("Tank:" + snapshot + "Fishies");
					if(snapshotToken) forwarder.collectToken(this.snapshot, left);;}
				else if(n < 0) forwarder.sendMarker(right);
			}
			default -> {
				record = n < 0 ? Record.RIGHT : Record.LEFT;
				snapshot = this.getFishCounter();
				forwarder.sendMarker(left);
				forwarder.sendMarker(right);
			}
		}
	}
}