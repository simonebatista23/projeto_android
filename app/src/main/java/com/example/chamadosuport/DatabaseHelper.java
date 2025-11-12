package com.example.chamadosuport;


import android.content.Context;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase;
import android.content.ContentValues;
import android.database.Cursor;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "login.db";
    private static final int DB_VERSION = 1;
    private static final String TABLE = "user";
    private static final String COL_ID = "id";
    private static final String COL_EMAIL = "email";
    private static final String COL_PASSWORD = "password";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String create = "CREATE TABLE " + TABLE + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_EMAIL + " TEXT, " +
                COL_PASSWORD + " TEXT)";
        db.execSQL(create);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE);
        onCreate(db);
    }

    // Método que sobrescreve o login atual
    public void saveUser(String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE, null, null); // Apenas um usuário
        ContentValues values = new ContentValues();
        values.put(COL_EMAIL, email);
        values.put(COL_PASSWORD, password);
        db.insert(TABLE, null, values);
    }

    // <<< COLOQUE AQUI O NOVO MÉTODO >>>
    public void addUser(String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_EMAIL, email);
        values.put(COL_PASSWORD, password);
        db.insert(TABLE, null, values);
    }

    public Cursor getUser() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE + " LIMIT 1", null);
    }

    public Cursor getAllUsers() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE, null);
    }

    public void clearUser() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE, null, null);
    }
}
