package hotel.view;

import hotel.model.Board;
import hotel.model.Game;
import hotel.model.Hotel;
import hotel.model.HotelTile;
import hotel.model.PlayerTile;
import hotel.model.Tile;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("serial")
class BoardUI extends javax.swing.JPanel {

    private GameUI frame;

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        Game model = getFrame().getModel();
        Board board = model.getBoard();

        // Determine size for each tile
        int rows = board.getRows();
        int cols = board.getColumns();
        int height = getHeight();
        int width = getWidth();
        int tileSize = Math.min(height / rows, width / cols);

        // Draw tiles
        for (int i = 0; i < rows; i++)
            for (int j = 0; j < cols; j++) {
                Tile t = board.get(i, j);
                Graphics gCopy = g.create();
                gCopy.translate(j * tileSize, i * tileSize);
                if (t instanceof HotelTile)
                    paintHotelTile((HotelTile) t, gCopy, tileSize);
                else
                    paintPlayerTile((PlayerTile) t, gCopy, tileSize);
            }

        // Draw players
        Point[] positions = model.getPlayerPositions();
        Map<Point, List<Integer>> pos = new HashMap<>();
        for (int k = 0; k < positions.length; k++) {
            if (positions[k] == null)
                continue;

            List<Integer> ps = pos.get(positions[k]);
            if (ps == null) {
                ps = new ArrayList<>();
                pos.put(positions[k], ps);
            }
            ps.add(k + 1);
        }

        pos.forEach((point, players) -> {
            Graphics gCopy = g.create();
            gCopy.translate(point.x * tileSize, point.y * tileSize);
            drawPlayers(players, gCopy, tileSize);
        });
    }

    private void paintHotelTile(HotelTile ht, Graphics g, int tileSize) {
        Hotel h = ht.getHotel();
        int p = h.getOwnerId();
        g.setColor((p > 0) ? getFrame().getPlayerColor(h.getOwnerId()) : Color.GRAY);
        g.fillRect(0, 0, tileSize, tileSize);

        String hid = String.valueOf(h.getId());
        drawString(hid, g, 4 * tileSize / 10);

        String state = String.valueOf(h.getCurrentBuild());
        Graphics gCopy = g.create();
        gCopy.translate(4 * tileSize / 10, 4 * tileSize / 10);
        drawString(state, gCopy, 6 * tileSize / 10); // it would be nice for this to be right-justified
    }

    private void paintPlayerTile(PlayerTile pt, Graphics g, int tileSize) {
        String t = null;
        Color c = null;
        switch (pt.getType()) {
            case START:
                t = "S";
                c = Color.CYAN;
                break;
            case CITY_HALL:
                t = "C";
                c = Color.DARK_GRAY;
                break;
            case BANK:
                t = "B";
                c = Color.LIGHT_GRAY;
                break;
            case BUY:
                t = "H";
                c = Color.PINK;
                break;
            case BUILD:
                t = "E";
                c = Color.YELLOW;
                break;
            case FREE:
                t = "F";
                c = Color.MAGENTA;
                break;
        }

        g.setColor(c);
        g.fillRect(0, 0, tileSize, tileSize);
        drawString(t, g, 4 * tileSize / 10);

        int entranceX = -1;
        int entranceY = -1;
        int entranceHeight = -1;
        int entranceWidth = -1;
        switch (pt.getEntrance()) {
            case EAST:
                entranceX = 9 * tileSize / 10;
                entranceY = 4 * tileSize / 10;
                entranceHeight = 2 * tileSize / 10;
                entranceWidth = 1 * tileSize / 10;
                break;
            case SOUTH:
                entranceX = 4 * tileSize / 10;
                entranceY = 9 * tileSize / 10;
                entranceHeight = 1 * tileSize / 10;
                entranceWidth = 2 * tileSize / 10;
                break;
            case WEST:
                entranceX = 0 * tileSize / 10;
                entranceY = 4 * tileSize / 10;
                entranceHeight = 2 * tileSize / 10;
                entranceWidth = 1 * tileSize / 10;
                break;
            case NORTH:
                entranceX = 4 * tileSize / 10;
                entranceY = 0 * tileSize / 10;
                entranceHeight = 1 * tileSize / 10;
                entranceWidth = 2 * tileSize / 10;
                break;
        }

        if (entranceX > -1) {
            g.setColor(Color.ORANGE);
            g.fillRect(entranceX, entranceY, entranceWidth, entranceHeight);
        }
    }

    @SuppressWarnings("fallthrough")
    private void drawPlayers(List<Integer> players, Graphics g, int tileSize) {
        GameUI f = getFrame();
        switch (players.size()) {
            case 3:
                // Draw third player on the bottom-left
                g.setColor(f.getPlayerColor(players.get(2)));
                g.fillOval(0 * tileSize / 10, 6 * tileSize / 10, 4 * tileSize / 10, 4 * tileSize / 10);
            // Intentional fall-through
            case 2:
                // Draw second player on the top-right
                g.setColor(f.getPlayerColor(players.get(1)));
                g.fillOval(6 * tileSize / 10, 0 * tileSize / 10, 4 * tileSize / 10, 4 * tileSize / 10);
            // Intentional fall-through
            case 1:
                // Draw third player on the center
                g.setColor(f.getPlayerColor(players.get(0)));
                g.fillOval(3 * tileSize / 10, 3 * tileSize / 10, 4 * tileSize / 10, 4 * tileSize / 10);
        }
    }

    private Font optimalFont(Graphics g, int targetHeight) {
        int currHeight = g.getFontMetrics().getHeight();
        Font f = g.getFont();
        // For simplicity's sake assume that font height is linear to font size.
        // Anyway, this is far from producing a font with glyphs that actually
        // render close to the specified height...
        return f.deriveFont((float) targetHeight / currHeight * f.getSize2D());
    }

    private void drawString(String s, Graphics g, int height) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(Color.BLACK);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(optimalFont(g2, height));
        g2.drawString(s, 0, g2.getFontMetrics().getHeight());
    }

    private GameUI getFrame() {
        if (frame == null)
            frame = (GameUI) javax.swing.SwingUtilities.getAncestorOfClass(GameUI.class, this);
        return frame;
    }
}
