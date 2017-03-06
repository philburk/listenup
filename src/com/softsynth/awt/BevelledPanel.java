package com.softsynth.awt;
import java.awt.*;

/**
 * A Panel with a bevelled inset border.
 *
 * @author (C) 2003 Phil Burk, All Rights Reserved
 */

public class BevelledPanel extends InsetPanel {

    public BevelledPanel(int insetSize) {
        super(insetSize);
    }

    /**
     * Draw bevel and label.
     */
    public void paint(Graphics g) {
        super.paint(g);
        int width = getWidth();
        int height = getHeight();

        // draw left edge
        g.setColor( ColorTools.brighter( getBackground(), 0.7 ) );
        int start = 0;
        int end = height;
        for (int i = 0; i < insetSize; i++) {
            g.drawLine(i, start, i, end);
            start += 1;
            end -= 1;
        }
        // draw top edge
        start = 0;
        end = width;
        for (int i = 0; i < insetSize; i++) {
            g.drawLine(start, i, end, i);
            start += 1;
            end -= 1;
        }
        // draw right edge
        g.setColor(getBackground().darker());
        start = 0;
        end = height;
        int x = width - 1;
        for (int i = 0; i < insetSize; i++) {
            g.drawLine( x, start, x, end);
            start += 1;
            end -= 1;
            x -= 1;
        }
        // draw bottom edge
        start = 0;
        end = width;
        int y = height - 1;
        for (int i = 0; i < insetSize; i++) {
            g.drawLine(start, y, end, y);
            start += 1;
            end -= 1;
            y -= 1;
        }
    }
}