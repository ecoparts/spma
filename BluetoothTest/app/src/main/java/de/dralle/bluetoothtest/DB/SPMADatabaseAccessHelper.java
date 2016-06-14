package de.dralle.bluetoothtest.DB;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by nils on 11.06.16.
 */
public class SPMADatabaseAccessHelper {
    /**
     * Log tag. Used to identify this´ class log messages in log output
     */
    private static final String LOG_TAG = SPMADatabaseAccessHelper.class.getName();

    private Context context;

    private SQLiteOpenHelper db = null;

    public SPMADatabaseAccessHelper(Context context) {
        this.context = context;
        db = new SPMADatabaseHelper(context);
    }

    public User addUser(String name) {
        SQLiteDatabase connection = db.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("Name", name);
        long rowid = connection.insert("User", null, cv);
        Cursor c = connection.rawQuery("select ID, Name from User where User._rowid_=?", new String[]{rowid + ""});
        c.moveToNext();
        String sid = c.getString(0);
        name = c.getString(1);
        int id = Integer.parseInt(sid);
        c.close();
        connection.close();
        Log.i(LOG_TAG, "New User " + name + " with id " + id + " inserted");
        User u = new User();
        u.setId(id);
        u.setName(name);
        return u;
    }

    public User getUser(int id) {
        SQLiteDatabase connection = db.getReadableDatabase();
        Cursor c = connection.rawQuery("select ID, Name from User where User.ID=?", new String[]{id + ""});
        if (c.moveToNext()) {
            String name = c.getString(1);
            c.close();
            connection.close();
            User u = new User();
            u.setId(id);
            u.setName(name);
            return u;
        } else {
            return null;
        }
    }

    /**
     * Looks for a device´s friendly name. This is not the device´s bluetooth name, but the user name used by the app on the remote device
     * @param address Remote device address
     * @return Remote device friendly name/remote device user name. returns address when no name was found.
     */
    public String getDeviceFriendlyName(String address) {
        SQLiteDatabase connection = db.getReadableDatabase();
        Cursor c = connection.rawQuery("select FriendlyName from Devices where Address=?", new String[]{"'" + address + "'"});

        if(c.moveToNext()){
            String name = c.getString(0);
            c.close();
            connection.close();
           return name;
        }else{
            return address;
        }

    }
}
