package de.dralle.bluetoothtest.DB;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * Created by nils on 19.06.16.
 */
public class UserAccessHelper {
    /**
     * Log tag. Used to identify this´ class log messages in log output
     */
    private static final String LOG_TAG = UserAccessHelper.class.getName();
    /**
     * SQLite database connection
     */
    private SQLiteDatabase connection;

    public UserAccessHelper(SQLiteDatabase connection) {
        this.connection = connection;
    }

    /**
     * Adds a new user to the table
     * @param name New users'name
     * @return New user
     */
    @Deprecated
    public User addUser(String name) {
        ContentValues cv = new ContentValues();
        cv.put("Name", name);
        long rowid = connection.insert("User", null, cv);
        Cursor c = connection.rawQuery("select ID, Name from User where User._rowid_=?", new String[]{rowid + ""});
        c.moveToNext();
        int id = c.getInt(0);
        name = c.getString(1);
        c.close();
        Log.i(LOG_TAG, "New User " + name + " with id " + id + " inserted");
        User u = new User();
        u.setId(id);
        u.setName(name);
        return u;
    }
    /**
     * Updates a database User
     *
     * @param u Userdata to be updated
     * @return User given
     */
    public User createOrUpdateUser(User u) {
        ContentValues cv = new ContentValues();

        cv.put("Name", u.getName());
        cv.put("AES", u.getAes());
        cv.put("RSAPrivate", u.getRsaPrivate());
        cv.put("RSAPublic", u.getRsaPublic());

        //Check if user is already there
        Cursor c = connection.rawQuery("select count(*) from User where ID = ?", new String[]{u.getId() + ""});
        if (c.moveToNext()) {
            int cnt = c.getInt(0);
            if (cnt == 0) {
                //Insert
                cv.put("ID", u.getId());
                connection.insert("User", null, cv);
                Log.i(LOG_TAG, "User " + u.getName() + " with id " + u.getId() + " inserted");
            } else {
                //Update. No need to check for count, because primary key
                connection.update("User", cv, "ID = ?", new String[]{u.getId() + ""});
                Log.i(LOG_TAG, "User " + u.getName() + " with id " + u.getId() + " updated");
            }
        }
        c.close();
        return u;
    }
}
