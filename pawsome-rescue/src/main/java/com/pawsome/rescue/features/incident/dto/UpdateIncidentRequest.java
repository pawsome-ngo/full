package com.pawsome.rescue.features.incident.dto;
import com.pawsome.rescue.features.incident.entity.Incident;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UpdateIncidentRequest {

    @NotBlank(message = "Informer name cannot be blank")
    @Size(max = 100, message = "Informer name is too long")
    private String informerName;

    @NotBlank(message = "Contact number cannot be blank")
    @Pattern(regexp = "^\\d{10}$", message = "Contact number must be 10 digits")
    private String contactNumber;

    @NotNull(message = "Animal type cannot be null")
    private Incident.AnimalType animalType; // Use your Enum here

    @NotBlank(message = "Description cannot be blank")
    @Size(max = 1000, message = "Description is too long")
    private String description;

    @NotBlank(message = "Location description cannot be blank")
    @Size(max = 255, message = "Location description is too long")
    private String location;

    // Latitude and Longitude are optional
    private Double latitude;
    private Double longitude;

    // Getters and Setters
    public String getInformerName() {
        return informerName;
    }

    public void setInformerName(String informerName) {
        this.informerName = informerName;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public Incident.AnimalType getAnimalType() {
        return animalType;
    }

    public void setAnimalType(Incident.AnimalType animalType) {
        this.animalType = animalType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
}