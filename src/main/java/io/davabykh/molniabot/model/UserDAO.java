package io.davabykh.molniabot.model;

import io.davabykh.molniabot.config.BotConfig;
import io.davabykh.molniabot.service.TelegramBot;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.Query;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.NoSuchElementException;

import static io.davabykh.molniabot.service.TelegramBot.NOMENCLATURE;

@Component
public class UserDAO {

    @Autowired
    UserRepository userRepository;


    public User getUserByUserName(String userName){
        try (Session session = HibernateUtil.getSession()) {
            String hql = "from users where userName = '" + userName + "'";
            Query<User> query = session.createQuery(hql , User.class);
            try{
                return query.list().getFirst();
            } catch (NoSuchElementException e){
                return null;
            }
        }
    }

    public String getInformationAboutUserByContractNumber (String contractNumber){
        User user;
        try (Session session = HibernateUtil.getSession()) {
            String hql = "from users where contractNumber = '" + contractNumber + "'";
            Query<User> query = session.createQuery(hql , User.class);
            try {
                user = query.list().getFirst();
                return "Организация: " + user.getOrganization() + "\nАдрес обьекта: " + user.getAddress() + "\nНомер договора: " +
                        NOMENCLATURE + user.getContractNumber() + "\nSIM-Card: "
                        + user.getSIM() + "\nКонтактный телефон: " + user.getPhoneNumber() +
                        "\n@" + user.getUserName();
            } catch (NoSuchElementException e){
                return "Нет пользователей с таким номером договора";
            }
        }
    }

    public boolean isUserBanned (Long chatId){
        if(userRepository.existsById(chatId)) {
            User user = userRepository.findById(chatId).orElse(new User());
            return user.isBanned();
        }else {
            return false;
        }
    }
}
