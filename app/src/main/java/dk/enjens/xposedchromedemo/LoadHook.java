package dk.enjens.xposedchromedemo;

import android.app.AndroidAppHelper;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.widget.ImageButton;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

public class LoadHook implements IXposedHookLoadPackage {
    ImageButton homeButton;

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if(!lpparam.packageName.equals("com.android.chrome"))
            return;
        XposedBridge.log("Loaded app: " + lpparam.packageName);
        Class<?> tintedImgButton = findClass("org.chromium.chrome.browser.widget.TintedImageButton", lpparam.classLoader);
        Class<?> toolbarPhone = findClass("org.chromium.chrome.browser.toolbar.ToolbarPhone", lpparam.classLoader);

        findAndHookMethod(toolbarPhone, "onFinishInflate", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                // this will be called before the clock was updated by the original method
            }
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                XposedBridge.log("in onFinishInflate");
                homeButton = (ImageButton)getObjectField(param.thisObject, "mHomeButton");
                homeButton.setBackgroundColor(Color.RED);
                homeButton.setAlpha(0.2f);
            }
        });

        findAndHookMethod(toolbarPhone, "onClick", View.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                XposedBridge.log("in onClick");
                View v = (View)param.args[0];
                if(homeButton != null && v == homeButton)
                {
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_MAIN);
                    intent.addCategory(Intent.CATEGORY_LAUNCHER);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    // We don't have access to any useful context here, so hardcode our known exported activity
                    ComponentName component = ComponentName.unflattenFromString("dk.enjens.xposedchromedemo/dk.enjens.xposedchromedemo.SettingsActivity");
                    if(component == null)  {
                        XposedBridge.log("Component == null!?");
                        return;
                    }
                    intent.setComponent(component);
                    AndroidAppHelper.currentApplication().startActivity(intent);
                }
            }
        });
    }
}
