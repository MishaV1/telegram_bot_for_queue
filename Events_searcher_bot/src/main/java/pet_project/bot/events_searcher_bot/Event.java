package pet_project.bot.events_searcher_bot;

import lombok.Data;

@Data
public class Event {
    private int id;
    private String name;
    private Long player_id;
    private String game;
    private String description;
    private String criteries;
}
