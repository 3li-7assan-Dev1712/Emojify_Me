package com.example.emojify_me;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AppExcecuters {

    private static Executor deskIO;
    private final static Object LOCK = new Object();
    private static  AppExcecuters sInstance;
    private AppExcecuters(Executor _daskIO){
        this.deskIO = _daskIO;
    }
    public Executor deskIO(){
        return deskIO;
    }
    public static AppExcecuters getsInstance(){
        if(sInstance == null){
            synchronized (LOCK){
                sInstance = new AppExcecuters(Executors.newSingleThreadExecutor());
            }
        }
        return sInstance;
    }
}
