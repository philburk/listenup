/*
 * Created by Nick on Oct 9, 2004
 *
 */
package com.softsynth.javasonics.recplay;

import java.util.*;

/**
 * @author Nick Didkovsky, (c) 2004 All rights reserved, Email:
 *         didkovn@mail.rockefeller.edu
 *  
 */
public class TimeStampedAudioBufferList
{

    Vector timeStampedAudioBuffers;

    TimeStampedAudioBufferList()
    {
        timeStampedAudioBuffers = new Vector();
    }

    public void add(AudioBufferTimeStamp ts)
    {
        timeStampedAudioBuffers.addElement(ts);
    }

    public void clear()
    {
        timeStampedAudioBuffers.removeAllElements();
    }

    public int timeToFrame(double time)
    {
        int indexOfBuffer = -1;
        double closestTimeStamp = -1;
        int closestStartFrame = -1;
        for (int i = 0; i < timeStampedAudioBuffers.size(); i++)
        {
            AudioBufferTimeStamp abts = (AudioBufferTimeStamp) timeStampedAudioBuffers
                    .elementAt(i);
            closestTimeStamp = abts.getTimeStamp();
            closestStartFrame = abts.getStartFrame();
            //            System.out.println(closestTimeStamp + ", " + closestStartFrame);
            if (closestTimeStamp >= time)
            {
                //                System.out.println("BRREAK");
                indexOfBuffer = i;
                break;
            }
        }

        if (indexOfBuffer <= 0)
        {
            return closestStartFrame;
        }
        AudioBufferTimeStamp abts1 = (AudioBufferTimeStamp) timeStampedAudioBuffers
                .elementAt(indexOfBuffer - 1);
        AudioBufferTimeStamp abts2 = (AudioBufferTimeStamp) timeStampedAudioBuffers
                .elementAt(indexOfBuffer);
        double x1 = abts1.getTimeStamp();
        double y1 = abts1.getStartFrame();
        double x2 = abts2.getTimeStamp();
        double y2 = abts2.getStartFrame();
        double slope = (y2 - y1) / (x2 - x1);
        int y = (int) (slope * (time - x1) + y1);
        return y;
    }

    public String toString()
    {
        StringBuffer buf = new StringBuffer();
        buf.append("\nTimeStampedAudioBufferList, size="
                + timeStampedAudioBuffers.size() + "\n");
        for (Enumeration e = timeStampedAudioBuffers.elements(); e
                .hasMoreElements();)
        {
            AudioBufferTimeStamp abts = (AudioBufferTimeStamp) e.nextElement();
            double timeStamp = abts.getTimeStamp();
            int startFrame = abts.getStartFrame();
            buf.append("timeStamp " + timeStamp + ", startFrame " + startFrame
                    + "\n");
        }
        return buf.toString();
    }

    public static void main(String[] args)
    {
        TimeStampedAudioBufferList list = new TimeStampedAudioBufferList();
        double timeStamp = 0;
                for (int i = 0; i < 20; i++)
                {
                    list.add(new AudioBufferTimeStamp(100 * i, timeStamp));
                    timeStamp += 0.1;
                }
                for (int i = 0; i < 20; i++)
                {
                    timeStamp += 0.1;
                    list.add(new AudioBufferTimeStamp(100 * (20 - i), timeStamp));
                }
//        list.add(new AudioBufferTimeStamp(13824, 100, 2.72));
//        list.add(new AudioBufferTimeStamp(13312, 100, 2.752));
        System.out.println(list.toString());
        System.out.println(list.timeToFrame(0.05));
    }
}