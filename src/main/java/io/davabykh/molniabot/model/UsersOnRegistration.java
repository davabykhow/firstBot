package io.davabykh.molniabot.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity(name = "usersOnRegistration")
public class UsersOnRegistration {

    @Id
    private Long chatId;
    private String district;

    private String sim;

    private String contactPhone;

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getSim() {
        return sim;
    }

    public void setSim(String sim) {
        this.sim = sim;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }
}
