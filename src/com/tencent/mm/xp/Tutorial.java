package com.tencent.mm.xp;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import android.app.Activity;
import android.content.ContentValues;
import android.util.Log;
import android.widget.Toast;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class Tutorial implements IXposedHookLoadPackage {

	public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {

		String packageName = lpparam.packageName;
		Log.d("Hook Loaded app: ", packageName);

		if (!packageName.equals("com.tencent.mm")) {
			return;
		}

		Class<?> dbClazz = XposedHelpers.findClass("com.tencent.wcdb.database.SQLiteDatabase", lpparam.classLoader);
		if (dbClazz != null) {
			Log.d("Hook Hook", "Find the class!!!");
		} else {
			Log.d("Hook Hook", "Cannot Find the class!!!");
		}

		findAndHookMethod("com.tencent.wcdb.database.SQLiteDatabase", lpparam.classLoader, "insert", String.class, String.class, ContentValues.class,
				new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						Object[] args = param.args;
						for (int i = 0; i < args.length; i++) {
							Object arg = args[i];
							Log.d("Hook insert", i + " : " + arg.toString());
						}
					}
				});

		findAndHookMethod("com.tencent.wcdb.database.SQLiteDatabase", lpparam.classLoader, "insertWithOnConflict", String.class, String.class,
				ContentValues.class, int.class, new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						Log.d("Hook", "------------------------insert start---------------------" + "\n\n");
						Log.d("Hook", "param args1:" + (String) param.args[0]);
						Log.d("Hook", "param args1:" + (String) param.args[1]);
						ContentValues contentValues = (ContentValues) param.args[2];
						Log.d("Hook", "param args3 contentValues:");

						for (Map.Entry<String, Object> item : contentValues.valueSet()) {
							if (item.getValue() != null) {
								Log.d("Hook", item.getKey() + " : " + item.getValue().toString());
							} else {
								Log.d("Hook", item.getKey() + " : " + "null");
							}
						}
						Log.d("Hook", "------------------------insert over---------------------" + "\n\n");

						String msgType = (String) param.args[0];
						Activity mContext = getTopActivity();
						if (msgType.equals("message")) {
							String talker = contentValues.getAsString("talker");
							String content = contentValues.getAsString("content");
							Long createTime = contentValues.getAsLong("createTime");

							SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm"); 
							String date_string = sdf.format(new Date(createTime)); 
							
							String message = talker + "\r\n" + date_string + " : " + content;
							Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();

//							FrameLayout view = (FrameLayout) mContext.getWindow().getDecorView();
//							FrameLayout frame = new FrameLayout(mContext);
//							FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
//							params.gravity = Gravity.CENTER;
//							frame.setLayoutParams(params);
//							Button b = new Button(mContext);
//							FrameLayout.LayoutParams b_p = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
//							b_p.gravity = Gravity.CENTER_HORIZONTAL;
//							b.setLayoutParams(b_p);
//							b.setText("北京");
//							frame.addView(b, b_p);
//							view.addView(frame, params);
							
							// TextView textView = new TextView(mContext);//新建控件
							// textView.setGravity(Gravity.CENTER);//居中
							// LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
							// ViewGroup.LayoutParams.WRAP_CONTENT);
							// //设置控件的宽高
							// layoutParams.setMargins(5, 5, 5, 5);//设置控件与上下左右的距离
							// textView.setGravity(Gravity.LEFT);
							// textView.setBackgroundResource(android.R.color.white);//设置背景色
							// textView.setLayoutParams(layoutParams);//上面设置控件的高宽后就落实
							// textView.append(message + "\r\n");//控件内容

						}
					}
				});

	}

	@SuppressWarnings("unchecked")
	private static Activity getTopActivity() {
		try {

			Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
			Object activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null);
			Field activitiesField = activityThreadClass.getDeclaredField("mActivities");
			activitiesField.setAccessible(true);

			Map<Object, Object> activities = (Map<Object, Object>) activitiesField.get(activityThread);
			if (activities == null) {
				return null;
			}

			for (Object activityRecord : activities.values()) {
				Class<?> activityRecordClass = activityRecord.getClass();
				Field pausedField = activityRecordClass.getDeclaredField("paused");
				pausedField.setAccessible(true);
				if (!pausedField.getBoolean(activityRecord)) {
					Field activityField = activityRecordClass.getDeclaredField("activity");
					activityField.setAccessible(true);
					Activity activity = (Activity) activityField.get(activityRecord);
					return activity;
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}
}
