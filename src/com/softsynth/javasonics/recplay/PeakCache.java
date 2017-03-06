package com.softsynth.javasonics.recplay;

import com.softsynth.javasonics.util.Logger;

/**
 * Cache min and max data for peaks derived from the waveform.
 * 
 * @author Phil Burk (C) 2004
 */
public class PeakCache extends PeakAnalyser
{
	private short[] mins;
	private short[] maxs;
	private boolean[] valids;
	private static final int CACHE_SIZE = 2048;
	private int samplesPerBin = 1;
	/** Number of bins that cover the available data. May or may not be valid. */
	private int numCachedBins = 0;
	private long lastNumSamples = 0;

	/**
	 *  
	 */
	public PeakCache(Recording recording)
	{
		super( recording );
		mins = new short[CACHE_SIZE];
		maxs = new short[CACHE_SIZE];
		clear();
	}

	int nextPowerOf2( int n )
	{
		int log2 = 0;
		while( n > 0 )
		{
			log2 += 1;
			n = n >> 1;
		}
		return 1 << log2;
	}

	/**
	 * Squeeze data into half the space.
	 */
	private void squeezeCacheByHalf()
	{
		
		int halfNumBins = numCachedBins / 2;
		int i = 0;
		for( ; i < halfNumBins; i++ )
		{
			int ri = i * 2;
			valids[i] = valids[ri] && valids[ri + 1];
			if( valids[i] )
			{
				mins[i] = (mins[ri] < mins[ri + 1]) ? mins[ri] : mins[ri + 1];
				maxs[i] = (maxs[ri] > maxs[ri + 1]) ? maxs[ri] : maxs[ri + 1];
			}
		}
		for( ; i < numCachedBins; i++ )
		{
			valids[i] = false;
		}
		numCachedBins = halfNumBins;
		samplesPerBin *= 2;
		Logger.println(2, "PeakCache.squeezeCacheByHalf() samplesPerBin = " + samplesPerBin +
				", numCachedBins " + numCachedBins );
	}

	/**
	 * Make sure the data maps entirely into the cache. Adjust bin size as
	 * needed.
	 */
	private void fitDataToCache()
	{
		long totalSamples = recording.getMaxSamplesPlayable();
		if( totalSamples < lastNumSamples )
		{
			clear();
		}
		lastNumSamples = totalSamples;
		// Round up to some reasonable number of samples.
		int newSamplesPerBin = (int) ((totalSamples + CACHE_SIZE - 1) / CACHE_SIZE);
		newSamplesPerBin = nextPowerOf2( newSamplesPerBin );
		if( newSamplesPerBin < 1 )
			newSamplesPerBin = 1;
		while( newSamplesPerBin > samplesPerBin )
		{
			if( numCachedBins > 0 )
			{
				// squeeze old results into half the space.
				squeezeCacheByHalf();
			}
			else
			{
				samplesPerBin = newSamplesPerBin;
			}
		}
		numCachedBins = (int) (totalSamples / samplesPerBin);
	}

	/**
	 *  
	 */
	public void clear()
	{
		numCachedBins = 0;
		samplesPerBin = 1;
		valids = new boolean[CACHE_SIZE];
		lastNumSamples = 0;
	}

	private void fillCacheRegion( int startBin, int endBin )
	{
		//Logger.println( 3, " fillCacheRegion(" + startBin + "," + endBin + ")" );
		PeakMeasurements measurement = super.measurePeaks( startBin, endBin,
				numCachedBins );
		int j = 0;
		for( int i = startBin; i < endBin; i++ )
		{
			mins[i] = measurement.mins[j];
			maxs[i] = measurement.maxs[j];
			valids[i] = true;
			j++;
		}
	}

	/**
	 * Scan cache for contiguous invalid regions and fill them.
	 */
	private void fillCacheGaps()
	{
		boolean inDirtyRegion = false;
		int startBin = 0;
		int endBin = 0;
		for( int i = 0; i < numCachedBins; i++ )
		{
			if( !inDirtyRegion && !valids[i] )
			{
				startBin = i;
				inDirtyRegion = true;// entering invalid region
			}
			else if( inDirtyRegion && valids[i] )
			{
				endBin = i;
				inDirtyRegion = false;
				fillCacheRegion( startBin, endBin );
			}
		}
		if( inDirtyRegion )
		{
			fillCacheRegion( startBin, numCachedBins );
		}
	}

	/** Fill cache where needed. */
	private void fillCache()
	{
		fitDataToCache();
		fillCacheGaps();
	}

	/**
	 * Measure peak values of a recording based on the number of bins.
	 * 
	 * @return samples measured
	 */
	public synchronized PeakMeasurements measurePeaks( int startBin,
			int endBin, int totalBins )
	{
		fillCache();

		PeakMeasurements measurement = new PeakMeasurements( totalBins );
		int numBins = endBin - startBin;
		long totalSamples = numCachedBins;
		long numSamples = totalSamples * numBins / totalBins;
		measurement.numSamples = (int) numSamples;
		int startIndex = (int) (totalSamples * startBin / totalBins);
		for( int column = startBin; column < numBins; column++ )
		{
			// calculate index of sample after this column
			// Use long so that calculation does not overflow driving index
			// negative.
			int index1 = (int) (((column) * numSamples) / numBins);
			int index2 = (int) (((column + 1) * numSamples) / numBins);
			int numRead = index2 - index1;
			// find min and max of bin
			short maxSample = 0;
			short minSample = 0;
			if( numRead == 0 )
			{
				minSample = mins[index1];
				maxSample = maxs[index1];
			}
			else
			{
				for( int i = index1; i < index2; i++ )
				{
					minSample = (mins[i] < minSample) ? mins[i] : minSample;
					maxSample = (maxs[i] > maxSample) ? maxs[i] : maxSample;
				}
			}
			measurement.mins[column] = minSample;
			measurement.maxs[column] = maxSample;
		}
		return measurement;
	}

	/**
	 * @param writeIndex
	 * @param numFrames
	 */
	private void invalidate( int writeIndex, int numFrames )
	{
		int startBin = writeIndex / samplesPerBin;
		int endBin = (writeIndex + numFrames) / samplesPerBin;
		for( int i = startBin; i <= endBin; i++ )
		{
			valids[i] = false;
		}
	}

	/**
	 * Account for insertion of new data into cache.
	 * 
	 * @param writeIndex
	 * @param numSamples
	 */
	public void insert( int writeIndex, int numSamples )
	{
		fitDataToCache();
		Logger.println( 3, "PeakCache.insert: writeIndex = " + writeIndex
				+ ", numSamples = " + numSamples + ", samplesPerBin = "
				+ samplesPerBin );
		int totalSamples = recording.getMaxSamplesPlayable();
		int numToEnd = totalSamples - writeIndex;
		if( numToEnd > 0 )
		{
			invalidate( writeIndex, numToEnd );
		}
	}
}