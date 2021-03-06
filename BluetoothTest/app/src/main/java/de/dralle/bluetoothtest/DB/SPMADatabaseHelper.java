package de.dralle.bluetoothtest.DB;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import de.dralle.bluetoothtest.R;

/**
 * Created by nils on 09.06.16.
 */
public class SPMADatabaseHelper extends SQLiteOpenHelper {
    /**
     * Log tag. Used to identify this´ class log messages in log output
     */
    private static final String LOG_TAG = SPMADatabaseHelper.class.getName();

    private Context context;

    public SPMADatabaseHelper(Context context) {
        super(
                context,
                context.getResources().getString(R.string.dbname),
                null,
                Integer.parseInt(context.getResources().getString(R.string.db_version)));
        this.context = context;
    }


    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        Log.i(LOG_TAG, "Database opened");
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        for (String sql : context.getResources().getStringArray(R.array.dbcreate))
            sqLiteDatabase.execSQL(sql);
        Log.i(LOG_TAG, "Database created");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        Log.i(LOG_TAG, "Database upgrading from " + i + " to " + i1);
        for (String sql : context.getResources().getStringArray(R.array.dbdrop))
            sqLiteDatabase.execSQL(sql);
        Log.i(LOG_TAG, "Database 'upgraded'");
        onCreate(sqLiteDatabase);


    }
    @Override
    public void onDowngrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        Log.i(LOG_TAG, "Database downgrading from " + i + " to " + i1);
        onUpgrade(sqLiteDatabase,i,i1);


    }
}
