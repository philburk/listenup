/*
 * Created on Nov 15, 2003
 */
package com.softsynth.javasonics.recplay;

import com.softsynth.javasonics.DeviceUnavailableException;


/**
 * Someone interested in knowing when a recording is ready. 
 * @author Phil Burk (C) 2003
 */
public interface DownloadListener {
	void progress( int bytesDownloaded, int bytesTotal );
	void gotEnoughToPlay(Recording recording) throws DeviceUnavailableException;
	void finished(Recording recording);
	void caughtException( String msg, Throwable e  );
	void failed();
	double getStartPosition();
}
