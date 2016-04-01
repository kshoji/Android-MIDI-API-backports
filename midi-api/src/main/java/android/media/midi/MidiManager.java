package android.media.midi;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

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
 * This class is the public application interface to the MIDI service.
 *
 * <p>You can obtain an instance of this class by calling
 * {@link android.content.Context#getSystemService(java.lang.String) Context.getSystemService()}.
 *
 * {@samplecode
 * MidiManager manager = (MidiManager) getSystemService(Context.MIDI_SERVICE);}
 */
public final class MidiManager {
    private MultipleMidiService usbMidiService;
    private BleMidiCentralService bleMidiCentralService;
    private BleMidiPeripheralService bleMidiPeripheralService;

    private Map<Object, MidiDeviceInfo> midiDeviceInfos = new HashMap<Object, MidiDeviceInfo>();

    private static volatile int portNumber = 0;

    private final OnMidiDeviceAttachedListener usbMidiDeviceAttachedListener = new OnMidiDeviceAttachedListener() {
        @Override
        public void onDeviceAttached(final UsbDevice usbDevice) {
            // do nothing
        }

        @Override
        public synchronized void onMidiInputDeviceAttached(final MidiInputDevice midiInputDevice) {
            final MidiDeviceInfo midiDeviceInfo = new MidiDeviceInfo(MidiDeviceInfo.TYPE_USB, portNumber, 1, 0,
            new String[] { midiInputDevice.getDeviceAddress() }, new String[] {}, new Bundle(),
            false);
            portNumber++;
            midiDeviceInfos.put(midiInputDevice, midiDeviceInfo);

            for (final DeviceListener deviceListener : deviceListeners.values()) {
                deviceListener.onDeviceAdded(midiDeviceInfo);
            }
        }

        @Override
        public synchronized void onMidiOutputDeviceAttached(final MidiOutputDevice midiOutputDevice) {
            final MidiDeviceInfo midiDeviceInfo = new MidiDeviceInfo(MidiDeviceInfo.TYPE_USB, portNumber, 0, 1,
                    new String[] {}, new String[] { midiOutputDevice.getDeviceAddress() }, new Bundle(),
                    false);
            portNumber++;
            midiDeviceInfos.put(midiOutputDevice, midiDeviceInfo);

            for (final DeviceListener deviceListener : deviceListeners.values()) {
                deviceListener.onDeviceAdded(midiDeviceInfo);
            }
        }
    };

    private final OnMidiDeviceDetachedListener usbMidiDeviceDetachedListener = new OnMidiDeviceDetachedListener() {
        @Override
        public void onDeviceDetached(final UsbDevice usbDevice) {
            // do nothing
        }

        @Override
        public synchronized void onMidiInputDeviceDetached(final MidiInputDevice midiInputDevice) {
            final MidiDeviceInfo midiDeviceInfo = midiDeviceInfos.remove(midiInputDevice);

            for (final DeviceListener deviceListener : deviceListeners.values()) {
                deviceListener.onDeviceRemoved(midiDeviceInfo);
            }
        }

        @Override
        public synchronized void onMidiOutputDeviceDetached(final MidiOutputDevice midiOutputDevice) {
            final MidiDeviceInfo midiDeviceInfo = midiDeviceInfos.remove(midiOutputDevice);

            for (final DeviceListener deviceListener : deviceListeners.values()) {
                deviceListener.onDeviceRemoved(midiDeviceInfo);
            }
        }
    };

    private final jp.kshoji.blemidi.listener.OnMidiDeviceAttachedListener bleMidiDeviceAttachedListener = new jp.kshoji.blemidi.listener.OnMidiDeviceAttachedListener() {
        @Override
        public synchronized void onMidiInputDeviceAttached(final jp.kshoji.blemidi.device.MidiInputDevice midiInputDevice) {
            final MidiDeviceInfo midiDeviceInfo = new MidiDeviceInfo(MidiDeviceInfo.TYPE_BLUETOOTH, portNumber, 1, 0,
                    new String[] { midiInputDevice.getDeviceAddress() }, new String[] {}, new Bundle(),
                    false);
            portNumber++;
            midiDeviceInfos.put(midiInputDevice, midiDeviceInfo);

            for (final DeviceListener deviceListener : deviceListeners.values()) {
                deviceListener.onDeviceAdded(midiDeviceInfo);
            }
        }

        @Override
        public synchronized void onMidiOutputDeviceAttached(final jp.kshoji.blemidi.device.MidiOutputDevice midiOutputDevice) {
            final MidiDeviceInfo midiDeviceInfo = new MidiDeviceInfo(MidiDeviceInfo.TYPE_BLUETOOTH, portNumber, 0, 1,
                    new String[] {}, new String[] { midiOutputDevice.getDeviceAddress() }, new Bundle(),
                    false);
            portNumber++;
            midiDeviceInfos.put(midiOutputDevice, midiDeviceInfo);

            for (final DeviceListener deviceListener : deviceListeners.values()) {
                deviceListener.onDeviceAdded(midiDeviceInfo);
            }
        }
    };

    private final jp.kshoji.blemidi.listener.OnMidiDeviceDetachedListener bleMidiDeviceDetachedListener = new jp.kshoji.blemidi.listener.OnMidiDeviceDetachedListener() {
        @Override
        public synchronized void onMidiInputDeviceDetached(final jp.kshoji.blemidi.device.MidiInputDevice midiInputDevice) {
            final MidiDeviceInfo midiDeviceInfo = midiDeviceInfos.remove(midiInputDevice);

            for (final DeviceListener deviceListener : deviceListeners.values()) {
                deviceListener.onDeviceRemoved(midiDeviceInfo);
            }
        }

        @Override
        public synchronized void onMidiOutputDeviceDetached(final jp.kshoji.blemidi.device.MidiOutputDevice midiOutputDevice) {
            final MidiDeviceInfo midiDeviceInfo = midiDeviceInfos.remove(midiOutputDevice);

            for (final DeviceListener deviceListener : deviceListeners.values()) {
                deviceListener.onDeviceRemoved(midiDeviceInfo);
            }
        }
    };

    private final ServiceConnection usbMidiServiceConnection = new ServiceConnection(){
        @Override
        public void onServiceConnected(final ComponentName name, final IBinder service) {
            usbMidiService = ((LocalBinder)service).getService();
            usbMidiService.setOnMidiDeviceAttachedListener(usbMidiDeviceAttachedListener);
            usbMidiService.setOnMidiDeviceDetachedListener(usbMidiDeviceDetachedListener);
        }

        @Override
        public void onServiceDisconnected(final ComponentName name) {
            usbMidiService = null;
        }
    };

    private final ServiceConnection bleMidiCentralServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(final ComponentName name, final IBinder service) {
            bleMidiCentralService = ((BleMidiCentralService.LocalBinder)service).getService();
            bleMidiCentralService.setOnMidiDeviceAttachedListener(bleMidiDeviceAttachedListener);
            bleMidiCentralService.setOnMidiDeviceDetachedListener(bleMidiDeviceDetachedListener);
        }

        @Override
        public void onServiceDisconnected(final ComponentName name) {
            bleMidiCentralService = null;
        }
    };

    private final ServiceConnection bleMidiPeripheralServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(final ComponentName name, final IBinder service) {
            bleMidiPeripheralService = ((BleMidiPeripheralService.LocalBinder)service).getService();
            bleMidiPeripheralService.setOnMidiDeviceAttachedListener(bleMidiDeviceAttachedListener);
            bleMidiPeripheralService.setOnMidiDeviceDetachedListener(bleMidiDeviceDetachedListener);
        }

        @Override
        public void onServiceDisconnected(final ComponentName name) {
            bleMidiPeripheralService = null;
        }
    };


    /**
     * Intent for starting BluetoothMidiService
     * @hide
     */
    public static final String BLUETOOTH_MIDI_SERVICE_INTENT = "android.media.midi.BluetoothMidiService";

    /**
     * BluetoothMidiService package name
     * @hide
     */
    public static final String BLUETOOTH_MIDI_SERVICE_PACKAGE = "com.android.bluetoothmidiservice";

    /**
     * BluetoothMidiService class name
     * @hide
     */
    public static final String BLUETOOTH_MIDI_SERVICE_CLASS = "com.android.bluetoothmidiservice.BluetoothMidiService";

    private final Map<DeviceCallback, DeviceListener> deviceListeners = new ConcurrentHashMap<DeviceCallback,DeviceListener>();

    // Binder stub for receiving device notifications from MidiService
    private class DeviceListener {
        private final DeviceCallback callback;
        private final Handler handler;

        public DeviceListener(final DeviceCallback callback, final Handler handler) {
            this.callback = callback;
            this.handler = handler;
        }

        @SuppressLint("NewApi")
        public void onDeviceAdded(final MidiDeviceInfo device) {
            if (handler != null) {
                handler.post(new Runnable() {
                    @SuppressLint("NewApi")
                    @Override public void run() {
                        callback.onDeviceAdded(device);
                    }
                });
            } else {
                callback.onDeviceAdded(device);
            }
        }

        @SuppressLint("NewApi")
        public void onDeviceRemoved(final MidiDeviceInfo device) {
            if (handler != null) {
                handler.post(new Runnable() {
                    @SuppressLint("NewApi")
                    @Override public void run() {
                        callback.onDeviceRemoved(device);
                    }
                });
            } else {
                callback.onDeviceRemoved(device);
            }
        }

        @SuppressLint("NewApi")
        public void onDeviceStatusChanged(final MidiDeviceStatus status) {
            if (handler != null) {
                handler.post(new Runnable() {
                    @SuppressLint("NewApi")
                    @Override public void run() {
                        callback.onDeviceStatusChanged(status);
                    }
                });
            } else {
                callback.onDeviceStatusChanged(status);
            }
        }
    }

    /**
     * Callback class used for clients to receive MIDI device added and removed notifications
     */
    public static class DeviceCallback {
        /**
         * Called to notify when a new MIDI device has been added
         *
         * @param device a {@link MidiDeviceInfo} for the newly added device
         */
        public void onDeviceAdded(final MidiDeviceInfo device) {
        }

        /**
         * Called to notify when a MIDI device has been removed
         *
         * @param device a {@link MidiDeviceInfo} for the removed device
         */
        public void onDeviceRemoved(final MidiDeviceInfo device) {
        }

        /**
         * Called to notify when the status of a MIDI device has changed
         *
         * @param status a {@link MidiDeviceStatus} for the changed device
         */
        public void onDeviceStatusChanged(final MidiDeviceStatus status) {
        }
    }

    /**
     * Listener class used for receiving the results of {@link #openDevice} and
     * {@link #openBluetoothDevice}
     */
    public interface OnDeviceOpenedListener {
        /**
         * Called to respond to a {@link #openDevice} request
         *
         * @param device a {@link MidiDevice} for opened device, or null if opening failed
         */
        void onDeviceOpened(MidiDevice device);
    }

    private static volatile MidiManager instance;
    private static final Object SINGLETON_LOCK = new Object();
    public static MidiManager getInstance(final Context context) {
        MidiManager result = instance;
        if (result == null) {
            synchronized (SINGLETON_LOCK) {
                result = instance;
                if (result == null) {
                    result = new MidiManager(context);
                    instance = result;
                }
            }
        }

        return result;
    }

    MidiManager(final Context context) {
        Intent intent = new Intent(context, MultipleMidiService.class);
        context.startService(intent);
        context.bindService(intent, usbMidiServiceConnection, Context.BIND_AUTO_CREATE);

        if (BleUtils.isBleSupported(context) && BleUtils.isBluetoothEnabled(context)) {
            intent = new Intent(context, BleMidiCentralService.class);
            context.startService(intent);
            context.bindService(intent, bleMidiCentralServiceConnection, Context.BIND_AUTO_CREATE);

            if (BleUtils.isBlePeripheralSupported(context)) {
                intent = new Intent(context, BleMidiPeripheralService.class);
                context.startService(intent);
                context.bindService(intent, bleMidiPeripheralServiceConnection, Context.BIND_AUTO_CREATE);
            }
        }
    }

    /**
     * Registers a callback to receive notifications when MIDI devices are added and removed.
     *
     * @param callback a {@link DeviceCallback} for MIDI device notifications
     * @param handler The {@link android.os.Handler Handler} that will be used for delivering the
     *                device notifications. If handler is null, then the thread used for the
     *                callback is unspecified.
     */
    public void registerDeviceCallback(final DeviceCallback callback, final Handler handler) {
        final DeviceListener deviceListener = new DeviceListener(callback, handler);
        deviceListeners.put(callback, deviceListener);
    }

    /**
     * Unregisters a {@link DeviceCallback}.
     *
     * @param callback a {@link DeviceCallback} to unregister
     */
    public void unregisterDeviceCallback(final DeviceCallback callback) {
        deviceListeners.remove(callback);
    }

    /**
     * Gets the list of all connected MIDI devices.
     *
     * @return an array of all MIDI devices
     */
    public MidiDeviceInfo[] getDevices() {
        final Collection<MidiDeviceInfo> deviceInfos = midiDeviceInfos.values();
        return deviceInfos.toArray(new MidiDeviceInfo[deviceInfos.size()]);
    }

    @SuppressLint("NewApi")
    private void sendOpenDeviceResponse(final MidiDevice device, final OnDeviceOpenedListener listener, final Handler handler) {
        if (handler != null) {
            handler.post(new Runnable() {
                @SuppressLint("NewApi")
                @Override public void run() {
                    listener.onDeviceOpened(device);
                }
            });
        } else {
            listener.onDeviceOpened(device);
        }
    }

    /**
     * Opens a MIDI device for reading and writing.
     *
     * @param deviceInfo a {@link android.media.midi.MidiDeviceInfo} to open
     * @param listener a {@link MidiManager.OnDeviceOpenedListener} to be called
     *                 to receive the result
     * @param handler the {@link android.os.Handler Handler} that will be used for delivering
     *                the result. If handler is null, then the thread used for the
     *                listener is unspecified.
     */
    public void openDevice(final MidiDeviceInfo deviceInfo, final OnDeviceOpenedListener listener, final Handler handler) {
        Object device = null;
        for (final Entry<Object, MidiDeviceInfo> entry : midiDeviceInfos.entrySet()) {
            if (entry.getValue() == deviceInfo) {
                device = entry.getKey();
                break;
            }
        }

        if (device == null) {
            return;
        }

        MidiDevice midiDevice = null;
        if (device instanceof MidiInputDevice) {
            midiDevice = new MidiDevice(deviceInfo, (MidiInputDevice) device);
        } else if (device instanceof MidiOutputDevice) {
            midiDevice = new MidiDevice(deviceInfo, (MidiOutputDevice) device);
        } else if (device instanceof jp.kshoji.blemidi.device.MidiInputDevice) {
            midiDevice = new MidiDevice(deviceInfo, (jp.kshoji.blemidi.device.MidiInputDevice) device);
        } else if (device instanceof jp.kshoji.blemidi.device.MidiOutputDevice) {
            midiDevice = new MidiDevice(deviceInfo, (jp.kshoji.blemidi.device.MidiOutputDevice) device);
        }

        if (midiDevice != null) {
            sendOpenDeviceResponse(midiDevice, listener, handler);
        }
    }

    /**
     * Opens a Bluetooth MIDI device for reading and writing.
     *
     * @param bluetoothDevice a {@link android.bluetooth.BluetoothDevice} to open as a MIDI device
     * @param listener a {@link MidiManager.OnDeviceOpenedListener} to be called to receive the
     * result
     * @param handler the {@link android.os.Handler Handler} that will be used for delivering
     *                the result. If handler is null, then the thread used for the
     *                listener is unspecified.
     */
    public void openBluetoothDevice(final BluetoothDevice bluetoothDevice, final OnDeviceOpenedListener listener, final Handler handler) {
        for (final Object device : midiDeviceInfos.keySet()) {
            if (device != null) {
                if (device instanceof jp.kshoji.blemidi.device.MidiInputDevice) {
                    final jp.kshoji.blemidi.device.MidiInputDevice midiInputDevice = (jp.kshoji.blemidi.device.MidiInputDevice) device;
                    if (midiInputDevice.getDeviceAddress().equals(bluetoothDevice.getAddress())) {
                        final MidiDevice midiDevice = new MidiDevice(midiDeviceInfos.get(device), midiInputDevice);
                        sendOpenDeviceResponse(midiDevice, listener, handler);
                        break;
                    }
                } else if (device instanceof jp.kshoji.blemidi.device.MidiOutputDevice) {
                    final jp.kshoji.blemidi.device.MidiOutputDevice midiOutputDevice = (jp.kshoji.blemidi.device.MidiOutputDevice) device;
                    if (midiOutputDevice.getDeviceAddress().equals(bluetoothDevice.getAddress())) {
                        final MidiDevice midiDevice = new MidiDevice(midiDeviceInfos.get(device), midiOutputDevice);
                        sendOpenDeviceResponse(midiDevice, listener, handler);
                        break;
                    }
                }
            }
        }
    }
}
