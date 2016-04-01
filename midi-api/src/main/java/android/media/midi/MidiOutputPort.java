package android.media.midi;

import android.annotation.SuppressLint;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import jp.kshoji.driver.midi.device.MidiInputDevice;
import jp.kshoji.driver.midi.listener.OnMidiInputEventListener;

/**
 * This class is used for receiving data from a port on a MIDI device
 */
@SuppressLint("NewApi")
public final class MidiOutputPort extends MidiSender implements Closeable {


    private static final String TAG = "MidiOutputPort";

    private static volatile int portNumber = 0;

    private final int myPortNumber;
    private Set<MidiReceiver> receivers = new HashSet<MidiReceiver>();
    private final MidiInputDevice usbMidiInputDevice;
    private final jp.kshoji.blemidi.device.MidiInputDevice bleMidiInputDevice;

    /* package */ MidiOutputPort(final MidiInputDevice midiInputDevice) {
        usbMidiInputDevice = midiInputDevice;
        bleMidiInputDevice = null;
        myPortNumber = portNumber++;

        usbMidiInputDevice.setMidiEventListener(usbMidiInputEventListener);
    }

    /* package */ MidiOutputPort(final jp.kshoji.blemidi.device.MidiInputDevice midiInputDevice) {
        usbMidiInputDevice = null;
        bleMidiInputDevice = midiInputDevice;
        myPortNumber = portNumber++;

        bleMidiInputDevice.setOnMidiInputEventListener(bleMidiInputEventListener);
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
    public void onConnect(final MidiReceiver receiver) {
        receivers.add(receiver);
    }

    @SuppressLint("Override")
    @Override
    public void onDisconnect(final MidiReceiver receiver) {
        receivers.remove(receiver);
    }

    @Override
    public void close() throws IOException {
        if (usbMidiInputDevice != null) {
            usbMidiInputDevice.setMidiEventListener(null);
        }
        if (bleMidiInputDevice != null) {
            bleMidiInputDevice.setOnMidiInputEventListener(null);
        }
    }

    @SuppressLint("Override")
    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    /**
     * event listener for USB MIDI
     */
    private final OnMidiInputEventListener usbMidiInputEventListener = new OnMidiInputEventListener() {
        @Override
        public void onMidiMiscellaneousFunctionCodes(final MidiInputDevice midiInputDevice, final int cable, final int data1, final int data2, final int data3) {
            // ignore this method
        }

        @Override
        public void onMidiCableEvents(final MidiInputDevice midiInputDevice, final int cable, final int data1, final int data2, final int data3) {
            // ignore this method
        }

        @Override
        public void onMidiSystemCommonMessage(final MidiInputDevice midiInputDevice, final int cable, final byte[] bytes) {
            sendMidiMessage(bytes);
        }

        @Override
        public void onMidiSystemExclusive(final MidiInputDevice midiInputDevice, final int cable, final byte[] bytes) {
            sendMidiMessage(bytes);
        }

        @Override
        public void onMidiNoteOff(final MidiInputDevice midiInputDevice, final int cable, final int channel, final int note, final int velocity) {
            sendMidiMessage(new byte[] {(byte) (channel | 0x80), (byte) note, (byte) velocity});
        }

        @Override
        public void onMidiNoteOn(final MidiInputDevice midiInputDevice, final int cable, final int channel, final int note, final int velocity) {
            sendMidiMessage(new byte[] {(byte) (channel | 0x90), (byte) note, (byte) velocity});
        }

        @Override
        public void onMidiPolyphonicAftertouch(final MidiInputDevice midiInputDevice, final int cable, final int channel, final int note, final int pressure) {
            sendMidiMessage(new byte[] {(byte) (channel | 0xa0), (byte) note, (byte) pressure});
        }

        @Override
        public void onMidiControlChange(final MidiInputDevice midiInputDevice, final int cable, final int channel, final int function, final int value) {
            sendMidiMessage(new byte[] {(byte) (channel | 0xb0), (byte) function, (byte) value});
        }

        @Override
        public void onMidiProgramChange(final MidiInputDevice midiInputDevice, final int cable, final int channel, final int program) {
            sendMidiMessage(new byte[] {(byte) (channel | 0xc0), (byte) program});
        }

        @Override
        public void onMidiChannelAftertouch(final MidiInputDevice midiInputDevice, final int cable, final int channel, final int pressure) {
            sendMidiMessage(new byte[] {(byte) (channel | 0xd0), (byte) pressure});
        }

        @Override
        public void onMidiPitchWheel(final MidiInputDevice midiInputDevice, final int cable, final int channel, final int amount) {
            sendMidiMessage(new byte[] {(byte) (channel | 0xe0), (byte) (amount & 0x7f), (byte)(amount >> 7)});
        }

        @Override
        public void onMidiSingleByte(final MidiInputDevice midiInputDevice, final int cable, final int data) {
            // ignore this method
        }

        @Override
        public void onMidiTimeCodeQuarterFrame(final MidiInputDevice midiInputDevice, final int cable, final int timing) {
            sendMidiMessage(new byte[] {(byte) 0xf1, (byte) timing});
        }

        @Override
        public void onMidiSongSelect(final MidiInputDevice midiInputDevice, final int cable, final int song) {
            sendMidiMessage(new byte[] {(byte) 0xf3, (byte) song});
        }

        @Override
        public void onMidiSongPositionPointer(final MidiInputDevice midiInputDevice, final int cable, final int position) {
            sendMidiMessage(new byte[] {(byte) 0xf2, (byte) (position & 0x7f), (byte)(position >> 7)});
        }

        @Override
        public void onMidiTuneRequest(final MidiInputDevice midiInputDevice, final int cable) {
            sendMidiMessage(new byte[] {(byte) 0xf6});
        }

        @Override
        public void onMidiTimingClock(final MidiInputDevice midiInputDevice, final int cable) {
            sendMidiMessage(new byte[] {(byte) 0xf8});
        }

        @Override
        public void onMidiStart(final MidiInputDevice midiInputDevice, final int cable) {
            sendMidiMessage(new byte[] {(byte) 0xfa});
        }

        @Override
        public void onMidiContinue(final MidiInputDevice midiInputDevice, final int cable) {
            sendMidiMessage(new byte[] {(byte) 0xfb});
        }

        @Override
        public void onMidiStop(final MidiInputDevice midiInputDevice, final int cable) {
            sendMidiMessage(new byte[] {(byte) 0xfc});
        }

        @Override
        public void onMidiActiveSensing(final MidiInputDevice midiInputDevice, final int cable) {
            sendMidiMessage(new byte[] {(byte) 0xfe});
        }

        @Override
        public void onMidiReset(final MidiInputDevice midiInputDevice, final int cable) {
            sendMidiMessage(new byte[] {(byte) 0xff});
        }

        @Override
        public void onMidiRPNReceived(final MidiInputDevice midiInputDevice, final int cable, final int channel, final int i2, final int i3, final int i4) {
            // ignore this method
        }

        @Override
        public void onMidiNRPNReceived(final MidiInputDevice midiInputDevice, final int cable, final int channel, final int i2, final int i3, final int i4) {
            // ignore this method
        }

        @Override
        public void onMidiRPNReceived(final MidiInputDevice midiInputDevice, final int cable, final int channel, final int i2, final int i3) {
            // ignore this method
        }

        @Override
        public void onMidiNRPNReceived(final MidiInputDevice midiInputDevice, final int cable, final int channel, final int i2, final int i3) {
            // ignore this method
        }
    };

    /**
     * send MIDI message to receivers
     *
     * @param message the raw MIDI message
     */
    private synchronized void sendMidiMessage(final byte[] message) {
        for (final MidiReceiver receiver : receivers) {
            try {
                receiver.send(message, 0, message.length);
            } catch (final IOException ignored) {
                // do nothing
            }
        }
    }

    /**
     * event listener for BLE MIDI
     */
    private final jp.kshoji.blemidi.listener.OnMidiInputEventListener bleMidiInputEventListener = new jp.kshoji.blemidi.listener.OnMidiInputEventListener() {

        @Override
        public void onMidiSystemExclusive(final jp.kshoji.blemidi.device.MidiInputDevice midiInputDevice, final byte[] bytes) {
            sendMidiMessage(bytes);
        }

        @Override
        public void onMidiNoteOff(final jp.kshoji.blemidi.device.MidiInputDevice midiInputDevice, final int channel, final int note, final int velocity) {
            sendMidiMessage(new byte[] {(byte) (channel | 0x80), (byte) note, (byte) velocity});
        }

        @Override
        public void onMidiNoteOn(final jp.kshoji.blemidi.device.MidiInputDevice midiInputDevice, final int channel, final int note, final int velocity) {
            sendMidiMessage(new byte[] {(byte) (channel | 0x90), (byte) note, (byte) velocity});
        }

        @Override
        public void onMidiPolyphonicAftertouch(final jp.kshoji.blemidi.device.MidiInputDevice midiInputDevice, final int channel, final int note, final int pressure) {
            sendMidiMessage(new byte[] {(byte) (channel | 0xa0), (byte) note, (byte) pressure});
        }

        @Override
        public void onMidiControlChange(final jp.kshoji.blemidi.device.MidiInputDevice midiInputDevice, final int channel, final int function, final int value) {
            sendMidiMessage(new byte[] {(byte) (channel | 0xb0), (byte) function, (byte) value});
        }

        @Override
        public void onMidiProgramChange(final jp.kshoji.blemidi.device.MidiInputDevice midiInputDevice, final int channel, final int program) {
            sendMidiMessage(new byte[] {(byte) (channel | 0xc0), (byte) program});
        }

        @Override
        public void onMidiChannelAftertouch(final jp.kshoji.blemidi.device.MidiInputDevice midiInputDevice, final int channel, final int pressure) {
            sendMidiMessage(new byte[] {(byte) (channel | 0xd0), (byte) pressure});
        }

        @Override
        public void onMidiPitchWheel(final jp.kshoji.blemidi.device.MidiInputDevice midiInputDevice, final int channel, final int amount) {
            sendMidiMessage(new byte[] {(byte) (channel | 0xe0), (byte) (amount & 0x7f), (byte)(amount >> 7)});
        }

        @Override
        public void onMidiTimeCodeQuarterFrame(final jp.kshoji.blemidi.device.MidiInputDevice midiInputDevice, final int timing) {
            sendMidiMessage(new byte[] {(byte) 0xf1, (byte) timing});
        }

        @Override
        public void onMidiSongSelect(final jp.kshoji.blemidi.device.MidiInputDevice midiInputDevice, final int song) {
            sendMidiMessage(new byte[] {(byte) 0xf3, (byte) song});
        }

        @Override
        public void onMidiSongPositionPointer(final jp.kshoji.blemidi.device.MidiInputDevice midiInputDevice, final int position) {
            sendMidiMessage(new byte[] {(byte) 0xf2, (byte) (position & 0x7f), (byte)(position >> 7)});
        }

        @Override
        public void onMidiTuneRequest(final jp.kshoji.blemidi.device.MidiInputDevice midiInputDevice) {
            sendMidiMessage(new byte[] {(byte) 0xf6});
        }

        @Override
        public void onMidiTimingClock(final jp.kshoji.blemidi.device.MidiInputDevice midiInputDevice) {
            sendMidiMessage(new byte[] {(byte) 0xf8});
        }

        @Override
        public void onMidiStart(final jp.kshoji.blemidi.device.MidiInputDevice midiInputDevice) {
            sendMidiMessage(new byte[] {(byte) 0xfa});
        }

        @Override
        public void onMidiContinue(final jp.kshoji.blemidi.device.MidiInputDevice midiInputDevice) {
            sendMidiMessage(new byte[] {(byte) 0xfb});
        }

        @Override
        public void onMidiStop(final jp.kshoji.blemidi.device.MidiInputDevice midiInputDevice) {
            sendMidiMessage(new byte[] {(byte) 0xfc});
        }

        @Override
        public void onMidiActiveSensing(final jp.kshoji.blemidi.device.MidiInputDevice midiInputDevice) {
            sendMidiMessage(new byte[] {(byte) 0xfe});
        }

        @Override
        public void onMidiReset(final jp.kshoji.blemidi.device.MidiInputDevice midiInputDevice) {
            sendMidiMessage(new byte[] {(byte) 0xff});
        }

        @Override
        public void onRPNMessage(final jp.kshoji.blemidi.device.MidiInputDevice midiInputDevice, final int i, final int i1, final int i2) {
            // ignore this method
        }

        @Override
        public void onNRPNMessage(final jp.kshoji.blemidi.device.MidiInputDevice midiInputDevice, final int i, final int i1, final int i2) {
            // ignore this method
        }
    };
}
