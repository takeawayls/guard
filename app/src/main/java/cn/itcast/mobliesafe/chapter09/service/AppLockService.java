package cn.itcast.mobliesafe.chapter09.service;

import java.util.List;

import cn.itcast.mobliesafe.chapter09.EnterPswActivity;
import cn.itcast.mobliesafe.chapter09.db.dao.AppLockDao;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;

/***
 * 程序锁服务
 * 
 * @author admin
 */
public class AppLockService extends Service {
	/** 是否开启程序锁服务的标志 */
	private boolean flag = false;
	private AppLockDao dao;
	private Uri uri = Uri.parse("content://com.itcast.mobilesafe.applock");
	private List<String> packagenames;
	private Intent intent;
	private ActivityManager am;
	private List<RunningTaskInfo> taskInfos;
	private RunningTaskInfo taskInfo;
	private String pacagekname;
	private String tempStopProtectPackname;
	private AppLockReceiver receiver;
	private MyObserver observer;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		// 创建AppLockDao实例
		dao = new AppLockDao(this);
		observer = new MyObserver(new Handler());
		getContentResolver().registerContentObserver(uri, true,
				observer);
		// 获取数据库中的所有包名
		packagenames = dao.findAll();
		receiver = new AppLockReceiver();
		IntentFilter filter = new IntentFilter("cn.itcast.mobliesafe.applock");
		filter.addAction(Intent.ACTION_SCREEN_ON);
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		registerReceiver(receiver, filter);
		// 创建Intent实例，用来打开输入密码页面
		intent = new Intent(AppLockService.this, EnterPswActivity.class);
		// 获取ActivityManager对象
		am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		startApplockService();
		super.onCreate();
	}

	/***
	 * 开启监控程序服务
	 */
	private void startApplockService() {
		new Thread() {
			public void run() {
				flag = true;
				while (flag) {
					// 监视任务栈的情况。 最近使用的打开的任务栈在集合的最前面
					taskInfos = am.getRunningTasks(1);
					// 最近使用的任务栈
					taskInfo = taskInfos.get(0);
					pacagekname = taskInfo.topActivity.getPackageName();
					// 判断这个包名是否需要被保护。
					if (packagenames.contains(pacagekname)) {
						// 判断当前应用程序是否需要临时停止保护（输入了正确的密码）
						if (!pacagekname.equals(tempStopProtectPackname)) {
							// 需要保护
							// 弹出一个输入密码的界面。
							intent.putExtra("packagename", pacagekname);
							intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							startActivity(intent);
						}
					}
					try {
						Thread.sleep(30);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			};
		}.start();
	}

	// 广播接收者
	class AppLockReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if ("cn.itcast.mobliesafe.applock".equals(intent.getAction())) {
				tempStopProtectPackname = intent.getStringExtra("packagename");
			} else if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
				tempStopProtectPackname = null;
				// 停止监控程序
				flag = false;
			} else if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
				// 开启监控程序
				if (flag == false) {
					startApplockService();
				}
			}
		}
	}

	// 内容观察者
	class MyObserver extends ContentObserver {

		public MyObserver(Handler handler) {
			super(handler);
		}

		@Override
		public void onChange(boolean selfChange) {
			packagenames = dao.findAll();
			super.onChange(selfChange);
		}
	}
	
	@Override
	public void onDestroy() {
		flag = false;
		unregisterReceiver(receiver);
		receiver = null;
		getContentResolver().unregisterContentObserver(observer);
		observer = null;
		super.onDestroy();
	}
}
