package com.factory.handbag;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;

public class FactoryDb extends SQLiteOpenHelper {
    public static final String DB_NAME = "factory.db";
    public static final int DB_VERSION = 2;

    public FactoryDb(Context context) { super(context, DB_NAME, null, DB_VERSION); }

    @Override public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
        db.enableWriteAheadLogging();
    }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE brands(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL UNIQUE,contact TEXT,phone TEXT,notes TEXT,created_at TEXT DEFAULT CURRENT_TIMESTAMP)");
        db.execSQL("CREATE TABLE teams(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL UNIQUE,notes TEXT,created_at TEXT DEFAULT CURRENT_TIMESTAMP)");
        db.execSQL("CREATE TABLE workers(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,type TEXT NOT NULL CHECK(type IN ('SALARY','PIECE')),fixed_salary REAL DEFAULT 0,piece_rate REAL DEFAULT 0,team_id INTEGER,active INTEGER DEFAULT 1,created_at TEXT DEFAULT CURRENT_TIMESTAMP,FOREIGN KEY(team_id) REFERENCES teams(id))");
        db.execSQL("CREATE TABLE production_orders(id INTEGER PRIMARY KEY AUTOINCREMENT,order_no TEXT NOT NULL UNIQUE,brand_id INTEGER NOT NULL,bag_model TEXT NOT NULL,ordered_qty INTEGER NOT NULL,rate_per_bag REAL DEFAULT 0,team_id INTEGER,status TEXT DEFAULT 'OPEN',start_date TEXT,due_date TEXT,notes TEXT,created_at TEXT DEFAULT CURRENT_TIMESTAMP,FOREIGN KEY(brand_id) REFERENCES brands(id),FOREIGN KEY(team_id) REFERENCES teams(id))");
        db.execSQL("CREATE TABLE production_logs(id INTEGER PRIMARY KEY AUTOINCREMENT,order_id INTEGER NOT NULL,stage TEXT NOT NULL,entry_date TEXT NOT NULL,input_qty INTEGER DEFAULT 0,good_qty INTEGER DEFAULT 0,rework_qty INTEGER DEFAULT 0,damaged_qty INTEGER DEFAULT 0,reason TEXT,notes TEXT,created_at TEXT DEFAULT CURRENT_TIMESTAMP,FOREIGN KEY(order_id) REFERENCES production_orders(id))");
        db.execSQL("CREATE TABLE worker_work(id INTEGER PRIMARY KEY AUTOINCREMENT,worker_id INTEGER NOT NULL,order_id INTEGER,work_date TEXT NOT NULL,qty REAL DEFAULT 0,rate REAL DEFAULT 0,amount REAL DEFAULT 0,notes TEXT,created_at TEXT DEFAULT CURRENT_TIMESTAMP,FOREIGN KEY(worker_id) REFERENCES workers(id),FOREIGN KEY(order_id) REFERENCES production_orders(id))");
        db.execSQL("CREATE TABLE advances(id INTEGER PRIMARY KEY AUTOINCREMENT,worker_id INTEGER NOT NULL,advance_date TEXT NOT NULL,amount REAL NOT NULL,notes TEXT,created_at TEXT DEFAULT CURRENT_TIMESTAMP,FOREIGN KEY(worker_id) REFERENCES workers(id))");
        db.execSQL("CREATE TABLE worker_payments(id INTEGER PRIMARY KEY AUTOINCREMENT,worker_id INTEGER NOT NULL,payment_date TEXT NOT NULL,amount REAL NOT NULL,period_label TEXT,notes TEXT,created_at TEXT DEFAULT CURRENT_TIMESTAMP,FOREIGN KEY(worker_id) REFERENCES workers(id))");
        db.execSQL("CREATE TABLE expenses(id INTEGER PRIMARY KEY AUTOINCREMENT,expense_date TEXT NOT NULL,category TEXT NOT NULL,amount REAL NOT NULL,vendor TEXT,brand_id INTEGER,order_id INTEGER,notes TEXT,created_at TEXT DEFAULT CURRENT_TIMESTAMP,FOREIGN KEY(brand_id) REFERENCES brands(id),FOREIGN KEY(order_id) REFERENCES production_orders(id))");
        db.execSQL("CREATE TABLE brand_payments(id INTEGER PRIMARY KEY AUTOINCREMENT,brand_id INTEGER NOT NULL,order_id INTEGER,payment_date TEXT NOT NULL,amount REAL NOT NULL,notes TEXT,created_at TEXT DEFAULT CURRENT_TIMESTAMP,FOREIGN KEY(brand_id) REFERENCES brands(id),FOREIGN KEY(order_id) REFERENCES production_orders(id))");
        db.execSQL("CREATE TABLE dispatches(id INTEGER PRIMARY KEY AUTOINCREMENT,order_id INTEGER NOT NULL,dispatch_date TEXT NOT NULL,qty INTEGER NOT NULL,notes TEXT,created_at TEXT DEFAULT CURRENT_TIMESTAMP,FOREIGN KEY(order_id) REFERENCES production_orders(id))");
        createV2(db);
        createIndexes(db);
    }

    void createV2(SQLiteDatabase db){
        db.execSQL("CREATE TABLE IF NOT EXISTS materials(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL UNIQUE,category TEXT,unit TEXT DEFAULT 'piece',active INTEGER DEFAULT 1,created_at TEXT DEFAULT CURRENT_TIMESTAMP)");
        db.execSQL("CREATE TABLE IF NOT EXISTS purchases(id INTEGER PRIMARY KEY AUTOINCREMENT,purchase_date TEXT NOT NULL,material_id INTEGER,qty REAL DEFAULT 0,unit TEXT,unit_cost REAL DEFAULT 0,amount REAL NOT NULL,vendor TEXT,order_id INTEGER,notes TEXT,created_at TEXT DEFAULT CURRENT_TIMESTAMP,FOREIGN KEY(material_id) REFERENCES materials(id),FOREIGN KEY(order_id) REFERENCES production_orders(id))");
    }

    void createIndexes(SQLiteDatabase db){
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_logs_order ON production_logs(order_id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_logs_date ON production_logs(entry_date)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_dispatch_order ON dispatches(order_id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_dispatch_date ON dispatches(dispatch_date)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_expenses_date ON expenses(expense_date)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_advances_worker ON advances(worker_id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_purchases_date ON purchases(purchase_date)");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if(oldVersion < 2){ createV2(db); createIndexes(db); }
    }

    public long insert(String table, ContentValues cv) { return getWritableDatabase().insertOrThrow(table, null, cv); }
    public int update(String table, ContentValues cv, String where, String[] args){ return getWritableDatabase().update(table,cv,where,args); }
    public Cursor query(String sql, String... args) { return getReadableDatabase().rawQuery(sql, args); }
    public double scalarDouble(String sql, String... args) { try (Cursor c = query(sql,args)) { return c.moveToFirst() ? c.getDouble(0) : 0; } }
    public int scalarInt(String sql, String... args) { try (Cursor c = query(sql,args)) { return c.moveToFirst() ? c.getInt(0) : 0; } }
    public String scalarString(String sql, String... args) { try (Cursor c = query(sql,args)) { return c.moveToFirst() ? c.getString(0) : ""; } }
}
