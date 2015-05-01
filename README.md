# GridStrument

An experiment in OSC control via android touch events.  Sends events from the Android device to a
host computer that listens & responds to OSC messages.

Inspired by the LinnStrument http://www.rogerlinndesign.com/linnstrument.html and Madrona Labs
SoundPlane http://madronalabs.com/soundplane.

Is it possible for an Android tablet App to give a similar experience?  Let's see.

## Status

Basically works with the NVIDIA SHIELD Tablet + Reaper if you have this ReaperOSC config.

```text
VKB_MIDI_NOTE i/vkb_midi/@/note/@
VKB_MIDI_CC i/vkb_midi/@/cc/@
VKB_MIDI_PITCH i/vkb_midi/@/pitch
```

Now looking for sounds & synths that especially benefit from the instrument interface.

## Usage

Requires an device running Lollipop (5.0) Android or later.  I've tested on the NVIDIA Shield Tablet.

Requires a host computer application that understands OSC input.

Update via the Settings menu with your local configuration information
* Server IP Address = something like 10.10.0.19
* Server PORT Number = some number like 8675
* Pitch Bend Range = bend across how many notes before saturating?  Typically 2 or 12.  This should match your synth.
* Starting Note = MIDI note value for the lower left corner grid cell.  Default is 48 or C3.

Sends OSC messages with this format:
* note on/off = `/vkb_midi/<channel>/note/<note_number>` and the parameter is 0-127 velocity.  0 indicates note off
* y-axis modulation = `/vkb_midi/<channel>/pitch` and the parameter is 0-0x3fff bend where 0x2000 is the center.
* x-axis modulation = `/vkb_midi/<channel>/cc/1` (mod-wheel) and the parameter is 0-127.

The y-axis modulation is controlled by the Pitch Bend Range.

The x-axis modulation saturates over a single cell size.

## Notes

Originally written to use MIDI with Humatic nmj, mnet MIDI tools, but I found sending MIDI messages
unreliable.

Looked at https://github.com/kshoji/USB-MIDI-Driver, but that is a Android<->USB MIDI device
connector.

Could consider http://www.juce.com/ for their midi connectivity, but that seemed like a lot of
extra work.

Switched to OSC based on this http://physcomp.org/tutorial-android-osc-communication/ and
https://github.com/Gkxd/OSCTutorial/blob/master/app/src/main/java/com/dhua/osctutorial/MainActivity.java
which uses http://www.illposed.com/software/javaosc.html

This seems to work, but network latency can sometimes be annoyingly high.

### USB Tethering

Wired tethering the Android device to the host PC should reduce latency, jitter, etc.

With the USB Cord plugged in, go to Settings -> More... -> Tethering & portable hotspot -> USB Tethering and enable tethering.

* On the Mac, this driver works for me http://joshuawise.com/horndis.  See your server IP address in System Preferences -> Network.
* On the PC, *TBD*
* On Linux, *TBD*

## License

Copyright (c) 2015, Roger Allen.

Distributed under the MIT License.  See the LICENSE.txt file.
