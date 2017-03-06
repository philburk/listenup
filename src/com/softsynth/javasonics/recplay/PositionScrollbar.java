package com.softsynth.javasonics.recplay;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import com.softsynth.awt.SafeImageFactory;

/**
 * Show value between 0.0 and 1.0.
 * 
 * @author (C) 1997 Phil Burk, SoftSynth.com, All Rights Reserved
 */

/* ========================================================================== */
public class PositionScrollbar extends PositionCanvas
{
    int thumbWidth = 8;
    int thumbHeight = 17;
    Image thumbImageEnabled;
    Image thumbImageDisabled;
    boolean useImage = true;

    Integer dragged = new Integer(0);
    Integer released = new Integer(1);
    private boolean bevelled = false;

    int valueToX(double value)
    {
        int xspan = bounds().width;
        int x1 = (int) ((xspan - thumbWidth) * value);
        return x1 + (thumbWidth >> 1) + 1;
    }

    double xToValue(int x)
    {
        int xspan = bounds().width;
        double vv = ((double) (x - (thumbWidth >> 1) - 1)) / ((double) (xspan - thumbWidth));
        return vv;
    }

    private Image createThumbImage(Color color)
    {
        Image img = SafeImageFactory.createImage( this, thumbWidth, thumbHeight);
        Graphics gi = img.getGraphics();
        gi.setColor(color);
        gi.fill3DRect(0, 0, thumbWidth, thumbHeight, true);
        gi.setColor(getForeground());
        gi.drawLine(thumbWidth / 2, (int) (thumbHeight * 0.2), thumbWidth / 2,
                (int) (thumbHeight * 0.8));
        return img;
    }

    protected void sizeChanged(int width, int height)
    {
        super.sizeChanged(width, height);
        thumbHeight = (int) (0.8 * height);
        if (useImage)
        {
            thumbImageEnabled = createThumbImage(getBackground());
            thumbImageDisabled = thumbImageEnabled;
            //            thumbImageDisabled = createThumbImage( getBackground().darker()
            // );
        }
    }

    public void drawBackground(Graphics g, int width, int height)
    {
        if (isBevelled())
        {
            g.setColor(getBackground());
            g.fill3DRect(0, 0, width, height, false);
        }
        g.setColor(getForeground());
        int x1 = valueToX(0.0);
        int x2 = valueToX(1.0);
        g.drawLine(x1, height / 2, x2, height / 2);
    }

    /* Override default paint action. */
    public void paint(Graphics g)
    {
        checkSize();
        int width = bounds().width;
        int height = bounds().height;
        drawBackground(g, width, height);
        int xp = valueToX(value);
        //		g.fillOval( xp - (thumbWidth>>1), (height - thumbHeight)>>1,
        // thumbWidth, thumbHeight );
        g.setColor(Color.gray);
        Image img = isEnabled() ? thumbImageEnabled : thumbImageDisabled;
        g.drawImage(img, xp - (thumbWidth >> 1), 1 + (height - thumbHeight) >> 1, this);
    }

    public static void main(String[] args)
    {
        Frame f = new Frame("PositionScrollbar");
        f.addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent e)
            {
                System.exit(0);
            }
        });
        final PositionScrollbar sb1 = new PositionScrollbar();
        sb1.addObserver(new Observer()
        {
            public void update(Observable arg0, Object arg1)
            {
                double value = sb1.getValue();
                System.out.println("Value " + value);
            }
        });
        f.add(sb1);
        f.pack();
        f.setVisible(true);
    }

    /**
     * @return Returns whether bevelled.
     */
    public boolean isBevelled()
    {
        return bevelled;
    }

    /**
     * @param bevelled
     *            The bevelled to set.
     */
    public void setBevelled(boolean bevelled)
    {
        this.bevelled = bevelled;
    }
}