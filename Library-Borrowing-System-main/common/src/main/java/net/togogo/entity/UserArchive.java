package net.togogo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.envers.Audited;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Audited
@Table(name= "t_user_archive")//对应数据库中的表名
public class UserArchive {
    @Id
    private Long id;//保留id，可能会有逻辑错误
     @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 100)
    private String password;

    private String email;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @Column(name = "delete_time",nullable = false)//nullable = false 表示该字段不能为空，不能为 null
    private LocalDateTime deleteTime; // 删除时间

    @Column(name = "phone_number",nullable = false,unique = true,length = 11)
    private String phone;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "DELETED";
    
    public static UserArchive fromUser(User user) {
        return UserArchive.builder()
                .id(user.getId())
                .username(user.getUsername())
                .password(user.getPassword())
                .email(user.getEmail())
                .createTime(user.getCreateTime())
                .updateTime(LocalDateTime.now())
                .deleteTime(LocalDateTime.now())
                .phone(user.getPhone())
                .build();
    }

    public User toUser(){
        return User.builder()
                .id(this.id)
                .username(this.username)
                .password(this.password)
                .email(this.email)
                .createTime(this.createTime)
                .phone(this.phone)
                .build();
    }

    public void markAsRestored() {
        this.status = "RESTORED";
    }
}
