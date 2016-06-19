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
}
