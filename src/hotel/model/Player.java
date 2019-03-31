package hotel.model;

import java.util.HashSet;
import java.util.Set;

class Player {

    private final int id;
    private int money = 12000;
    private int maxMoney = money;
    private boolean active = true;
    private final Set<Hotel> hotels = new HashSet<>();

    Player(int id) {
        this.id = id;
    }

    int getId() {
        return id;
    }

    int getMoney() {
        return money;
    }

    int getMaxMoney() {
        return maxMoney;
    }

    void earn(int money) {
        this.money += money;
        if (this.money > maxMoney)
            maxMoney = this.money;
    }

    void pay(int money) {
        this.money -= money;
    }

    boolean isActive() {
        return active;
    }

    void setActive(boolean active) {
        this.active = active;
    }

    boolean owns(Hotel h) {
        return hotels.contains(h);
    }

    Set<Hotel> getHotels() {
        // Return a copy because we don't want modifications of hotels via this
        // interface. Moreover, returning hotels would sooner or later lead to
        // ConcurrentModificationsExceptions
        return new HashSet<>(hotels);
    }

    void acquireHotel(Hotel h) {
        hotels.add(h);
    }

    void loseHotel(Hotel h) {
        hotels.remove(h);
    }
}
