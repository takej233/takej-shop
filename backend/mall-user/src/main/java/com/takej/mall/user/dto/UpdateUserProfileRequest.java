package com.takej.mall.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 修改用户资料的请求参数。
 */
public class UpdateUserProfileRequest {

    /** 用户昵称 */
    @NotBlank(message = "nickname cannot be blank")
    @Size(max = 50, message = "nickname cannot exceed 50 characters")
    private String nickname;

    /** 头像地址 */
    @Size(max = 255, message = "avatar cannot exceed 255 characters")
    private String avatar;

    /** 性别：0未知，1男，2女 */
    private Integer gender;

    /** 手机号 */
    @Size(max = 20, message = "phone cannot exceed 20 characters")
    private String phone;

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public Integer getGender() {
        return gender;
    }

    public void setGender(Integer gender) {
        this.gender = gender;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}