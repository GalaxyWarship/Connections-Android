package com.galaxy.connection;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.galaxy.connection.api.ApiRequest;
import com.galaxy.connection.base.BaseActivity;
import com.galaxy.connection.utils.UUIDUtil;
import com.wang.avi.AVLoadingIndicatorView;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class WelcomeActivity extends BaseActivity {

    private AVLoadingIndicatorView loadingView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_welcome);

        loadingView = (AVLoadingIndicatorView) findViewById(R.id.loading);

        registerOrLogin();
    }

    @Override
    protected void onPause() {
        super.onPause();
        loadingView.hide();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadingView.show();
    }

    private void registerOrLogin() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String email = preferences.getString("email", null);
        if (TextUtils.isEmpty(email)) {
            gotoRegister();
            return;
        } else {
            login(email);
        }
    }

    private void login(String email) {
        Observable.just(email)
                .subscribeOn(Schedulers.io())
                .map(new Function<String, ApiRequest.LoginData>() {
                    @Override
                    public ApiRequest.LoginData apply(String email) throws Exception {
                        return new ApiRequest.LoginData(email, UUIDUtil.getUUID(getApplicationContext()));
                    }
                })
                .flatMap(new Function<ApiRequest.LoginData, ObservableSource<ApiRequest.LoginResponse>>() {
                    @Override
                    public ObservableSource<ApiRequest.LoginResponse> apply(ApiRequest.LoginData loginData) throws Exception {
                        return ApiRequest.login(loginData);
                    }
                }).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<ApiRequest.LoginResponse>() {
                    @Override
                    public void accept(ApiRequest.LoginResponse loginResponse) throws Exception {
                        gotoContacts();
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                    }
                });
    }

    private void gotoContacts() {
        startActivity(new Intent(this, ContactsActivity.class));
        finish();
    }

    private void gotoRegister() {
        startActivity(new Intent(this, RegisterActivity.class));
        finish();
    }
}
