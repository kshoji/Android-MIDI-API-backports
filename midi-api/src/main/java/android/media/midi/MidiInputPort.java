package android.media.midi;

import android.annotation.SuppressLint;

import java.io.Closeable;
import java.io.IOException;

import jp.kshoji.driver.midi.device.MidiOutputDevice;

/**
 * This class is used for sending data to a port on a MIDI device
 */
@SuppressLint("NewApi")
public final class MidiInputPort extends MidiReceiver implements Closeable {
    private static final String TAG = "MidiInputPort";

    private static volatile int portNumber = 0;

    private final int myPortNumber;
    private final MidiOutputDevice usbMidiOutputDevice;
    private final jp.kshoji.blemidi.device.MidiOutputDevice bleMidiOutputDevice;

    /* package */ MidiInputPort(MidiOutputDevice usbMidiInputDevice) {
        usbMidiOutputDevice = usbMidiInputDevice;
        bleMidiOutputDevice = null;
        myPortNumber = portNumber++;
    }

    /* package */ MidiInputPort(jp.kshoji.blemidi.device.MidiOutputDevice bleMidiInputDevice) {
        usbMidiOutputDevice = null;
        bleMidiOutputDevice = bleMidiInputDevice;
        myPortNumber = portNumber++;
    }

    /**
     * Returns the port number of this port
     *
     * @return the port's port number
     */
    public final int getPortNumber() {
        return myPortNumber;
    }

    @SuppressLint("Override")
    @Override
    public void onSend(byte[] msg, int offset, int count, long timestamp) throws IOException {
        if (offset < 0 || count < 0 || msg == null || offset + count > msg.length) {
            throw new IllegalArgumentException("offset or count out of range");
        }

        if (usbMidiOutputDevice != null) {
            if ((msg[0] & 0xff) == 0xf0) {
                // sysex
                usbMidiOutputDevice.sendMidiSystemExclusive(0, msg);
            } else {
                sendMidiMessage(msg[0], msg[1], msg[2]);
            }
        }
        if (bleMidiOutputDevice != null) {
            if ((msg[0] & 0xff) == 0xf0) {
                // sysex
                bleMidiOutputDevice.sendMidiSystemExclusive(msg);
            } else {
                sendMidiMessage(msg[0], msg[1], msg[2]);
            }
        }
    }

    /**
     * Send a MIDI message with 3 bytes raw MIDI data
     *
     * @param byte1 the first byte
     * @param byte2 the second byte: ignored when 1 byte message
     * @param byte3 the third byte: ignored when 1-2 byte message
     */
    private void sendMidiMessage(final int byte1, final int byte2, final int byte3) {
        switch (byte1 & 0xf0) {
            case 0x80: // Note Off
                if (usbMidiOutputDevice != null) {
                    usbMidiOutputDevice.sendMidiNoteOff(0, byte1, byte2, byte3);
                }
                if (bleMidiOutputDevice != null) {
                    bleMidiOutputDevice.sendMidiNoteOff(byte1, byte2, byte3);
                }
                break;
            case 0x90: // Note On
                if (usbMidiOutputDevice != null) {
                    usbMidiOutputDevice.sendMidiNoteOn(0, byte1, byte2, byte3);
                }
                if (bleMidiOutputDevice != null) {
                    bleMidiOutputDevice.sendMidiNoteOn(byte1, byte2, byte3);
                }
                break;
            case 0xa0: // Poly Pressure
                if (usbMidiOutputDevice != null) {
                    usbMidiOutputDevice.sendMidiPolyphonicAftertouch(0, byte1, byte2, byte3);
                }
                if (bleMidiOutputDevice != null) {
                    bleMidiOutputDevice.sendMidiPolyphonicAftertouch(byte1, byte2, byte3);
                }
                break;
            case 0xb0: // Control Change
                if (usbMidiOutputDevice != null) {
                    usbMidiOutputDevice.sendMidiControlChange(0, byte1, byte2, byte3);
                }
                if (bleMidiOutputDevice != null) {
                    bleMidiOutputDevice.sendMidiControlChange(byte1, byte2, byte3);
                }
                break;
            case 0xc0: // Program Change
                if (usbMidiOutputDevice != null) {
                    usbMidiOutputDevice.sendMidiProgramChange(0, byte1, byte2);
                }
                if (bleMidiOutputDevice != null) {
                    bleMidiOutputDevice.sendMidiProgramChange(byte1, byte2);
                }
                break;
            case 0xd0: // Channel Pressure
                if (usbMidiOutputDevice != null) {
                    usbMidiOutputDevice.sendMidiChannelAftertouch(0, byte1, byte2);
                }
                if (bleMidiOutputDevice != null) {
                    bleMidiOutputDevice.sendMidiChannelAftertouch(byte1, byte2);
                }
                break;
            case 0xe0: // Pitch Bend
                if (usbMidiOutputDevice != null) {
                    usbMidiOutputDevice.sendMidiPitchWheel(0, byte1, byte2 << 7 | byte3);
                }
                if (bleMidiOutputDevice != null) {
                    bleMidiOutputDevice.sendMidiPitchWheel(byte1, byte2 << 7 | byte3);
                }
                break;
            case 0xf0: // SysEx with 3 bytes
                switch (byte1) {
                    case 0xf0: // Start Of Exclusive
                    case 0xf7: // End of Exclusive
                        // ignored
                        break;

                    case 0xf4: // (Undefined MIDI System Common)
                    case 0xf5: // (Undefined MIDI System Common / Bus Select?)
                    case 0xf9: // (Undefined MIDI System Real-time)
                    case 0xfd: // (Undefined MIDI System Real-time)
                        // ignored
                        break;

                    case 0xf6: // Tune Request
                        if (usbMidiOutputDevice != null) {
                            usbMidiOutputDevice.sendMidiTuneRequest(0);
                        }
                        if (bleMidiOutputDevice != null) {
                            bleMidiOutputDevice.sendMidiTuneRequest();
                        }
                        break;
                    case 0xf8: // Timing Clock
                        if (usbMidiOutputDevice != null) {
                            usbMidiOutputDevice.sendMidiTimingClock(0);
                        }
                        if (bleMidiOutputDevice != null) {
                            bleMidiOutputDevice.sendMidiTimingClock();
                        }
                        break;
                    case 0xfa: // Start
                        if (usbMidiOutputDevice != null) {
                            usbMidiOutputDevice.sendMidiStart(0);
                        }
                        if (bleMidiOutputDevice != null) {
                            bleMidiOutputDevice.sendMidiStart();
                        }
                        break;
                    case 0xfb: // Continue
                        if (usbMidiOutputDevice != null) {
                            usbMidiOutputDevice.sendMidiContinue(0);
                        }
                        if (bleMidiOutputDevice != null) {
                            bleMidiOutputDevice.sendMidiContinue();
                        }
                        break;
                    case 0xfc: // Stop
                        if (usbMidiOutputDevice != null) {
                            usbMidiOutputDevice.sendMidiStop(0);
                        }
                        if (bleMidiOutputDevice != null) {
                            bleMidiOutputDevice.sendMidiStop();
                        }
                        break;
                    case 0xfe: // Active Sensing
                        if (usbMidiOutputDevice != null) {
                            usbMidiOutputDevice.sendMidiActiveSensing(0);
                        }
                        if (bleMidiOutputDevice != null) {
                            bleMidiOutputDevice.sendMidiActiveSensing();
                        }
                        break;
                    case 0xff: // System Reset
                        if (usbMidiOutputDevice != null) {
                            usbMidiOutputDevice.sendMidiReset(0);
                        }
                        if (bleMidiOutputDevice != null) {
                            bleMidiOutputDevice.sendMidiReset();
                        }
                        break;

                    case 0xf1: // MIDI Time Code
                        if (usbMidiOutputDevice != null) {
                            usbMidiOutputDevice.sendMidiTimeCodeQuarterFrame(0, byte1);
                        }
                        if (bleMidiOutputDevice != null) {
                            bleMidiOutputDevice.sendMidiTimeCodeQuarterFrame(byte1);
                        }
                        break;
                    case 0xf3: // Song Select
                        if (usbMidiOutputDevice != null) {
                            usbMidiOutputDevice.sendMidiSongSelect(0, byte1);
                        }
                        if (bleMidiOutputDevice != null) {
                            bleMidiOutputDevice.sendMidiSongSelect(byte1);
                        }
                        break;
                    case 0xf2: // Song Point Pointer
                        // Three byte message
                        if (usbMidiOutputDevice != null) {
                            usbMidiOutputDevice.sendMidiSongPositionPointer(0, byte1 << 7 | byte2);
                        }
                        if (bleMidiOutputDevice != null) {
                            bleMidiOutputDevice.sendMidiSongPositionPointer(byte1 << 7 | byte2);
                        }
                        break;
                    default:
                        break;
                }
                break;
            default:
                // ignored
        }
    }

    @SuppressLint("Override")
    @Override
    public void onFlush() throws IOException {
    }

    @Override
    public void close() throws IOException {
    }

    @SuppressLint("Override")
    @Override
    protected void finalize() throws Throwable {
    }
}
