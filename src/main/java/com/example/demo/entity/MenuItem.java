package com.example.demo.entity;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class MenuItem {
    @Id
    private Long id;

    private String title;

    private String route;

    private Boolean enabled;

    // getter / setter
}