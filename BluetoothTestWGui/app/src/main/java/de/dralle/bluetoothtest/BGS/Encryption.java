package de.dralle.bluetoothtest.BGS;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import de.dralle.bluetoothtest.DB.SPMADatabaseAccessHelper;
import de.dralle.bluetoothtest.DB.User;


/**
 * Created by Niklas on 21.05.16.
 * Modified by Nils on 19.06.16
 */
public class Encryption {

    /**
     * Log tag. Used to identify this´ class log messages in log output
     */
    private static final String LOG_TAG = Encryption.class.getName();
    /**
     * Context to be ujsed
     */
    private Context context;

    public void setContext(Context context) {
        this.context = context;
    }

    public String decryptWithRSA(String text, String rsaPrivateKey) {
        // BASE64 String zu Byte-Array
        byte[] crypted = Base64.decode(text, Base64.DEFAULT);

        byte[] keyBytes = Base64.decode(rsaPrivateKey.getBytes(), Base64.DEFAULT);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory fact = null;
        try {
            fact = KeyFactory.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        PrivateKey priv = null;
        try {
            priv = fact.generatePrivate(keySpec);
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }

        // entschluesseln
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        }
        try {
            cipher.init(Cipher.DECRYPT_MODE, priv);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }


        byte[] decoded = new byte[0];
        try {
            decoded = cipher.doFinal(crypted);
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }

        return new String(decoded);
    }

    public String encryptWithRSA(String text, String rsaPublicKey) {
        //Cipher erstellen und verschluesseln
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        }

        byte[] keyBytes = Base64.decode(rsaPublicKey.getBytes(), Base64.DEFAULT);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = null;
        try {
            keyFactory = KeyFactory.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        PublicKey key = null;
        try {
            key = keyFactory.generatePublic(spec);
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }


        try {
            cipher.init(Cipher.ENCRYPT_MODE, key);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }

        byte[] encrypted = new byte[0];
        try {
            encrypted = cipher.doFinal(text.getBytes());
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }

        //byte array zu Base64 konvertieren
        String encoded = Base64.encodeToString(encrypted, Base64.DEFAULT);

        return encoded;
    }

    public String encryptWithAES(String text, String aesKey) {
        //Cipher erstellen und verschluesseln
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance("AES");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        }

        byte[] keyBytes = Base64.decode(aesKey.getBytes(), Base64.DEFAULT);
        SecretKey originalAESKey = new SecretKeySpec(keyBytes, "AES");


        try {
            cipher.init(Cipher.ENCRYPT_MODE, originalAESKey);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }

        byte[] encrypted = new byte[0];
        try {
            encrypted = cipher.doFinal(text.getBytes());
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }

        //byte array zu Base64 konvertieren
        String encoded = Base64.encodeToString(encrypted, Base64.DEFAULT);

        return encoded;
    }

    public String decryptWithAES(String text, String aesKey) {
        // BASE64 String zu Byte-Array
        byte[] crypted = Base64.decode(text, Base64.DEFAULT);

        byte[] keyBytes = Base64.decode(aesKey.getBytes(), Base64.DEFAULT);
        SecretKey originalAESKey = new SecretKeySpec(keyBytes, "AES");

        // entschluesseln
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance("AES");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        }
        try {
            cipher.init(Cipher.DECRYPT_MODE, originalAESKey);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }


        byte[] decoded = new byte[0];
        try {
            decoded = cipher.doFinal(crypted);
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }

        return new String(decoded);
    }


    public SecretKey newAESkey(int bitLength) {
        //AES Algorithmus
        KeyGenerator keygen = null;
        try {
            keygen = KeyGenerator.getInstance("AES");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        if (keygen != null) {
            //Länge 256-bit
            keygen.init(bitLength);
            //Schlüssel generieren
            SecretKey aesKey = keygen.generateKey();

            return aesKey;
        }
        return null;

    }

    public KeyPair newRSAkeys(int bitLength) {
        KeyPairGenerator keygen = null;
        try {
            keygen = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        if (keygen != null) {
            keygen.initialize(bitLength);
            KeyPair keys = keygen.genKeyPair();
            return keys;
        }
        return null;
    }

    /**
     * Generate (new) crypto keys
     */
    public void generateKeys(int userId, boolean override) {
        Log.i(LOG_TAG, "Now trying to generate new crypto keys for user " + userId);
        SPMADatabaseAccessHelper db = SPMADatabaseAccessHelper.getInstance(context);
        User u = db.getUser(userId);
        if (u != null) {
            if (u.getAes() == null || u.getRsaPublic() == null || u.getRsaPrivate() == null || override) {
                SecretKey aes = newAESkey(256);
                KeyPair rsa = newRSAkeys(512);
                u.setId(userId);
                u.setAes(Base64.encodeToString(aes.getEncoded(), Base64.DEFAULT));
                u.setRsaPrivate(Base64.encodeToString(rsa.getPrivate().getEncoded(), Base64.DEFAULT));
                u.setRsaPublic(Base64.encodeToString(rsa.getPublic().getEncoded(), Base64.DEFAULT));
                Log.v(LOG_TAG, "RSA Public key is " + u.getRsaPublic());
                db.createOrUpdateUser(u);
                Log.i(LOG_TAG, "New crypto keys generated");
            } else {
                Log.i(LOG_TAG, "generateKeys: Keys exist");
            }
        } else {
            Log.i(LOG_TAG, "generateKeys: User null");
        }


    }

}
