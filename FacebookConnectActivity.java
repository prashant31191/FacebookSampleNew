package com.nexercise.client.android.activities;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import com.facebook.FacebookRequestError;
import com.facebook.HttpMethod;
import com.facebook.LoggingBehavior;
import com.facebook.Request;
import com.facebook.RequestAsyncTask;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.Settings;
import com.facebook.UiLifecycleHelper;
import com.nexercise.client.android.NexerciseApplication;
import com.nexercise.client.android.base.BaseActivity;
import com.nexercise.client.android.constants.FacebookConstants;
import com.nexercise.client.android.constants.MessagesConstants;
import com.nexercise.client.android.constants.UserPreferencesConstants;
import com.nexercise.client.android.model.DataLayer;
import com.nexercise.client.android.task.ConnectToFacebook;
import com.nexercise.client.android.utils.Funcs;
import com.nexercise.client.android.utils.Logger;
import com.nexercise.client.android.utils.Logger.ACTIVITY_STATUS;

public class FacebookConnectActivity extends BaseActivity {

	public static boolean postInvite;
	HashMap<String, Object> userFacebookData;
	private Session.StatusCallback statusCallback = new SessionStatusCallback();
    private UiLifecycleHelper uiHelper;
    private static final List<String> PERMISSIONS = Arrays.asList("publish_actions");
	boolean linked = false;
	Activity activity = this;
	
	Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {

			switch (msg.what) {

			case UserPreferencesConstants.SHOW_MESSAGE:
				((NexerciseApplication) FacebookConnectActivity.this
						.getApplication()).showFloatingTextActivity(
						FacebookConnectActivity.this,
						msg.getData().getString("ToastMessage"));
				break;

			case UserPreferencesConstants.SHOW_SIMPLE_MESSAGE:
				Toast.makeText(FacebookConnectActivity.this,
						msg.getData().getString("ToastMessage"), Toast.LENGTH_SHORT).show();
				FacebookConnectActivity.this.finish();
				break;

			case UserPreferencesConstants.POST_FACEBOOK_INVITE:
				postOnFacebookWall();
				FacebookConnectActivity.this.finish();
				break;
			}

		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
        uiHelper = new UiLifecycleHelper(this, statusCallback);
        uiHelper.onCreate(savedInstanceState);
	}

	@Override
	public void initComponents() {
		// setContentView(R.layout.dialog_showmessage);
		Logger.activityStatusChanged(this.getLocalClassName(),
				ACTIVITY_STATUS.STARTED);
		Settings.addLoggingBehavior(LoggingBehavior.INCLUDE_ACCESS_TOKENS);
		initFaceBookLogin();
	}

	public void initFaceBookLogin() {
		try {
			Session session = Session.getActiveSession();
			if (session == null) {
				session = new Session(this);
			}
			Session.setActiveSession(session);
			if (!session.isOpened() && !session.isClosed()) {
				session.openForRead(new Session.OpenRequest(
						FacebookConnectActivity.this)
						.setCallback(statusCallback).setPermissions(Arrays.asList(FacebookConstants.READ_PERMISSIONS)));;
			} else {
				Session.openActiveSession(FacebookConnectActivity.this, true,
						statusCallback);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void setListeners() {
		// TODO Auto-generated method stub

	}

	@Override
	public void loadData() {
		// TODO Auto-generated method stub

	}

	@Override
	public void fetchData() {
		// TODO Auto-generated method stub

	}

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
		if(requestCode == FacebookConstants.FACEBOOK_REQUEST_CODE){
        	boolean key = data.getBooleanExtra("requestSatatus",false);
        	if(resultCode == 1001 && key == true){
    			try {
					postOnFacebookWall();
				} catch (Exception e) {
					e.printStackTrace();
				}
        	}
		}else{
			uiHelper.onActivityResult(requestCode, resultCode, data);
		}
    }

	private void postOnFacebookWall() {

		boolean flag = true;

		Bundle bundle = new Bundle();

		String userMessage = "";

		bundle.putString("name", FacebookConstants.ADD_FRIEND_TITLE);
		bundle.putString("caption", FacebookConstants.ADD_FRIEND_CAPTION);
		bundle.putString("description",
				FacebookConstants.ADD_FRIEND_DESCRIPTION);
		bundle.putString("message", userMessage);
		bundle.putString("link", FacebookConstants.ADD_FRIEND_LINK);
		bundle.putString("picture", FacebookConstants.ADD_FRIEND_THUMBNAIL);
		if(hasPublishPermission()){
			try {
				Request.Callback callback = new Request.Callback() {
					public void onCompleted(Response response) {
						JSONObject graphResponse = response.getGraphObject()
								.getInnerJSONObject();
						String postId = null;
						try {
							postId = graphResponse.getString("id");
						} catch (JSONException e) {
	
						}
						FacebookRequestError error = response.getError();
						if (error != null) {
							Toast.makeText(
									FacebookConnectActivity.this
											.getApplicationContext(),
									error.getErrorMessage(), Toast.LENGTH_SHORT)
									.show();
						} else {
							Toast.makeText(
									FacebookConnectActivity.this
											.getApplicationContext(),
									postId, Toast.LENGTH_LONG).show();
						}
					}
				};
				Session session = Session.getActiveSession();
				if (session != null) {
					Request request = new Request(session, "me/feed", bundle,
							HttpMethod.POST, callback);
	
					RequestAsyncTask task = new RequestAsyncTask(request);
					task.execute();
				}
	
			} catch (Exception e) {
	
				flag = false;
				Funcs.showShortToast("Could not post  " + e.getMessage(), this);
			}
	
			if (flag) {
				UserPreferencesConstants.FACEBOOK_SENT = true;
				Funcs.showShortToast(MessagesConstants.POST_SUBMITTED_FACEBOOK,
						this);
			}
		}else{
			getNexerciseApplication().showFacebookPermissionRequestActivity(FacebookConnectActivity.this,FacebookConstants.FACEBOOK_REQUEST_CODE);

		}

	}

    @Override
    public void onPause() {
        super.onPause();
        uiHelper.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        uiHelper.onDestroy();
    }
    @Override
	public void onResume() {
		Logger.activityStatusChanged(this.getLocalClassName(),
				ACTIVITY_STATUS.RESUMED);
        super.onResume();
        uiHelper.onResume();
    }

    @Override
	public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        uiHelper.onSaveInstanceState(outState);

    }


	private DataLayer getDataLayer() {

		return ((NexerciseApplication) this.getApplication())
				.getDataLayerInstance();
	}

	private class SessionStatusCallback implements Session.StatusCallback {
		@Override
		public void call(Session session, SessionState state,
				Exception exception) {
			if (state.isOpened()) {
				String accessToken = "";
				try {
					accessToken = session.getAccessToken();
					// // launch Dialog
					new ConnectToFacebook(FacebookConnectActivity.this,
							getDataLayer().getUserInfo(), handler, accessToken,
							false).execute();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			if (exception != null) {
				try {
					String error = exception.getMessage();
					Funcs.showShortToast(error, FacebookConnectActivity.this);
				} catch (Exception e) {
				}
				FacebookConnectActivity.this.finish();
			}
		}

	}
	
    private boolean hasPublishPermission() {
        Session session = Session.getActiveSession();
        return session != null && session.getPermissions().contains("publish_actions");
    }
    
	private NexerciseApplication getNexerciseApplication() {
		return ((NexerciseApplication) this.getApplication());
	}
}
