# GridStrument

An experiment in MIDI control via android touch events.

Uses the nmj library, requires the mnet midi host app. http://www.humatic.de/htools/

Inspired by the LinnStrument http://www.rogerlinndesign.com/linnstrument.html and Madrona Labs SoundPlane http://madronalabs.com/soundplane.

Is it possible for an Android tablet App to give a similar experience?  Let's see.

## Status as of 4/26/2015

Errors appear to come from missing note-on events.  If you quickly tap
2 fingers, I see events on the server for the sendNoteOn calls but
only 1 note-on message appears on the client.  Appears related to the
timing of the noteOn events on the server.  If they are within 0.001s,
it gets dropped. Example:

```text
**SERVER**
04-26 10:24:27.163  30112-30112/com.gmail.rallen.gridstrument D/sendNoteOn﹕ ch=0
04-26 10:24:27.164  30112-30112/com.gmail.rallen.gridstrument D/sendNoteOn﹕ ch=1
04-26 10:24:27.240  30112-30112/com.gmail.rallen.gridstrument D/sendNoteOff﹕ ch=1
04-26 10:24:27.267  30112-30112/com.gmail.rallen.gridstrument D/sendNoteOff﹕ ch=0

**CLIENT**
359922598751 note-on 0 69 0.496063
  <missing note-on 1 67>
359922679854 note-off 1 67
ERROR: channel  1  not playing note  67
359922692396 note-off 0 69
```

So, it appears that I need a more reliable midi library in order to
make this experiment useful.

## Notes

Consider https://github.com/kshoji/USB-MIDI-Driver?  No, that is a Android<->USB MIDI device connector.

Consider http://www.juce.com/ ?
