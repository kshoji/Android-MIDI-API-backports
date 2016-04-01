package android.media.midi;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.os.IBinder;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jp.kshoji.blemidi.service.BleMidiCentralService;
import jp.kshoji.blemidi.service.BleMidiPeripheralService;
import jp.kshoji.blemidi.util.BleUtils;
import jp.kshoji.driver.midi.device.MidiInputDevice;
import jp.kshoji.driver.midi.device.MidiOutputDevice;
import jp.kshoji.driver.midi.listener.OnMidiDeviceAttachedListener;
import jp.kshoji.driver.midi.listener.OnMidiDeviceDetachedListener;
import jp.kshoji.driver.midi.service.MultipleMidiService;
import jp.kshoji.driver.midi.service.MultipleMidiService.LocalBinder;

/**
 * A service that implements a virtual MIDI device.
 * Subclasses must implement the {@link #onGetInputPortReceivers} method to provide a
 * list of {@link MidiReceiver}s to receive data sent to the device's input ports.
 * Similarly, subclasses can call {@link #getOutputPortReceivers} to fetch a list
 * of {@link MidiReceiver}s for sending data out the output ports.
 *
 * <p>To extend this class, you must declare the service in your manifest file with
 * an intent filter with the {@link #SERVICE_INTERFACE} action
 * and meta-data to describe the virtual device.
 For example:</p>
 * <pre>
 * &lt;service android:name=".VirtualDeviceService"
 *          android:label="&#64;string/service_name">
 *     &lt;intent-filter>
 *         &lt;action android:name="android.media.midi.MidiDeviceService" />
 *     &lt;/intent-filter>
 *           &lt;meta-data android:name="android.media.midi.MidiDeviceService"
 android:resource="@xml/device_info" />
 * &lt;/service></pre>
 */
public abstract class MidiDeviceService extends Service {
    private static final String TAG = "MidiDeviceService";

    public static final String SERVICE_INTERFACE = "android.media.midi.MidiDeviceService";

    private MultipleMidiService usbMidiService;
    private BleMidiCentralService bleMidiCentralService;
    private BleMidiPeripheralService bleMidiPeripheralService;

    private final Set<MidiInputDevice> usbMidiInputDevices = new HashSet<MidiInputDevice>();
    private final Set<jp.kshoji.blemidi.device.MidiInputDevice> bleMidiInputDevices = new HashSet<jp.kshoji.blemidi.device.MidiInputDevice>();

    private final Map<Object, MidiOutputPort> midiOutputPorts = new HashMap<Object, MidiOutputPort>();
    private final Map<Object, MidiReceiver> outputPortReceivers = new HashMap<Object, MidiReceiver>();

    private final Set<MidiReceiver> inputPortReceivers = new HashSet<MidiReceiver>();

    private final OnMidiDeviceAttachedListener usbMidiDeviceAttachedListener = new OnMidiDeviceAttachedListener() {
        @Override
        public void onDeviceAttached(UsbDevice usbDevice) {
            // do nothing
        }

        @Override
        public synchronized void onMidiInputDeviceAttached(MidiInputDevice midiInputDevice) {
            midiOutputPorts.put(midiInputDevice, new MidiOutputPort(midiInputDevice));
        }

        @Override
        public synchronized void onMidiOutputDeviceAttached(MidiOutputDevice midiOutputDevice) {
            outputPortReceivers.put(midiOutputDevice, new MidiInputPort(midiOutputDevice));
        }
    };

    private final OnMidiDeviceDetachedListener usbMidiDeviceDetachedListener = new OnMidiDeviceDetachedListener() {
        @Override
        public void onDeviceDetached(UsbDevice usbDevice) {
            // do nothing
        }

        @Override
        public synchronized void onMidiInputDeviceDetached(MidiInputDevice midiInputDevice) {
            usbMidiInputDevices.remove(midiInputDevice);
            midiOutputPorts.remove(midiInputDevice);
        }

        @Override
        public synchronized void onMidiOutputDeviceDetached(MidiOutputDevice midiOutputDevice) {
            outputPortReceivers.remove(midiOutputDevice);
        }
    };

    private final jp.kshoji.blemidi.listener.OnMidiDeviceAttachedListener bleMidiDeviceAttachedListener = new jp.kshoji.blemidi.listener.OnMidiDeviceAttachedListener() {
        @Override
        public synchronized void onMidiInputDeviceAttached(jp.kshoji.blemidi.device.MidiInputDevice midiInputDevice) {
            bleMidiInputDevices.add(midiInputDevice);
            midiOutputPorts.put(midiInputDevice, new MidiOutputPort(midiInputDevice));
        }

        @Override
        public synchronized void onMidiOutputDeviceAttached(jp.kshoji.blemidi.device.MidiOutputDevice midiOutputDevice) {
            outputPortReceivers.put(midiOutputDevice, new MidiInputPort(midiOutputDevice));
        }
    };

    private final jp.kshoji.blemidi.listener.OnMidiDeviceDetachedListener bleMidiDeviceDetachedListener = new jp.kshoji.blemidi.listener.OnMidiDeviceDetachedListener() {
        @Override
        public synchronized void onMidiInputDeviceDetached(jp.kshoji.blemidi.device.MidiInputDevice midiInputDevice) {
            bleMidiInputDevices.remove(midiInputDevice);
            midiOutputPorts.remove(midiInputDevice);
        }

        @Override
        public synchronized void onMidiOutputDeviceDetached(jp.kshoji.blemidi.device.MidiOutputDevice midiOutputDevice) {
            outputPortReceivers.remove(midiOutputDevice);
        }
    };

    private final ServiceConnection usbMidiServiceConnection = new ServiceConnection(){
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            usbMidiService = ((LocalBinder)service).getService();
            usbMidiService.setOnMidiDeviceAttachedListener(usbMidiDeviceAttachedListener);
            usbMidiService.setOnMidiDeviceDetachedListener(usbMidiDeviceDetachedListener);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            usbMidiService = null;
        }
    };

    private final ServiceConnection bleMidiCentralServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            bleMidiCentralService = ((BleMidiCentralService.LocalBinder)service).getService();
            bleMidiCentralService.setOnMidiDeviceAttachedListener(bleMidiDeviceAttachedListener);
            bleMidiCentralService.setOnMidiDeviceDetachedListener(bleMidiDeviceDetachedListener);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bleMidiCentralService = null;
        }
    };

    private final ServiceConnection bleMidiPeripheralServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            bleMidiPeripheralService = ((BleMidiPeripheralService.LocalBinder)service).getService();
            bleMidiPeripheralService.setOnMidiDeviceAttachedListener(bleMidiDeviceAttachedListener);
            bleMidiPeripheralService.setOnMidiDeviceDetachedListener(bleMidiDeviceDetachedListener);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bleMidiPeripheralService = null;
        }
    };

    @SuppressLint("NewApi")
    @Override
    public void onCreate() {
        Intent intent = new Intent(this, MultipleMidiService.class);
        startService(intent);
        bindService(intent, usbMidiServiceConnection, Context.BIND_AUTO_CREATE);

        if (BleUtils.isBleSupported(this) && BleUtils.isBluetoothEnabled(this)) {
            intent = new Intent(this, BleMidiCentralService.class);
            startService(intent);
            bindService(intent, bleMidiCentralServiceConnection, Context.BIND_AUTO_CREATE);

            if (BleUtils.isBlePeripheralSupported(this)) {
                intent = new Intent(this, BleMidiPeripheralService.class);
                startService(intent);
                bindService(intent, bleMidiPeripheralServiceConnection, Context.BIND_AUTO_CREATE);
            }
        }

        final MidiReceiver[] midiReceivers = onGetInputPortReceivers();
        if (midiReceivers != null) {
            for (final MidiReceiver midiReceiver : midiReceivers) {
                inputPortReceivers.add(midiReceiver);
            }
        }
    }

    /**
     * Returns an array of {@link MidiReceiver} for the device's input ports.
     * Subclasses must override this to provide the receivers which will receive
     * data sent to the device's input ports. An empty array should be returned if
     * the device has no input ports.
     * @return array of MidiReceivers
     */
    abstract public MidiReceiver[] onGetInputPortReceivers();

    /**
     * Returns an array of {@link MidiReceiver} for the device's output ports.
     * These can be used to send data out the device's output ports.
     * @return array of MidiReceivers
     */
    public final MidiReceiver[] getOutputPortReceivers() {
        final Collection<MidiReceiver> receivers = outputPortReceivers.values();
        return receivers.toArray(new MidiReceiver[receivers.size()]);
    }

    /**
     * returns the {@link MidiDeviceInfo} instance for this service
     * @return our MidiDeviceInfo
     */
    public final MidiDeviceInfo getDeviceInfo() {
        // returns empty MidiDeviceInfo
        return new MidiDeviceInfo(MidiDeviceInfo.TYPE_VIRTUAL, 0, 0, 0, new String[] {}, new String[] {}, new Bundle(), true);
    }

    /**
     * Called to notify when an our {@link MidiDeviceStatus} has changed
     * @param status the number of the port that was opened
     */
    public void onDeviceStatusChanged(MidiDeviceStatus status) {
    }

    /**
     * Called to notify when our device has been closed by all its clients
     */
    public void onClose() {
    }
}