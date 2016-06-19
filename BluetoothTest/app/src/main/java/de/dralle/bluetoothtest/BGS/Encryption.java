package de.dralle.bluetoothtest.BGS;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Key;
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


/**
 * Created by Niklas on 21.05.16.
 * Modified by Nils on 19.06.16
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

    public static String decryptWithRSA(String text, String rsaPrivateKey){
        // BASE64 String zu Byte-Array
        byte[] crypted = Base64.decode(text, Base64.DEFAULT);

        byte[] keyBytes = Base64.decode(rsaPrivateKey.getBytes(),Base64.DEFAULT);
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

    public static String encryptWithRSA(String text, String rsaPublicKey){
        //Cipher erstellen und verschluesseln
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        }

        byte[] keyBytes = Base64.decode(rsaPublicKey.getBytes(),Base64.DEFAULT);
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

    public static String encryptWithAES(String text, String aesKey){
        //Cipher erstellen und verschluesseln
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance("AES");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        }

        byte[] keyBytes = Base64.decode(aesKey.getBytes(),Base64.DEFAULT);
       SecretKey originalAESKey=new SecretKeySpec(keyBytes,"AES");




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

    public static String decryptWithAES(String text, String aesKey){
        // BASE64 String zu Byte-Array
        byte[] crypted = Base64.decode(text, Base64.DEFAULT);

        byte[] keyBytes = Base64.decode(aesKey.getBytes(),Base64.DEFAULT);
        SecretKey originalAESKey=new SecretKeySpec(keyBytes,"AES");

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

    public static SecretKey newAESkey(int bitLength)  {
        //AES Algorithmus
        KeyGenerator keygen = null;
        try {
            keygen = KeyGenerator.getInstance("AES");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        if(keygen!=null){
            //Länge 256-bit
            keygen.init(bitLength);
            //Schlüssel generieren
            SecretKey aesKey = keygen.generateKey();

            return aesKey;
        }
        return null;

    }

    public static KeyPair newRSAkeys(int bitLength){
        KeyPairGenerator keygen = null;
        try {
            keygen = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        if(keygen!=null) {
            keygen.initialize(bitLength);
            KeyPair keys = keygen.genKeyPair();
            return keys;
        }
        return null;
    }

    public void saveAES(Key key, String filename, Context ctx) throws IOException {
        //speichervorgang
        byte[] bytes = key.getEncoded();
        FileOutputStream fo = ctx.openFileOutput(filename + ".key", Context.MODE_PRIVATE);
        fo.write(bytes);
        fo.close();
    }

    public void readAES(String filename, Context ctx){

        try {
            FileInputStream fi = ctx.openFileInput(filename + ".key");
            byte[] encodedKey = new byte[(int) fi.getChannel().size()];
            fi.read(encodedKey);
            fi.close();

            Log.v("KEY aus READ", encodedKey.toString());
            //set the key
            SecretKey key = new SecretKeySpec(encodedKey, "AES");
            setKey(key);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //Maybe Refactoring needed
    //x509 encoded
    public void saveRSAPublicKey(Key publicKey, String filename, Context ctx) throws IOException {
        X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(publicKey.getEncoded());
        FileOutputStream fos = ctx.openFileOutput(filename + "_public.key",Context.MODE_PRIVATE );
        fos.write(x509EncodedKeySpec.getEncoded());
        fos.close();
    }
    //PKCS8 Encoded -> Public Key Cryptography Standards
    public void saveRSAPrivateKey(Key privateKey, String filename, Context ctx) throws IOException {
        PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(privateKey.getEncoded());
        FileOutputStream fos = ctx.openFileOutput(filename + "_private.key", Context.MODE_PRIVATE);
        fos.write(pkcs8EncodedKeySpec.getEncoded());
        fos.close();
    }

    public void readRSAPublicKey(String filename, Context ctx){
        try {
            FileInputStream fi = ctx.openFileInput(filename + "_public.key");
            byte[] encodedPublic = new byte[(int) fi.getChannel().size()];
            fi.read(encodedPublic);
            fi.close();

            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(encodedPublic);
            PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);
            setKey(publicKey);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
    }

    public void readRSAPrivateKey (String filename, Context ctx) {
        FileInputStream fi = null;
        try {
            fi = ctx.openFileInput(filename + "_private.key");
            byte[] encodedPrivate = new byte[(int) fi.getChannel().size()];
            fi.read(encodedPrivate);
            fi.close();

            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(encodedPrivate);
            PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);
            setKey(privateKey);
            
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }

    }

}
