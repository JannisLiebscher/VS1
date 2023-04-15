import aqua.blatt1.broker.ClientCollection;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.Properties;
import aqua.blatt1.common.msgtypes.*;
import aqua.blatt2.broker.PoisonPill;
import messaging.Endpoint;
import messaging.Message;
//test

import javax.swing.*;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Broker {
    private static final int NUM_THREADS = 5;
    private Endpoint endpoint = new Endpoint(Properties.PORT);
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
        System.out.println("Broker exit, killing threads");
        executor.shutdown();
        stop.interrupt();
    }
    private void stop(){
        JOptionPane.showMessageDialog(null, "Press OK to stop Server", "Aufgabe 2 Stop Request", 2);
        terminate();
    }
    private void terminate(){
        done = true;
        endpoint.send(new InetSocketAddress("localhost", Properties.PORT), new StopRequest());
    }

    public void register(int port) {
        lock.writeLock().lock();
        if(clients.size() == 0) endpoint.send(new InetSocketAddress("localhost", port), new TokenRequest());
        clients.add("tank" + counter, port);
        // Neu registrierter Client
        endpoint.send(new InetSocketAddress("localhost", port), new RegisterResponse("tank" + counter));
        endpoint.send(new InetSocketAddress("localhost", port), new UpdateNeighbor(
                new InetSocketAddress("localhost", (int) clients.getLeftNeighorOf(clients.size() - 1)),
                new InetSocketAddress("localhost", (int) clients.getRightNeighorOf(clients.size() - 1)))
        );
        // Linker Nachbar von registrierter Client
        endpoint.send(new InetSocketAddress("localhost",
                (int) clients.getLeftNeighorOf(clients.size() - 1)),
                new UpdateNeighbor(null,
                new InetSocketAddress("localhost", port))
        );
        // Rechter Nachbar von neu registrierter Client
        endpoint.send(new InetSocketAddress("localhost",
                        (int) clients.getRightNeighorOf(clients.size() - 1)),
                new UpdateNeighbor(new InetSocketAddress("localhost", port),null)
        );
        System.out.println("Registered tank" + counter);
        counter++;
        lock.writeLock().unlock();
    }

    public void deregister(int port) {
        lock.writeLock().lock();
        int left = (int) clients.getLeftNeighorOf(clients.indexOf(port));
        int right = (int) clients.getRightNeighorOf(clients.indexOf(port));
        endpoint.send(new InetSocketAddress("localhost", left),
                new UpdateNeighbor(null, new InetSocketAddress(right)));
        endpoint.send(new InetSocketAddress("localhost", right),
                new UpdateNeighbor(new InetSocketAddress(left), null));
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
    }
    public class BrokerTask  implements Runnable {
        private Message message;
        BrokerTask(Message message){
            this.message = message;
        }

        @Override
        public void run() {
            int port = message.getSender().getPort();
            if(message.getPayload() instanceof RegisterRequest) {
                System.out.println("Received register Request");
                register(port);
            }
            else if(message.getPayload() instanceof DeregisterRequest) {
                System.out.println("Received deregister Request");
                deregister(port);
            }
            else if(message.getPayload() instanceof HandoffRequest) {
                System.out.println("Received handoffFish Request");
                handoffFish(port, ((HandoffRequest) message.getPayload()).getFish());
            }
            else if(message.getPayload() instanceof PoisonPill) {
                System.out.println("Received Poison Pill");
                terminate();
            }
            else if(message.getPayload() instanceof StopRequest)
                System.out.println("Received stop request, shutdown");
            else {
                System.out.println("Unsupported Request");
            }
        }
    }
}