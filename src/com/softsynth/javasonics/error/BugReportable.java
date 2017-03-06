package com.softsynth.javasonics.error;


public interface BugReportable
{
	CoreDump createCoreDump( long timestamp );
}
