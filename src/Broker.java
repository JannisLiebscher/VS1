import aqua.blatt1.broker.ClientCollection;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.msgtypes.*;
import messaging.*;

import javax.swing.*;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Broker {
    private static final int NUM_THREADS = 5;
    private Endpoint endpoint = new Endpoint(4711);
    private ClientCollection clients = new ClientCollection();
    private int counter = 0;
    private ReadWriteLock lock = new ReentrantReadWriteLock();
    boolean done = false;
    public void broker() {
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        Thread stop = new Thread(() -> stop());
        stop.start();
        while(!done) {
            Message message = endpoint.blockingReceive();
            executor.execute(new BrokerTask(message));
        }
        System.out.println("while ende");
    }
    private void stop(){
        JOptionPane.showMessageDialog(null, "Press OK button to stop Server");
        done = true;
        System.out.println("stop ende");
    }

    public void register(int port) {
        lock.writeLock().lock();
        clients.add("tank" + counter, port);
        lock.writeLock().unlock();
        endpoint.send(new InetSocketAddress("localhost", port), new RegisterResponse("tank" + counter));
        System.out.println("Registered tank" + counter);
        counter++;
    }
    public void deregister(int port) {
        lock.writeLock().lock();
        clients.remove(clients.indexOf(port));
        lock.writeLock().unlock();
    }

    public void handoffFish(int port, FishModel fish) {
        lock.readLock().lock();
        int index = clients.indexOf(port);
        if (fish.getDirection().getVector() < 0) {
            int neighbour = index == 0 ? clients.size() - 1 : index - 1;
            int sendTo = (int) clients.getClient(neighbour);
            lock.readLock().unlock();
            endpoint.send(new InetSocketAddress("localhost", sendTo), new HandoffRequest(fish));
        } else {
            int neighbour = index == clients.size() - 1 ? 0 : index + 1;
            int sendTo = (int) clients.getClient(neighbour);
            lock.readLock().unlock();
            endpoint.send(new InetSocketAddress("localhost", sendTo), new HandoffRequest(fish));
        }
    }

    public static void main( String[] args) {
        Broker broker = new Broker();
        broker.broker();
        System.out.println("main ende");
    }
    public class BrokerTask  implements Runnable {
        private Message message;
        BrokerTask(Message message){
            this.message = message;
        }

        @Override
        public void run() {
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

}
