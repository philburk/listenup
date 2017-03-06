package com.softsynth.upload;

/**
 * Delay to simulate a slow network.
 * @author Phil Burk (C) 2003
 */
public class SlowNetworkSimulator
{
	/** Used to simulate slow networks transfers. */
	private long startMSec;
	private double elapsedTime;
	final static boolean hangAtMiddle = false;
	final static int hangAtSize = 10000;
	private int bytesMoved;
	private double bytesPerSecond = 800.0;

	public void simulateNetworkDelay(int numBytes)
	{
		bytesMoved += numBytes;
		System.out.println("simulateNetworkDelay: " + bytesMoved);
		elapsedTime += (numBytes / bytesPerSecond);
		// avoid error accumulation
		long targetMSec = startMSec + (long) (elapsedTime * 1000.0);
		long msecToWait = targetMSec - System.currentTimeMillis();
		if (msecToWait > 0)
		{
			try
			{
				Thread.sleep(msecToWait);
			} catch (InterruptedException e)
			{
			}
		}
		if (hangAtMiddle)
		{
			if (((bytesMoved - numBytes) < hangAtSize)
				&& (bytesMoved >= hangAtSize))
			{
				try
				{
					double hangTime = 15.0;
					System.out.println(
						"HANG SIMULATED DOWNLOAD at " + bytesMoved);
					Thread.sleep((int) (hangTime * 1000));
					elapsedTime += hangTime;
					System.out.println("RESUME SIMULATED DOWNLOAD");
				} catch (InterruptedException e)
				{
				}
			}
		}
	}

	/** Just for symmetry. */
	public void stop()
	{
	}

	/** Initialize clocks for real-time simulation. */
	public void start()
	{
		System.out.println("WARNING: SlowNetSimulator in use!!!");
		startMSec = System.currentTimeMillis();
		elapsedTime = 0.0;
	}
}
