package com.aria.entity;

import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;

@Entity
@Table(name="suppliers")
public class Supplier {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(nullable=false,length=160)
    private String name;
    @Column(length=160)
    private String contact;
    @Column(length=1000)
    private String notes;
    @Column(nullable=false)
    private boolean active = true;
    public Supplier() {}
    public Long getId() { return id; }
    public void setId(Long value) { id = value; }
    public String getName() { return name; }
    public void setName(String value) { name = value; }
    public String getContact() { return contact; }
    public void setContact(String value) { contact = value; }
    public String getNotes() { return notes; }
    public void setNotes(String value) { notes = value; }
    public boolean getActive() { return active; }
    public void setActive(boolean value) { active = value; }
}
