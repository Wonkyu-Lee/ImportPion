package me.insertcoin.testpion;

import android.content.Context;
import android.view.Display;
import android.view.WindowManager;

import me.insertcoin.lib.pion.gl.GlCore;

/**
 * Created by blazeq on 2016. 2. 18..
 */
public class Application extends android.app.Application {
    private static Application mInstance;

    public static Application getInstance() {
        if (mInstance == null) {
            throw new IllegalStateException("Application is not created!");
        }

        return mInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;

        Display display = ((WindowManager)getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        GlCore.getInstance().setDisplayFps(display.getRefreshRate());
    }
}
