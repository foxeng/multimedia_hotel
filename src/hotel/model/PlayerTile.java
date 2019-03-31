package hotel.model;

public class PlayerTile extends Tile {

    private final Type type;
    private PlayerTile next;    // move to Board?
    private Entrance entrance = Entrance.NONE;

    /**
     *
     */
    public enum Type {
        START, CITY_HALL, BANK, BUY, BUILD, FREE
    }

    /**
     *
     */
    public enum Entrance {
        NONE, EAST, SOUTH, WEST, NORTH
    }

    PlayerTile(Type type) {
        this.type = type;
    }

    /**
     *
     * @return
     */
    public Type getType() {
        return type;
    }

    /**
     *
     * @return
     */
    public Entrance getEntrance() {
        return entrance;
    }

    PlayerTile getNext() {
        return next;
    }

    void setNext(PlayerTile next) {
        this.next = next;
    }

    void setEntrance(Entrance entrance) {
        this.entrance = entrance;
    }

}
