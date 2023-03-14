import aqua.blatt1.broker.ClientCollection;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.msgtypes.DeregisterRequest;
import aqua.blatt1.common.msgtypes.HandoffRequest;
import aqua.blatt1.common.msgtypes.RegisterRequest;
import aqua.blatt1.common.msgtypes.RegisterResponse;
import messaging.*;

import java.net.InetSocketAddress;

public class Broker {
    private Endpoint endpoint = new Endpoint(4711);
    private ClientCollection clients = new ClientCollection();
    private int counter = 0;

    public void broker() {
        while(true) {
            Message message = endpoint.blockingReceive();
            int port = message.getSender().getPort();
            if(message.getPayload() instanceof RegisterRequest) register(port);
            else if(message.getPayload() instanceof DeregisterRequest) deregister(port);
            else if(message.getPayload() instanceof HandoffRequest) handoffFish(port,
                    ((HandoffRequest) message.getPayload()).getFish());
            else {
                System.out.println("Unsupported Request");
            }
        }
    }

    public void register(int port) {
        clients.add("tank" + counter, port);
        endpoint.send(new InetSocketAddress("localhost", port), new RegisterResponse("tank" + counter));
        System.out.println("Registered tank" + counter);
        counter++;
    }
    public void deregister(int port) {
        clients.remove(clients.indexOf(port));
    }

    public void handoffFish(int port, FishModel fish) {
        int index = clients.indexOf(port);
        if (fish.getDirection().getVector() < 0) {
            int neighbour = index == 0 ? clients.size() - 1 : index - 1;
            int sendTo = (int) clients.getClient(neighbour);
            endpoint.send(new InetSocketAddress("localhost", sendTo), new HandoffRequest(fish));
        } else {
            int neighbour = index == clients.size() - 1 ? 0 : index + 1;
            int sendTo = (int) clients.getClient(neighbour);
            endpoint.send(new InetSocketAddress("localhost", sendTo), new HandoffRequest(fish));
        }
    }

    public static void main( String[] args) {
        Broker broker = new Broker();
        broker.broker();
    }

}
