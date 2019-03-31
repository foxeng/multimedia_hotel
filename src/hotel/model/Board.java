package hotel.model;

import java.awt.Point;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

public class Board {

    private final int BOARD_ROWS = 12;
    private final int BOARD_COLUMNS = 15;
    private final Tile[][] board = new Tile[BOARD_ROWS][BOARD_COLUMNS];
    private final Map<Hotel, Set<PlayerTile>> hotelFronts = new HashMap<>();
    private PlayerTile start;

    Board(File f, Map<Integer, Hotel> hotels) {
        try (Scanner s = new Scanner(new BufferedReader(new FileReader(f))).useDelimiter(",|(\\s+)")) {
            for (int i = 0; i < BOARD_ROWS; i++)
                for (int j = 0; j < BOARD_COLUMNS; j++)
                    if (s.hasNextInt()) {
                        int id = s.nextInt();
                        board[i][j] = new HotelTile(hotels.get(id));
                    } else
                        switch (s.next()) {
                            case "S":
                                start = new PlayerTile(PlayerTile.Type.START);
                                board[i][j] = start;
                                break;
                            case "C":
                                board[i][j] = new PlayerTile(PlayerTile.Type.CITY_HALL);
                                break;
                            case "B":
                                board[i][j] = new PlayerTile(PlayerTile.Type.BANK);
                                break;
                            case "H":
                                board[i][j] = new PlayerTile(PlayerTile.Type.BUY);
                                break;
                            case "E":
                                board[i][j] = new PlayerTile(PlayerTile.Type.BUILD);
                                break;
                            case "F":
                                board[i][j] = new PlayerTile(PlayerTile.Type.FREE);
                                break;
                        }
        } catch (FileNotFoundException e) {
            System.err.println("File " + f.toString() + " not found");
            System.exit(1);
        }

        determinePath();
        // determineHotelFronts should be called after determinePath
        determineHotelFronts();
    }

    /**
     * Returns the number of rows on this board.
     * 
     * @return the number of rows on this board
     */
    public int getRows() {
        return BOARD_ROWS;
    }

    /**
     * Returns the number of columns on this board.
     * 
     * @return the number of columns on this board
     */
    public int getColumns() {
        return BOARD_COLUMNS;
    }

    /**
     * Returns the tile at the specified position on this board.
     * 
     * @param i row of the Tile to return
     * @param j column of the Tile to return
     *
     * @return the Tile at the specified position on this board or null if the
     * index is out of range
     */
    public Tile get(int i, int j) {
        if (!validIndex(i, j))
            return null;
        return board[i][j];
    }

    PlayerTile getStart() {
        return start;
    }

    Hotel getHotel(PlayerTile pt) {
        Point p = indexOf(pt);
        switch (pt.getEntrance()) {
            case EAST:
                return ((HotelTile) board[p.y][p.x + 1]).getHotel();
            case SOUTH:
                return ((HotelTile) board[p.y + 1][p.x]).getHotel();
            case WEST:
                return ((HotelTile) board[p.y][p.x - 1]).getHotel();
            case NORTH:
                return ((HotelTile) board[p.y - 1][p.x]).getHotel();
            default:
                return null;
        }
    }

    Map<PlayerTile.Entrance, Hotel> getHotels(PlayerTile pt) {
        Map<PlayerTile.Entrance, Hotel> hotels = new HashMap<>();

        Point p = indexOf(pt);
        if (p.x + 1 < BOARD_COLUMNS) {
            HotelTile ht;
            try {
                ht = (HotelTile) board[p.y][p.x + 1];
                hotels.put(PlayerTile.Entrance.EAST, ht.getHotel());
            } catch (ClassCastException _e) {
                // east neighbor not a hotel, ignore it
            }
        }
        if (p.y + 1 < BOARD_ROWS) {
            HotelTile ht;
            try {
                ht = (HotelTile) board[p.y + 1][p.x];
                hotels.put(PlayerTile.Entrance.SOUTH, ht.getHotel());
            } catch (ClassCastException _e) {
                // south neighbor not a hotel, ignore it
            }
        }
        if (p.x - 1 >= 0) {
            HotelTile ht;
            try {
                ht = (HotelTile) board[p.y][p.x - 1];
                hotels.put(PlayerTile.Entrance.WEST, ht.getHotel());
            } catch (ClassCastException _e) {
                // west neighbor not a hotel, ignore it
            }
        }
        if (p.y - 1 >= 0) {
            HotelTile ht;
            try {
                ht = (HotelTile) board[p.y - 1][p.x];
                hotels.put(PlayerTile.Entrance.NORTH, ht.getHotel());
            } catch (ClassCastException _e) {
                // north neighbor not a hotel, ignore it
            }
        }

        return hotels;
    }

    Set<PlayerTile> getHotelFront(Hotel h) {
        // Return a copy because we don't want modifications of hotelFronts via
        // this interface. Moreover, returning hotelFront would sooner or later
        // lead to ConcurrentModificationsExceptions
        return new HashSet<>(hotelFronts.get(h));
    }

    Point indexOf(Tile t) {
        for (int i = 0; i < BOARD_ROWS; i++)
            for (int j = 0; j < BOARD_COLUMNS; j++)
                if (board[i][j] == t)
                    return new Point(j, i);
        return null;
    }

    private boolean validIndex(int i, int j) {
        return i >= 0 && i < BOARD_ROWS && j >= 0 && j < BOARD_COLUMNS;
    }

    private void determinePath() {
        // Find the first PlayerTile from the top left that's not free
        int i = 0, j = 0;
        while (!(board[i][j] instanceof PlayerTile)
                || (((PlayerTile) board[i][j]).getType() == PlayerTile.Type.FREE)) {
            j = (j < BOARD_COLUMNS - 1) ? (j + 1) : 0;
            if (j == 0)
                i++;
        }

        // The next of the found tile is the one on its right
        PlayerTile prev = (PlayerTile) board[i][j];
        prev.setNext((PlayerTile) board[i][j + 1]);

        // Set the next for the rest of the tiles
        PlayerTile curr = prev.getNext();
        j++;
        while (curr.getNext() == null) {
            PlayerTile pt = null;
            if ((j + 1 < BOARD_COLUMNS) && (board[i][j + 1] instanceof PlayerTile)
                    && (pt = (PlayerTile) board[i][j + 1]).getType() != PlayerTile.Type.FREE
                    && pt != prev) {
                curr.setNext(pt);
                j++;
            } else if ((i + 1 < BOARD_ROWS) && (board[i + 1][j] instanceof PlayerTile)
                    && (pt = (PlayerTile) board[i + 1][j]).getType() != PlayerTile.Type.FREE
                    && pt != prev) {
                curr.setNext(pt);
                i++;
            } else if ((j - 1 >= 0) && (board[i][j - 1] instanceof PlayerTile)
                    && (pt = (PlayerTile) board[i][j - 1]).getType() != PlayerTile.Type.FREE
                    && pt != prev) {
                curr.setNext(pt);
                j--;
            } else if ((i - 1 >= 0) && (board[i - 1][j] instanceof PlayerTile)
                    && (pt = (PlayerTile) board[i - 1][j]).getType() != PlayerTile.Type.FREE
                    && pt != prev) {
                curr.setNext(pt);
                i--;
            }
            prev = curr;
            curr.setNext(pt);
            curr = pt;
        }
    }

    private void determineHotelFronts() {
        PlayerTile pt = start;
        do {
            for (Map.Entry<PlayerTile.Entrance, Hotel> e : getHotels(pt).entrySet()) {
                hotelFronts.putIfAbsent(e.getValue(), new HashSet<>());
                hotelFronts.get(e.getValue()).add(pt);
            }
            pt = pt.getNext();
        } while (pt != start);
    }
}
