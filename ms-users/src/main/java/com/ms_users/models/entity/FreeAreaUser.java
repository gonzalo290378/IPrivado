package com.ms_users.models.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "free_area_users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FreeAreaUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "is_enabled")
    private Boolean isEnabled;

}