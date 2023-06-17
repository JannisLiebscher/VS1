package aqua.blatt1.client;

import messaging.Endpoint;
import messaging.Message;
import org.apache.commons.lang3.SerializationUtils;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;

public class SecureEndpointSymetric extends Endpoint {
    private Endpoint endpoint;
    private SecretKeySpec key = new SecretKeySpec("CAFEBABECAFEBABE".getBytes(StandardCharsets.UTF_8),"AES");
    Cipher encryptCipher;
    Cipher decryptCipher;


    public SecureEndpointSymetric() {
        endpoint  = new Endpoint();
        try {
            encryptCipher = Cipher.getInstance("AES");
            decryptCipher = Cipher.getInstance("AES");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException ex) {
            throw new RuntimeException(ex);
        }
    }
    public SecureEndpointSymetric(int port) {
        endpoint  = new Endpoint(port);
        try {
            encryptCipher = Cipher.getInstance("AES");
            decryptCipher = Cipher.getInstance("AES");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException ex) {
            throw new RuntimeException(ex);
        }
    }
    @Override
    public void send(InetSocketAddress receiver, Serializable payload) {
        try {
            // Serializing the payload to bytes
            byte[] payloadBytes = SerializationUtils.serialize(payload);

            // Initializing the encryptCipher in encryption mode with the secret key
            encryptCipher.init(Cipher.ENCRYPT_MODE, key);

            // Encrypting the payload bytes
            byte[] encryptedBytes = encryptCipher.doFinal(payloadBytes);

            // Sending the encrypted payload using the underlying endpoint
            endpoint.send(receiver, encryptedBytes);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to send encrypted payload.", ex);
        }

    }
    private Message decrypt(Message decrypt) {
        try {
            // Entschlüsselung der Nachricht
            byte[] encryptedBytes = (byte[]) decrypt.getPayload();

            // Initialisieren des decryptCipher im Entschlüsselungsmodus mit dem geheimen Schlüssel
            decryptCipher.init(Cipher.DECRYPT_MODE, key);

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
