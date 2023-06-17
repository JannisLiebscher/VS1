package aqua.blatt1.client;

import aqua.blatt1.common.msgtypes.KeyExchangeRequest;
import aqua.blatt1.common.msgtypes.KeyExchangeResponse;
import messaging.Endpoint;
import messaging.Message;
import org.apache.commons.lang3.SerializationUtils;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class SecureEndpoint extends Endpoint {
    private Endpoint endpoint;
    KeyPairGenerator keyPairGen;
    KeyPair keyPair;
    Cipher encryptCipher;
    Cipher decryptCipher;

    private Map<InetSocketAddress, Key> publicKeys = new HashMap<>();
    private Map<InetSocketAddress, Serializable> queue = new HashMap<>();

    public SecureEndpoint() {
        endpoint  = new Endpoint();
        try {
            keyPairGen = KeyPairGenerator.getInstance("RSA");
            keyPairGen.initialize(2048);
            // Schlüsselpaar generieren
            keyPair = keyPairGen.generateKeyPair();

            // Cipher-Objekte initialisieren
            encryptCipher = Cipher.getInstance("RSA");
            decryptCipher = Cipher.getInstance("RSA");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException ex) {
            throw new RuntimeException(ex);
        }
    }

    public  SecureEndpoint(int port) {
        endpoint  = new Endpoint(port);
        try {
            keyPairGen = KeyPairGenerator.getInstance("RSA");
            keyPairGen.initialize(2048);
            // Schlüsselpaar generieren
            keyPair = keyPairGen.generateKeyPair();

            // Cipher-Objekte initialisieren
            encryptCipher = Cipher.getInstance("RSA");
            decryptCipher = Cipher.getInstance("RSA");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException ex) {
            throw new RuntimeException(ex);
        }
    }
    @Override
    public void send(InetSocketAddress receiver, Serializable payload) {
        if(!publicKeys.containsKey(receiver)) {
            endpoint.send(receiver,  new KeyExchangeRequest());
            queue.put(receiver, payload);
            return;
        }
        try {
            // Serializing the payload to bytes
            byte[] payloadBytes = SerializationUtils.serialize(payload);

            // Initializing the encryptCipher in encryption mode with the secret key
            encryptCipher.init(Cipher.ENCRYPT_MODE, publicKeys.get(receiver));

            // Encrypting the payload bytes
            byte[] encryptedBytes = encryptCipher.doFinal(payloadBytes);

            // Sending the encrypted payload using the underlying endpoint
            endpoint.send(receiver, encryptedBytes);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to send encrypted payload.", ex);
        }

    }
    private Message decrypt(Message decrypt) {
        if(decrypt.getPayload()  instanceof KeyExchangeRequest) {
            endpoint.send(decrypt.getSender(),  new KeyExchangeResponse(keyPair.getPublic()));
            return decrypt;
        }
        if(decrypt.getPayload()  instanceof KeyExchangeResponse) {
            publicKeys.put(decrypt.getSender(), ((KeyExchangeResponse) decrypt.getPayload()).getKey());
            this.send(decrypt.getSender(), queue.get(decrypt.getSender()));
            queue.remove(decrypt.getSender());
            return decrypt;
        }
        try {
            // Entschlüsselung der Nachricht
            byte[] encryptedBytes = (byte[]) decrypt.getPayload();

            // Initialisieren des decryptCipher im Entschlüsselungsmodus mit dem geheimen Schlüssel
            decryptCipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());

            // Entschlüsseln der verschlüsselten Bytes
            byte[] decryptedBytes = decryptCipher.doFinal(encryptedBytes);

            return new Message(SerializationUtils.deserialize(decryptedBytes), decrypt.getSender());
        } catch (Exception ex) {
            throw new RuntimeException("Failed to decrypt received message.", ex);
        }
    }
    @Override
    public Message nonBlockingReceive() {
        // Empfangen der verschlüsselten Nachricht vom zugrunde liegenden Endpoint
        Message encryptedMessage = endpoint.nonBlockingReceive();
        return decrypt(encryptedMessage);
    }
    @Override
    public Message blockingReceive() {
        // Empfangen der verschlüsselten Nachricht vom zugrunde liegenden Endpoint
        Message encryptedMessage = endpoint.blockingReceive();
        return decrypt(encryptedMessage);
    }
}
