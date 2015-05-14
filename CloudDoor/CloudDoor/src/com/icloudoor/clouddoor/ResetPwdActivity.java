package com.icloudoor.clouddoor;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.android.volley.Request.Method;
import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class ResetPwdActivity extends Activity implements TextWatcher {
	private EditText ETInputOldPwd;
	private EditText ETInputNewPwd;
	private EditText ETConfirmNewPwd;
	private TextView TVResetDone;
	private RelativeLayout IVBack;
	private URL resetPwdURL;
	private RequestQueue mQueue;
	private String oldPwd, newPwd, confirmPwd;

	private int statusCode;
	private String HOST = "http://zone.icloudoor.com/icloudoor-web";
	private String sid = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActionBar().hide();
		setContentView(R.layout.set_detail_reset_pwd);

		mQueue = Volley.newRequestQueue(this);

		IVBack = (RelativeLayout) findViewById(R.id.btn_back_reset_pwd);
		ETInputOldPwd = (EditText) findViewById(R.id.input_old_pwd);
		ETInputNewPwd = (EditText) findViewById(R.id.input_new_pwd);
		ETConfirmNewPwd = (EditText) findViewById(R.id.confirm_new_pwd);
		TVResetDone = (TextView) findViewById(R.id.reset_pwd_done);
		
		TVResetDone.setTextColor(0xFFcccccc);
		TVResetDone.setEnabled(false);
		
		ETInputNewPwd.addTextChangedListener(this);
		ETConfirmNewPwd.addTextChangedListener(this);
		
		sid = loadSid();

		IVBack.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
//				Intent intent = new Intent();
//				intent.setClass(v.getContext(), SettingDetailActivity.class);
//				startActivity(intent);
				finish();
			}

		});

		TVResetDone.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				try {
					resetPwdURL = new URL(HOST
							+ "/user/manage/changePassword.do" + "?sid=" + sid);
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}

				oldPwd = ETInputOldPwd.getText().toString();
				newPwd = ETInputNewPwd.getText().toString();
				confirmPwd = ETConfirmNewPwd.getText().toString();
				if (newPwd.equals(confirmPwd)) {
					MyJsonObjectRequest mJsonRequest = new MyJsonObjectRequest(
							Method.POST, resetPwdURL.toString(), null,
							new Response.Listener<JSONObject>() {

								@Override
								public void onResponse(JSONObject response) {
									try {
										if (response.getString("sid") != null) {
											sid = response.getString("sid");
											saveSid(sid);
										}
										statusCode = response.getInt("code");
									} catch (JSONException e) {
										e.printStackTrace();
									}
									Log.e("TEST",
											"statusCode: "
													+ String.valueOf(statusCode));
									Log.e("TEST",
											"response: " + response.toString());
									try {
										Log.e("TEST",
												"sid: "
														+ response
																.getString("sid"));
									} catch (JSONException e) {
										e.printStackTrace();
									}
									if (statusCode == 1) {
//										Intent intent = new Intent();
//										intent.setClass(getApplicationContext(),
//												SettingDetailActivity.class);
//										startActivity(intent);
										
										finish();
									}else if (statusCode == -41) {
										Toast.makeText(getApplicationContext(), R.string.weak_pwd,
												Toast.LENGTH_SHORT).show();
									}else if (statusCode == -51) {
										Toast.makeText(getApplicationContext(), R.string.wrong_old_pwd,
												Toast.LENGTH_SHORT).show();
									}
								}
							}, new Response.ErrorListener() {

								@Override
								public void onErrorResponse(VolleyError error) {

								}
							}) {
						@Override
						protected Map<String, String> getParams()
								throws AuthFailureError {
							Map<String, String> map = new HashMap<String, String>();
							map.put("oldPassword", oldPwd);
							map.put("newPassword", confirmPwd);
							return map;
						}
					};
					mQueue.add(mJsonRequest);
				} else {
					Toast.makeText(v.getContext(), R.string.diff_pwd,
							Toast.LENGTH_SHORT).show();
				}
			}

		});

	}
	
	public void saveSid(String sid) {
		SharedPreferences savedSid = getSharedPreferences("SAVEDSID",
				MODE_PRIVATE);
		Editor editor = savedSid.edit();
		editor.putString("SID", sid);
		editor.commit();
	}

	public String loadSid() {
		SharedPreferences loadSid = getSharedPreferences("SAVEDSID", 0);
		return loadSid.getString("SID", null);
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterTextChanged(Editable s) {
		if(ETInputNewPwd.getText().toString().length() > 7 && ETConfirmNewPwd.getText().toString().length() > 7){
			TVResetDone.setTextColor(0xFFffffff);
			TVResetDone.setEnabled(true);
		} else {
			TVResetDone.setTextColor(0xFFcccccc);
			TVResetDone.setEnabled(false);
		}
	}	
	
}