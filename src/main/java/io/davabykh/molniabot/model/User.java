package io.davabykh.molniabot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;


@Entity(name = "users")
public class User {

    @Id
    @Column(name = "chat_id")
    private Long chatId;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "organization")
    private String organization;

    @Column(name = "address")
    private String address;

    @Column(name = "contract_number")
    private String contractNumber;

    @Column(name = "sim")
    private String SIM;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "on_registration")
    private boolean onRegistration;

    @Column(name = "on_direct_mode")
    private boolean onDirectMode;

    @Column(name = "banned")
    private boolean banned;

    public boolean isBanned() {
        return banned;
    }

    public void setBanned(boolean banned) {
        this.banned = banned;
    }

    public boolean isOnDirectMode() {
        return onDirectMode;
    }

    public void setOnDirectMode(boolean onDirectMode) {
        this.onDirectMode = onDirectMode;
    }

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
