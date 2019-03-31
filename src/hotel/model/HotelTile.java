package hotel.model;

public class HotelTile extends Tile {

    private final Hotel hotel;

    HotelTile(Hotel hotel) {
        this.hotel = hotel;
    }

    /**
     *
     * @return
     */
    public Hotel getHotel() {
        return hotel;
    }
}
