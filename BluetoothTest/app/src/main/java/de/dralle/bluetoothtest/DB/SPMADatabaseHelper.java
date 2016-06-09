package de.dralle.bluetoothtest.DB;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import de.dralle.bluetoothtest.R;

/**
 * Created by nils on 09.06.16.
 */
public class SPMADatabaseHelper extends SQLiteOpenHelper{

    private Context context;

    public SPMADatabaseHelper(Context context){
        super(
                context,
                context.getResources().getString(R.string.dbname),
                null,
                Integer.parseInt(context.getResources().getString(R.string.version)));
        this.context=context;
    }



    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {

    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }
}
