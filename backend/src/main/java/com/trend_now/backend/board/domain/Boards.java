package com.trend_now.backend.board.domain;

import com.trend_now.backend.config.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "boards", uniqueConstraints = {
        @UniqueConstraint(name = "uk_name", columnNames = {"name"})
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Getter
public class Boards extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "board_id")
    private Long id;

    @Column(nullable = false)
    private String name;

    private String summary;

    @Enumerated(EnumType.STRING)
    private BoardCategory boardCategory;

    @Builder.Default
    @Column(nullable = false)
    private boolean deleted = false;

    public void changeDeleted() {
        this.deleted = !this.deleted;
    }
}
