package cn.itcast.mobliesafe;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.Toast;
import cn.itcast.mobliesafe.chapter01.adapter.HomeAdapter;
import cn.itcast.mobliesafe.chapter02.LostFindActivity;
import cn.itcast.mobliesafe.chapter02.dialog.InterPasswordDialog;
import cn.itcast.mobliesafe.chapter02.dialog.InterPasswordDialog.MyCallBack;
import cn.itcast.mobliesafe.chapter02.dialog.SetUpPasswordDialog;
import cn.itcast.mobliesafe.chapter02.receiver.MyDeviceAdminReciever;
import cn.itcast.mobliesafe.chapter02.utils.MD5Utils;
import cn.itcast.mobliesafe.chapter03.SecurityPhoneActivity;
import cn.itcast.mobliesafe.chapter04.AppManagerActivity;
import cn.itcast.mobliesafe.chapter05.VirusScanActivity;
import cn.itcast.mobliesafe.chapter06.CacheClearListActivity;
import cn.itcast.mobliesafe.chapter07.ProcessManagerActivity;
import cn.itcast.mobliesafe.chapter08.TrafficMonitoringActivity;
import cn.itcast.mobliesafe.chapter09.AdvancedToolsActivity;
import cn.itcast.mobliesafe.chapter10.SettingsActivity;

public class HomeActivity extends Activity {

	/** 声明GridView 该控件类似ListView */
	private GridView gv_home;
	/** 存储手机防盗密码的sp */
	private SharedPreferences msharedPreferences;
	/** 设备管理 员 */
	private DevicePolicyManager policyManager;
	/** 申请权限 */
	private ComponentName componentName;
	private long mExitTime;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// 初始化布局
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_home);
		msharedPreferences = getSharedPreferences("config", MODE_PRIVATE);
		// 初始化GridView
		gv_home = (GridView) findViewById(R.id.gv_home);
		gv_home.setAdapter(new HomeAdapter(HomeActivity.this));
		// 设置条目的点击事件
		gv_home.setOnItemClickListener(new OnItemClickListener() {
			// parent代表gridView,view代表每个条目的view对象,postion代表每个条目的位置
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				switch (position) {
				case 0: // 点击手机防盗
					if (isSetUpPassword()) {
						// 弹出输入密码对话框
						showInterPswdDialog();
					} else {
						// 弹出设置密码对话框
						showSetUpPswdDialog();
					}
					break;
				case 1: // 点击通讯卫士
					startActivity(SecurityPhoneActivity.class);
					break;
				case 2: // 软件管家
					startActivity(AppManagerActivity.class);
					break;
				case 3:// 手机杀毒
					startActivity(VirusScanActivity.class);
					break;
				case 4:// 缓存清理
					startActivity(CacheClearListActivity.class);
					break;
				case 5:// 进程管理
					startActivity(ProcessManagerActivity.class);
					break;
				case 6: // 流量统计
					startActivity(TrafficMonitoringActivity.class);
					break;
				case 7: // 高级工具
					startActivity(AdvancedToolsActivity.class);
					break;
				case 8: // 设置中心
					startActivity(SettingsActivity.class);
					break;
				}
			}
		});
		// 1.获取设备管理员
		policyManager=(DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        // 2.申请权限, MyDeviceAdminReciever继承自DeviceAdminReceiver
		componentName=new ComponentName(this, MyDeviceAdminReciever.class);
	    // 3.判断,如果没有权限则申请权限
		boolean active=policyManager.isAdminActive(componentName);
		if(!active) {
			//没有管理员的权限，则获取管理员的权限
			Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
			intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName);
			intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "获取超级管理员权限，用于远程锁屏和清除数据");
			startActivity(intent);
		}
		
	}
	/***
	 * 弹出设置密码对话框
	 */
	private void showSetUpPswdDialog() {
		final SetUpPasswordDialog setUpPasswordDialog = new SetUpPasswordDialog(
				HomeActivity.this);
		setUpPasswordDialog
				.setCallBack(new cn.itcast.mobliesafe.chapter02.dialog.SetUpPasswordDialog.MyCallBack() {

					@Override
					public void ok() {
						String firstPwsd = setUpPasswordDialog.mFirstPWDET
								.getText().toString().trim();
						String affirmPwsd = setUpPasswordDialog.mAffirmET
								.getText().toString().trim();
						if (!TextUtils.isEmpty(firstPwsd)
								&& !TextUtils.isEmpty(affirmPwsd)) {
							if (firstPwsd.equals(affirmPwsd)) {
								// 两次密码一致,存储密码
								savePswd(affirmPwsd);
								setUpPasswordDialog.dismiss();
								// 显示输入密码对话框
								showInterPswdDialog();
							} else {
								Toast.makeText(HomeActivity.this, "两次密码不一致！", 0).show(); 
							}
						} else {
							Toast.makeText(HomeActivity.this, "密码不能为空！", 0).show();
						}
					}

					@Override
					public void cancle() {
						setUpPasswordDialog.dismiss();
					}
				});
		setUpPasswordDialog.setCancelable(true);
		setUpPasswordDialog.show();
	}

	/**
	 * 弹出输入密码对话框
	 */
	private void showInterPswdDialog() {
		final String password = getPassword();
		final InterPasswordDialog mInPswdDialog = new InterPasswordDialog(
				HomeActivity.this);
		mInPswdDialog.setCallBack(new MyCallBack() {
			@Override
			public void confirm() {
				if (TextUtils.isEmpty(mInPswdDialog.getPassword())) {
					Toast.makeText(HomeActivity.this, "密码不能为空！", 0).show();
				} else if (password.equals(MD5Utils.encode(mInPswdDialog
						.getPassword()))) {
					// 进入防盗主界面
					mInPswdDialog.dismiss();
					startActivity(LostFindActivity.class);
				} else {
					// 对话框消失，弹出土司
					mInPswdDialog.dismiss();
					Toast.makeText(HomeActivity.this, "密码有误，请重新输入！", 0).show();
				}
			}

			@Override
			public void cancle() {
				mInPswdDialog.dismiss();
			}
		});
		mInPswdDialog.setCancelable(true);
		// 让对话框显示
		mInPswdDialog.show();
	}

	/***
	 * 保存密码
	 * 
	 * @param affirmPwsd
	 */
	private void savePswd(String affirmPwsd) {
		Editor edit = msharedPreferences.edit();
		// 为了防止用户隐私被泄露，因此需要加密密码
		edit.putString("PhoneAntiTheftPWD", MD5Utils.encode(affirmPwsd));
		edit.commit();
	}

	/***
	 * 获取密码
	 * 
	 * @return sp存储的密码
	 */
	private String getPassword() {
		String password = msharedPreferences.getString("PhoneAntiTheftPWD",
				null);
		if (TextUtils.isEmpty(password)) {
			return "";
		}
		return password;
	}

	/** 判断用户是否设置过手机防盗密码 */
	private boolean isSetUpPassword() {
		String password = msharedPreferences.getString("PhoneAntiTheftPWD",
				null);
		if (TextUtils.isEmpty(password)) {
			return false;
		}
		return true;
	}

	/**
	 * 开启新的activity不关闭自己
	 * 
	 * @param cls
	 *            新的activity的字节码
	 */
	public void startActivity(Class<?> cls) {
		Intent intent = new Intent(HomeActivity.this, cls);
		startActivity(intent);
	}

	/***
	 * 按两次返回键退出程序
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if ((System.currentTimeMillis() - mExitTime) > 2000) {
				Toast.makeText(this, "再按一次退出程序", Toast.LENGTH_SHORT).show();
				mExitTime = System.currentTimeMillis();
			} else {
				System.exit(0);
			}
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
}
