package io.davabykh.molniabot.service;


import io.davabykh.molniabot.config.BotConfig;
import io.davabykh.molniabot.model.User;
import io.davabykh.molniabot.model.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    @Autowired
    private UserRepository userRepository;
    final BotConfig config;
    static final String HELP_TEXT = "Some help text\noh sorry\nfuck u";

    public  TelegramBot(BotConfig config){
        this.config = config;
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "get a welcome message"));
        listOfCommands.add(new BotCommand("/mydata", "get all my data"));
        listOfCommands.add(new BotCommand("/deletedata", "delete all my data"));
        listOfCommands.add(new BotCommand("/help", "get help"));
        listOfCommands.add(new BotCommand("/settings", "set your properties"));
        try{
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(),null));
        }
        catch (TelegramApiException e)
        {
            log.error("Error at adding menu to bot: " + e.getMessage());
        }


    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken(){
        return config.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if(update.hasMessage() && update.getMessage().hasText()){
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            String userFirstName = update.getMessage().getChat().getFirstName();

            switch (messageText){
                case "/start":
                    registerUser(update.getMessage());
                    startCommandReceive(chatId, userFirstName);
                    break;
                case "/help":
                    sendMessage(chatId, HELP_TEXT);
                    break;
                default: sendMessage(chatId, "fuck u");
            }
        }

    }

    private void startCommandReceive(long chatId, String userFirstName){
        String answer = "Hi, " + userFirstName + ", nice to meet u!";
        sendMessage(chatId, answer);
    }

    private void sendMessage(long chatId, String textToSend){
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(textToSend);

        try{
            execute(message);
            log.info("Reply to user " + chatId + ": " + textToSend);
        }
        catch (TelegramApiException e){
            log.error("Error:" + e.getMessage());
        }
    }

    private void registerUser(Message message){
        if(!userRepository.existsById(message.getChatId())){
            Chat chat = message.getChat();

            User user = new User();
            user.setUserName(chat.getUserName());
            user.setChatId(chat.getId());
            user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));

            userRepository.save(user);

            log.info("User save: " + chat.getId());
        }
    }
}
