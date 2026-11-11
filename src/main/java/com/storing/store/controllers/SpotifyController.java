package com.storing.store.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.io.File;
import java.io.FileReader;
import java.util.Properties;

@Controller
public class SpotifyController {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Properties envProps = new Properties();

    private String accessToken;
    private LocalDateTime tokenExpiry;

    public SpotifyController() {
        // Load .env file directly on startup
        loadEnvFile();

        System.out.println("=== DIRECT .env CONFIG ===");
        System.out.println("Client ID: " + getClientId());
        System.out.println("User ID: " + getUserId());
        System.out.println("Redirect URI: " + getRedirectUri());
    }

    private void loadEnvFile() {
        try {
            File envFile = new File(".env");
            if (envFile.exists()) {
                envProps.load(new FileReader(envFile));
                System.out.println("✅ .env file loaded directly");
            } else {
                System.out.println("⚠️  .env file not found");
            }
        } catch (Exception e) {
            System.err.println("❌ Error loading .env: " + e.getMessage());
        }
    }

    // Direct getters from .env
    private String getClientId() {
        return envProps.getProperty("SPOTIFY_CLIENT_ID",
                System.getProperty("SPOTIFY_CLIENT_ID", "default_client_id"));
    }

    private String getClientSecret() {
        return envProps.getProperty("SPOTIFY_CLIENT_SECRET",
                System.getProperty("SPOTIFY_CLIENT_SECRET", "default_client_secret"));
    }

    private String getUserId() {
        return envProps.getProperty("SPOTIFY_USER_ID",
                System.getProperty("SPOTIFY_USER_ID", "default_user_id"));
    }

    private String getRedirectUri() {
        return envProps.getProperty("SPOTIFY_REDIRECT_URI",
                System.getProperty("SPOTIFY_REDIRECT_URI", "http://127.0.0.1:8080/spotify/callback"));
    }

    // Update refreshAccessToken to use direct getters
    private void refreshAccessToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(getClientId(), getClientSecret());

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    "https://accounts.spotify.com/api/token", request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                accessToken = jsonNode.get("access_token").asText();
                tokenExpiry = LocalDateTime.now().plus(55, ChronoUnit.MINUTES);
                System.out.println("✅ Access token obtained!");
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to refresh access token", e);
        }
    }

    // Remove the @Autowired SpotifyConfig and update all methods to use direct getters
    @GetMapping("/spotify")
    public String spotifyDashboard(Model model) {
        try {
            if (accessToken == null || isTokenExpired()) {
                refreshAccessToken();
            }

            SpotifyData spotifyData = getSpotifyData();
            model.addAttribute("spotifyData", spotifyData);
            model.addAttribute("profile", spotifyData.getProfile());
            model.addAttribute("recentTracks", spotifyData.getTopTracks());
            model.addAttribute("lastUpdated", LocalDateTime.now());

        } catch (Exception e) {
            model.addAttribute("error", "Unable to load Spotify data: " + e.getMessage());
        }

        return "spotify";
    }

    @GetMapping("/spotify/playlist/{playlistId}")
    public String playlistDetail(@PathVariable String playlistId, Model model) {
        try {
            if (accessToken == null || isTokenExpired()) {
                refreshAccessToken();
            }

            // Validate playlist ID
            if (playlistId == null || playlistId.trim().isEmpty()) {
                model.addAttribute("error", "Invalid playlist ID");
                return "playlist-detail";
            }

            // Get playlist details
            SpotifyPlaylistDetail playlistDetail = getPlaylistDetail(playlistId);
            model.addAttribute("playlist", playlistDetail);
            model.addAttribute("lastUpdated", LocalDateTime.now());

        } catch (Exception e) {
            System.err.println("Error loading playlist " + playlistId + ": " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Unable to load playlist: " + e.getMessage());

            // Add empty playlist to avoid Thymeleaf errors
            SpotifyPlaylistDetail emptyPlaylist = new SpotifyPlaylistDetail();
            emptyPlaylist.setId(playlistId);
            emptyPlaylist.setName("Error Loading Playlist");
            emptyPlaylist.setTracks(new ArrayList<>());
            model.addAttribute("playlist", emptyPlaylist);
        }

        return "playlist-detail";
    }

    private SpotifyPlaylistDetail getPlaylistDetail(String playlistId) {
        HttpHeaders headers = createHeaders();
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    "https://api.spotify.com/v1/playlists/" + playlistId,
                    HttpMethod.GET, entity, String.class);

            JsonNode json = objectMapper.readTree(response.getBody());
            SpotifyPlaylistDetail playlist = new SpotifyPlaylistDetail();

            // Basic playlist info
            playlist.setId(playlistId);
            playlist.setName(json.has("name") ? json.get("name").asText() : "Unknown Playlist");
            playlist.setDescription(json.has("description") && !json.get("description").isNull() ?
                    json.get("description").asText() : "");

            // Handle images safely
            if (json.has("images") && json.get("images").size() > 0) {
                playlist.setImageUrl(json.get("images").get(0).get("url").asText());
            }

            // Get tracks with comprehensive debugging
            List<SpotifyTrack> tracks = new ArrayList<>();
            if (json.has("tracks") && json.get("tracks").has("items")) {
                JsonNode items = json.get("tracks").get("items");

                int totalTracks = 0;
                int tracksWithPreview = 0;
                int tracksWithoutPreview = 0;

                for (JsonNode item : items) {
                    if (item.has("track") && !item.get("track").isNull()) {
                        JsonNode trackNode = item.get("track");
                        totalTracks++;

                        SpotifyTrack track = new SpotifyTrack();

                        // Track name
                        track.setName(trackNode.has("name") ? trackNode.get("name").asText() : "Unknown Track");

                        // Artist
                        if (trackNode.has("artists") && trackNode.get("artists").size() > 0) {
                            track.setArtist(trackNode.get("artists").get(0).get("name").asText());
                        } else {
                            track.setArtist("Unknown Artist");
                        }

                        // PREVIEW URL - This is the key part
                        boolean hasPreview = false;
                        if (trackNode.has("preview_url") && !trackNode.get("preview_url").isNull()) {
                            String previewUrl = trackNode.get("preview_url").asText();
                            if (previewUrl != null && !previewUrl.isEmpty() && !previewUrl.equals("null")) {
                                track.setPreviewUrl(previewUrl);
                                hasPreview = true;
                                tracksWithPreview++;
                            } else {
                                track.setPreviewUrl(null);
                                tracksWithoutPreview++;
                            }
                        } else {
                            track.setPreviewUrl(null);
                            tracksWithoutPreview++;
                        }

                        // External URL
                        if (trackNode.has("external_urls") && trackNode.get("external_urls").has("spotify")) {
                            track.setExternalUrl(trackNode.get("external_urls").get("spotify").asText());
                        }

                        // Image URL
                        if (trackNode.has("album") && trackNode.get("album").has("images") &&
                                trackNode.get("album").get("images").size() > 0) {
                            track.setImageUrl(trackNode.get("album").get("images").get(0).get("url").asText());
                        }

                        // Duration
                        if (trackNode.has("duration_ms")) {
                            track.setDurationMs(trackNode.get("duration_ms").asLong());
                        }

                        // Debug logging for each track
                        System.out.println("Track: " + track.getName() +
                                " - Preview: " + (hasPreview ? "YES" : "NO") +
                                " - URL: " + track.getPreviewUrl());

                        tracks.add(track);
                    }
                }

                // Comprehensive debug output
                System.out.println("=== PLAYLIST ANALYSIS ===");
                System.out.println("Playlist: " + playlist.getName());
                System.out.println("Total tracks processed: " + totalTracks);
                System.out.println("Tracks with preview: " + tracksWithPreview);
                System.out.println("Tracks without preview: " + tracksWithoutPreview);
                System.out.println("Preview availability: " +
                        String.format("%.1f%%", (tracksWithPreview * 100.0 / totalTracks)));
            }

            playlist.setTracks(tracks);
            return playlist;

        } catch (Exception e) {
            System.err.println("ERROR in getPlaylistDetail: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to get playlist details: " + e.getMessage(), e);
        }
    }

    // Update all other methods that used spotifyConfig to use getClientId(), getUserId(), etc.
    private SpotifyData getSpotifyData() {
        SpotifyData data = new SpotifyData();
        try {
            data.setPlaylists(getPublicPlaylists());
            data.setProfile(getPublicProfile());
            data.setTopTracks(getRecentTracks());
            // We'll store the special tracks separately
            data.setMostPopularTrack(getMostPopularTrack());
            data.setLastAddedTrack(getLastAddedTrack());
        } catch (Exception e) {
            System.err.println("Error fetching Spotify data: " + e.getMessage());
        }
        return data;
    }

    private SpotifyProfile getPublicProfile() {
        HttpHeaders headers = createHeaders();
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    "https://api.spotify.com/v1/users/" + getUserId(),
                    HttpMethod.GET, entity, String.class);

            JsonNode json = objectMapper.readTree(response.getBody());
            SpotifyProfile profile = new SpotifyProfile();
            profile.setDisplayName(json.get("display_name").asText("Sujene"));
            if (json.has("images") && json.get("images").size() > 0) {
                profile.setImageUrl(json.get("images").get(0).get("url").asText());
            }
            profile.setEmail("Sujene's Spotify");
            return profile;
        } catch (Exception e) {
            SpotifyProfile profile = new SpotifyProfile();
            profile.setDisplayName("Sujene");
            profile.setEmail("Sujene's Spotify");
            return profile;
        }
    }

    private List<SpotifyTrack> getTopTracksFromPlaylists() {
        // Return empty list for now to avoid API errors
        return new ArrayList<>();
    }

    private List<SpotifyPlaylist> getPublicPlaylists() {
        HttpHeaders headers = createHeaders();
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    "https://api.spotify.com/v1/users/" + getUserId() + "/playlists",
                    HttpMethod.GET, entity, String.class);

            JsonNode json = objectMapper.readTree(response.getBody());
            List<SpotifyPlaylist> playlists = new ArrayList<>();

            for (JsonNode item : json.get("items")) {
                if (item.get("public").asBoolean()) {
                    SpotifyPlaylist playlist = new SpotifyPlaylist();
                    playlist.setId(item.get("id").asText()); // Make sure this line is here
                    playlist.setName(item.get("name").asText());
                    playlist.setDescription(item.has("description") ? item.get("description").asText() : "");
                    playlist.setExternalUrl(item.get("external_urls").get("spotify").asText());
                    playlist.setImageUrl(item.get("images").size() > 0 ?
                            item.get("images").get(0).get("url").asText() : null);
                    playlist.setTrackCount(item.get("tracks").get("total").asInt());
                    playlists.add(playlist);
                }
            }

            return playlists;
        } catch (Exception e) {
            System.err.println("Error getting playlists: " + e.getMessage());
            return new ArrayList<>(); // Return empty list instead of fallback
        }
    }

    private List<SpotifyTrack> getPlaylistTracks(String playlistId) {
        List<SpotifyTrack> tracks = new ArrayList<>();
        HttpHeaders headers = createHeaders();
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    "https://api.spotify.com/v1/playlists/" + playlistId + "/tracks?limit=5",
                    HttpMethod.GET, entity, String.class);

            JsonNode json = objectMapper.readTree(response.getBody());

            for (JsonNode item : json.get("items")) {
                if (item.has("track") && !item.get("track").isNull()) {
                    JsonNode trackNode = item.get("track");
                    SpotifyTrack track = new SpotifyTrack();
                    track.setName(trackNode.has("name") ? trackNode.get("name").asText() : "Unknown Track");

                    if (trackNode.has("artists") && trackNode.get("artists").size() > 0) {
                        track.setArtist(trackNode.get("artists").get(0).get("name").asText());
                    } else {
                        track.setArtist("Unknown Artist");
                    }

                    if (trackNode.has("preview_url") && !trackNode.get("preview_url").isNull()) {
                        track.setPreviewUrl(trackNode.get("preview_url").asText());
                    }

                    if (trackNode.has("external_urls") && trackNode.get("external_urls").has("spotify")) {
                        track.setExternalUrl(trackNode.get("external_urls").get("spotify").asText());
                    }

                    if (trackNode.has("album") && trackNode.get("album").has("images") &&
                            trackNode.get("album").get("images").size() > 0) {
                        track.setImageUrl(trackNode.get("album").get("images").get(0).get("url").asText());
                    }

                    if (trackNode.has("duration_ms")) {
                        track.setDurationMs(trackNode.get("duration_ms").asLong());
                    }

                    tracks.add(track);
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting playlist tracks: " + e.getMessage());
        }

        return tracks;
    }

    private List<SpotifyTrack> getRecentTracks() {
        List<SpotifyTrack> recentTracks = new ArrayList<>();
        try {
            // Get all public playlists
            List<SpotifyPlaylist> playlists = getPublicPlaylists();

            // Get a few tracks from the first few playlists (or recently updated ones)
            int tracksNeeded = 3;
            int playlistsChecked = 0;

            for (SpotifyPlaylist playlist : playlists) {
                if (playlistsChecked >= 2 || recentTracks.size() >= tracksNeeded) break;

                // Get first few tracks from this playlist
                List<SpotifyTrack> playlistTracks = getPlaylistTracks(playlist.getId());
                for (int i = 0; i < Math.min(2, playlistTracks.size()); i++) {
                    if (recentTracks.size() < tracksNeeded) {
                        recentTracks.add(playlistTracks.get(i));
                    }
                }
                playlistsChecked++;
            }

        } catch (Exception e) {
            System.err.println("Error getting recent tracks: " + e.getMessage());
        }
        return recentTracks;
    }

    private SpotifyTrack getMostPopularTrack() {
        try {
            List<SpotifyPlaylist> playlists = getPublicPlaylists();
            SpotifyTrack mostPopular = null;
            int maxPopularity = -1;

            for (SpotifyPlaylist playlist : playlists) {
                List<SpotifyTrack> tracks = getPlaylistTracksWithPopularity(playlist.getId());
                for (SpotifyTrack track : tracks) {
                    // In a real implementation, you'd get popularity from Spotify API
                    // For now, we'll simulate popularity based on track position
                    int popularity = track.getDurationMs() != null ?
                            track.getDurationMs().intValue() % 100 : 50;

                    if (popularity > maxPopularity) {
                        maxPopularity = popularity;
                        mostPopular = track;
                    }
                }
            }

            return mostPopular != null ? mostPopular : createDefaultTrack("Most Played Track");

        } catch (Exception e) {
            System.err.println("Error getting most popular track: " + e.getMessage());
            return createDefaultTrack("Most Played Track");
        }
    }

    private SpotifyTrack getLastAddedTrack() {
        try {
            List<SpotifyPlaylist> playlists = getPublicPlaylists();

            for (SpotifyPlaylist playlist : playlists) {
                List<SpotifyTrack> tracks = getPlaylistTracks(playlist.getId());
                if (!tracks.isEmpty()) {
                    return tracks.get(0); // Return first track from first playlist as "last added"
                }
            }

            return createDefaultTrack("Recently Added");

        } catch (Exception e) {
            System.err.println("Error getting last added track: " + e.getMessage());
            return createDefaultTrack("Recently Added");
        }
    }

    private List<SpotifyTrack> getPlaylistTracksWithPopularity(String playlistId) {
        // This would be similar to getPlaylistTracks but would include popularity data
        // For now, we'll use the same method
        return getPlaylistTracks(playlistId);
    }

    private SpotifyTrack createDefaultTrack(String name) {
        SpotifyTrack track = new SpotifyTrack();
        track.setName(name);
        track.setArtist("Your Music");
        track.setDurationMs(180000L); // 3 minutes
        track.setExternalUrl("https://open.spotify.com");
        return track;
    }


    private List<SpotifyPlaylist> getFallbackPlaylists() {
        List<SpotifyPlaylist> playlists = new ArrayList<>();

        String[] defaultPlaylists = {
                "Liked Songs", "Chill Vibes", "Workout Mix", "Road Trip", "Study Session"
        };

        for (String name : defaultPlaylists) {
            SpotifyPlaylist playlist = new SpotifyPlaylist();
            playlist.setName(name);
            playlist.setDescription("Sujene's " + name);
            playlist.setTrackCount(new Random().nextInt(50) + 20);
            playlists.add(playlist);
        }

        return playlists;
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        return headers;
    }

    private boolean isTokenExpired() {
        return tokenExpiry == null || LocalDateTime.now().isAfter(tokenExpiry);
    }

    // Data classes (keep the same as before)
    public static class SpotifyData {
        private SpotifyProfile profile;
        private List<SpotifyTrack> topTracks;
        private List<SpotifyPlaylist> playlists;
        private LocalDateTime lastUpdated;
        private SpotifyTrack mostPopularTrack;
        private SpotifyTrack lastAddedTrack;

        public SpotifyTrack getMostPopularTrack() { return mostPopularTrack; }
        public void setMostPopularTrack(SpotifyTrack mostPopularTrack) { this.mostPopularTrack = mostPopularTrack; }

        public SpotifyTrack getLastAddedTrack() { return lastAddedTrack; }
        public void setLastAddedTrack(SpotifyTrack lastAddedTrack) { this.lastAddedTrack = lastAddedTrack; }


        public SpotifyProfile getProfile() { return profile; }
        public void setProfile(SpotifyProfile profile) { this.profile = profile; }
        public List<SpotifyTrack> getTopTracks() { return topTracks; }
        public void setTopTracks(List<SpotifyTrack> topTracks) { this.topTracks = topTracks; }
        public List<SpotifyPlaylist> getPlaylists() { return playlists; }
        public void setPlaylists(List<SpotifyPlaylist> playlists) { this.playlists = playlists; }
        public LocalDateTime getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
    }

    public static class SpotifyProfile {
        private String displayName;
        private String email;
        private String imageUrl;

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
        }
    }

    public static class SpotifyPlaylist {
        private String id;
        private String name;
        private String description;
        private String externalUrl;
        private String imageUrl;
        private int trackCount;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getExternalUrl() { return externalUrl; }
        public void setExternalUrl(String externalUrl) { this.externalUrl = externalUrl; }
        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
        public int getTrackCount() { return trackCount; }
        public void setTrackCount(int trackCount) { this.trackCount = trackCount; }
    }

    // Add this data class to your SpotifyController
    public static class SpotifyPlaylistDetail {
        private String id;
        private String name;
        private String description;
        private String imageUrl;
        private List<SpotifyTrack> tracks;

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
        public List<SpotifyTrack> getTracks() { return tracks; }
        public void setTracks(List<SpotifyTrack> tracks) { this.tracks = tracks; }
    }

    // Update the existing SpotifyTrack class to include duration
    public static class SpotifyTrack {
        private String name = "Unknown Track";
        private String artist = "Unknown Artist";
        private String previewUrl;
        private String externalUrl = "https://open.spotify.com";
        private String imageUrl;
        private Long durationMs = 0L;

        // Add this method to extract track ID from Spotify URL
        public String getTrackId() {
            if (externalUrl == null || externalUrl.isEmpty()) {
                return null;
            }
            try {
                // Extract track ID from Spotify URL
                // Example: https://open.spotify.com/track/4cOdK2wGLETKBW3PvgPWqT
                String[] parts = externalUrl.split("/");
                if (parts.length > 0) {
                    String lastPart = parts[parts.length - 1];
                    // Remove any query parameters
                    if (lastPart.contains("?")) {
                        lastPart = lastPart.split("\\?")[0];
                    }
                    return lastPart;
                }
            } catch (Exception e) {
                System.err.println("Error extracting track ID from: " + externalUrl);
            }
            return null;
        }

        // Keep all your existing getters and setters...
        public String getFormattedDuration() {
            if (durationMs == null || durationMs == 0) return "0:00";
            long seconds = durationMs / 1000;
            long minutes = seconds / 60;
            seconds = seconds % 60;
            return String.format("%d:%02d", minutes, seconds);
        }

        public String getName() { return name != null ? name : "Unknown Track"; }
        public void setName(String name) { this.name = name; }

        public String getArtist() { return artist != null ? artist : "Unknown Artist"; }
        public void setArtist(String artist) { this.artist = artist; }

        public String getPreviewUrl() { return previewUrl; }
        public void setPreviewUrl(String previewUrl) { this.previewUrl = previewUrl; }

        public String getExternalUrl() { return externalUrl != null ? externalUrl : "https://open.spotify.com"; }
        public void setExternalUrl(String externalUrl) { this.externalUrl = externalUrl; }

        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

        public Long getDurationMs() { return durationMs != null ? durationMs : 0L; }
        public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    }
}