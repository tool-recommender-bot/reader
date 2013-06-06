package com.sismics.reader.activity;

import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.androidquery.AQuery;
import com.sismics.android.SismicsHttpResponseHandler;
import com.sismics.android.util.ConnectivityUtil;
import com.sismics.android.util.DialogUtil;
import com.sismics.reader.R;
import com.sismics.reader.listener.CallbackListener;
import com.sismics.reader.model.application.ApplicationContext;
import com.sismics.reader.resource.UserResource;
import com.sismics.reader.ui.form.Validator;
import com.sismics.reader.ui.form.validator.Required;
import com.sismics.reader.util.PreferenceUtil;

/**
 * Login activity.
 * 
 * @author bgamard
 */
public class LoginActivity extends Activity {

    /**
     * User interface.
     */
    private View loginForm;
    private View progressBar;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);
        
        AQuery aq = new AQuery(this);
        
        final EditText txtUsername = aq.id(R.id.txtUsername).getEditText();
        final EditText txtPassword = aq.id(R.id.txtPassword).getEditText();
        final Button btnConnect = aq.id(R.id.btnConnect).getButton();
        loginForm = aq.id(R.id.loginForm).getView();
        progressBar = aq.id(R.id.progressBar).getView();
        
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        
        loginForm.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        
        // Form validation
        final Validator validator = new Validator(false);
        validator.addValidable(this, txtUsername, new Required());
        validator.addValidable(this, txtPassword, new Required());
        validator.setOnValidationChanged(new CallbackListener() {
            @Override
            public void onComplete() {
                btnConnect.setEnabled(validator.isValidated());
            }
        });
        
        tryConnect();
        
        // Login button
        btnConnect.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                loginForm.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
                
                UserResource.login(getApplicationContext(), txtUsername.getText().toString(), txtPassword.getText().toString(), new SismicsHttpResponseHandler() {
                    @Override
                    public void onSuccess(JSONObject json) {
                        // Empty previous user caches
                        PreferenceUtil.resetUserCache(getApplicationContext());
                        
                        // Getting user info and redirecting to main activity
                        ApplicationContext.getInstance().fetchUserInfo(LoginActivity.this, new CallbackListener() {
                            @Override
                            public void onComplete() {
                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        });
                    }
                    
                    @Override
                    public void onFailure(Throwable t, String response) {
                        loginForm.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.GONE);
                        DialogUtil.showOkDialog(LoginActivity.this, R.string.login_fail_title, R.string.login_fail);
                    }
                });
            }
        });
    }

    /**
     * Try to get a "session".
     */
    private void tryConnect() {
        if (ApplicationContext.getInstance().isLoggedIn() || !ConnectivityUtil.checkConnectivity(this)) {
            // If there is no network connection
            // or we are already connected (from cache data)
            // redirecting to main activity
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        } else {
            // Trying to get user data
            UserResource.info(getApplicationContext(), new SismicsHttpResponseHandler() {
                @Override
                public void onSuccess(final JSONObject json) {
                    if (json.optBoolean("anonymous", true)) {
                        loginForm.setVisibility(View.VISIBLE);
                        return;
                    }
                    
                    // Save user data in application context
                    ApplicationContext.getInstance().setUserInfo(getApplicationContext(), json);
                    
                    // Redirecting to main activity
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                }
                
                @Override
                public void onFailure(Throwable t, String response) {
                    DialogUtil.showOkDialog(LoginActivity.this, R.string.network_error_title, R.string.network_error);
                    loginForm.setVisibility(View.VISIBLE);
                }
                
                @Override
                public void onFinish() {
                    progressBar.setVisibility(View.GONE);
                }
            });
        }
    }
}