package com.shut.profile.entity;

import com.shut.user.entity.User;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "professional_profiles")
public class ProfessionalProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "display_name", length = 150)
    private String displayName;

    @Column(length = 180)
    private String headline;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "years_experience", nullable = false)
    private Integer yearsExperience = 0;

    @Column(name = "base_city", length = 100)
    private String baseCity;

    @Column(name = "base_country", length = 100)
    private String baseCountry;

    @Column(name = "website_url", length = 500)
    private String websiteUrl;

    @Column(name = "instagram_username", length = 100)
    private String instagramUsername;

    @Column(name = "equipment_description", columnDefinition = "TEXT")
    private String equipmentDescription;

    @ElementCollection
    @CollectionTable(name = "professional_profile_skills", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "skill", nullable = false, length = 100)
    private Set<String> skills = new LinkedHashSet<>();

    @ElementCollection
    @CollectionTable(name = "professional_profile_service_cities", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "city", nullable = false, length = 100)
    private Set<String> serviceCities = new LinkedHashSet<>();

    @ElementCollection
    @CollectionTable(name = "professional_profile_collaborators", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "collaborator", nullable = false, length = 150)
    private Set<String> collaborators = new LinkedHashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected ProfessionalProfile() { }

    public ProfessionalProfile(User user) {
        this.user = user;
    }

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() { updatedAt = OffsetDateTime.now(); }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getHeadline() { return headline; }
    public void setHeadline(String headline) { this.headline = headline; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public Integer getYearsExperience() { return yearsExperience; }
    public void setYearsExperience(Integer yearsExperience) { this.yearsExperience = yearsExperience; }
    public String getBaseCity() { return baseCity; }
    public void setBaseCity(String baseCity) { this.baseCity = baseCity; }
    public String getBaseCountry() { return baseCountry; }
    public void setBaseCountry(String baseCountry) { this.baseCountry = baseCountry; }
    public String getWebsiteUrl() { return websiteUrl; }
    public void setWebsiteUrl(String websiteUrl) { this.websiteUrl = websiteUrl; }
    public String getInstagramUsername() { return instagramUsername; }
    public void setInstagramUsername(String instagramUsername) { this.instagramUsername = instagramUsername; }
    public String getEquipmentDescription() { return equipmentDescription; }
    public void setEquipmentDescription(String equipmentDescription) { this.equipmentDescription = equipmentDescription; }
    public Set<String> getSkills() { return Set.copyOf(skills); }
    public void setSkills(Set<String> values) { skills.clear(); if (values != null) skills.addAll(values); }
    public Set<String> getServiceCities() { return Set.copyOf(serviceCities); }
    public void setServiceCities(Set<String> values) { serviceCities.clear(); if (values != null) serviceCities.addAll(values); }
    public Set<String> getCollaborators() { return Set.copyOf(collaborators); }
    public void setCollaborators(Set<String> values) { collaborators.clear(); if (values != null) collaborators.addAll(values); }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
