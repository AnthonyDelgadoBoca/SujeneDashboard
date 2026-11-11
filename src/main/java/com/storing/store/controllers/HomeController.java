package com.storing.store.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.storing.store.models.Event;
import com.storing.store.services.EventService;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Controller
public class HomeController {

    private final EventService eventService;

    public HomeController(EventService eventService, RestTemplateBuilder restTemplateBuilder) {
        this.eventService = eventService;
        this.restTemplate = restTemplateBuilder.build();
    }


    @GetMapping("/")
    public String index(Model model) {
        int nextAniyear = LocalDate.now().getYear();
        LocalDate anniversary = LocalDate.of(nextAniyear, 10, 26);
        long daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), anniversary);
        if (daysUntil < 0){
            anniversary = LocalDate.of(nextAniyear+1, 10, 26);
        }
        daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), anniversary);
        model.addAttribute("daysUntilAnniversary", daysUntil);

        // Add reminders
        String reminderMessage = getDailyReminder();
        model.addAttribute("dailyReminder", reminderMessage);

        // Get events for today and next 4 days (5 days total)
        LocalDate today = LocalDate.now();
        List<EventWithDate> upcomingEvents = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            LocalDate date = today.plusDays(i);
            List<Event> events = eventService.getEventsForDate(date);
            for (Event event : events) {
                upcomingEvents.add(new EventWithDate(event, date));
            }
        }

        // Add to model
        model.addAttribute("upcomingEvents", upcomingEvents);
        model.addAttribute("hasEvents", !upcomingEvents.isEmpty());

        // Get weather data her house: 34.0041, -118.4111
        WeatherData weather = getWeatherData("34.0041", "-118.4111");
        model.addAttribute("weather", weather);
        return "index";
    }

    // Helper class to store event with its date
    public static class EventWithDate {
        private Event event;
        private LocalDate date;

        public EventWithDate(Event event, LocalDate date) {
            this.event = event;
            this.date = date;
        }

        public Event getEvent() { return event; }
        public LocalDate getDate() { return date; }
    }

    @GetMapping("/secret")
    public String secretPage(Model model) {
        model.addAttribute("message",
                "Sujene, this is our private digital garden. " +
                        "No one else can see this but you and me ❤️");
        return "secret";
    }

    @GetMapping("/interactive")
    public String interactive(Model model) {
        List<String> compliments = Arrays.asList(
                "You have the best smile",
                "You make every day brighter",
                "I love your empathy",
                "You're my good girl",
                "I'm probably missing you right now",
                "I wish I could hold you now",
                "You are so pretty, I could (and do) stare at you for hours",
                "Probably thinking of kissing you right now",
                "My hand misses your hand (and your throat)",
                "I love to make you happy and cared for",
                "I miss rubbing your nose",
                "I love how your smile lingers in my head",
                "My favorite place is always close you",
                "You make me want to be better every day",
                "I love the way you see the world",
                "You’re the most beautiful thought I have",
                "I crave your voice more than white rice (BIG DEAL)",
                "I never get tired of you, not for a second",
                "You’re my safest place and my wildest dream",
                "Your happiness is my favorite mission",
                "Even silence with you feels perfect",
                "I’d choose you in every lifetime",
                "My tongue remember yours too well",
                "You’re the best surprise life ever gave me",
                "The thought of you still gives me butterflies (I struggle to eat)",
                "You are petting me (or I am petting you) in my head right now",
                "Every night, I wish you were here beside me"
        );

        // Convert to JSON and add directly to model
        ObjectMapper mapper = new ObjectMapper();
        try {
            String json = mapper.writeValueAsString(compliments);
            model.addAttribute("complimentsJson", json);
        } catch (JsonProcessingException e) {
            model.addAttribute("complimentsJson", "[]");
        }

        return "interactive";
    }

    @GetMapping("/open-when")
    public String openWhenLetters() {
        return "open-when";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    private String getViewName() {
        return "index.html";
    }

    private final RestTemplate restTemplate;

    private WeatherData getWeatherData(String latitude, String longitude) {
        String url = String.format("https://api.open-meteo.com/v1/forecast?latitude=%s&longitude=%s&current_weather=true",
                latitude, longitude);

        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> currentWeather = (Map<String, Object>) response.getBody().get("current_weather");

                WeatherData weather = new WeatherData();
                weather.setTemperature(currentWeather.get("temperature").toString());
                weather.setWeatherCode(Integer.parseInt(currentWeather.get("weathercode").toString()));
                weather.setWindSpeed(currentWeather.get("windspeed").toString());

                return weather;
            }
        } catch (Exception e) {
            // Fallback to default weather if API fails
            WeatherData weather = new WeatherData();
            weather.setTemperature("72");
            weather.setWeatherCode(1); // 1 = partly cloudy
            return weather;
        }

        return null;
    }

    public static class WeatherData {
        private String temperature;
        private int weatherCode;
        private String windSpeed;

        // Getters and setters
        public String getTemperature() { return temperature; }
        public void setTemperature(String temperature) { this.temperature = temperature; }
        public int getWeatherCode() { return weatherCode; }
        public void setWeatherCode(int weatherCode) { this.weatherCode = weatherCode; }
        public String getWindSpeed() { return windSpeed; }
        public void setWindSpeed(String windSpeed) { this.windSpeed = windSpeed; }

        public String getWeatherEmoji() {
            // Map weather codes to emojis
            switch(weatherCode) {
                case 0: return "☀️"; // Clear sky
                case 1: return "🌤"; // Mainly clear
                case 2: return "⛅"; // Partly cloudy
                case 3: return "☁️"; // Overcast
                case 45: case 48: return "🌫"; // Fog
                case 51: case 53: case 55: return "🌧"; // Drizzle
                case 56: case 57: return "🌧❄️"; // Freezing drizzle
                case 61: case 63: case 65: return "🌧"; // Rain
                case 66: case 67: return "🌧❄️"; // Freezing rain
                case 71: case 73: case 75: return "❄️"; // Snow
                case 77: return "🌨"; // Snow grains
                case 80: case 81: case 82: return "🌧"; // Rain showers
                case 85: case 86: return "🌨"; // Snow showers
                case 95: case 96: case 99: return "⛈"; // Thunderstorm
                default: return "🌈";
            }
        }
    }

    
private String getDailyReminder() {
    DayOfWeek day = LocalDate.now().getDayOfWeek();
    int hour = LocalTime.now().getHour();

    boolean remoteDay = day == DayOfWeek.MONDAY || day == DayOfWeek.WEDNESDAY;
    boolean inPersonDay = day == DayOfWeek.TUESDAY || day == DayOfWeek.THURSDAY;
    boolean weekday = day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY;

    // Morning (6 AM - 11 AM)
    if (hour >= 6 && hour < 12) {
        switch (day) {
            case MONDAY:
                return "Good morning mi amor, Remote work means you have absolutely no excuse not to miss me today";
            case TUESDAY:
                return "Good morning my pretty girl. Go be ridiculously competent at the office. Try not to make everyone fall in love with you";
            case WEDNESDAY:
                return "Rise and shine, my beautiful remote worker, I Love you";
            case THURSDAY:
                return "Good morning mi coreana, Another day of being beautiful AND employed. Honestly, be more considerate for those of us without...";
            case FRIDAY:
                return "GOOD MORNING mi vida. It's Friday. I hope you're prepared to be missed this weekend";
            case SATURDAY:
                return "Good morning mi vida, beginning of the weekend";
            case SUNDAY:
                return "Buenos días mi vida. Sunday looks better when I imagine spending it with you";
            default:
                return "Good morning, my favorite distraction. I hope your day is as pretty as you are";
        }
    }

    // Lunch (12 PM - 1 PM)
    else if (hour >= 12 && hour < 13) {
        switch (day) {
            case MONDAY: case WEDNESDAY:
                return "hopefully you are eating lol, I miss you";
            case TUESDAY: case THURSDAY:
                return "Lunch check: did you bring to the office? I hoep you did, I Love you";
            case FRIDAY:
                return "A COMER ( to eat)";
            case SATURDAY: case SUNDAY:
                return "I bet you're doing something cute right now. Send evidence. This is a completely legitimate investigation.";
            default:
                return "Lunch break, pretty girl. I miss you. That is all. Carry on.";
        }
    }

    // Afternoon (1 PM - 4 PM)
    else if (hour >= 13 && hour < 16) {
        switch (day) {
            case MONDAY: case WEDNESDAY:
                return "I bet you are working or studying, what a productive girl, I Love you";
            case TUESDAY: case THURSDAY:
                return "You're probably being productive at work. Very attractive, I miss you";
            case FRIDAY:
                return "Almost weekend mi vida...";
            case SATURDAY: case SUNDAY:
                return "Friendly reminder that you are cute, smart, and probably too good for me lol";
            default:
                return "Just checking in on my favorite human. Hope something made you smile today.";
        }
    }

    // Evening (4 PM - 9 PM)
    else if (hour >= 16 && hour < 21) {
        switch (day) {
            case MONDAY: case WEDNESDAY:
                return "Work is hopefully wrapping up. Gym time or LSAT time? Either way, you got this my very msart JKG";
            case TUESDAY: case THURSDAY:
                return "You survived the office, now let's see if you can text your Antonio (Your Boyfriend), I Love you";
            case FRIDAY:
                return "It's Friday evening, mi amor. Please enjoy yourself";
            case SATURDAY: case SUNDAY:
                return "Gym? Studying? Relaxing? Whatever you're doing, I hope you know someone is proud of you";
            default:
                return "Hope you're having a lovely evening. I wish I could be there to annoy you in person.";
        }
    }

    // Night (9 PM - 4 AM)
    else {
        switch (day) {
            case MONDAY: case WEDNESDAY:
                return "Enough working and studying, if you have to stall by looking here it's time to take a break";
            case TUESDAY: case THURSDAY:
                return "Enough working and studying, if you have to stall by looking here it's time to take a break";
            case FRIDAY:
                return "Enough working and studying, if you have to stall by looking here it's time to take a break";
            case SATURDAY: case SUNDAY:
                return "Goodnight pretty girl. I hope you fall asleep smiling";
            default:
                return "Goodnight mi vida. I wish I could give you a hug instead of sending this little notification.";
        }
    }
}


}
