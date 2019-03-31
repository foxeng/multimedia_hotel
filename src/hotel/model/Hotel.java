package hotel.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class Hotel {

    private final int id;
    private String name;
    private int buyingCost, obligBuyingCost;
    private int entranceCost;
    private ArrayList<Integer> buildingCosts = new ArrayList<>();
    private ArrayList<Integer> stayingCosts = new ArrayList<>();
    private Player owner;
    private int state = -1;     // -1 -> not built, >= 0 -> built

    Hotel(File f, int id) {
        this.id = id;
        try (Scanner s = new Scanner(new BufferedReader(new FileReader(f))).useDelimiter(",|(\\s+)")) {
            name = s.nextLine();
            buyingCost = s.nextInt();
            obligBuyingCost = s.nextInt();
            entranceCost = s.nextInt();
            while (s.hasNextInt()) {
                buildingCosts.add(s.nextInt());
                stayingCosts.add(s.nextInt());
            }
        } catch (FileNotFoundException e) {
            System.err.println("File " + f.toString() + " not found");
            System.exit(1);
        } catch (NoSuchElementException e) {
            System.err.println("Corrupted hotel description file " + f.toString());
            System.exit(1);
        }
    }

    /**
     *
     * @return
     */
    public int getId() {
        return id;
    }

    /**
     *
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     *
     * @return
     */
    public int getOwnerId() {
        return (owner != null) ? owner.getId() : 0;
    }

    /**
     *
     * @return
     */
    public int getBuyingCost() {
        return buyingCost;
    }

    /**
     *
     * @return
     */
    public int getObligBuyingCost() {
        return obligBuyingCost;
    }

    /**
     *
     * @return
     */
    public int getEntranceCost() {
        return entranceCost;
    }

    /**
     *
     * @return
     */
    public int[] getBuildingCosts() {
        return buildingCosts.stream().mapToInt(c -> c).toArray();
    }

    /**
     *
     * @return
     */
    public int[] getStayingCosts() {
        return stayingCosts.stream().mapToInt(c -> c).toArray();
    }

    /**
     *
     * @return
     */
    public int getCurrentBuild() {
        return state;
    }

    Player getOwner() {
        return owner;
    }

    void setOwner(Player owner) {
        this.owner = owner;
    }

    int getBuildingCost() {
        if (state + 1 == buildingCosts.size())
            return 0;
        return buildingCosts.get(state + 1);
    }

    int getStayingCost() {
        if (state == -1)
            return 0;
        return stayingCosts.get(state);
    }

    boolean isBuilt() {
        return state >= 0;
    }

    void upgrade() {
        if (state + 1 < buildingCosts.size())
            state++;
    }

    void tearDown() {
        state = -1;
    }
}
