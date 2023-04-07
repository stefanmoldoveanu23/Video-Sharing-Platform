package Services;

import User.User;

import java.util.List;
import java.util.Map;

public class UserService extends Service<User> {
    private int current_user;

    public UserService() {
        super();
        this.current_user = -1;
    }

    public int getCurrentUserID() {
        return this.current_user;
    }

    public void register(User newUser) {
        for (User user: this.itemHashMap.values()) {
            if (newUser.equals(user)) {
                System.out.println("User with these credentials already exists.");
                return;
            }
        }

        this.add(newUser);
    }

    public void login(String email, String password) {
        User attempt = new User("", password, email);

        for (Map.Entry<Integer, User> entry: this.itemHashMap.entrySet()) {
            int id = entry.getKey();
            User user = entry.getValue();

            if (user.equals(attempt)) {
                this.current_user = id;
                System.out.println("Welcome, " + user.getName() + "!");
                return;
            }
        }

        System.out.println("There is no user with these credentials.");
    }

    private User getCurrentUser() {
        if (this.current_user == -1) {
            return null;
        }

        return this.get(this.current_user);
    }

    public List<Integer> getPosts() {
        User user = this.getCurrentUser();
        if (user == null) {
            return null;
        }

        return user.getPosts();
    }

    public List<Integer> getHistory() {User user = this.getCurrentUser();
        if (user == null) {
            return null;
        }

        return user.getHistory();
    }

    public void logout() {
        User user = this.getCurrentUser();

        if (user == null) {
            System.out.println("No user is logged in.");
            return;
        }

        current_user = -1;
        System.out.println("Goodbye, " + user.getName() + "!");
    }

    public void deleteAccount() {
        User user = this.getCurrentUser();

        if (user == null) {
            System.out.println("No user is logged in.");
            return;
        }

        String name = user.getName();

        this.remove(this.current_user);
        current_user = -1;
        System.out.println("Goodbye forever, " + name + "!");
    }

    public void sendComment(int commentID) {
        User user = this.getCurrentUser();
        if (user == null) {
            return;
        }

        user.addComment(commentID);
    }

    public void addToHistory(int postID) {
        User user = this.getCurrentUser();
        if (user == null) {
            return;
        }

        user.addToHistory(postID);
    }

    public void addToPosts(int postID) {
        User user = this.getCurrentUser();
        if (user == null) {
            return;
        }

        user.addToPosts(postID);
    }

    public void subscribe(int userID) {
        if (userID == this.current_user) {
            System.out.println("You cannot subscribe to yourself.");
            return;
        }

        User you = this.getCurrentUser();
        User them = this.get(userID);

        if (you == null || them == null) {
            System.out.println("Something went wrong retrieving user data.");
            return;
        }

        if (you.addToSubscribed(userID)) {
            them.addSubscriber();
        } else {
            them.removeSubscriber();
        }
    }

    public void addToPlaylist(int playlistID) {
        User user = this.getCurrentUser();
        if (user == null) {
            System.out.println("There is no user logged in.");
            return;
        }

        user.addToPlaylists(playlistID);
    }

    public void showData() {
        System.out.println(this.getCurrentUser());
    }

}