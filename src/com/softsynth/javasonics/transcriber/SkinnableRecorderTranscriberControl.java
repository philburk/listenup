package com.softsynth.javasonics.transcriber;

import java.awt.event.*;
import java.util.*;

import com.softsynth.awt.*;
import com.softsynth.javasonics.recplay.*;

/**
 * Add "Record" button to player controlling recording. This class provides a
 * graphical front end for the non-graphical Recorder Class.
 * 
 * @author (C) 2001 Phil Burk, All Rights Reserved
 */

public class SkinnableRecorderTranscriberControl extends SkinnablePlayerControl {
    Recorder recorder;
    ImageButton recordButton;
    PlaybackSpeedControlPanel playbackSpeedControlPanel;

    public void setPlayer(Player pPlayer) {
        recorder = (Recorder) pPlayer;
        super.setPlayer(pPlayer);
        if (playbackSpeedControlPanel != null) {
            playbackSpeedControlPanel.addObserver(new Observer() {
                public void update(Observable arg0, Object arg1) {
                    if (getPlayer() != null) {
                        getPlayer().setSlowForwardSpeed(playbackSpeedControlPanel.getSpeed());
                    } else {
                        System.out.println("SkinnableRecorderTranscriberControl player null");
                    }
                }
            });
        }
    }

    public void handleRecord() {
        // Disable now so we don't get two hits when asking mic permission.
        recordButton.setEnabled(false);
        recorder.recordAudio();
    }

    /**
     * Construct a GUI for recording and playing back audio with standard tape
     * transport controls.
     */
    public SkinnableRecorderTranscriberControl(VisualTheme theme, TranscriberVisualTheme transcriberTheme,
    		boolean showSpeedControl )
    {
        super(theme);

        recordButton = theme.createRecordButton();
        add(recordButton);

        recordButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                handleRecord();
            }
        });

        setToBeginButton(transcriberTheme.createToBeginButton());
        add(getToBeginButton());
        ((ImageButton) getToBeginButton()).addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                handleToBegin();
            }
        });
       
    
        setRewindButton(transcriberTheme.createRewindButton());
        add(getRewindButton());
        ((ImageButton) getRewindButton()).addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                handleRewind();
            }
        });
        
        setFastForwardButton(transcriberTheme.createFastForwardButton());
        add(getFastForwardButton());
        ((ImageButton) getFastForwardButton()).addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                handleFastForward();
            }
        });
   
        setToEndButton(transcriberTheme.createToEndButton());
        add(getToEndButton());
        ((ImageButton) getToEndButton()).addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                handleToEnd();
            }
        });
        
        if( showSpeedControl )
        {
	        playbackSpeedControlPanel = new PlaybackSpeedControlPanel();
	        //        ((PlaybackSpeedControlPanel)
	        // playbackSpeedControlPanel).setPlayer(player);
	        playbackSpeedControlPanel.setBevelled(true);
	        add(playbackSpeedControlPanel);
        }
    }

    /**
     * Enable or disable buttons based on current mode of operation.
     */
    public void updateButtons() {
        super.updateButtons();
        if (recordButton == null)
            return;

        if (recorder == null) {
            recordButton.setEnabled(false);
        } else {
            switch (recorder.getState()) {
            case Recorder.STOPPED:
            case Recorder.PAUSED:
                recordButton.setEnabled(recorder.isRecordable() && isEnabled());
                break;

            case Recorder.RECORDING:
            case Recorder.PLAYING:
                recordButton.setEnabled(false);
                break;
            }
        }
    }

    public void setSpeedScrollbarEnabled(boolean enabled) {
        if (playbackSpeedControlPanel != null)
            playbackSpeedControlPanel.setEnabled(enabled);
    }

}