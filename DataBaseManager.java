package com.farmappweb.pests.android.database;

import android.database.sqlite.SQLiteDatabase;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by juanlabrador on 30/11/16.
 */

public class DataBaseManager {

    private AtomicInteger mOpenCounter = new AtomicInteger();

    private static DataBaseManager instance;
    private static DataBaseHelper mHelper;
    private SQLiteDatabase mDatabase;

    public static synchronized void initializeInstance(DataBaseHelper helper) {
        if (instance == null) {
            instance = new DataBaseManager();
            mHelper = helper;
        }
    }

    public static synchronized DataBaseManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException(DataBaseManager.class.getSimpleName() +
                    " is not initialized, call initializeInstance(..) method first.");
        }

        return instance;
    }

    public static DataBaseHelper getDataBaseHelper() {
        return mHelper;
    }

    public synchronized SQLiteDatabase openDatabase() {
        if(mOpenCounter.incrementAndGet() == 1) {
            // Opening new database
            mDatabase = mHelper.getWritableDatabase();
        }
        return mDatabase;
    }

    public synchronized void closeDatabase() {
        if(mOpenCounter.decrementAndGet() == 0) {
            // Closing database
            mDatabase.close();
        }
    }
}