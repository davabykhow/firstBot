package io.davabykh.molniabot.service;

import io.davabykh.molniabot.config.BotConfig;
import io.davabykh.molniabot.model.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;


@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    @Autowired
    private UserRepository userRepository;
    final String ADMIN_ID;
    final BotConfig config;
    final String TOKEN;
    final String HELP_TEXT = "Нажмите /getcard для получения образца бля бла\n" +
            "Нажмите /getrequisites для получения реквизитов бла бла\n" +
            "Нажмите /adddocument тут видимо какая-то инструкция как прислать документы\n";
    final String START_TEXT = "Добрый день, вас приветсвует \nЕще текст\nКонец приветсвенного текста";
    final String ADD_DOCUMENT_TEXT = "Тут что-то с обьяснением как прислать документ";
    final String REQUISITES_TEXT = "Деньги отправьте на карту Давиду";
    final String ON_SELLING_CARD_TEXT = "Тут может быть подпись под дркументом";
    final String NO_SUPPORTED_TEXT = "Команда не поддерживается\nНажмите /help для отображения функционала бота";
    final String NO_SUPPORTED_ATTACHMENT = "Вложение не поддерживается\nНажмите /help для отображения функционала бота";
    final String LITTLE_HELP_TEXT = "Нажмите /help для отображения функционала бота";
    final String MESSAGE_DELIVERED = "Ваше сообщение отправлено в СМС \"Молния\"";

    public  TelegramBot(BotConfig config){
        this.config = config;
        TOKEN = config.getToken();
        ADMIN_ID = config.getAdminId();
        setMenu();
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken(){
        return TOKEN;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if(update.hasMessage()) {
            Message message = update.getMessage();
            if (message.hasText()) {
                textProcessor(message.getText(), message.getChatId());
            } else if (message.hasPhoto()) {
                photoProcessor(message.getPhoto(), message.getChatId());
            } else {
                sendMessage(update.getMessage().getChatId(), NO_SUPPORTED_ATTACHMENT);
            }
        }
    }

    private void setMenu(){
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/getcard", "Получить шаблон карточки обьекта"));
        listOfCommands.add(new BotCommand("/getrequisites", "Получить реквизиты для оплаты"));
        listOfCommands.add(new BotCommand("/adddocument", "Как прислать документ"));
        listOfCommands.add(new BotCommand("/help", "Справочная информация"));
        try{
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(),null));
            log.info("Successful adding menu");
        }
        catch (TelegramApiException e)
        {
            log.error("Error at adding menu to bot: " + e.getMessage());
        }
    }

    private void textProcessor(String message, long chatId){
        switch (message){
            case "/start":
                sendMessage(chatId, START_TEXT);
                sendMessage(chatId, HELP_TEXT);
                break;
            case "/getcard":
                sendCard(chatId);
                sendMessage(chatId, ON_SELLING_CARD_TEXT);
                sendMessage(chatId, LITTLE_HELP_TEXT);
                break;
            case "/getrequisites":
                sendMessage(chatId, REQUISITES_TEXT);
                sendMessage(chatId, LITTLE_HELP_TEXT);
                break;
            case "/adddocument":
                sendMessage(chatId, ADD_DOCUMENT_TEXT);
                sendMessage(chatId, LITTLE_HELP_TEXT);
                break;
            case "/help":
                sendMessage(chatId, HELP_TEXT);
                break;
            default: sendMessage(chatId, NO_SUPPORTED_TEXT);
        }
    }

    //словить ошибки
    private void photoProcessor(List<PhotoSize> photo, long chatId) {
        SendPhoto sendPhoto = new SendPhoto();

        try{
            String fileId = Objects.requireNonNull(photo.stream().max(Comparator.comparing(PhotoSize::getFileSize))
                    .orElse(null)).getFileId();

            URL url = new URL("https://api.telegram.org/bot" + TOKEN + "/getFile?file_id=" + fileId);

            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String res = in.readLine();
            String filePath = new JSONObject(res).getJSONObject("result").getString("file_path");
            String urlPhoto = "https://api.telegram.org/file/bot" + TOKEN + "/" + filePath;

            URL url2 = new URL(urlPhoto);
            BufferedImage img = ImageIO.read(url2);

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageIO.write(img, "jpg", os);
            InputStream is = new ByteArrayInputStream(os.toByteArray());
            sendPhoto.setPhoto(new InputFile(is, "caption"));
            sendPhoto.setChatId(ADMIN_ID);
            sendPhoto.setCaption("ghdfjhfdgh");
            execute(sendPhoto);
            sendMessage(chatId, MESSAGE_DELIVERED);
        }
        catch (Exception e){
            System.out.println(e.getMessage());
        }
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

    //словить ошибки
    private void sendCard(long chatId){
        try{
        Path path = Paths.get("./src/main/resources/Documents/Card.doc");
        File file = path.toFile();
        SendDocument sd = new SendDocument();
        sd.setDocument(new InputFile().setMedia(file));
        sd.setChatId(chatId);
        sd.setCaption("Приложение_№_2_карточка_объекта_к_Договору.doc");
        execute(sd);
        }
        catch (Exception e){
            System.out.println("vse ploxo");
        }
    }

    private void registration(long charId){

    }


}
