package net.togogo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在6到20个字符之间")
    private String newPassword;
}