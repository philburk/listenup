package com.softsynth.javasonics.footpedal.test;

import java.awt.Frame;

import com.softsynth.javasonics.footpedal.*;

/**
 * @author Phil Burk (C) 2004
 */
public class ShowFootPedals
{
	FootPedalMonitor monitor;
	/**
	 * 
	 */
	public ShowFootPedals()
	{
		monitor = new FootPedalMonitor( new Frame("Test footpedals"));
	}

	public static void main(String[] args)
	{
		ShowFootPedals app1 = new ShowFootPedals();
		app1.test();
		ShowFootPedals app2 = new ShowFootPedals();
		app2.test();
	}

	/**
	 * 
	 */
	private void test()
	{
		System.out.println("addFootPedalListener");
		monitor.addFootPedalListener(new FootPedalListener()
		{
			public void buttonPressed(FootPedalEvent event)
			{
				System.out.println("Pressed " + event.id);

			}

			public void buttonReleased(FootPedalEvent event)
			{
				System.out.println("Released " + event.id);
			}
		});
		System.out.println("Start monitor");
		monitor.start();
		try
		{
			Thread.sleep(8 * 1000);
		} catch (InterruptedException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Stop monitor");
		monitor.stop();
	}
}
