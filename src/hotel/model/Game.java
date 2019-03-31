package hotel.model;

import hotel.view.GameUI;
import java.awt.Point;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Game {

    /**
     * NOTE: it would be better to implement the observer pattern (this as the
     * observable, GAMEUI as the observer, either directly or using JavaBeans.
     */
    private final GameUI view;
    private final Board board;
    private final Map<Integer, Hotel> hotels = new HashMap<>();
    private final int PLAYERS = 3;
    private final List<Player> players = new ArrayList<>();
    private final Map<Player, PlayerTile> positions = new HashMap<>();
    private Player current;
    private final Player bank = null;

    /**
     * Constructs a game model from a random configuration, linked with the
     * view specified. The configuration is picked in random among the ones
     * available, lying in directories inside ./board/. The players' turns are
     * also determined randomly.
     * 
     * @param view the view this game model is paired with
     */
    public Game(GameUI view) {
        this.view = view;
        view.setRequestMoneyEnabled(false);
        view.setBuyHotelEnabled(false);
        view.setRequestBuildingEnabled(false);
        view.setBuyEntranceEnabled(false);

        // Read game configuration (pick a random one)
        File[] boardsDirs = new File("boards").listFiles(f -> f.isDirectory());
        if (boardsDirs == null) {
            System.err.println("No \"boards\" directory found");
            System.exit(1);
        }
        File boardDir = boardsDirs[Utils.randomInt(0, boardsDirs.length - 1)];
        readHotels(boardDir);
        board = new Board(new File(boardDir, "board.txt"), hotels);
        view.setAvailableHotels(hotels.size());

        // Create players, randomly pick their turns and position them on the start
        for (int i = 1; i <= PLAYERS; i++)
            players.add(new Player(i));
        Collections.shuffle(players);
        current = players.get(players.size() - 1);  // current will be set to the next player in playRound
        players.forEach(p -> positions.put(p, board.getStart()));
        view.setPlayerTurns(players.stream().mapToInt(p -> p.getId()).toArray());
        players.stream().forEach(p -> view.setPlayerMoney(p.getId(), p.getMoney()));
    }

    /**
     * Proceed to the next round in this game. The player whose turn is next
     * from the current one becomes the current. The dice is rolled and the
     * current player advances as many tiles as its result (or to the next tile
     * not occupied by another player if the target tile is occupied). If the
     * tile the player eventually lands on has an entrance for a hotel owned by
     * another player, the current pays the owner for the stay. In this case, if
     * the current player doesn't have enough money to make this payment and he
     * is eligible for money from the bank, he is awarded this money
     * automatically and the condition is checked again. If he doesn't have
     * enough money and he is not eligible for money from the bank, he goes
     * bankrupt and the owner of the hotel is awarded all of the current
     * player's remaining money.
     */
    public void playRound() {
        view.setRequestMoneyEnabled(false);    // a player can only get money from the bank on the round he goes through it
        view.setBuyHotelEnabled(false);
        view.setRequestBuildingEnabled(false);
        view.setBuyEntranceEnabled(false);

        // Determine next player
        current = nextPlayer();
        view.setCurrentPlayer(current.getId());

        // Roll dice
        int dice = Utils.rollDice();
        view.setDiceResult(dice);

        // Move player
        List<PlayerTile> path = playerMovement(dice);
        PlayerTile dest = path.get(path.size() - 1);
        positions.put(current, dest);
        view.setPlayerMoved();

        // Determine what the player can do in this round
        boolean passedBank = path.subList(1, path.size()).stream().anyMatch(pt -> pt.getType() == PlayerTile.Type.BANK);    // only search in new tiles the player visits
        view.setRequestMoneyEnabled(passedBank);
        boolean passedCityHall = path.subList(1, path.size()).stream().anyMatch(pt -> pt.getType() == PlayerTile.Type.CITY_HALL);    // only search in new tiles the player visits
        view.setBuyEntranceEnabled(passedCityHall);
        if (dest.getType() == PlayerTile.Type.BUY) {
            boolean adjacentNotBuilt = board.getHotels(dest).values().stream().anyMatch(h -> !h.isBuilt());
            if (adjacentNotBuilt)
                view.setBuyHotelEnabled(true);
        } else if (dest.getType() == PlayerTile.Type.BUILD) {
            view.setBuyEntranceEnabled(true);
            view.setRequestBuildingEnabled(true);
        }

        // If there is an entrance on dest, pay for the stay
        if (dest.getEntrance() != PlayerTile.Entrance.NONE) {
            // Determine how much to pay and to whom
            Hotel h = board.getHotel(dest);
            int cost = h.getStayingCost() * dice;
            Player owner = h.getOwner(); // owner != null because there is an entrance
            if (owner != current)
                if (current.getMoney() < cost) {
                    if (passedBank) // Current is eligible for money from the bank, that might save him

                        requestMoney();
                    if (current.getMoney() < cost) // Current is going bankrupt

                        goBankrupt(owner);
                } else
                    moveMoney(current, owner, cost);
        }
    }

    /**
     * Awards 1000 MLs to the current player.
     */
    public void requestMoney() {
        // Check for the precondition here? (to not rely on the view)
        moveMoney(bank, current, 1000);
        view.setRequestMoneyEnabled(false);
    }

    /**
     * Purchases the hotel specified for the current player. For this to succeed
     * the id specified should be a valid hotel id and the hotel should be
     * adjacent to the player tile the current player is currently on.
     * Additionally, the hotel should not be owned by the current player and, if
     * owned by another player it should not be built. The hotel's cost is the
     * buying price if no one owns the hotel already and the obligatory buying
     * price if not. The current player should have at least as much money as
     * this cost. If all of the above hold true the money is withdrawn from the
     * player and in the case of buying from another player, it is deposited to
     * the original owner and the current player becomes the owner of the
     * specified hotel. Otherwise no change to any player's money or properties
     * happens.
     * 
     * @param hid the hotel id of the hotel to purchase
     *
     * @return true if the hotel was purchased successfully, false otherwise
     */
    public boolean buyHotel(int hid) {
        Hotel h = hotels.get(hid);
        if (h == null)
            return false; // hid is not a valid hotel id
        Map<PlayerTile.Entrance, Hotel> adjacentHotels = board.getHotels(positions.get(current));
        if (!adjacentHotels.containsValue(h))
            return false; // h is not adjacent to current player's tile
        Player owner = h.getOwner();
        if (owner == current)
            return false; // h already owned by current
        else if (owner != null && h.isBuilt())
            return false; // h is owned by another player and already built
        int cost = (owner == null) ? h.getBuyingCost() : h.getObligBuyingCost();
        if (current.getMoney() < cost)
            return false; // current can't afford it

        moveMoney(current, owner, cost);
        moveHotel(owner, current, h);
        view.setBuyHotelEnabled(false);    // the player can buy only 1 hotel per round
        return true;
    }

    /**
     * Processes a request to build for the hotel specified. For this to proceed
     * the id specified should be a valid hotel id and the hotel should be owned
     * by the current player and it should not already be fully built. If all of
     * the above hold true, the process advances as follows: with a 50% chance
     * the cost is the building cost for the next available upgrade for the
     * specified hotel, with a 15% chance it is zero, with a 15% chance it is
     * double the building cost mentioned earlier and with a 20% chance the
     * request is rejected. Except in the latter case, if the current player has
     * enough money to cover the cost determined, this money is withdrawn from
     * him and the hotel is upgraded to the next building state available for
     * it. In any other case, no change to the player's money or the hotel's
     * status happens.
     * 
     * @param hid the hotel id of the hotel to build for
     *
     * @return true if the upgrade was completed successfully, false otherwise
     */
    public boolean requestBuilding(int hid) {
        // Check here that the player is on the right tile? (to not rely on the view)
        Hotel h = hotels.get(hid);
        if (h == null)
            return false; // hid is not a valid hotel id
        if (h.getOwner() != current)
            return false; // h not owned by current
        if (h.getBuildingCost() == 0)
            return false; // h can't be further upgraded
        int cost;
        int rand = Utils.randomInt(1, 100);
        if (rand <= 50)
            cost = h.getBuildingCost();
        else if (rand <= 70)
            return false; // request rejected
        else if (rand <= 85)
            cost = 0;
        else
            cost = 2 * h.getBuildingCost();

        if (current.getMoney() < cost)
            return false; // current can't afford it

        moveMoney(current, bank, cost);
        h.upgrade();
        view.setRequestBuildingEnabled(false);    // the player can build only once per round
        // notify the view of the upgrade? for now, it keeps track and updates itself if the request is granted
        return true;
    }

    /**
     * Purchases an entrance for the hotel specified. For this to succeed the
     * id specified should be a valid hotel id, the hotel should be owned by the
     * current player and should be built. In addition, the current player
     * should have at least as much money as the entrance price for the
     * specified hotel and there should be at least one player tile available
     * (not containing an entrance and of type "BUY" or "BUILD") adjacent to
     * the hotel. If all of the above hold true, the money is withdrawn from
     * the player and an entrance is placed on a player tile picked in random
     * among the available ones, oriented towards the specified hotel. Otherwise
     * no change to the player's money happens and no entrance is added.
     * 
     * @param hid the hotel id of the hotel to add the entrance to
     *
     * @return true if the entrance was purchased successfully, false otherwise
     */
    public boolean buyEntrance(int hid) {
        // Check here that the player is on the right tile? (to not rely on the view)
        Hotel h = hotels.get(hid);
        if (h == null)
            return false; // hid is not a valid hotel id
        if (h.getOwner() != current)
            return false; // h not owned by current
        if (!h.isBuilt())
            return false; // h not built
        int cost = h.getEntranceCost();
        if (current.getMoney() < cost)
            return false; // current can't afford it
        for (PlayerTile pt : board.getHotelFront(h))
            if (pt.getEntrance() == PlayerTile.Entrance.NONE
                    && (pt.getType() == PlayerTile.Type.BUILD
                    || pt.getType() == PlayerTile.Type.BUY)) {
                moveMoney(current, bank, cost);
                // Need to determine on which side of the tile the hotel lies
                for (Map.Entry<PlayerTile.Entrance, Hotel> e : board.getHotels(pt).entrySet())
                    if (e.getValue() == h) {
                        pt.setEntrance(e.getKey());
                        view.setBuyEntranceEnabled(false);    // the player can buy only 1 entrance per round
                        return true;
                    }
            }
        return false;   // no empty tile in h's front
    }

    /**
     * Returns the maximum amount of money recorded during this game for each
     * player. The amounts are ordered by ascending player id. If a player is
     * bankrupt, the corresponding amount is present as normal.
     * 
     * @return the maximum amount of money recorded for each player
     */
    public int[] getPlayersMaxMoney() {
        int[] maxMoney = new int[PLAYERS];
        players.forEach(p -> {
            maxMoney[p.getId() - 1] = p.getMaxMoney();
        });
        return maxMoney;
    }

    /**
     * Returns the number of entrances for all the hotels owned by each player.
     * The numbers of entrances are ordered by ascending player id. If a player
     * is bankrupt, the corresponding number of entrances is present, but zero.
     * 
     * @return the number of entrances for all the hotels owned by each player
     */
    public int[] getPlayersEntrances() {
        int[] entrances = new int[PLAYERS]; // report even for bankrupt players
        for (Player p : players)
            for (Hotel h : p.getHotels())
                for (PlayerTile pt : board.getHotelFront(h))
                    if (board.getHotel(pt) == h)
                        entrances[p.getId() - 1]++;
        return entrances;
    }

    /**
     * Returns the positions of all the players. The positions are ordered by
     * ascending player id. If a player is bankrupt, his position is present,
     * but null.
     * 
     * @return the positions of all the players
     */
    public Point[] getPlayerPositions() {   // return a map playerId -> position?
        Point[] pos = new Point[PLAYERS];   // report even for bankrupt players
        positions.forEach((p, pt) -> {
            pos[p.getId() - 1] = board.indexOf(pt);
        });

        return pos;
    }

    /**
     * Returns the hotel with the id specified.
     * 
     * @param hid the hotel id of the hotel to return
     *
     * @return the hotel with the id specified or null if the id is invalid
     */
    public Hotel getHotel(int hid) {
        return hotels.get(hid);
    }

    /**
     * Returns the board for this game.
     * 
     * @return the board for this game
     */
    public Board getBoard() {
        return board;
    }

    /**
     * Returns the ids of all the hotels for in this game.
     * 
     * @return the ids of all the hotels for in this game
     */
    public int[] getHotelIds() {
        return hotels.keySet().stream().mapToInt(hid -> hid).toArray();
    }

    private Player nextPlayer() {
        int turn = players.indexOf(current);
        Player next;
        do {
            turn++;
            next = players.get(turn % players.size());
        } while (!next.isActive());

        return next;
    }

    private List<PlayerTile> playerMovement(int steps) {
        List<PlayerTile> path = new ArrayList<>();
        PlayerTile dest = positions.get(current);
        path.add(dest);
        for (int i = 0; (i < steps) || (playerInTile(dest) != null); i++) {
            dest = dest.getNext();
            path.add(dest);
        }

        return path;
    }

    private void goBankrupt(Player creditor) {
        moveMoney(current, creditor, current.getMoney());
        for (Hotel ch : current.getHotels()) {
            for (PlayerTile pt : board.getHotelFront(ch))
                if (board.getHotel(pt) == ch)
                    pt.setEntrance(PlayerTile.Entrance.NONE);
            ch.tearDown();
            moveHotel(current, bank, ch);
        }
        current.setActive(false);
        positions.put(current, null);

        view.setPlayerBankrupt(current.getId());
        view.setRequestMoneyEnabled(false);
        view.setBuyHotelEnabled(false);
        view.setRequestBuildingEnabled(false);
        view.setBuyEntranceEnabled(false);

        // Check if the game is over
        if (players.stream().filter(p -> p.isActive()).count() == 1) {
            // Game over
            Player winner = players.stream().filter(p -> p.isActive()).findFirst().get();
            view.setWinner(winner.getId());
        }
    }

    private void readHotels(File hd) {
        File[] hotelFiles = hd.listFiles((_f, n) -> !n.equals("board.txt"));
        for (File f : hotelFiles) {
            int id = Integer.parseInt(f.getName().replaceFirst("\\.txt", ""));
            hotels.put(id, new Hotel(f, id));
        }
    }

    private Player playerInTile(PlayerTile pt) {
        return players.stream().filter(p -> positions.get(p) == pt).findFirst().orElse(null);
    }

    private void moveMoney(Player from, Player to, int money) {
        if (from != null) {
            from.pay(money);
            view.setPlayerMoney(from.getId(), from.getMoney());
        }
        if (to != null) {
            to.earn(money);
            view.setPlayerMoney(to.getId(), to.getMoney());
        }
    }

    private void moveHotel(Player from, Player to, Hotel h) {
        if (from != null)
            from.loseHotel(h);
        if (to != null)
            to.acquireHotel(h);
        h.setOwner(to);
        view.setAvailableHotels((int) hotels.entrySet().stream().filter(e -> e.getValue().getOwner() == null).count());
    }
}
