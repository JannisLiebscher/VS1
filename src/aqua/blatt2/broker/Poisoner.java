package aqua.blatt2.broker;

import java.net.InetSocketAddress;

import javax.swing.JOptionPane;

import messaging.Endpoint;
import aqua.blatt1.common.Properties;

public class Poisoner {
	private final Endpoint endpoint;
	private final InetSocketAddress broker;

	public Poisoner() {
		this.endpoint = new Endpoint();
		this.broker = new InetSocketAddress(Properties.HOST, Properties.PORT);
	}

	public void sendPoison() {
		endpoint.send(broker, new PoisonPill());
	}

	public static void main(String[] args) {
		JOptionPane.showMessageDialog(null, "Press OK to send Poison Pill to Server",
				"Poisoner", 2);
		new Poisoner().sendPoison();
	}
}
