package services;

import entities.BaseEntity;
import entities.User.Playlist;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Stack;

public class PlaylistService extends Service<Playlist>{
    private static PlaylistService instance = null;
    public static PlaylistService getInstance() {
        if (PlaylistService.instance == null) {
            PlaylistService.instance = new PlaylistService();
        }

        return PlaylistService.instance;
    }

    private Playlist openPlaylist;
    private Stack<Integer> pastPositions;
    private Integer currentPosition;

    public PlaylistService() { openPlaylist = null; }

    public Playlist getOpenPlaylist() {
        return this.openPlaylist;
    }

    public Integer getOpenPlaylistID() {
        if (this.openPlaylist == null) {
            return null;
        }
        return this.openPlaylist.getID();
    }

    public void openPlaylist(Integer id) {
        this.openPlaylist = this.get(id);
        if (this.openPlaylist == null) {
            return;
        }

        if (this.openPlaylist.getVideos().isEmpty()) {
            System.out.println("This playlist is empty.");
            this.openPlaylist = null;
            return;
        }

        this.pastPositions = new Stack<>();
        this.currentPosition = 0;
        System.out.println("Opened playlist with id = " + id + ".");
        System.out.println("Currently watching:");
        System.out.println(this.openPlaylist.getVideos().get(this.currentPosition));
    }

    public void closePlaylist() {
        this.openPlaylist = null;
    }

    public void addVideo(Integer videoID, Integer playlistID) {
        Statement stmt;

        try {
            stmt = Service.connection.createStatement();
        } catch (SQLException sqlE) {
            System.out.println("Error creating jdbc statement!");
            System.out.println(sqlE.getMessage());
            return;
        }

        String sqlGetPost = "SELECT DISTINCT 1 FROM PLAYLISTCONTENT WHERE postID = " + videoID + " AND playlistID = " + playlistID;
        try {
            ResultSet res = stmt.executeQuery(sqlGetPost);

            if (res.next()) {
                throw new SQLException("Video with id = " + videoID + " is already in playlist with id = " + playlistID + "!");
            }
        } catch (SQLException sqlE) {
            System.out.println("Error adding video with id = " + videoID + " to playlist with id = " + playlistID + "!");
            System.out.println("Select statement: " + sqlGetPost);
            System.out.println(sqlE.getMessage());
            return;
        }

        String sqlInsert = "INSERT INTO PLAYLISTCONTENT(postID, playlistID) VALUES(" + videoID + ", " + playlistID + ")";

        try {
            Statement insertStmt = Service.connection.createStatement();
            int res = insertStmt.executeUpdate(sqlInsert);
            if (res == 1) {
                AuditService.getInstance().writeAction("Video with id " + videoID + " has been added to playlist with id " + playlistID + ".");
            } else {
                throw new SQLException("The number of inserted rows is " + res + "!");
            }
        } catch (SQLException sqlE) {
            System.out.println("Error inserting video with id = " + videoID + " to playlist with id = " + playlistID + "!");
            System.out.println(sqlE.getMessage());
            return;
        }

        System.out.println("Successfully added this post to playlist with id = " + playlistID + ".");
    }

    public List<Playlist> getAllFromUser(Integer ownerID) {
        String sqlGet = "SELECT * FROM PLAYLIST WHERE ownerID = " + ownerID;
        ResultSet res;

        try {
            Statement getStmt = Service.connection.createStatement();
            res = getStmt.executeQuery(sqlGet);
        } catch (SQLException sqlE) {
            System.out.println("Error getting playlists belonging to user with id = " + ownerID + "!");
            return null;
        }

        List<Playlist> playlists = new ArrayList<>();
        try {
            while(true) {
                try {
                    if (!res.next()) {
                        break;
                    }
                } catch (SQLException sqlE) {
                    System.out.println("Error retrieving next playlist for user with id = " + ownerID + "!");
                    throw sqlE;
                }

                Playlist playlist = (Playlist)BaseEntity.getFromSelect(res);
                this.selectVideos(playlist);

                playlists.add(playlist);
            }
        } catch (Exception e) {
            System.out.println("Error getting data of playlists belonging to user with id = " + ownerID + "!");
            System.out.println(e.getMessage());
            return null;
        }

        AuditService.getInstance().writeAction("User with id " + ownerID + " requested a list of all of their playlists.");
        return playlists;
    }

    public int add(Scanner sc, Integer ownerID) {
        Playlist playlist = new Playlist(ownerID);
        playlist.read(sc);

        try {
            playlist = super.add(playlist);
            if (playlist != null) {
                AuditService.getInstance().writeAction("User with id " + ownerID + " created a new playlist with id " + playlist.getID() + ".");
            }
        } catch (Exception e) {
            System.out.println("Error adding new playlist to database.");
            System.out.println(e.getMessage());
            return 1;
        }

        return 0;
    }

    public Playlist get(Integer id) {
        Playlist playlist = super.get(id);
        if (playlist == null) {
            return null;
        }

        this.selectVideos(playlist);

        return playlist;
    }

    private void selectVideos(Playlist playlist) {
        String sqlGetVideos = "SELECT * FROM PLAYLISTCONTENT pc JOIN POST p ON p.id = pc.postID WHERE pc.playlistID = " + playlist.getID();
        ResultSet videos;

        try {
            Statement videosStmt = Service.connection.createStatement();
            videos = videosStmt.executeQuery(sqlGetVideos);
        } catch (SQLException sqlE) {
            System.out.println("Error getting list of videos from database for playlist with id = " + playlist.getID() + "!");
            System.out.println(sqlE.getMessage());
            return;
        }

        try {
            playlist.getVideosFromSelect(videos);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void switchOrdering() {
        this.getOpenPlaylist().switchOrdering();

        if (!this.set(this.getOpenPlaylist())) {
            this.getOpenPlaylist().switchOrdering();
        } else {
            AuditService.getInstance().writeAction("User with id " + this.getOpenPlaylist().getOwnerID() + " has switched the ordering of playlist with id " + this.getOpenPlaylistID() + ".");
        }
    }

    public void previousVideo() {
        if (this.pastPositions.isEmpty()) {
            System.out.println("There is no previous video!");
            return;
        }

        this.currentPosition = this.pastPositions.pop();
        System.out.println("Currently watching:");
        System.out.println(this.openPlaylist.getVideos().get(this.currentPosition));
    }

    public void nextVideo() {
        this.pastPositions.push(this.currentPosition);
        this.currentPosition = this.openPlaylist.getNext(this.currentPosition);

        System.out.println("Currently watching:");
        System.out.println(this.openPlaylist.getVideos().get(this.currentPosition));
    }

    public void setVideo(Integer position) {
        this.currentPosition = position;

        System.out.println("Currently watching:");
        System.out.println(this.openPlaylist.getVideos().get(this.currentPosition));
    }

    public void removeCurrentVideo() {
        Integer videoID = this.openPlaylist.getVideos().get(this.currentPosition).getID();
        String sqlDelete = "DELETE FROM PLAYLISTCONTENT WHERE postID = " + videoID;

        try {
            Statement deleteStmt = Service.connection.createStatement();
            int res = deleteStmt.executeUpdate(sqlDelete);
            if (res == 1) {
                AuditService.getInstance().writeAction("User with id " + this.getOpenPlaylist().getOwnerID() + " has removed video with id " + videoID + " from playlist with id " + this.getOpenPlaylistID() + ".");
            } else {
                throw new SQLException("The number of removed videos is " + res + "!");
            }
        } catch (SQLException sqlE) {
            System.out.println("Error removing video with id = " + videoID + " from playlist with id = " + this.getOpenPlaylistID() + "!");
            System.out.println("Delete statement: " + sqlDelete);
            System.out.println(sqlE.getMessage());

            return;
        }

        this.openPlaylist.removeVideo(this.currentPosition);
        if (this.openPlaylist.getVideos().isEmpty()) {
            this.openPlaylist = null;
            return;
        }

        --this.currentPosition;
        this.nextVideo();
    }
}