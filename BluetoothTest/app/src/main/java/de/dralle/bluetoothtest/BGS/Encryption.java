package de.dralle.bluetoothtest.BGS;

import android.util.Base64;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;


/**
 * Created by Niklas on 21.05.16.
 */
public class Encryption {
    private Key key = null;
    private String algorithm = null;

    public Encryption (Key k, String algorithm) {
        this.key = k;
        this.algorithm = algorithm;
    }

    public Encryption (String algorithm) {
        this.algorithm = algorithm;
    }

    public Key getKey() {
        return key;
    }

    public void setKey(Key key) {
        this.key = key;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public String encrypt(String text) throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException, IllegalBlockSizeException {
        //Cipher erstellen und verschluesseln
        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.ENCRYPT_MODE, key);

        byte[] encrypted = cipher.doFinal(text.getBytes());

        //byte array zu Base64 konvertieren
        String encoded = Base64.encodeToString(encrypted, Base64.DEFAULT);

        return encoded;
    }

    public String decrypt(String encrypted) throws BadPaddingException, IllegalBlockSizeException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException {
        // BASE64 String zu Byte-Array
        byte[] crypted = Base64.decode(encrypted, Base64.DEFAULT);

        // entschluesseln
        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decoded = cipher.doFinal(crypted);

        return new String(decoded);
    }

    public static SecretKey newAESkey(int bitLength) throws NoSuchAlgorithmException {
        //AES Algorithmus
        KeyGenerator keygen = KeyGenerator.getInstance("AES");
        //Länge 256-bit
        keygen.init(bitLength);
        //Schlüssel generieren
        SecretKey aesKey = keygen.generateKey();

        return aesKey;
    }

    public static KeyPair newRSAkeys(int bitLength) throws NoSuchAlgorithmException {
        KeyPairGenerator keygen = KeyPairGenerator.getInstance("RSA");
        keygen.initialize(bitLength);
        KeyPair keys = keygen.genKeyPair();
        return keys;
    }

}
