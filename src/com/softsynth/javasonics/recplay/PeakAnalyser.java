package com.softsynth.javasonics.recplay;

/**
 * @author Phil Burk (C) 2004
 */
public class PeakAnalyser
{
	protected Recording recording;
	/**
	 * 
	 */
	public PeakAnalyser( Recording recording )
	{
		this.recording = recording;
	}

	/** Measure peak values of a recording based on the number of bins.
	 * 
	 * @return samples measured
	 */
	public synchronized PeakMeasurements measurePeaks( int numBins )
	{
		return measurePeaks( 0, numBins, numBins );
	}

	/** Measure peak values of a recording based on the number of bins.
	 * 
	 * @return samples measured
	 */
	public synchronized PeakMeasurements measurePeaks( int startBin, int endBin, int totalBins )
	{
		int numBins = endBin - startBin;
		PeakMeasurements measurement = new PeakMeasurements(numBins);
		// Use long so that calculation does not overflow driving index2 negative.
		long totalSamples = recording.getMaxSamplesPlayable();
		long numSamples = totalSamples * numBins / totalBins;
		measurement.numSamples = (int) numSamples;
		int bufSize = (int) (2 + (numSamples / numBins));
		short[] sampleBuffer = new short[bufSize];
		int index1 = (int) (totalSamples * startBin / totalBins);
		for (int i = 0; i < numBins; i++)
		{
			int binIndex = startBin + i;
			// calculate index of sample after this column
			int index2 = (int) (((binIndex+1) * numSamples) / numBins);
			int numRead = index2 - index1;
			if( numRead > bufSize ) numRead = bufSize;
			recording.read( index1, sampleBuffer, 0, numRead);
			index1 += numRead;

			// find min and max of bin
			short maxSample = 0;
			short minSample = 0;
			for (int j = 0; j < numRead; j++)
			{
				short sample = sampleBuffer[j];
				minSample = (sample < minSample) ? sample : minSample;
				maxSample = (sample > maxSample) ? sample : maxSample;
			}
			measurement.mins[i] = minSample;
			measurement.maxs[i] = maxSample;
		}
		return measurement;
	}

	/**
	 * @param index
	 * @param count
	 */
	public void delete( int index, int count )
	{
		// Nothing to do cuz we don't store results.
	}

	/**
	 * @param writeIndex
	 * @param numSamples
	 */
	public void insert( int writeIndex, int numSamples )
	{
		// Nothing to do cuz we don't store results.
	}

}
