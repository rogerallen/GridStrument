# GridStrument

An experiment in OSC control via android touch events.

Inspired by the LinnStrument http://www.rogerlinndesign.com/linnstrument.html and Madrona Labs SoundPlane http://madronalabs.com/soundplane.

Is it possible for an Android tablet App to give a similar experience?  Let's see.

## Status as of 4/28/2015

Basically works with Reaper if you have this ReaperOSC config

```text
VKB_MIDI_NOTE i/vkb_midi/@/note/@
VKB_MIDI_CC i/vkb_midi/@/cc/@
VKB_MIDI_PITCH i/vkb_midi/@/pitch
```

and you edit the mOSCServerIP in the code to match your host.

## Notes

Originally written with Humatic nmj, mnet MIDI tools.  But, I found
them unreliable for this purpose.

Consider https://github.com/kshoji/USB-MIDI-Driver?  No, that is a
Android<->USB MIDI device connector.

Consider http://www.juce.com/ ?

Switching to OSC.  This looks useful.
http://physcomp.org/tutorial-android-osc-communication/ and
https://github.com/Gkxd/OSCTutorial/blob/master/app/src/main/java/com/dhua/osctutorial/MainActivity.java

Now uses http://www.illposed.com/software/javaosc.html
