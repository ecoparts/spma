package de.dralle.bluetoothtest.DB;

/**
 * Created by nils on 09.06.16.
 */
public class User {
    private int id;
    private String name;
    private String aes, rsaPrivate,rsaPublic;

    public String getAes() {
        return aes;
    }

    public void setAes(String aes) {
        this.aes = aes;
    }

    public String getRsaPrivate() {
        return rsaPrivate;
    }

    public void setRsaPrivate(String rsaPrivate) {
        this.rsaPrivate = rsaPrivate;
    }

    public String getRsaPublic() {
        return rsaPublic;
    }

    public void setRsaPublic(String rsaPublic) {
        this.rsaPublic = rsaPublic;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
