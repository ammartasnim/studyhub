package com.dsi.studyhub.entities;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "clients")
@DiscriminatorValue("CLIENT")
public class Client extends User {

    private String pfp; //URL

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(unique = true, nullable = false)
    private String email;

    private String phone;

    @Column(nullable = false)
    private boolean banned = false;

    @Override
    public boolean isAccountNonLocked() { return !banned; }

    @Override
    public boolean isEnabled() { return !banned; }
}