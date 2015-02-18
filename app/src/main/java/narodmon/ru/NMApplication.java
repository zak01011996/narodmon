package narodmon.ru;

import android.app.Application;
import android.content.Context;

public class NMApplication extends Application {


    private static Context context;

    @Override
    public void onCreate() {
        NMApplication.context=getApplicationContext();
    }

    public static Context getAppContext() {
        return NMApplication.context;
    }
}
