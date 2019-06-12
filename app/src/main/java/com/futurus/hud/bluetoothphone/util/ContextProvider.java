package com.futurus.hud.bluetoothphone.util;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.futurus.hud.bluetoothphone.BuildConfig;

import timber.log.Timber;

public class ContextProvider extends ContentProvider {
    private static Context mContext;

    @Override
    public boolean onCreate() {
        mContext = getContext();
        initTimber();
        return true;
    }

    private void initTimber() {
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
//            Timber.tag("MainActivity0000");
        }else {
            Timber.plant(new CrashReportingTree());
        }
    }
    private static class CrashReportingTree extends Timber.Tree {
        @Override
        protected void log(int priority, String tag, String message, Throwable t) {

        }
    }
    public static Context getAppContext() {
        return mContext;
    }

    @Override
    public Cursor query(Uri uri, String[] strings, String s, String[] strings1, String s1) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        return null;
    }

    @Override
    public int delete(Uri uri, String s, String[] strings) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String s, String[] strings) {
        return 0;
    }
}
