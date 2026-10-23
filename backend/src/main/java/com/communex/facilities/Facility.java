
package com.communex.facilities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name="facilities", indexes={
    @Index(name="idx_facility_category_active", columnList="category, active"),
    @Index(name="idx_facility_name", columnList="name")
})
public class Facility {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, length=160)
    private String name;

    @Column(nullable=false, length=60)
    private String category;

    @Column(length=2000)
    private String description;

    @Column(length=500)
    private String address;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column(length=80)
    private String phone;

    @Column(length=300)
    private String website;

    @Column(length=500)
    private String openingInformation;

    @Column(nullable=false)
    private boolean verified = false;

    @Column(nullable=false)
    private boolean active = true;

    @Column(nullable=false, updatable=false)
    private LocalDateTime createdAt;

    @Column(nullable=false)
    private LocalDateTime updatedAt;

    public Facility() {}

    @PrePersist
    public void create() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    public void update() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String v) { name = v; }
    public String getCategory() { return category; }
    public void setCategory(String v) { category = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { description = v; }
    public String getAddress() { return address; }
    public void setAddress(String v) { address = v; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double v) { latitude = v; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double v) { longitude = v; }
    public String getPhone() { return phone; }
    public void setPhone(String v) { phone = v; }
    public String getWebsite() { return website; }
    public void setWebsite(String v) { website = v; }
    public String getOpeningInformation() { return openingInformation; }
    public void setOpeningInformation(String v) { openingInformation = v; }
    public boolean isVerified() { return verified; }
    public void setVerified(boolean v) { verified = v; }
    public boolean isActive() { return active; }
    public void setActive(boolean v) { active = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
