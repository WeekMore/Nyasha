package com.nyasha.server;

/**
 * @author yuxiangll
 * @since 2024/1/8 07:37
 * IntelliJ IDEA
 */

public record  User(String username, String password) {

    @Override
    public String toString() {
        return "User{" +
                "username='" + username + '\'' +
                ", password='" + password + '\'' +
                '}';
    }

}
