package io.davabykh.molniabot.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;


@Entity(name = "users")
public class User {

    @Id
    private Long chatId;

    private String userName;

    private String organization;

    private String address;

    private String contractNumber;

    private String SIM;

    private String phoneNumber;

    private boolean onRegistration;

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getContractNumber() {
        return contractNumber;
    }

    public void setContractNumber(String contractNumber) {
        this.contractNumber = contractNumber;
    }

    public String getSIM() {
        return SIM;
    }

    public void setSIM(String SIM) {
        this.SIM = SIM;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public boolean isOnRegistration() {
        return onRegistration;
    }

    public void setOnRegistration(boolean onRegistration) {
        this.onRegistration = onRegistration;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
