package hotel.model;

import java.util.concurrent.ThreadLocalRandom;

class Utils {

    static int randomInt(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);   // random int in [min, max]
    }

    static int rollDice() {
        return randomInt(1, 6);
    }
}
