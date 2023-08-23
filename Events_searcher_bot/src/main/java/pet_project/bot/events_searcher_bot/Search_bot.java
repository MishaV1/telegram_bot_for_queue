package pet_project.bot.events_searcher_bot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import pet_project.bot.events_searcher_bot.config.BotConfig;
import pet_project.bot.events_searcher_bot.rabbitMQ.RabbitMQProduceServiceImpl;

import java.util.*;

@Component
public class Search_bot extends TelegramLongPollingBot{
    private static long id_counter = 0;
    Map<Long, Map<String, Boolean>> map_of_registering = new HashMap<>();

    Map<Long, Map<String, Boolean>> map_of_registering_events = new HashMap<>();

    Map<Long, Profile> map_profiles = new HashMap<>();

    Map<Long, Event> map_events = new HashMap<>();
    public Set<Long> set_reguistered_profilers = new HashSet<>();

    @Autowired
    private RabbitMQProduceServiceImpl rabbitMQProduceService;

    final BotConfig botConfig;

    public Search_bot(BotConfig config){
        this.botConfig = config;
    }

    @Override
    public String getBotUsername() {
        return botConfig.getBotName();
    }

    @Override
    public String getBotToken() {
        return botConfig.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {

            if(update.getMessage().getText().equals("/start")
                    && !set_reguistered_profilers.contains(update.getMessage().getChatId())){
                sendMsg(update.getMessage().getChatId(), "Здраствуйте!! Это бот для поиска " +
                        "игроков для совместных прохождений.", 1);
            }else if (update.getMessage().getText().equals("Создание аккаунта")
                    && !set_reguistered_profilers.contains(update.getMessage().getChatId())) {
                map_profiles.put(update.getMessage().getChatId(), new Profile());

                Map<String, Boolean> map = new HashMap<>();
                map.put("Имя", false);
                map.put("Возраст", false);
                map.put("Discord", false);
                map.put("Игра", false);
                map.put("Описание", false);
                map_of_registering.put(update.getMessage().getChatId(), map);

                sendMsg(update.getMessage().getChatId(), "Выберите с чего начать", 2);
            } else if(set_reguistered_profilers.contains(update.getMessage().getChatId())) {
                if(update.getMessage().getText().equals("Редактирование профиля")){
                    sendMsg(update.getMessage().getChatId(), "Функционал пока не работает", 3);
                } else if(update.getMessage().getText().equals("Создание события")
                        && !map_of_registering_events.containsKey(update.getMessage().getChatId())
                        && !map_events.containsKey(update.getMessage().getChatId())){
                    Event event = new Event();
                    event.setId(id_counter++);
                    event.setPlayer_id(update.getMessage().getChatId());
                    map_events.put(update.getMessage().getChatId(), event);

                    Map<String, Boolean> map = new HashMap<>();
                    map.put("Название", false);
                    map.put("Игра", false);
                    map.put("Описание", false);
                    map.put("Критерии", false);

                    map_of_registering_events.put(update.getMessage().getChatId(), map);

                    sendMsg(update.getMessage().getChatId(), "Выберите с чего начать", 4);
                }else if(map_of_registering_events.containsKey(update.getMessage().getChatId())){
                    if(!map_of_registering_events.get(update.getMessage().getChatId()).containsValue(true)){
                        Map<String, Boolean> map = map_of_registering_events.get(update.getMessage().getChatId());
                        map.put(update.getMessage().getText(), true);
                    }else{
                        Map<String, Boolean> map = map_of_registering_events.get(update.getMessage().getChatId());
                        String text = getKeyByValue(map, true);
                        map.remove(text);

                        Event event = map_events.get(update.getMessage().getChatId());
                        set_in_event(event, text, update.getMessage().getText());
                        if(map.size() == 0) {
                            map_of_registering_events.remove(update.getMessage().getChatId());
                            rabbitMQProduceService.sendMessage(event.toString(), "event");
                            sendMsg(update.getMessage().getChatId(), "Поздравляем!! "
                                    + event.getName() + " всем разослан", 3);
                        }else{
                            sendMsg(update.getMessage().getChatId(), "Готово", 4);
                        }
                    }
                }else{
                    sendMsg(update.getMessage().getChatId(), "Добрый день", 3);
                }
            } else if(!map_of_registering.get(update.getMessage().getChatId()).containsValue(true)){
                Map<String, Boolean> map = map_of_registering.get(update.getMessage().getChatId());
                map.put(update.getMessage().getText(), true);

            }else{
                Map<String, Boolean> map = map_of_registering.get(update.getMessage().getChatId());
                String text = getKeyByValue(map, true);
                map.remove(text);

                Profile profile = map_profiles.get(update.getMessage().getChatId());
                set_in_profile(profile,text, update.getMessage().getText());
                if(map.size() == 0) {
                    map_of_registering.remove(update.getMessage().getChatId());
                    set_reguistered_profilers.add(update.getMessage().getChatId());
                    rabbitMQProduceService.sendMessage(profile.toString(), "player");
                    sendMsg(update.getMessage().getChatId(), "Поздравляем Вы прошли регистрацию "
                            + profile.getName(), 3);
                }else{
                    sendMsg(update.getMessage().getChatId(), "Готово", 2);
                }


            }


    }



    public void set_in_event(Event event, String text, String message){
        if(text.equals("Название")){
            event.setName(message);
        }else if(text.equals("Игра")){
            event.setGame(message);
        }else if(text.equals("Описанние")){
            event.setDescription(message);
        }else{
            event.setDescription(message);
        }
    }





    public void set_in_profile(Profile profile, String text, String message){
        if(text.equals("Имя")){
            profile.setName(message);
        }else if(text.equals("Возраст")){
            profile.setAge(Integer.valueOf(message));
        }else if(text.equals("Discord")){
            profile.setDiscorde(message);
        }else if(text.equals("Игра")){
            profile.setGame(message);
        }else{
            profile.setDescription(message);
        }
    }

    public static <T, E> T getKeyByValue(Map<T, E> map, E value) {
        for (Map.Entry<T, E> entry : map.entrySet()) {
            if (Objects.equals(value, entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    public synchronized void creating_profile(){


    }


    public synchronized void setMessage(Message message){
        if(message.getText().equals("Создание аккаунта")){
//            sendMsg(message.getChatId(), );
        }
    }

    public synchronized void setButtons(SendMessage message, int num){
        // Создаем клавиуатуру
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        message.setReplyMarkup(replyKeyboardMarkup);
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(true);
        long id = Long.parseLong(message.getChatId());
        // Создаем список строк клавиатуры
        List<KeyboardRow> keyboard = new ArrayList<>();


        switch (num){
            case 1 :
                KeyboardRow first = new KeyboardRow();
                first.add(new KeyboardButton("Создание аккаунта"));
                keyboard.add(first);
                replyKeyboardMarkup.setKeyboard(keyboard);
                break;
            case 2 :


                KeyboardRow row = new KeyboardRow();
                Map<String, Boolean> map = map_of_registering.get(id);
                for(String key : map.keySet()){
                    row.add(new KeyboardButton(key));
                }

                keyboard.add(row);
                replyKeyboardMarkup.setKeyboard(keyboard);
                break;
            case 3 :
                KeyboardRow first_3 = new KeyboardRow();
                KeyboardRow second_3 = new KeyboardRow();
                second_3.add(new KeyboardButton("Редактирование профиля"));
                first_3.add(new KeyboardButton("Создание события"));
                keyboard.add(second_3);
                keyboard.add(first_3);
                replyKeyboardMarkup.setKeyboard(keyboard);
                break;
            case 4 :
                KeyboardRow row_4  = new KeyboardRow();
                Map<String, Boolean> map_4 = map_of_registering_events.get(id);
                for(String key : map_4.keySet()){
                    row_4.add(new KeyboardButton(key));
                }

                keyboard.add(row_4);
                replyKeyboardMarkup.setKeyboard(keyboard);
                break;

        }

    }


    public synchronized void sendMsg(long chatId, String text, int num){
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);

        if(num != 0){
            setButtons(message, num);
        }


        try {
            execute(message);
        }catch (Exception e){
            e.printStackTrace();
        }
    }


}
