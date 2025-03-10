package com.trend_now.backend.user.domain;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(nullable = false, name = "name")
    private String name;

    @Column(nullable = false, unique = true, name = "email")
    private String email;

    @Column(nullable = false, unique = true, name = "provider")
    private String provider;

    @Column(nullable = false, unique = true, name = "sns_id")
    private String snsId;
}
