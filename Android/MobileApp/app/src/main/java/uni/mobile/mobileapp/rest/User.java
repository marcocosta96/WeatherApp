package uni.mobile.mobileapp.rest;

import com.google.gson.annotations.Expose;

public class User {
    @Expose
    Integer id;
    @Expose
    String name;
    @Expose
    String provider_token;
    @Expose
    String phone;
    @Expose
    String email;
    @Expose
    String auth_token;

    public Integer getId() {
        return id;
    }

    public User(String name, String provider_token, String phone, String email, String auth_token) {
        this.name = name;
        this.provider_token = provider_token;
        this.phone = phone;
        this.email = email;
        this.auth_token = auth_token;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProvider_token() {
        return provider_token;
    }

    public void setProvider_token(String provider_token) {
        this.provider_token = provider_token;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAuth_token() {
        return auth_token;
    }

    public void setAuth_token(String auth_token) {
        this.auth_token = auth_token;
    }
}
