package com.softsynth.awt;
import java.awt.*;
/**
 * A Panel with an inset border.
 *
 * @author (C) 2003 Phil Burk, All Rights Reserved
 */

public class InsetPanel extends Panel {

    protected int insetSize = 4;

    public InsetPanel(int insetSize) {
        setInset(insetSize);
    }

    public void setInset(int inset) {
        insetSize = inset;
    }

    public Insets getInsets() {
        return new Insets(insetSize, insetSize, insetSize, insetSize);
    }

}
