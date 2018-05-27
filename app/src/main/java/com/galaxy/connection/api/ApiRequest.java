package com.galaxy.connection.api;

import com.galaxy.connection.Const;

import java.io.IOException;

import io.reactivex.Observable;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;

public class ApiRequest {

    public interface Register {
        @POST("/register")
        Observable<RegisterResponse> register(@Body RegisterData data);
    }

    public interface Login {
        @POST("/login")
        Observable<LoginResponse> login(@Body LoginData data);
    }

    public static Observable<RegisterResponse> register(RegisterData data) throws IOException {

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Const.API_SERVER)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .build();
        Register register = retrofit.create(Register.class);

        return register.register(data);
    }

    public static Observable<LoginResponse> login(LoginData data) throws IOException {

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Const.API_SERVER)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .build();
        Login login = retrofit.create(Login.class);

        return login.login(data);
    }

    public static class RegisterData {
        public final String email;
        public final String uuid;

        public RegisterData(String email, String uuid) {
            this.email = email;
            this.uuid = uuid;
        }
    }

    public static class RegisterResponse {

        private int errorCode;

        public int getErrorCode() {
            return errorCode;
        }

        public void setErrorCode(int errorCode) {
            this.errorCode = errorCode;
        }
    }

    public static class LoginData {
        public final String email;

        public final String uuid;
        public LoginData(String email, String uuid) {
            this.email = email;
            this.uuid = uuid;
        }

    }

    public static class LoginResponse {

        private int errorCode;

        public int getErrorCode() {
            return errorCode;
        }

        public void setErrorCode(int errorCode) {
            this.errorCode = errorCode;
        }
    }
}
