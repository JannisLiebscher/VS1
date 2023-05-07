package aqua.blatt1.broker;

import java.util.*;

/*
 * This class is not thread-safe and hence must be used in a thread-safe way, e.g. thread confined or 
 * externally synchronized. 
 */

public class ClientCollection<T> implements Iterable {


	public class Client {
		final String id;
		final T client;
		public Date timestamp;

		Client(String id, T client,Date timestamp) {
			this.id = id;
			this.client = client;
			this.timestamp = timestamp;
		}
	}

	private final List<Client> clients;

	public ClientCollection() {
		clients = new ArrayList<Client>();
	}

	public ClientCollection<T> add(String id, T client, Date timestamp) {
		clients.add(new Client(id, client,timestamp));
		return this;
	}

	public ClientCollection<T> remove(int index) {
		clients.remove(index);
		return this;
	}

	public int indexOf(String id) {
		for (int i = 0; i < clients.size(); i++)
			if (clients.get(i).id.equals(id))
				return i;
		return -1;
	}

	public int indexOf(T client) {
		for (int i = 0; i < clients.size(); i++)
			if (clients.get(i).client.equals(client))
				return i;
		return -1;
	}

	public T getClient(int index) {
		return clients.get(index).client;
	}
	public Date getTimestamp(int index) {
		return clients.get(index).timestamp;
	}
	public void setTimestamp(int index, Date timestamp) {
		clients.get(index).timestamp = timestamp;
	}

	public int size() {
		return clients.size();
	}

	public T getLeftNeighorOf(int index) {
		return index == 0 ? clients.get(clients.size() - 1).client : clients.get(index - 1).client;
	}

	public T getRightNeighorOf(int index) {
		return index < clients.size() - 1 ? clients.get(index + 1).client : clients.get(0).client;
	}

	public Iterator<Client> iterator() {
		return new ClientIterator(clients);
	}
	public class ClientIterator implements Iterator<Client> {

		List<Client> clients;
		private int index;

		public ClientIterator(List<Client> clients) {
			this.clients = clients;
			index = 0;
		}

		@Override
		public boolean hasNext() {
			return index < clients.size();
		}

		@Override
		public Client next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}
			Client client = clients.get(index);
			index++;
			return client;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

}
