package anuj.wifidirect;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;


/**
 * Created by Mobilyte on 2/18/2016.
 */
public class GlobalActivity extends Application {
    private static Context context;

    public static synchronized Context getGlobalContext() {
        return context;
    }

    @Override
    public void onCreate() {

        super.onCreate();
        if (GlobalActivity.context == null) {
            GlobalActivity.context = getApplicationContext();
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }
}
