package edu.automarket.listing.dto;

import edu.automarket.listing.model.BodyType;
import edu.automarket.listing.model.CarBrand;
import edu.automarket.listing.model.CarColor;
import edu.automarket.listing.model.CarCondition;
import edu.automarket.listing.model.City;
import edu.automarket.listing.model.DriveType;
import edu.automarket.listing.model.FuelType;
import edu.automarket.listing.model.TransmissionType;
import org.hibernate.validator.constraints.Length;

import java.util.List;

public final class GetPublishedListingsRequestDTO {
    private Long publishedBefore;
    private int offset;
    private int size = 20;

    // filters
    @Length(max = 200)
    private String query;
    private List<CarBrand> brand;
    private List<CarCondition> condition;
    private Integer mileageMin;
    private Integer mileageMax;
    private Long priceMin;
    private Long priceMax;
    private List<City> city;
    private List<CarColor> color;
    private List<TransmissionType> transmission;
    private List<FuelType> fuelType;
    private Double tankVolumeMin;
    private Double tankVolumeMax;
    private List<DriveType> driveType;
    private List<BodyType> bodyType;
    private Integer yearMin;
    private Integer yearMax;
    private Double engineVolumeMin;
    private Double engineVolumeMax;
    private List<Integer> ownersCount;

    public long getPublishedBefore() {
        if (publishedBefore == null) {
            publishedBefore = System.currentTimeMillis();
        }
        return publishedBefore;
    }

    public void setPublishedBefore(long publishedBefore) {
        this.publishedBefore = publishedBefore;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public boolean hasQuery() {
        return query != null && !query.isBlank();
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public List<CarBrand> getBrand() {
        return brand;
    }

    public void setBrand(List<CarBrand> brand) {
        this.brand = brand;
    }

    public List<CarCondition> getCondition() {
        return condition;
    }

    public void setCondition(List<CarCondition> condition) {
        this.condition = condition;
    }

    public Integer getMileageMin() {
        return mileageMin;
    }

    public void setMileageMin(Integer mileageMin) {
        this.mileageMin = mileageMin;
    }

    public Integer getMileageMax() {
        return mileageMax;
    }

    public void setMileageMax(Integer mileageMax) {
        this.mileageMax = mileageMax;
    }

    public Long getPriceMin() {
        return priceMin;
    }

    public void setPriceMin(Long priceMin) {
        this.priceMin = priceMin;
    }

    public Long getPriceMax() {
        return priceMax;
    }

    public void setPriceMax(Long priceMax) {
        this.priceMax = priceMax;
    }

    public List<City> getCity() {
        return city;
    }

    public void setCity(List<City> city) {
        this.city = city;
    }

    public List<CarColor> getColor() {
        return color;
    }

    public void setColor(List<CarColor> color) {
        this.color = color;
    }

    public List<TransmissionType> getTransmission() {
        return transmission;
    }

    public void setTransmission(List<TransmissionType> transmission) {
        this.transmission = transmission;
    }

    public List<FuelType> getFuelType() {
        return fuelType;
    }

    public void setFuelType(List<FuelType> fuelType) {
        this.fuelType = fuelType;
    }

    public Double getTankVolumeMin() {
        return tankVolumeMin;
    }

    public void setTankVolumeMin(Double tankVolumeMin) {
        this.tankVolumeMin = tankVolumeMin;
    }

    public Double getTankVolumeMax() {
        return tankVolumeMax;
    }

    public void setTankVolumeMax(Double tankVolumeMax) {
        this.tankVolumeMax = tankVolumeMax;
    }

    public List<DriveType> getDriveType() {
        return driveType;
    }

    public void setDriveType(List<DriveType> driveType) {
        this.driveType = driveType;
    }

    public List<BodyType> getBodyType() {
        return bodyType;
    }

    public void setBodyType(List<BodyType> bodyType) {
        this.bodyType = bodyType;
    }

    public Integer getYearMin() {
        return yearMin;
    }

    public void setYearMin(Integer yearMin) {
        this.yearMin = yearMin;
    }

    public Integer getYearMax() {
        return yearMax;
    }

    public void setYearMax(Integer yearMax) {
        this.yearMax = yearMax;
    }

    public Double getEngineVolumeMin() {
        return engineVolumeMin;
    }

    public void setEngineVolumeMin(Double engineVolumeMin) {
        this.engineVolumeMin = engineVolumeMin;
    }

    public Double getEngineVolumeMax() {
        return engineVolumeMax;
    }

    public void setEngineVolumeMax(Double engineVolumeMax) {
        this.engineVolumeMax = engineVolumeMax;
    }

    public List<Integer> getOwnersCount() {
        return ownersCount;
    }

    public void setOwnersCount(List<Integer> ownersCount) {
        this.ownersCount = ownersCount;
    }
}
