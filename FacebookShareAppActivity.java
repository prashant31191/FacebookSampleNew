package com.nexercise.client.android.activities;

import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;

import com.facebook.FacebookAuthorizationException;
import com.facebook.FacebookOperationCanceledException;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.widget.FacebookDialog;
import com.nexercise.client.android.constants.FacebookConstants;
import com.nexercise.client.android.constants.UserPreferencesConstants;
import com.nexercise.client.android.helpers.FacebookHelper;
import com.nexercise.client.android.helpers.PreferenceHelper;
import com.socialize.api.SocializeSession;
import com.socialize.error.SocializeException;
import com.socialize.listener.SocializeAuthListener;
import com.socialize.networks.facebook.FacebookUtils;

public class FacebookShareAppActivity extends Activity {

    private UiLifecycleHelper uiHelper;
    private static final List<String> PERMISSIONS = Arrays.asList("publish_actions");
	boolean linked = false;
	Activity activity = this;
	
    private Session.StatusCallback callback = new Session.StatusCallback() {
        @Override
        public void call(Session session, SessionState state, Exception exception) {
            onSessionStateChange(session, state, exception);
        }
    };
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
        uiHelper = new UiLifecycleHelper(this, callback);
        uiHelper.onCreate(savedInstanceState);
        performPublishPermissionRequest();
		
	}
    private boolean hasPublishPermission() {
        Session session = Session.getActiveSession();
        return session != null && session.getPermissions().contains("publish_actions");
    }
	
    private void onSessionStateChange(Session session, SessionState state, Exception exception) {
    	String accessToken = "";
    	if ((exception instanceof FacebookOperationCanceledException ||
                exception instanceof FacebookAuthorizationException)) {
        	setResultAndClose(false);
        } else if (state == SessionState.OPENED_TOKEN_UPDATED) {
        	accessToken = session.getAccessToken();
        	FacebookHelper.saveAccessToken(activity, accessToken);
			try {
				linked = FacebookUtils.isLinkedForWrite(activity,
						FacebookConstants.PUBLISH_PERMISSIONS);
			} catch (Exception e) {
			}
			if (!linked) {
				FacebookUtils.linkForWrite(activity,
						FacebookHelper.getAccessToken(activity), false,
						new SocializeAuthListener() {
	
					@Override
					public void onError(SocializeException error) {
						// TODO Auto-generated method stub
						setResultAndClose(false);	
					}
	
					@Override
					public void onCancel() {
						// TODO Auto-generated method stub
						setResultAndClose(false);
					}
	
					@Override
					public void onAuthSuccess(
							SocializeSession session) {
						
						setResultAndClose(true);
					}
					@Override
					public void onAuthFail(SocializeException error) {
						// TODO Auto-generated method stub
						setResultAndClose(false);
					}
				}, FacebookConstants.PUBLISH_PERMISSIONS); // getNexerciseApplication().getSocializeAuthListener());
			}else{
				setResultAndClose(true);
			}
        }
    }
    
    @Override
	public void onResume() {
        super.onResume();
        uiHelper.onResume();
    }

    @Override
	public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        uiHelper.onSaveInstanceState(outState);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
       // uiHelper.onActivityResult(requestCode, resultCode, data);
        uiHelper.onActivityResult(requestCode, resultCode, data, new FacebookDialog.Callback() {
            @Override
            public void onError(FacebookDialog.PendingCall pendingCall, Exception error, Bundle data) {
                Log.e("Activity", String.format("Error: %s", error.toString()));
                setResultAndClose(false);
            }

            @Override
            public void onComplete(FacebookDialog.PendingCall pendingCall, Bundle data) {
                Log.i("Activity", "Success!");
                setResultAndClose(true);
            }
        });
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
    
    private void performPublishPermissionRequest() {
        Session session = Session.getActiveSession();
        if (session != null) {
			if (FacebookDialog.canPresentShareDialog(getApplicationContext(),
					FacebookDialog.ShareDialogFeature.SHARE_DIALOG)) {
				String url = PreferenceHelper.getStringPreference(activity, UserPreferencesConstants.USER_PREFERENCES, UserPreferencesConstants.USER_SHORTENED_URL);

		        FacebookDialog shareDialog = new FacebookDialog.ShareDialogBuilder(this)
		        .setLink(url)
		        .setName(FacebookConstants.ADD_FRIEND_TITLE).setDescription(FacebookConstants.ADD_FRIEND_DESCRIPTION).setPicture(FacebookConstants.ADD_FRIEND_THUMBNAIL).build();
		        //.setCaption(FacebookConstants.FACEBOOK_SHARE_DIALOG_CAPTION)
		        uiHelper.trackPendingDialogCall(shareDialog.present());

			}else{
	            if (!hasPublishPermission()) {
	            	try{
	            		session.requestNewPublishPermissions(new Session.NewPermissionsRequest(this, PERMISSIONS));
	            	}catch(Exception e){
	            		e.printStackTrace();
	            		setResultAndClose(false,false);
	            	}
	            }else{
	            	setResultAndClose(true,true);
	            }
			}
        }
    }
    
    public void setResultAndClose(boolean result){
    	Intent intent = new Intent();
    	intent.putExtra("requestSatatus", result);
    	setResult(1001, intent);
    	finish();
    }
    public void setResultAndClose(boolean result, boolean shared){
    	Intent intent = new Intent();
    	intent.putExtra("share", shared);
    	intent.putExtra("requestSatatus", result);
    	setResult(1001, intent);
    	finish();
    }
}
