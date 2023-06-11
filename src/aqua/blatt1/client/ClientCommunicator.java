package aqua.blatt1.client;

import java.net.InetSocketAddress;

import aqua.blatt1.common.msgtypes.*;
import messaging.Endpoint;
import messaging.Message;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.Properties;

public class ClientCommunicator {
	private final Endpoint endpoint;

	public ClientCommunicator() {
		endpoint = new Endpoint();
	}

	public class ClientForwarder {
		private final InetSocketAddress broker;

		private ClientForwarder() {
			this.broker = new InetSocketAddress(Properties.HOST, Properties.PORT);
		}

		public void register() {
			endpoint.send(broker, new RegisterRequest());
		}

		public void deregister(String id) {
			endpoint.send(broker, new DeregisterRequest(id));
		}

		public void handOff(FishModel fish,InetSocketAddress adress) {
			endpoint.send(adress, new HandoffRequest(fish));
		}
		public void handoffToken(InetSocketAddress adress) {
			endpoint.send(adress, new TokenRequest());
		}
		public void sendMarker(InetSocketAddress adress) {
			endpoint.send(adress, new SnapshotMarker());
		}
		public void collectToken(int snapshot, InetSocketAddress adress) {
			endpoint.send(adress, new SnapshotToken(snapshot));
		}
		public void sendLocation(InetSocketAddress adress,String fishId) {
			endpoint.send(adress,new LocationRequest(fishId));
		}
		public void sendNameResolutionRequest(String tankId, String fishId) {
			endpoint.send(broker,new NameResolutionRequest(tankId,fishId));
		}
	}

	public class ClientReceiver extends Thread {
		private final TankModel tankModel;

		private ClientReceiver(TankModel tankModel) {
			this.tankModel = tankModel;
		}

		@Override
		public void run() {
			while (!isInterrupted()) {
				Message msg = endpoint.blockingReceive();

				if (msg.getPayload() instanceof RegisterResponse)
					tankModel.onRegistration(((RegisterResponse) msg.getPayload()).getId(), ((RegisterResponse) msg.getPayload()).getLease());

				if (msg.getPayload() instanceof HandoffRequest)
					tankModel.receiveFish(((HandoffRequest) msg.getPayload()).getFish());

				if (msg.getPayload() instanceof UpdateNeighbor)
					tankModel.newNeighbor(((UpdateNeighbor) msg.getPayload()).getRi(),((UpdateNeighbor) msg.getPayload()).getLeft());

				if (msg.getPayload() instanceof TokenRequest)
					tankModel.giveToken();
				if (msg.getPayload() instanceof SnapshotRequest)
					tankModel.initiateSnapshot();
				if(msg.getPayload() instanceof DeregisterMessage)
					tankModel.leaseFinish();
				if (msg.getPayload() instanceof SnapshotToken)
					tankModel.collectSnapshot(((SnapshotToken) msg.getPayload()).getSnapshot());
				if (msg.getPayload() instanceof SnapshotMarker) {
					if (msg.getSender().equals(tankModel.left)) tankModel.setEnum(-1);
					else if (msg.getSender().equals(tankModel.right)) tankModel.setEnum(1);
					else System.out.println("Sender was not a neighbour");
				}
				if (msg.getPayload() instanceof NameResolutionResponse) {
					InetSocketAddress adress = new InetSocketAddress("localhost",((NameResolutionResponse) msg.getPayload()).getAquaPort());
					String fishId = ((NameResolutionResponse) msg.getPayload()).getRequestId();
					endpoint.send(adress,new LocationUpdate(fishId));
				}
				if (msg.getPayload() instanceof LocationUpdate) {
					String fishId = ((LocationUpdate) msg.getPayload()).getRequestId();
					int port = msg.getSender().getPort();
					tankModel.updateHomeAgent(fishId,port);
				}
				if (msg.getPayload() instanceof LocationRequest) {
					tankModel.locateFishGlobally(((LocationRequest) msg.getPayload()).getId());
				}
			}
			System.out.println("Receiver stopped.");
		}
	}

	public ClientForwarder newClientForwarder() {
		return new ClientForwarder();
	}

	public ClientReceiver newClientReceiver(TankModel tankModel) {
		return new ClientReceiver(tankModel);
	}

}
