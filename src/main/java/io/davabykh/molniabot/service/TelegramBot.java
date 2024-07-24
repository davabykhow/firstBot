package io.davabykh.molniabot.service;

import io.davabykh.molniabot.config.BotConfig;
import io.davabykh.molniabot.model.*;
import io.davabykh.molniabot.model.User;
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
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
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
    public static final String NOMENCLATURE = "47/23-15/254/84512";
    private AdminCondition adminCondition = AdminCondition.NOTHING;
    @Autowired
    private UserDAO userDAO;

    final BotConfig config;
    final String TOKEN;
    final String PATH_TO_REGISTRATION_EXAMPLE_IMAGE = "./src/main/resources/Documents/Registration.jpg";
    final String PATH_TO_REGISTRATION_EXAMPLE_CARD = "./src/main/resources/Documents/Card.doc";
    final String NAME_FOR_REGISTRATION_EXAMPLE_CARD = "Приложение_№_2_карточка_объекта_к_Договору.doc";
    final String HELP_TEXT_REGISTERED = "Текст вспомогательный для зарегистрированных пользователей";
    final String HELP_TEXT_NOT_REGISTERED = "Текст вспомогательный для незарегистрированных пользователей";
    final String START_TEXT = "Добрый день, вас приветсвует \nЕще текст\nКонец приветсвенного текста";
    final String ADD_DOCUMENT_TEXT = "Тут что-то с обьяснением как прислать документ";
    final String REQUISITES_TEXT = "Деньги отправьте на карту Давиду";
    final String ON_SELLING_CARD_TEXT = "Тут может быть подпись под дркументом";
    final String NO_SUPPORTED_TEXT = "Команда не поддерживается\nНажмите /help для отображения функционала бота";
    final String NO_SUPPORTED_ATTACHMENT = "Вложение не поддерживается\nНажмите /help для отображения функционала бота";
    final String LITTLE_HELP_TEXT = "Нажмите /help для отображения функционала бота";
    final String MESSAGE_DELIVERED = "Ваше сообщение отправлено в СМС \"Молния\" по г.Минску, ожидайте ответа";
    final String GO_TO_REGISTRATION_TEXT = "Укажите некоторую информацию для оператора";
    final String VERIFY_REGISTRATION1 = "Вы ввели:\n\nРайон: ";
    final String VERIFY_REGISTRATION2 = "\nСим-карта: ";
    final String VERIFY_REGISTRATION3 = "\nКонтактный телефон: ";
    final String VERIFY_REGISTRATION4 = "\n\nДля того чтобы исправить данные отправьте их снова.\nДля подтверждения нажмите /confirmregistration";
    final String RESELL_REGISTRATION_DATA = "\nВведите данные в три строки, как в примере.";
    final String SUCCESSFUL_REGISTRATION_TEXT = "Регистрация прошла успешно. Спасибо!";
    final String SUCCESSFUL_REGISTRATION_TEXT_TO_ADMIN = "Зарегистрирован новый пользователь:\n\n";
    final String CAPTION_UNDER_REGISTRATION_EXAMPLE = "Тест и картинку заменить!!!!!!!!";
    final String START_DIRECT_MODE_TEXT = "Опишите чем вам помочь, специалист ответит вам в этом чате или свяжется лично.\nТакже вы можете прислать вложения.";
    final String EXIT_FROM_DIRECT_MODE_TEXT = "Если вам необходимо что-то добавить, воспользуйтесь кнопкой связи с оператором повторно";
    final String NOT_REGISTERED_SEND_MEDIA = "Для того чтобы отправить вложение или медиафайл специалисту, Вам необходимо указать от имени какой организации Вы пишите";
    final String USER_IS_BANED = "Внесено в список блокировки следующее имя пользователя @";
    final String NO_SUCH_MATCHES = "Совпадения не найдены:(";
    final String IS_BANNED_TEXT = "Доступ к этому чату для вас заблокирован, если это ошибка позвоните по номеру 112";

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

            if (userDAO.isUserBanned(chatId)){
                sendMessage(chatId, IS_BANNED_TEXT);
                return;
            }

            if(chatId == ADMIN_ID){
                adminTextProcessor(message);
                sendMessage(ADMIN_ID, "admin text", getAdminDefaultInlineKeyboardMarkup());
            } else if (message.hasText()) {
                textProcessor(message, chatId);
            } else if (message.hasPhoto()) {
                User user = userRepository.findById(chatId).orElse(new User());
                if(userRepository.existsById(chatId) && !user.isOnRegistration()){
                    photoProcessor(message, chatId, ADMIN_ID);
                } else {
                    sendMessage(chatId, NOT_REGISTERED_SEND_MEDIA, getNotRegisteredSendMediaInlineKeyboardMarkup());
                }
            } else if(message.hasDocument()){
                User user = userRepository.findById(chatId).orElse(new User());
                if(userRepository.existsById(chatId) && !user.isOnRegistration()){
                    documentProcessor(message, chatId, ADMIN_ID);
                } else {
                    sendMessage(chatId, NOT_REGISTERED_SEND_MEDIA, getNotRegisteredSendMediaInlineKeyboardMarkup());
                }
            }

            /*if (message.hasText()) {
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
            }*/
        } else if (update.hasCallbackQuery()) {
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            if (userDAO.isUserBanned(chatId)){
                sendMessage(chatId, IS_BANNED_TEXT);
                return;
            }

            if(update.getCallbackQuery().getMessage().getChatId() == ADMIN_ID){
                adminCallBackTextProcessor(update);
            } else {
                callBackTextProcessor(update);
            }
        }
    }

    private void setMenu(){
        List<BotCommand> listOfCommands = new ArrayList<>();
        /*listOfCommands.add(new BotCommand("/getcard", "Получить шаблон карточки обьекта"));
        listOfCommands.add(new BotCommand("/getrequisites", "Получить реквизиты для оплаты"));
        listOfCommands.add(new BotCommand("/adddocument", "Как прислать документ"));*/
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

    private void adminCallBackTextProcessor(Update update){
        String callBackText = update.getCallbackQuery().getData();
        switch (callBackText) {
            case "/find_by_contract_num":
                sendMessage(ADMIN_ID, "Введите номер договора без " + NOMENCLATURE);
                adminCondition = AdminCondition.FIND_USER_BY_CONTRACT_NUMBER;
                break;
            case "/ban":
                sendMessage(ADMIN_ID, "Введите имя пользователя без \\'@\\' для бана");
                adminCondition = AdminCondition.BAN_USER;
                break;
        }
    }

    private void callBackTextProcessor(Update update){
        String callBackText = update.getCallbackQuery().getData();
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        switch (callBackText){
            case "/getCard":
            case "Получить шаблон карточки обьекта":
                sendCard(chatId, PATH_TO_REGISTRATION_EXAMPLE_CARD, NAME_FOR_REGISTRATION_EXAMPLE_CARD);
                sendMessage(chatId, ON_SELLING_CARD_TEXT, getDefaultInlineKeyboardMarkup());
                break;
            case "/getRequisites":
            case "Получить реквизиты для оплаты":
                sendMessage(chatId, REQUISITES_TEXT, getDefaultInlineKeyboardMarkup());
                break;
            case "/communication":
                User user = userRepository.findById(chatId).orElse(new User());
                if(!userRepository.existsById(chatId) || user.isOnRegistration()){
                    sendMessage(chatId, GO_TO_REGISTRATION_TEXT);
                    /*может рухнуть*/createNewUserByChatId(chatId, update.getCallbackQuery().getMessage().getChat().getUserName());
                    sendMessage(chatId, "Введите название организации");
                } else {
                    startDirectMode(chatId);
                }
                break;
            case "/confirmRegistration":
                sendMessage(chatId, SUCCESSFUL_REGISTRATION_TEXT);
                sendMessage(ADMIN_ID, SUCCESSFUL_REGISTRATION_TEXT_TO_ADMIN + getInformationAboutUserByChatId(chatId));
                startDirectMode(chatId);
                break;
            case "/reRegistration":
                /*может рухнуть*/createNewUserByChatId(chatId, update.getCallbackQuery().getMessage().getChat().getUserName());
                sendMessage(chatId, "Введите название организации");
                break;
        }
    }

    private void textProcessor(Message message, long chatId){
        switch (message.getText()){
            case "/start":
                sendMessage(chatId, START_TEXT, getDefaultInlineKeyboardMarkup());
                break;
            /* case "/getcard":
            case "Получить шаблон карточки обьекта":
                sendCard(chatId, PATH_TO_REGISTRATION_EXAMPLE_CARD, NAME_FOR_REGISTRATION_EXAMPLE_CARD);
                sendMessage(chatId, ON_SELLING_CARD_TEXT, getDefaultInlineKeyboardMarkup());
                break;
            case "/getrequisites":
            case "Получить реквизиты для оплаты":
                sendMessage(chatId, REQUISITES_TEXT, getDefaultInlineKeyboardMarkup());
                break;
            case "/adddocument":
                sendMessage(chatId, ADD_DOCUMENT_TEXT, getDefaultInlineKeyboardMarkup());
                break;*/
            case "/help":
                if(userRepository.existsById(chatId)){
                    sendMessage(chatId, HELP_TEXT_REGISTERED);
                } else {
                    sendMessage(chatId, HELP_TEXT_NOT_REGISTERED);
                }
                break;
            default:
                if(userRepository.existsById(chatId)){
                    User user = userRepository.findById(chatId).orElse(new User());
                    if(user.isOnRegistration()) {
                        continueRegistration(chatId, message.getText());
                        } else if (user.isOnDirectMode()) {
                        sendMessageToAdminFromDirectMode(chatId, message);
                        disableDirectMode(chatId);
                    }
                } else {
                    sendMessage(chatId, NO_SUPPORTED_TEXT, getDefaultInlineKeyboardMarkup());
                }

        }
    }

    private void adminTextProcessor(Message message){
        String messageText = message.getText();
        switch (adminCondition){
            case BAN_USER:
                if (banUserByUserName(messageText)) {
                    sendMessage(ADMIN_ID, USER_IS_BANED + messageText);
                } else {
                    sendMessage(ADMIN_ID, NO_SUCH_MATCHES + messageText);
                }
                adminCondition = AdminCondition.NOTHING;
                break;
            case FIND_USER_BY_CONTRACT_NUMBER:
                sendMessage(ADMIN_ID, userDAO.getInformationAboutUserByContractNumber(messageText));
                adminCondition = AdminCondition.NOTHING;
                break;
        }
    }

    private void sendMessageToAdminFromDirectMode(long chatId, Message message){
        sendMessage(ADMIN_ID, message.getText() + getCaptionForForwardMessage(getInformationAboutUserByChatId(chatId), message.getChat().getUserName()));
        sendMessage(chatId, MESSAGE_DELIVERED);
    }

    private void startDirectMode(long chatId){
        sendMessage(chatId, START_DIRECT_MODE_TEXT, getInlineKeyboardMarkupWithoutCommunicationButton());
        User user = userRepository.findById(chatId).orElse(new User());
        if(!user.isOnDirectMode()) {
            user.setOnDirectMode(true);
            userRepository.save(user);
        } else {
            if(user.getChatId() == null){
                log.error("NO USER ID TO CHANGE IT TO DIRECT MODE");
            }
            if(user.isOnDirectMode()){
                log.error("CALL startDirectMode TO USER IN DIRECT MODE " + chatId);
            }
        }
    }

    private void disableDirectMode(long chatId){
        User user = userRepository.findById(chatId).orElse(new User());
        if(user.isOnDirectMode()) {
            user.setOnDirectMode(false);
            userRepository.save(user);
        } else {
            if(user.getChatId() == null){
                log.error("NO USER ID TO CHANGE IT FROM DIRECT MODE");
            }
            if(!user.isOnDirectMode()){
                log.error("CALL disableDirectMode TO USER NOT IN DIRECT MODE " + chatId);
            }
        }
        sendMessage(chatId, EXIT_FROM_DIRECT_MODE_TEXT, getDefaultInlineKeyboardMarkup());
    }

    private void createNewUserByChatId(long chatId, String userName){
        User user = new User();
        user.setChatId(chatId);
        user.setOnRegistration(true);
        user.setUserName(userName);
        userRepository.save(user);
    }

    //валидаторы
    private void continueRegistration(long chatId, String text){
        User user = userRepository.findById(chatId).orElse(new User());
        if(user.isOnRegistration()){
            if(user.getOrganization() == null){
                user.setOrganization(text);
                userRepository.save(user);
                sendMessage(chatId, "Введите адрес");
           } else if (user.getAddress() == null) {
                user.setAddress(text);
                userRepository.save(user);
                sendMessage(chatId, "Введите номер договора без номенклатуры\nВсё, что после " + NOMENCLATURE);
            } else if (user.getContractNumber() == null) {
                //проверка что нет номенклатуры
                user.setContractNumber(text);
                userRepository.save(user);
                sendMessage(chatId, "Введите номер сим-карты");
            } else if (user.getSIM() == null) {
                user.setSIM(text);
                userRepository.save(user);
                sendMessage(chatId, "Введите ваш контактный телефон");
            } else if (user.getPhoneNumber() == null) {
                //проверка номера телефона
                user.setPhoneNumber(text);
                user.setOnRegistration(false);
                userRepository.save(user);
                sendMessage(chatId, "Предоставленная вами информация:\n\n" + getInformationAboutUserByChatId(chatId), getAfterRegistrationInlineKeyboardMarkup());
            }
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
            sendMessage(senderChatId, MESSAGE_DELIVERED, getDefaultInlineKeyboardMarkup());
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
            log.info("Message has been send to: " + chatId);
        } catch (TelegramApiException e){
            log.error("ERROR while sending message:" + e.getMessage());
        }
    }

    private void sendMessage(long chatId, String textToSend, ReplyKeyboard replyKeyboard){
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(textToSend);
        message.setReplyMarkup(replyKeyboard);

        try{
            execute(message);
            log.info("Message has been send to: " + chatId);
        } catch (TelegramApiException e){
            log.error("ERROR while sending message:" + e.getMessage());
        }
    }

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
                sendMessage(senderChatId, MESSAGE_DELIVERED, getDefaultInlineKeyboardMarkup());

            } catch (TelegramApiException e) {
                log.error("Error while download and send file: " + e.getMessage());
            }
        }
    }
/*

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
    //exceptions
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

 private ReplyKeyboardMarkup getDefaultReplyKeyboardMarkup(){
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRowList = new ArrayList<>();
        KeyboardRow kbr = new KeyboardRow();
        kbr.add("Получить шаблон карточки обьекта");
        keyboardRowList.add(kbr);
        KeyboardRow kbr2 = new KeyboardRow();
        kbr2.add("Получить реквизиты для оплаты");
        keyboardRowList.add(kbr2);
        KeyboardRow kbr3 = new KeyboardRow();
        kbr3.add("Какие-то еще фичи");
        keyboardRowList.add(kbr3);
        replyKeyboardMarkup.setKeyboard(keyboardRowList);
        return replyKeyboardMarkup;
    }
*/
    private String getInformationAboutUserByChatId (long chatId){
       if(userRepository.existsById(chatId)){
            User user = userRepository.findById(chatId).orElse(new User());
            return "Организация: " + user.getOrganization() + "\nАдрес обьекта: " + user.getAddress() + "\nНомер договора: " +
                    NOMENCLATURE + user.getContractNumber() + "\nSIM-Card: "
                    + user.getSIM() + "\nКонтактный телефон: " + user.getPhoneNumber();
       } else {
           return null;
       }
    }

    private String getCaptionForForwardMessage(String personInfo, String userName){
        return "\nПрислано от:\n\n" + personInfo + "\n@" + userName;
    }

    private boolean banUserByUserName(String userName){
        User user = userDAO.getUserByUserName(userName);
        if (user == null){
            return false;
        }else{
            user.setBanned(true);
            userRepository.save(user);
            return true;
        }
    }

    private InlineKeyboardMarkup getDefaultInlineKeyboardMarkup() {

        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();

        List<InlineKeyboardButton> rowInLine1 = new ArrayList<>();
        InlineKeyboardButton button1 = new InlineKeyboardButton();

        button1.setText("Получить шаблон карточки объекта");
        button1.setCallbackData("/getCard");

        rowInLine1.add(button1);
        rowsInLine.add(rowInLine1);

        List<InlineKeyboardButton> rowInLine2 = new ArrayList<>();
        InlineKeyboardButton button2 = new InlineKeyboardButton();

        button2.setText("Получить реквизиты для оплаты");
        button2.setCallbackData("/getRequisites");

        rowInLine2.add(button2);
        rowsInLine.add(rowInLine2);

        List<InlineKeyboardButton> rowInLine3 = new ArrayList<>();
        InlineKeyboardButton button3 = new InlineKeyboardButton();

        button3.setText("Связаться с Галей");
        button3.setCallbackData("/communication");

        rowInLine3.add(button3);
        rowsInLine.add(rowInLine3);

        markupInLine.setKeyboard(rowsInLine);
        return markupInLine;
    }

    private InlineKeyboardMarkup getInlineKeyboardMarkupWithoutCommunicationButton() {

        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();

        List<InlineKeyboardButton> rowInLine1 = new ArrayList<>();
        InlineKeyboardButton button1 = new InlineKeyboardButton();

        button1.setText("Получить шаблон карточки объекта");
        button1.setCallbackData("/getCard");

        rowInLine1.add(button1);
        rowsInLine.add(rowInLine1);

        List<InlineKeyboardButton> rowInLine2 = new ArrayList<>();
        InlineKeyboardButton button2 = new InlineKeyboardButton();

        button2.setText("Получить реквизиты для оплаты");
        button2.setCallbackData("/getRequisites");

        rowInLine2.add(button2);
        rowsInLine.add(rowInLine2);

        markupInLine.setKeyboard(rowsInLine);
        return markupInLine;
    }

    private InlineKeyboardMarkup getAfterRegistrationInlineKeyboardMarkup() {

        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();

        List<InlineKeyboardButton> rowInLine1 = new ArrayList<>();
        InlineKeyboardButton button1 = new InlineKeyboardButton();

        button1.setText("Подтвердить");
        button1.setCallbackData("/confirmRegistration");

        rowInLine1.add(button1);
        rowsInLine.add(rowInLine1);

        List<InlineKeyboardButton> rowInLine2 = new ArrayList<>();
        InlineKeyboardButton button2 = new InlineKeyboardButton();

        button2.setText("Ошибка? Ввести заново");
        button2.setCallbackData("/reRegistration");

        rowInLine2.add(button2);
        rowsInLine.add(rowInLine2);

        markupInLine.setKeyboard(rowsInLine);
        return markupInLine;
    }

    private InlineKeyboardMarkup getNotRegisteredSendMediaInlineKeyboardMarkup() {

        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();

        List<InlineKeyboardButton> rowInLine1 = new ArrayList<>();
        InlineKeyboardButton button1 = new InlineKeyboardButton();

        button1.setText("Представиться");
        button1.setCallbackData("/communication");

        rowInLine1.add(button1);
        rowsInLine.add(rowInLine1);

        markupInLine.setKeyboard(rowsInLine);
        return markupInLine;
    }

    private InlineKeyboardMarkup getAdminDefaultInlineKeyboardMarkup() {

        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();

        List<InlineKeyboardButton> rowInLine1 = new ArrayList<>();
        InlineKeyboardButton button1 = new InlineKeyboardButton();

        button1.setText("Забанить пользователя");
        button1.setCallbackData("/ban");

        rowInLine1.add(button1);
        rowsInLine.add(rowInLine1);

        List<InlineKeyboardButton> rowInLine2 = new ArrayList<>();
        InlineKeyboardButton button2 = new InlineKeyboardButton();

        button2.setText("Найти пользователя по номеру договора");
        button2.setCallbackData("/find_by_contract_num");

        rowInLine2.add(button2);
        rowsInLine.add(rowInLine2);

        markupInLine.setKeyboard(rowsInLine);
        return markupInLine;
    }

}
