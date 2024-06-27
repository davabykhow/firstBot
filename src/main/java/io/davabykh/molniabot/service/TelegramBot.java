package io.davabykh.molniabot.service;

import io.davabykh.molniabot.config.BotConfig;
import io.davabykh.molniabot.model.User;
import io.davabykh.molniabot.model.UserRepository;
import io.davabykh.molniabot.model.UsersOnRegistration;
import io.davabykh.molniabot.model.UsersOnRegistrationRepository;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
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
import java.util.*;


@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    private static final Logger log = LoggerFactory.getLogger(TelegramBot.class);
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UsersOnRegistrationRepository usersOnRegistrationRepository;
    final long ADMIN_ID;

    final BotConfig config;
    final String TOKEN;
    final String PATH_TO_REGISTRATION_EXAMPLE_IMAGE = "./src/main/resources/Documents/Registration.jpg";
    final String PATH_TO_REGISTRATION_EXAMPLE_CARD = "./src/main/resources/Documents/Card.doc";
    final String NAME_FOR_REGISTRATION_EXAMPLE_CARD = "Приложение_№_2_карточка_объекта_к_Договору.doc";
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
    final String MESSAGE_DELIVERED = "Ваше сообщение отправлено в СМС \"Молния\" по г.Минску";
    final String GO_TO_REGISTRATION_TEXT = "регайся давай в три строки как в примере";
    final String VERIFY_REGISTRATION1 = "Вы ввели:\n\nРайон: ";
    final String VERIFY_REGISTRATION2 = "\nСим-карта: ";
    final String VERIFY_REGISTRATION3 = "\nКонтактный телефон: ";
    final String VERIFY_REGISTRATION4 = "\n\nДля того чтобы исправить данные отправьте их снова.\nДля подтверждения нажмите /confirmregistration";
    final String RESELL_REGISTRATION_DATA = "\nВведите данные в три строки, как в примере.";
    final String SUCCESSFUL_REGISTRATION_TEXT = "Регистрация прошла успешно.";
    final String SUCCESSFUL_REGISTRATION_TEXT_TO_ADMIN = "Зарегистрирован новый пользователь:\n\n";
    final String CAPTION_UNDER_REGISTRATION_EXAMPLE = "Тест и картинку заменить!!!!!!!!";

    public  TelegramBot(BotConfig config){
        this.config = config;
        TOKEN = config.getToken();
        ADMIN_ID = Long.parseLong(config.getAdminId());
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
            long chatId = message.getChatId();
            if (message.hasText()) {
                if(!userRepository.existsById(chatId)){
                    registration(chatId, message);
                } else {
                    textProcessor(message.getText(), chatId);
                }
            } else if (message.hasPhoto()) {
                photoProcessor(message, chatId, ADMIN_ID);
            } else if(message.hasDocument()){
                documentProcessor(message, message.getChatId(), ADMIN_ID);
            } else {
                sendMessage(chatId, NO_SUPPORTED_ATTACHMENT);
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
                sendCard(chatId, PATH_TO_REGISTRATION_EXAMPLE_CARD, NAME_FOR_REGISTRATION_EXAMPLE_CARD);
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
    //разделить на методы и закинуть половину в фотосендер
    private void photoProcessor(Message message, long senderChatId, long recipientChatId) {
        SendPhoto sendPhoto = new SendPhoto();
        List<PhotoSize> photo = message.getPhoto();
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
            sendPhoto.setChatId(recipientChatId);
            sendPhoto.setCaption(getCaptionForForwardMessage(getInformationAboutUserByChatId(senderChatId),message.getChat().getUserName()));
            execute(sendPhoto);
            sendMessage(senderChatId, MESSAGE_DELIVERED);
        }
        catch (Exception e){
            System.out.println(e.getMessage());
        }
    }

    //ready
    private void sendPhoto(long recipientChatId, String uri, String caption) {
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setPhoto(new InputFile(new File(uri)));
        sendPhoto.setChatId(recipientChatId);
        sendPhoto.setCaption(caption);

        try{
            execute(sendPhoto);
            log.info("Photo has been send to: " + recipientChatId);
        } catch (TelegramApiException e){
            log.error("ERROR while sending photo:" + e.getMessage());
        }
    }

    //ready
    private void sendMessage(long chatId, String textToSend){
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(textToSend);

        try{
            execute(message);
            log.info("Message has been send to: " + chatId);
        } catch (TelegramApiException e){
            log.error("ERROR while sending message:" + e.getMessage());
        }
    }

    //ready
    private void sendCard(long chatId, String stringPath, String caption){
        Path path = Paths.get(stringPath);
        File file = path.toFile();
        SendDocument sd = new SendDocument();
        sd.setDocument(new InputFile().setMedia(file));
        sd.setChatId(chatId);
        sd.setCaption(caption);

        try{
            execute(sd);
            log.info("Card has been send to: " + chatId);
        } catch (TelegramApiException e){
            log.error("ERROR while sending card:" + e.getMessage());
        }
    }

    //empty
    private void sendDocument(long chatId, String stringPath, String caption){
        /*try{
            log.info("Document has been send to: " + chatId);
        } catch (TelegramApiException e){
            log.error("ERROR while sending document:" + e.getMessage());
        }*/
    }

    //развернуть файл без загрузки на сервер (я не умею блять)
    //разделить на методы и закинуть половину в документсендер
    private void documentProcessor(Message message, long senderChatId, long recipientChatId) {

        if (message.hasDocument()){

            String doc_id = message.getDocument().getFileId();
            String doc_name = message.getDocument().getFileName();
            String getID = String.valueOf(message.getFrom().getId());

            GetFile getFile = new GetFile();
            getFile.setFileId(doc_id);

            try {
                org.telegram.telegrambots.meta.api.objects.File file = execute(getFile);
                downloadFile(file, new File("./data/userDoc/"+getID+"_"+doc_name));
                SendDocument sd = new SendDocument();
                sd.setDocument(new InputFile(new File("./data/userDoc/"+getID+"_"+doc_name)));
                sd.setChatId(recipientChatId);
                sd.setCaption(getCaptionForForwardMessage(getInformationAboutUserByChatId(senderChatId),message.getChat().getUserName()));
                execute(sd);
                sendMessage(senderChatId, MESSAGE_DELIVERED);

            } catch (TelegramApiException e) {
                log.error("Error while download and send file: " + e.getMessage());
            }
        }
    }

    //exceptions or ready
    private void registration(long chatId, Message message){
        UsersOnRegistration tempUser = new UsersOnRegistration();
        User userToMainTable = new User();
        tempUser.setChatId(chatId);

        if(!usersOnRegistrationRepository.existsById(chatId)){
            sendMessage(chatId, START_TEXT);
            sendMessage(chatId, GO_TO_REGISTRATION_TEXT);
            sendPhoto(chatId, PATH_TO_REGISTRATION_EXAMPLE_IMAGE, CAPTION_UNDER_REGISTRATION_EXAMPLE);

            usersOnRegistrationRepository.save(tempUser);
        } else {
            if(Objects.equals(message.getText(), "/confirmregistration")){
                if(usersOnRegistrationRepository.existsById(chatId)){

                    tempUser = usersOnRegistrationRepository.findById(chatId).orElse(new UsersOnRegistration());

                    userToMainTable.setContactPhone(tempUser.getContactPhone());
                    userToMainTable.setSim(tempUser.getSim());
                    userToMainTable.setDistrict(tempUser.getDistrict());
                    userToMainTable.setChatId(chatId);

                    userRepository.save(userToMainTable);
                    usersOnRegistrationRepository.deleteById(chatId);

                    sendMessage(chatId, SUCCESSFUL_REGISTRATION_TEXT);
                    sendMessage(chatId, HELP_TEXT);

                    sendMessage(ADMIN_ID, SUCCESSFUL_REGISTRATION_TEXT_TO_ADMIN + getInformationAboutUserByChatId(chatId) +
                            "\n@" + message.getChat().getUserName());
                }
            } else {
                String[] messageText = message.getText().split("\\n");
                if(messageText.length != 3){
                    sendMessage(chatId, RESELL_REGISTRATION_DATA);
                    sendPhoto(chatId, PATH_TO_REGISTRATION_EXAMPLE_IMAGE, CAPTION_UNDER_REGISTRATION_EXAMPLE);
                } else {
                    sendMessage(chatId, VERIFY_REGISTRATION1 + messageText[0] +
                            VERIFY_REGISTRATION2 + messageText[1] + VERIFY_REGISTRATION3 + messageText[2] + VERIFY_REGISTRATION4);
                    tempUser.setDistrict(messageText[0]);
                    tempUser.setSim(messageText[1]);
                    tempUser.setContactPhone(messageText[2]);
                    usersOnRegistrationRepository.save(tempUser);
                }
            }
        }
    }

    //move from here
    private String getInformationAboutUserByChatId (long chatId){
        if(userRepository.existsById(chatId)){
            User user = userRepository.findById(chatId).orElse(new User());
            return "Район: " + user.getDistrict() + "\nSIM-Card: "
                    + user.getSim() + "\nКонтактный телефон: " + user.getContactPhone();
        } else {
            return null;
        }
    }
    //ready
    private String getCaptionForForwardMessage(String personInfo, String userName){
        return "Прислано от:\n\n" + personInfo + "\n@" + userName;
    }

}
