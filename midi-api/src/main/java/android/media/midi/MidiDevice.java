package android.media.midi;

import android.annotation.SuppressLint;

import java.io.Closeable;
import java.io.IOException;

import jp.kshoji.driver.midi.device.MidiInputDevice;
import jp.kshoji.driver.midi.device.MidiOutputDevice;

/**
 * This class is used for sending and receiving data to and from a MIDI device
 * Instances of this class are created by {@link MidiManager#openDevice}.
 */
public final class MidiDevice implements Closeable {
    private static final String TAG = "MidiDevice";

    private final MidiDeviceInfo mDeviceInfo;

    private final MidiInputDevice usbMidiInputDevice;
    private final MidiOutputDevice usbMidiOutputDevice;
    private final jp.kshoji.blemidi.device.MidiInputDevice bleMidiInputDevice;
    private final jp.kshoji.blemidi.device.MidiOutputDevice bleMidiOutputDevice;

    /**
     * This class represents a connection between the output port of one device
     * and the input port of another. Created by {@link #connectPorts}.
     * Close this object to terminate the connection.
     */
    public class MidiConnection implements Closeable {
        private final MidiInputPort midiInputPort;
        private final MidiOutputPort midiOutputPort;

        MidiConnection(final MidiOutputPort outputPortToken, final MidiInputPort inputPort) {
            midiInputPort = inputPort;
            midiOutputPort = outputPortToken;
        }

        @SuppressLint("NewApi")
        @Override
        public void close() throws IOException {
            // close input port
            midiInputPort.close();
            // close output port
            midiOutputPort.close();
        }

        @SuppressLint("NewApi")
        @Override
        protected void finalize() throws Throwable {
            try {
                close();
            } finally {
                super.finalize();
            }
        }
    }

    /* package */ MidiDevice(final MidiDeviceInfo deviceInfo, final MidiInputDevice midiInputDevice) {
        mDeviceInfo = deviceInfo;

        usbMidiInputDevice = midiInputDevice;
        usbMidiOutputDevice = null;
        bleMidiInputDevice = null;
        bleMidiOutputDevice = null;
    }

    /* package */ MidiDevice(final MidiDeviceInfo deviceInfo, final MidiOutputDevice midiOutputDevice) {
            mDeviceInfo = deviceInfo;

        usbMidiInputDevice = null;
        usbMidiOutputDevice = midiOutputDevice;
        bleMidiInputDevice = null;
        bleMidiOutputDevice = null;
    }

    /* package */ MidiDevice(final MidiDeviceInfo deviceInfo, final jp.kshoji.blemidi.device.MidiInputDevice midiInputDevice) {
        mDeviceInfo = deviceInfo;

        usbMidiInputDevice = null;
        usbMidiOutputDevice = null;
        bleMidiInputDevice = midiInputDevice;
        bleMidiOutputDevice = null;
    }

    /* package */ MidiDevice(final MidiDeviceInfo deviceInfo, final jp.kshoji.blemidi.device.MidiOutputDevice midiOutputDevice) {
        mDeviceInfo = deviceInfo;

        usbMidiInputDevice = null;
        usbMidiOutputDevice = null;
        bleMidiInputDevice = null;
        bleMidiOutputDevice = midiOutputDevice;
    }

    /**
     * Returns a {@link MidiDeviceInfo} object, which describes this device.
     *
     * @return the {@link MidiDeviceInfo} object
     */
    public MidiDeviceInfo getInfo() {
        return mDeviceInfo;
    }

    /**
     * Called to open a {@link MidiInputPort} for the specified port number.
     *
     * An input port can only be used by one sender at a time.
     * Opening an input port will fail if another application has already opened it for use.
     * A {@link MidiDeviceStatus} can be used to determine if an input port is already open.
     *
     * @param portNumber the number of the input port to open
     * @return the {@link MidiInputPort} if the open is successful,
     *         or null in case of failure.
     */
    public MidiInputPort openInputPort(final int portNumber) {
        if (usbMidiOutputDevice != null) {
            return new MidiInputPort(usbMidiOutputDevice);
        } else if (bleMidiOutputDevice != null) {
            return new MidiInputPort(bleMidiOutputDevice);
        }
        return null;
    }

    /**
     * Called to open a {@link MidiOutputPort} for the specified port number.
     *
     * An output port may be opened by multiple applications.
     *
     * @param portNumber the number of the output port to open
     * @return the {@link MidiOutputPort} if the open is successful,
     *         or null in case of failure.
     */
    public MidiOutputPort openOutputPort(final int portNumber) {
        if (usbMidiInputDevice != null) {
            return new MidiOutputPort(usbMidiInputDevice);
        } else if (bleMidiInputDevice != null) {
            return new MidiOutputPort(bleMidiInputDevice);
        }
        return null;
    }

    /**
     * Connects the supplied {@link MidiInputPort} to the output port of this device
     * with the specified port number. Once the connection is made, the MidiInput port instance
     * can no longer receive data via its {@link MidiReceiver#onSend} method.
     * This method returns a {@link MidiDevice.MidiConnection} object, which can be used
     * to close the connection.
     *
     * @param inputPort the inputPort to connect
     * @param outputPortNumber the port number of the output port to connect inputPort to.
     * @return {@link MidiDevice.MidiConnection} object if the connection is successful,
     *         or null in case of failure.
     */
    @SuppressLint("NewApi")
    public MidiConnection connectPorts(final MidiInputPort inputPort, final int outputPortNumber) {
        if (outputPortNumber < 0 || outputPortNumber >= mDeviceInfo.getOutputPortCount()) {
            throw new IllegalArgumentException("outputPortNumber out of range");
        }

        final MidiOutputPort midiOutputPort = openOutputPort(outputPortNumber);
        if (midiOutputPort != null) {
            midiOutputPort.onConnect(inputPort);
        }

        return new MidiConnection(midiOutputPort, inputPort);
    }

    @Override
    public void close() throws IOException {
        // close usbMidiInputDevice, usbMidiOutputDevice, bleMidiInputDevice, bleMidiOutputDevice
    }

    @SuppressLint("NewApi")
    @Override
    protected void finalize() throws Throwable {
        try {
            close();
        } finally {
            super.finalize();
        }
    }

    @SuppressLint("NewApi")
    @Override
    public String toString() {
        return "MidiDevice: " + mDeviceInfo.toString();
    }
}
