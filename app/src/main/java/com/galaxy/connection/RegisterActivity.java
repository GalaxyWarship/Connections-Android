package com.galaxy.connection;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.EditText;

import com.galaxy.connection.api.ApiRequest;
import com.galaxy.connection.base.BaseActivity;
import com.galaxy.connection.utils.UUIDUtil;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class RegisterActivity extends BaseActivity {

    private EditText emailView;
    private Disposable subscribe;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_register);
        emailView = (EditText) findViewById(R.id.email);
        View registerView = findViewById(R.id.register);
        registerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkEmail()) {
                    register();
                }
            }
        });
    }

    private boolean checkEmail() {
        return true;//todo
    }

    private void register() {
        String email = emailView.getText().toString();

        subscribe = Observable.just(email)
                .subscribeOn(Schedulers.io())
                .map(new Function<String, ApiRequest.RegisterData>() {
                    @Override
                    public ApiRequest.RegisterData apply(String email) throws Exception {
                        return new ApiRequest.RegisterData(email, UUIDUtil.resetUUID(getApplicationContext()));
                    }
                })
                .flatMap(new Function<ApiRequest.RegisterData, ObservableSource<ApiRequest.RegisterResponse>>() {
                    @Override
                    public ObservableSource<ApiRequest.RegisterResponse> apply(ApiRequest.RegisterData registerData) throws Exception {
                        return ApiRequest.register(registerData);
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<ApiRequest.RegisterResponse>() {
                    @Override
                    public void accept(ApiRequest.RegisterResponse response) throws Exception {

                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                    }
                });
    }

}
