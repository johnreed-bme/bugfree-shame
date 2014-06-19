package sleeper;

import com.nokia.lwuit.components.FormItem;
import com.nokia.lwuit.components.FormItemListener;
import com.nokia.lwuit.components.PickerItemContainer;
import com.nokia.lwuit.components.SwitchComponent;
import com.nokia.lwuit.components.SwitchComponentListener;
import com.nokia.lwuit.components.UnitPicker;
import com.nokia.lwuit.components.UnitPickerListener;
import com.nokia.mid.ui.DeviceControl;
import com.sun.lwuit.Button;
import com.sun.lwuit.Command;
import com.sun.lwuit.Component;
import com.sun.lwuit.Container;
import com.sun.lwuit.Dialog;
import com.sun.lwuit.Display;
import com.sun.lwuit.Form;
import com.sun.lwuit.Image;
import com.sun.lwuit.Label;
import com.sun.lwuit.List;
import com.sun.lwuit.TextArea;
import com.sun.lwuit.events.*;
import com.sun.lwuit.layouts.BoxLayout;
import com.sun.lwuit.layouts.CoordinateLayout;
import com.sun.lwuit.layouts.GridLayout;
import com.sun.lwuit.list.DefaultListModel;
import com.sun.lwuit.list.ListModel;
import com.sun.lwuit.plaf.Style;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Vector;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.io.file.FileSystemRegistry;
import javax.microedition.media.Manager;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;
import javax.microedition.media.PlayerListener;
import javax.microedition.midlet.*;

public class MusicSleeper extends MIDlet implements ActionListener, PlayerListener {
    
    Vector hour, min, sec;
    private Form mainForm = null;
    private Form cdScreen = null;
    private Form optionsScreen = null;
    private FormItem remainingText = null;
    private FormItem countdownHours = null;
    private UnitPicker cdHoursPicker = null;
    private FormItem countdownMins = null;
    private UnitPicker cdMinsPicker = null;
    private FormItem countdownSecs = null;
    private UnitPicker cdSecsPicker = null;
    private Command  cmdChoose = null;
    private Command  cmdShowBrowser = null;
    private Command  cmdListen = null;
    private Command  cmdStop = null;
    private Command  cmdStopCountdown = null;
    private Command  cmdMain = null;
    private Command  cmdOptions = null;
    private Command  cmdAbout = null;
    private Command  cmdHelp = null;
    private Command  cmdCountdown = null;
    private Command  cmdCancel = null;
    private String browsePath = null;
    private Label countdownStringItem = null;
    private FormItem remainingTime = null;
    private FormItem musicChosen = null;
    private String musicChosenPath = null;
    private Image imMP3 = null;
    private Image imRoot = null;
    private Image imFolder = null;
    private Image imFile = null;
    private Player mp3Player = null;
    private FileConnection  mp3File = null;
    private InputStream mp3FileInputStream = null;
    private MainThread countdownThread = null;
    private VibrateThread vibrateThread = null;
    private DefaultToneThread defaultToneThread = null;
    private keepScreenOnThread keepScreenOn = null;
    private boolean vibrate_on = true;
    private boolean vibrating = false;
    private boolean default_tone_on = true;
    private boolean tone_playing = false;
    private boolean countDown_on = false;
    private boolean cdScreenEnabled = false;
    private boolean cdInterrupted = false;
    private int inactivity = 0;
    
    private class UnitPickerVibra extends UnitPicker {

		public UnitPickerVibra(String arg0, String arg1, Vector arg2) {
			super(arg0, arg1, arg2);
		}
    	
		public void notifyPickerItemContainerListener(PickerItemContainer container){
			super.notifyPickerItemContainerListener(container);
		}
    }
    
    private class VibrateThread extends Thread {
    	public void run() {
    		while (vibrate_on && vibrating) {
    			for (int i=0; i<3; i++) {
    				Display.getInstance().vibrate(400);
    				try {
						sleep(500);
					} catch (InterruptedException e) {
					}
    			}
    			try {
					sleep(500);
				} catch (InterruptedException e) {
				}
    		}
    	}
    }
    
    private class DefaultToneThread extends Thread {
    	public void run() {
    		while (default_tone_on && tone_playing) {
    			try {
					Manager.playTone(63, 500, 100);
				} catch (MediaException e1) {
				}
    			try {
					sleep(1000);
				} catch (InterruptedException e) {
					
				}
    		}
    	}
    }
    
    private class keepScreenOnThread extends Thread {
    	public void run() {
    		
    		while (countDown_on) {
    			
    			if (inactivity >= 30) {
    				DeviceControl.setLights(0, 1);
    				if (!cdScreenEnabled) {
    					Display.getInstance().setForceFullScreen(true);
    					cdScreen.show();
    					cdScreenEnabled = true;
    				} else {
    					remainingText.setItemText(remainingTime.getItemValueText());
    					remainingText.getStyle().setBgColor(0x000000);
    			        remainingText.getStyle().setFgColor(0x00eeff);
    			        remainingText.setX(0);
    			        remainingText.setY((cdScreen.getHeight() / 2) - (20));
    				}
    			}
    			
    			try {
					sleep(1000);
					inactivity++;
				} catch (InterruptedException e) {
					
				}
    		}
    		if (cdScreenEnabled) {
    			Display.getInstance().setForceFullScreen(false);
    			mainForm.show();
    		}
    		cdScreenEnabled = false;
    		DeviceControl.setLights(0, 100);
    	}
    }
    
    private class MainThread extends Thread {
        int cdTimer = 0;
        public MainThread(final int cdTimer) {
            this.cdTimer = cdTimer;
        }
        public void run() {
            
            stopPlay();
            
            countDown_on = true;
            keepScreenOn = new keepScreenOnThread();
            keepScreenOn.start();
            
            if ((musicChosenPath == null)
					|| (musicChosen.getItemValueText().equals("None"))) {
                
            } else {
                openMusic(musicChosenPath.concat(musicChosen.getItemValueText()));
            }
            
            mainForm.removeCommand(cmdCountdown);
            mainForm.removeCommand(cmdOptions);
            mainForm.addCommand(cmdStopCountdown);
            
            int remainingHours = 0;
            int remainingMins = 0;
            int remainingSecs = 0;
            
            while (cdTimer > 0) {
                try {
                    remainingHours = cdTimer / (60 * 60);
                    remainingMins = (cdTimer % (60 * 60)) / 60;
                    remainingSecs = (cdTimer % (60 * 60)) % 60;
                    remainingTime.setItemValueText(Integer.toString(remainingHours) + " Hours " + 
                                                    Integer.toString(remainingMins) + " Mins " +
                                                    Integer.toString(remainingSecs) + " Secs");
                    sleep(1000);
                    cdTimer--;
                } catch (InterruptedException ex) {
                }
            }
            
            remainingTime.setItemValueText("0 Hours 0 Mins 0 Secs");
            
            countDown_on = false;
            inactivity = 0;
            mainForm.removeCommand(cmdStopCountdown);

            if (!cdInterrupted) {
            	playMusic();
    			vibrateThread = new VibrateThread();
    			vibrating = true;
    			vibrateThread.start();
    			defaultToneThread = new DefaultToneThread();
    			if (musicChosen.getItemValueText().equals("None"))
    				tone_playing = true;
    			else
    				tone_playing = false;
    			defaultToneThread.start();
            } else {
            	cdInterrupted = false;
            }
            
        }
    }
    
    public MusicSleeper () {
    	super();
    	
        hour = new Vector();
        hour.addElement("Hr");
        hour.addElement("Hr");
        
        min = new Vector();
        min.addElement("Min");
        min.addElement("Min");
        
        sec = new Vector();
        sec.addElement("Sec");
        sec.addElement("Sec");
        
    }
    
    public void startApp() {
        Display.init(this);
        
        //// Creating commands
        cmdShowBrowser = new Command ("Browse");
        cmdCountdown = new Command ("Arm");
        cmdListen = new Command ("Listen");
        cmdStop = new Command ("Stop");
        cmdStopCountdown = new Command ("Disarm");
        cmdChoose = new Command ("Choose");
        cmdCancel = new Command ("Cancel");
        cmdMain = new Command ("Main");
        cmdOptions = new Command ("Options");
        cmdAbout = new Command ("About");
        cmdHelp = new Command ("Help");
        
        //// Creating power-saver count-down screen
        cdScreen = new Form();
        cdScreen.setBackCommand(cmdMain);
        Style style = cdScreen.getStyle();
        style.setBgImage(null);
        style.setBgColor(0x000000);
        style.setFgColor(0x000000);
        cdScreen.setSelectedStyle(style);
        cdScreen.setUnselectedStyle(style);
        cdScreen.setLayout(new CoordinateLayout(cdScreen.getWidth(), cdScreen.getHeight()));
        remainingText = new FormItem("0:0:0", false);
        remainingText.setEnabled(false);
        remainingText.getStyle().setBgColor(0x000000);
        remainingText.getStyle().setFgColor(0x00eeff);
        remainingText.setX(0);
        remainingText.setY((cdScreen.getHeight() / 2) - (20));
        cdScreen.addComponent(remainingText);
        cdScreen.addCommandListener(new ActionListener(){
			public void actionPerformed(ActionEvent arg0) {
				if (arg0.getCommand().equals(cmdMain)) {
					Display.getInstance().setForceFullScreen(false);
					mainForm.show();
		            inactivity = 0;
		            cdScreenEnabled = false;
		            DeviceControl.setLights(0, 100);
				}
			}
        });
        cdScreen.addPointerReleasedListener(new ActionListener(){
			public void actionPerformed(ActionEvent arg0) {
				Display.getInstance().setForceFullScreen(false);
				mainForm.show();
				inactivity = 0;
				cdScreenEnabled = false;
				DeviceControl.setLights(0, 100);
			}
        });
        
        
        //// Creating options screen
        optionsScreen = new Form("Options");
        optionsScreen.setBackCommand(cmdMain);
        final SwitchComponent switchVibration = new SwitchComponent(
        	    "Vibration", "On", "Vibrate on alarm",
        	    false);
        switchVibration.setSwitchOn(true);
        switchVibration.setSwitchComponentListener(new SwitchComponentListener() {
        	    public void notifySwitchListener(SwitchComponent switchComponent) {
        	        if (switchComponent == switchVibration) {
        	            if (switchComponent.isSwitchOn()) {
        	                switchComponent.setSwitchOn(true);
        	                switchComponent.setValue("On");
        	                vibrate_on = true;
        	            }
        	            else {
        	                switchComponent.setSwitchOn(false);
        	                switchComponent.setValue("Off");
        	                vibrate_on = false;
        	            }
        	        }
        	    }
        	});
        final SwitchComponent switchDefaultTone = new SwitchComponent(
        	    "Default Tone", "On", "Play default tone when no music is selected",
        	    false);
        switchDefaultTone.setSwitchOn(true);
        switchDefaultTone.setSwitchComponentListener(new SwitchComponentListener() {
        	    public void notifySwitchListener(SwitchComponent switchComponent) {
        	        if (switchComponent == switchDefaultTone) {
        	            if (switchComponent.isSwitchOn()) {
        	                switchComponent.setSwitchOn(true);
        	                switchComponent.setValue("On");
        	                default_tone_on = true;
        	            }
        	            else {
        	                switchComponent.setSwitchOn(false);
        	                switchComponent.setValue("Off");
        	                default_tone_on = false;
        	            }
        	        }
        	    }
        	});
        optionsScreen.addComponent(switchVibration);
        optionsScreen.addComponent(switchDefaultTone);
        optionsScreen.addCommandListener(new ActionListener(){
			public void actionPerformed(ActionEvent arg0) {
				if (arg0.getCommand().equals(cmdMain)) {
					mainForm.show();
				}
			}
        });
        
        
        
        mp3Player = null;
        mp3File = null;
        mp3FileInputStream = null;
        countdownThread = null;
        mainForm = new Form("MusicSleeper");
        countdownStringItem = new Label("Countdown from: ");
        remainingTime = new FormItem("Remaining time", "0 Hours 0 Mins 0 Secs", false);
        musicChosen = new FormItem("Music Chosen", "None", false);
        
        //// Actions for clicking on "Music Chosen" item
		musicChosen.setFormItemListener(new FormItemListener() {
			public void notifyFormItemListener(FormItem formItem,
					Component component, boolean actionButtonPressed) {
				if (!musicChosen.getItemValueText().equals("None")) {
					Dialog deleteChosenMusicDialog = new Dialog(
							"Discard chosen music");
					TextArea textArea = new TextArea(
							"Discard the currently selected music?");
					textArea.setGrowByContent(true);
					textArea.setEditable(false);
					deleteChosenMusicDialog.setLayout(new BoxLayout(
							BoxLayout.Y_AXIS));
					deleteChosenMusicDialog.addComponent(textArea);
					final Command cmdYes = new Command("Yes");
					final Command cmdNo = new Command("No");
					Command[] commands = new Command[2];
					commands[0] = cmdYes;
					commands[1] = cmdNo;
					deleteChosenMusicDialog.placeButtonCommands(commands);
					deleteChosenMusicDialog.setBackCommand(cmdNo);
					deleteChosenMusicDialog
							.addCommandListener(new ActionListener() {
								public void actionPerformed(ActionEvent arg0) {

									if (arg0.getCommand().equals(cmdYes)) {
										musicChosen.setItemValueText("None");
									} else if (arg0.getCommand().equals(cmdNo)) {
									}
								}
							});
					deleteChosenMusicDialog.show();
				} else if (!countDown_on) {
					browsePath = null;
		            showBrowserRoot();
				}
			}
		});
        
        mainForm.setLayout(new BoxLayout(BoxLayout.Y_AXIS));
        
        //// Creating hours picker
        countdownHours = new FormItem("Hours", "000 Hr", false);
        try {
        cdHoursPicker = new UnitPickerVibra("Countdown hours", "Hours", hour);
        cdHoursPicker.setValue(0, 0, 0, "Hr");
        cdHoursPicker.setTactileTouch(true);
        cdHoursPicker.setUnitPickerListener(new UnitPickerListener() {
            public void notifyUnitPickerListener(UnitPicker arg0) {
                countdownHours.setItemValueText(cdHoursPicker.getValue());
                mainForm.show();
            }
        });
        countdownHours.setFormItemListener(new FormItemListener() {
            public void notifyFormItemListener(
                    FormItem formItem,
                    Component component,
                    boolean actionButtonPressed) {
                cdHoursPicker.show();
                cdHoursPicker.setUnitPickerOn(true);
            }
        });
        } catch (Exception npe) {
            try {
                cdHoursPicker = new UnitPickerVibra("Countdown hours", "Hours", hour);
                cdHoursPicker.setValue(0, 0, 0, "Hr");
                cdHoursPicker.setTactileTouch(true);
                cdHoursPicker.setUnitPickerListener(new UnitPickerListener() {
                    public void notifyUnitPickerListener(UnitPicker arg0) {
                        countdownHours.setItemValueText(cdHoursPicker.getValue());
                        mainForm.show();
                    }
                });
                countdownHours.setFormItemListener(new FormItemListener() {
                    public void notifyFormItemListener(
                            FormItem formItem,
                            Component component,
                            boolean actionButtonPressed) {
                        cdHoursPicker.show();
                        cdHoursPicker.setUnitPickerOn(true);
                    }
                });
                } catch (Exception npe2) {
                	Dialog validDialog2 = new Dialog("Alert");
                    validDialog2.setTimeout(5000); // set timeout milliseconds
                    TextArea textArea2 = new TextArea(npe2.toString() + ": " + npe2.getMessage()); //pass the alert text here
                    textArea2.setGrowByContent(true);
                    textArea2.setEditable(false);
                    textArea2.setScrollVisible(true);
                    validDialog2.addComponent(textArea2);
                    validDialog2.show();
                }
        }
        
        
        //// Creating minutes picker
        countdownMins = new FormItem("Mins", "000 min", false);
        try {
        cdMinsPicker = new UnitPickerVibra("Countdown minutes", "Mins", min);
        cdMinsPicker.setValue(0, 0, 0, "Min");
        cdMinsPicker.setTactileTouch(true);
        cdMinsPicker.setUnitPickerListener(new UnitPickerListener() {
            public void notifyUnitPickerListener(UnitPicker arg0) {
                countdownMins.setItemValueText(cdMinsPicker.getValue());
                mainForm.show();
            }
        });
        countdownMins.setFormItemListener(new FormItemListener() {
            public void notifyFormItemListener(
                    FormItem formItem,
                    Component component,
                    boolean actionButtonPressed) {
                cdMinsPicker.show();
                cdMinsPicker.setUnitPickerOn(true);
            }
        });
        } catch (Exception npe) {
        	try {
                cdMinsPicker = new UnitPickerVibra("Countdown minutes", "Mins", min);
                cdMinsPicker.setValue(0, 0, 0, "Min");
                cdMinsPicker.setTactileTouch(true);
                cdMinsPicker.setUnitPickerListener(new UnitPickerListener() {
                    public void notifyUnitPickerListener(UnitPicker arg0) {
                        countdownMins.setItemValueText(cdMinsPicker.getValue());
                        mainForm.show();
                    }
                });
                countdownMins.setFormItemListener(new FormItemListener() {
                    public void notifyFormItemListener(
                            FormItem formItem,
                            Component component,
                            boolean actionButtonPressed) {
                        cdMinsPicker.show();
                        cdMinsPicker.setUnitPickerOn(true);
                    }
                });
                } catch (Exception npe2) {
                	Dialog validDialog = new Dialog("Alert");
                    validDialog.setTimeout(5000); // set timeout milliseconds
                    TextArea textArea = new TextArea(npe.toString() + ": " + npe.getMessage()); //pass the alert text here
                    textArea.setGrowByContent(true);
                    textArea.setEditable(false);
                    textArea.setScrollVisible(true);
                    validDialog.addComponent(textArea);
                    validDialog.show();
                }
        }
        
        //// Creating seconds picker
        countdownSecs = new FormItem("Secs", "000 Sec", false);
        try {
        cdSecsPicker = new UnitPickerVibra("Countdown seconds", "Secs", sec);
        cdSecsPicker.setValue(0, 0, 0, "Sec");
        cdSecsPicker.setTactileTouch(true);
        cdSecsPicker.setUnitPickerListener(new UnitPickerListener() {
            public void notifyUnitPickerListener(UnitPicker arg0) {
                countdownSecs.setItemValueText(cdSecsPicker.getValue());
                mainForm.show();
            }
        });
        countdownSecs.setFormItemListener(new FormItemListener() {
            public void notifyFormItemListener(
                    FormItem formItem,
                    Component component,
                    boolean actionButtonPressed) {
                cdSecsPicker.show();
                cdSecsPicker.setUnitPickerOn(true);
            }
        });
        } catch (Exception npe) {
        	try {
                cdSecsPicker = new UnitPickerVibra("Countdown seconds", "Secs", sec);
                cdSecsPicker.setValue(0, 0, 0, "Sec");
                cdSecsPicker.setTactileTouch(true);
                cdSecsPicker.setUnitPickerListener(new UnitPickerListener() {
                    public void notifyUnitPickerListener(UnitPicker arg0) {
                        countdownSecs.setItemValueText(cdSecsPicker.getValue());
                        mainForm.show();
                    }
                });
                countdownSecs.setFormItemListener(new FormItemListener() {
                    public void notifyFormItemListener(
                            FormItem formItem,
                            Component component,
                            boolean actionButtonPressed) {
                        cdSecsPicker.show();
                        cdSecsPicker.setUnitPickerOn(true);
                    }
                });
                } catch (Exception npe2) {
                	Dialog validDialog = new Dialog("Alert");
                    validDialog.setTimeout(5000); // set timeout milliseconds
                    TextArea textArea = new TextArea(npe.toString() + ": " + npe.getMessage()); //pass the alert text here
                    textArea.setGrowByContent(true);
                    textArea.setEditable(false);
                    textArea.setScrollVisible(true);
                    validDialog.addComponent(textArea);
                    validDialog.show();
                }
        }
        
        mainForm.addComponent(countdownStringItem);
        
        Container cdCont = null;
        cdCont = new Container(new GridLayout(1,3));
        
        cdCont.addComponent(countdownHours);
        cdCont.addComponent(countdownMins);
        cdCont.addComponent(countdownSecs);
        
        mainForm.addComponent(cdCont);
        
        mainForm.addComponent(remainingTime);
        mainForm.addComponent(musicChosen);
        
        mainForm.addCommand(cmdAbout);
        mainForm.addCommand(cmdHelp);
        mainForm.addCommand(cmdOptions);
        mainForm.addCommand(cmdCountdown);
        mainForm.addCommandListener(this);
        
        try {
        	imMP3 = Image.createImage("/sleeper/Music_note.png");
        } catch (IOException ex) {
            imMP3 = null;
        }
        
        try {
        	imRoot = Image.createImage("/sleeper/Root.png");
        } catch (IOException ex) {
        	imRoot = null;
        }
        
        try {
        	imFolder = Image.createImage("/sleeper/Folder.png");
        } catch (IOException ex) {
        	imFolder = null;
        }
        
        try {
        	imFile = Image.createImage("/sleeper/File.png");
        } catch (IOException ex) {
        	imFile = null;
        }
        
        mainForm.show();
    }
    
    public void pauseApp() {
    }
    
    public void destroyApp(boolean unconditional) {
    	DeviceControl.setLights(0, 100);
        notifyDestroyed();
    }
    
    public void actionPerformed(ActionEvent c) {
        if (c.getCommand().equals(cmdShowBrowser)) {
            
            browsePath = null;
            showBrowserRoot();
            
        } else if (c.getCommand().equals(cmdStop)) {
            
        	stopPlay();
        	mainForm.show();
            
        } else if (c.getCommand().equals(cmdStopCountdown)) {
            
            if (countdownThread != null) {
                if (countdownThread.cdTimer > 0) {
                	mainForm.removeCommand(cmdStopCountdown);
                	countDown_on = false;
                    stopPlay();
                    mainForm.show();
                    countdownThread.cdTimer = 0;
                    cdInterrupted = true;
                }
            }
            
            
        } else if (c.getCommand().equals(cmdCountdown)) {
        	if (((!vibrate_on) ) && (((!default_tone_on) &&
        			(musicChosen.getItemValueText().equals("None"))) )) {
        		Dialog alarmIsSilentDialog = new Dialog(
						"Alarm will be silent");
				TextArea textArea = null;
				textArea = new TextArea("Vibration is off.\nDefault Tone is off.\nNo music selected.");
				textArea.setGrowByContent(true);
				textArea.setEditable(false);
				alarmIsSilentDialog.setLayout(new BoxLayout(
						BoxLayout.Y_AXIS));
				alarmIsSilentDialog.addComponent(textArea);
				final Command cmdContinue = new Command("Continue");
				final Command cmdCancel = new Command("Cancel");
				Command[] commands = new Command[2];
				commands[0] = cmdContinue;
				commands[1] = cmdCancel;
				alarmIsSilentDialog.placeButtonCommands(commands);
				alarmIsSilentDialog.setBackCommand(cmdCancel);
				alarmIsSilentDialog
						.addCommandListener(new ActionListener() {
							public void actionPerformed(ActionEvent arg0) {

								if (arg0.getCommand().equals(cmdContinue)) {
									try {
				                        int countdownFrom = 0;
				                        countdownFrom += (Integer.valueOf(cdHoursPicker.getValue().substring(0, 3)).intValue() * 60 * 60);
				                        countdownFrom += (Integer.valueOf(cdMinsPicker.getValue().substring(0, 3)).intValue() * 60);
				                        countdownFrom += (Integer.valueOf(cdSecsPicker.getValue().substring(0, 3)).intValue());
				                        countdownWithMusic(countdownFrom);
				                    } catch (NumberFormatException nfe) {
				                        countdownWithMusic(0);
				                    }
								} else if (arg0.getCommand().equals(cmdCancel)) {
								}
							}
						});
				alarmIsSilentDialog.show();
        	} else {
                    try {
                        int countdownFrom = 0;
                        countdownFrom += (Integer.valueOf(cdHoursPicker.getValue().substring(0, 3)).intValue() * 60 * 60);
                        countdownFrom += (Integer.valueOf(cdMinsPicker.getValue().substring(0, 3)).intValue() * 60);
                        countdownFrom += (Integer.valueOf(cdSecsPicker.getValue().substring(0, 3)).intValue());
                        countdownWithMusic(countdownFrom);
                    } catch (NumberFormatException nfe) {
                        countdownWithMusic(0);
                    }
        	}
            
        } else if (c.getCommand().equals(cmdMain)) {
            
            mainForm.show();
            inactivity = 0;
            cdScreenEnabled = false;
            DeviceControl.setLights(0, 100);
            
        } else if (c.getCommand().equals(cmdOptions)) {
            
        	optionsScreen.show();
        	
        } else if (c.getCommand().equals(cmdAbout)) {
            
        	Dialog aboutDialog = new Dialog(
					"About");
			TextArea textArea = new TextArea(
					"MusicSleeper\nVersion 1.0.0\nMade by\nNagy Krisztian\n");
			textArea.setGrowByContent(true);
			textArea.setEditable(false);
			aboutDialog.setLayout(new BoxLayout(
					BoxLayout.Y_AXIS));
			aboutDialog.addComponent(textArea);
			final Command cmdBack = new Command("Back");
			aboutDialog.setBackCommand(cmdBack);
			aboutDialog.show();
        	
        } else if (c.getCommand().equals(cmdHelp)) {
            
        	Dialog helpDialog = new Dialog(
					"Help");
			TextArea textArea = new TextArea(
					"Set time length with \"Countdown from\" options.\nChoose music by clicking on \"Chosen music\" and browse for an MP3 file.\nOnly MP3 is supported\n" + 
						"Start count-down with Arm button, halt count-down with Disarm button.\nWhen timer reaches 0, alarm will sound, stop alarm with Stop button.\n" +
							"You can set options from within the Options menu item");
			textArea.setGrowByContent(true);
			textArea.setEditable(false);
			helpDialog.setLayout(new BoxLayout(
					BoxLayout.Y_AXIS));
			helpDialog.addComponent(textArea);
			final Command cmdBack = new Command("Back");
			helpDialog.setBackCommand(cmdBack);
			helpDialog.show();
        	
        }
    }
    
    private void showBrowserRoot() {
        
    	final Vector test_pointer;
    	test_pointer = new Vector();
    	
        final Form rootBrowser;
        rootBrowser = new Form("Root");
        rootBrowser.setBackCommand(cmdMain);
        if (mp3Player != null) {
        	rootBrowser.addCommand(cmdStop);
        }
        rootBrowser.addCommandListener(new ActionListener(){

			public void actionPerformed(ActionEvent arg0) {
				
				if (arg0.getCommand().equals(cmdMain)) {
					mainForm.show();
				} else if (arg0.getCommand().equals(cmdStop)) {
					stopPlay();
					rootBrowser.removeCommand(cmdStop);
				}
				
			}
        	
        });
        
        Enumeration drives = null;
        try {
        	drives = FileSystemRegistry.listRoots();
        } catch (SecurityException se) {
        	return;
        }
        
        ListModel model = new DefaultListModel();
        
        while (drives.hasMoreElements()) {
            String element = (String) drives.nextElement();
            Command item = new Command(element, imRoot);
            model.addItem(item);
        }
        
        final List rootList;
        rootList = new List(model);
        
        rootBrowser.addComponent(rootList);
        rootBrowser.show();
        
        rootBrowser.addPointerPressedListener(new ActionListener(){

			public void actionPerformed(ActionEvent arg0) {
				test_pointer.addElement("press_event");
			}
			
		});
		
        rootBrowser.addPointerDraggedListener(new ActionListener(){

			public void actionPerformed(ActionEvent arg0) {
				test_pointer.addElement("drag_event");
			}
			
		});
		
        rootBrowser.addPointerReleasedListener(new ActionListener(){

			public void actionPerformed(ActionEvent arg0) {
				if (!test_pointer.isEmpty()) {
					if (test_pointer.lastElement().toString()
							.equals("press_event")) {
						showBrowserPath(rootList.getSelectedItem().toString(), rootBrowser);
					}
					test_pointer.removeAllElements();
				}
			}
			
		});
    }

    private void showBrowserPath(String fileName, final Form previous) {
    	
        FileConnection conn = null;
        Enumeration elements = null;
        
        final Vector test_pointer;
        test_pointer = new Vector();
        
        if (browsePath == null)
        {
            browsePath = fileName;
        } else {
            
            if (((browsePath.lastIndexOf('/')) != (browsePath.length() - 1)) || ((fileName.indexOf('/') < 0) && (!fileName.equals(".."))))
            {
                return;
            }
            
            int lastSeparator = browsePath.lastIndexOf('/', browsePath.length() - 2);

            if (fileName.equals("..")) {
                
                browsePath = browsePath.substring(0, lastSeparator + 1);
                
            } else {
                browsePath = browsePath.concat(fileName);
            }
            
            if (browsePath.length() == 0)
            {
                showBrowserRoot();
                return;
            }
        }
        
        try {
            
            conn = (FileConnection) Connector.open("file:///" + browsePath, Connector.READ);
            elements = conn.list();
            
        } catch (IOException ex) {
            return;
        } catch (SecurityException se) {
        	return;
        }
        
        final Form contentBrowser;
        contentBrowser = new Form(browsePath);
        contentBrowser.addCommand(cmdMain);
        final Command cmdBack = new Command("Back");
        contentBrowser.setBackCommand(cmdBack);
        if (mp3Player != null) {
        	contentBrowser.addCommand(cmdStop);
        }
        contentBrowser.addCommandListener(new ActionListener(){

			public void actionPerformed(ActionEvent arg0) {
				
				if (arg0.getCommand().equals(cmdMain)) {
					mainForm.show();
				} else if (arg0.getCommand().equals(cmdStop)) {
					stopPlay();
					contentBrowser.removeCommand(cmdStop);
				} else if (arg0.getCommand().equals(cmdBack)) {
					previous.show();
				}
				
			}
        	
        });
        
		ListModel model = new DefaultListModel();

		model.addItem(new Command("..", null));
		while (elements.hasMoreElements()) {
			
			String element = (String) elements.nextElement();
			
			Command item = null;
			
			if (element.endsWith(".mp3")) {
				item = new Command(element, imMP3);
			} else if (element.endsWith("/")) {
				item = new Command(element, imFolder);
			} else {
				item = new Command(element, imFile);
			}
			
			model.addItem(item);
		}
		
		final List contentList;
		contentList = new List(model);

		contentBrowser.addComponent(contentList);
		

		contentBrowser.addPointerPressedListener(new ActionListener(){

			public void actionPerformed(ActionEvent arg0) {
				test_pointer.addElement("press_event");
			}
			
		});
		
		contentBrowser.addPointerDraggedListener(new ActionListener(){

			public void actionPerformed(ActionEvent arg0) {
				test_pointer.addElement("drag_event");
			}
			
		});
		
		contentBrowser.addPointerReleasedListener(new ActionListener() {

			public void actionPerformed(ActionEvent arg0) {
				if (!test_pointer.isEmpty()) {
					if (test_pointer.lastElement().toString()
							.equals("press_event")) {
						final String selectedItem = contentList.getSelectedItem()
								.toString();
						if (selectedItem.endsWith(".mp3")) {
							final Dialog mp3Dialog = new Dialog(selectedItem);
				            TextArea textArea = new TextArea("You can listen to the track here or choose it for the alarm sound.");
				            textArea.setGrowByContent(true);
				            textArea.setEditable(false);
				            mp3Dialog.setLayout(new BoxLayout(BoxLayout.Y_AXIS));
				            Container Buttons = new Container(new BoxLayout(BoxLayout.Y_AXIS));
				            mp3Dialog.addComponent(textArea);
				            final Button btnListen = new Button(cmdListen);
				            final Button btnChoose = new Button(cmdChoose);
				            btnChoose.setTactileTouch(false);
				            btnListen.setTactileTouch(true);
				            Buttons.addComponent(btnChoose);
				            Buttons.addComponent(btnListen);
				            mp3Dialog.addComponent(Buttons);
				            mp3Dialog.setBackCommand(cmdCancel);
				            mp3Dialog.setAutoDispose(false);
				            
				            mp3Dialog.addCommandListener(new ActionListener(){

								public void actionPerformed(ActionEvent arg0) {
									
									if (arg0.getCommand().equals(cmdChoose)) {
										musicChosenPath = browsePath;
										musicChosen.setItemValueText(selectedItem);
										mp3Dialog.dispose();
									} else if (arg0.getCommand().equals(cmdListen)) {
										stopPlay();
										contentBrowser.addCommand(cmdStop);
										btnListen.setCommand(cmdStop);
										Listen(browsePath.concat(selectedItem));
									} else if (arg0.getCommand().equals(cmdStop)) {
										stopPlay();
										btnListen.setCommand(cmdListen);
										contentBrowser.removeCommand(cmdStop);
									} else if (arg0.getCommand().equals(cmdCancel)) {
										mp3Dialog.dispose();
									}

								}
				            	
				            });
				            
				            mp3Dialog.show();
				            
						} else if (selectedItem.endsWith("/") || selectedItem.endsWith("..")) {
							showBrowserPath(selectedItem, contentBrowser);
						}
					}
					test_pointer.removeAllElements();
				}
			}

		});
		contentBrowser.show();
    }

    private void Listen(String soundPath) {
        
        if (mp3Player != null) {
            return;
        }
        try {
            mp3File = (FileConnection) Connector.open("file:///" + soundPath, Connector.READ);
            mp3FileInputStream = mp3File.openInputStream();
            mp3Player = Manager.createPlayer(mp3FileInputStream, "audio/mp3");
            mp3Player.addPlayerListener(this);
            mp3Player.realize();
        } catch (MediaException me) {
        } catch (IOException ex) {
            
        } catch (SecurityException se) {
            return;
        }
        try {
            if (mp3Player != null) {
                mp3Player.prefetch();
                mp3Player.start();
            }
        } catch (MediaException me) {
            
        } catch (SecurityException se) {
            
        }
        mainForm.addCommand(cmdStop);
    }
    
    private void openMusic(String soundPath) {
        
        try {
            mp3File = (FileConnection) Connector.open("file:///" + soundPath, Connector.READ);
            mp3FileInputStream = mp3File.openInputStream();
            mp3Player = Manager.createPlayer(mp3FileInputStream, "audio/mp3");
            mp3Player.addPlayerListener(this);
            mp3Player.realize();
        } catch (MediaException me) {
            
        } catch (IOException ex) {
            
        } catch (SecurityException se) {
            return;
        }
    }
    
    private void playMusic() {
        try {
            if (mp3Player != null) {
                mp3Player.prefetch();
                mp3Player.start();
            }
        } catch (MediaException me) {
            
        } catch (SecurityException se) {
            
        }
        mainForm.addCommand(cmdStop);
    }

    public void playerUpdate(Player player, String event, Object eventData) {
        if (player.equals(mp3Player)) {
            if (event.equals(STARTED)) {
                
            } else if (event.equals(STOPPED)) {
                
            } else if (event.equals(END_OF_MEDIA)) {
                stopPlay();
            }
        }
    }

	private void stopPlay() {
		try {
			vibrating = false;
			tone_playing = false;

			if (mp3Player != null) {
				mp3Player.close();
				mp3Player = null;
			}
			if (mp3FileInputStream != null) {
				mp3FileInputStream.close();
				mp3FileInputStream = null;
			}
			if (mp3File != null) {
				mp3File.close();
				mp3File = null;
			}
		} catch (Exception ex) {

		} finally {
			System.gc();
		}
		mainForm.removeCommand(cmdStop);
		mainForm.addCommand(cmdOptions);
		mainForm.addCommand(cmdCountdown);
	}

    private void countdownWithMusic(int intValue) {
		countdownThread = new MainThread(intValue);
		countdownThread.start();
    }
}
