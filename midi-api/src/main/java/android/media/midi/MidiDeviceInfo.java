/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.media.midi;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * This class contains information to describe a MIDI device.
 * For now we only have information that can be retrieved easily for USB devices,
 * but we will probably expand this in the future.
 *
 * This class is just an immutable object to encapsulate the MIDI device description.
 * Use the MidiDevice class to actually communicate with devices.
 */
public final class MidiDeviceInfo implements Parcelable {

    private static final String TAG = "MidiDeviceInfo";

    /**
     * Constant representing USB MIDI devices for {@link #getType}
     */
    public static final int TYPE_USB = 1;

    /**
     * Constant representing virtual (software based) MIDI devices for {@link #getType}
     */
    public static final int TYPE_VIRTUAL = 2;

    /**
     * Constant representing Bluetooth MIDI devices for {@link #getType}
     */
    public static final int TYPE_BLUETOOTH = 3;

    /**
     * Bundle key for the device's user visible name property.
     * The value for this property is of type {@link java.lang.String}.
     * Used with the {@link android.os.Bundle} returned by {@link #getProperties}.
     * For USB devices, this is a concatenation of the manufacturer and product names.
     */
    public static final String PROPERTY_NAME = "name";

    /**
     * Bundle key for the device's manufacturer name property.
     * The value for this property is of type {@link java.lang.String}.
     * Used with the {@link android.os.Bundle} returned by {@link #getProperties}.
     * Matches the USB device manufacturer name string for USB MIDI devices.
     */
    public static final String PROPERTY_MANUFACTURER = "manufacturer";

    /**
     * Bundle key for the device's product name property.
     * The value for this property is of type {@link java.lang.String}.
     * Used with the {@link android.os.Bundle} returned by {@link #getProperties}
     * Matches the USB device product name string for USB MIDI devices.
     */
    public static final String PROPERTY_PRODUCT = "product";

    /**
     * Bundle key for the device's version property.
     * The value for this property is of type {@link java.lang.String}.
     * Used with the {@link android.os.Bundle} returned by {@link #getProperties}
     * Matches the USB device version number for USB MIDI devices.
     */
    public static final String PROPERTY_VERSION = "version";

    /**
     * Bundle key for the device's serial number property.
     * The value for this property is of type {@link java.lang.String}.
     * Used with the {@link android.os.Bundle} returned by {@link #getProperties}
     * Matches the USB device serial number for USB MIDI devices.
     */
    public static final String PROPERTY_SERIAL_NUMBER = "serial_number";

    /**
     * Bundle key for the device's corresponding USB device.
     * The value for this property is of type {@link android.hardware.usb.UsbDevice}.
     * Only set for USB MIDI devices.
     * Used with the {@link android.os.Bundle} returned by {@link #getProperties}
     */
    public static final String PROPERTY_USB_DEVICE = "usb_device";

    /**
     * Bundle key for the device's corresponding Bluetooth device.
     * The value for this property is of type {@link android.bluetooth.BluetoothDevice}.
     * Only set for Bluetooth MIDI devices.
     * Used with the {@link android.os.Bundle} returned by {@link #getProperties}
     */
    public static final String PROPERTY_BLUETOOTH_DEVICE = "bluetooth_device";

    /**
     * Bundle key for the device's ALSA card number.
     * The value for this property is an integer.
     * Only set for USB MIDI devices.
     * Used with the {@link android.os.Bundle} returned by {@link #getProperties}
     *
     * @hide
     */
    public static final String PROPERTY_ALSA_CARD = "alsa_card";

    /**
     * Bundle key for the device's ALSA device number.
     * The value for this property is an integer.
     * Only set for USB MIDI devices.
     * Used with the {@link android.os.Bundle} returned by {@link #getProperties}
     *
     * @hide
     */
    public static final String PROPERTY_ALSA_DEVICE = "alsa_device";

    /**
     * ServiceInfo for the service hosting the device implementation.
     * The value for this property is of type {@link android.content.pm.ServiceInfo}.
     * Only set for Virtual MIDI devices.
     * Used with the {@link android.os.Bundle} returned by {@link #getProperties}
     *
     * @hide
     */
    public static final String PROPERTY_SERVICE_INFO = "service_info";

    /**
     * Contains information about an input or output port.
     */
    public static final class PortInfo {
        /**
         * Port type for input ports
         */
        public static final int TYPE_INPUT = 1;

        /**
         * Port type for output ports
         */
        public static final int TYPE_OUTPUT = 2;

        private final int mPortType;
        private final int mPortNumber;
        private final String mName;

        PortInfo(final int type, final int portNumber, final String name) {
            mPortType = type;
            mPortNumber = portNumber;
            mName = name == null ? "" : name;
        }

        /**
         * Returns the port type of the port (either {@link #TYPE_INPUT} or {@link #TYPE_OUTPUT})
         * @return the port type
         */
        public int getType() {
            return mPortType;
        }

        /**
         * Returns the port number of the port
         * @return the port number
         */
        public int getPortNumber() {
            return mPortNumber;
        }

        /**
         * Returns the name of the port, or empty string if the port has no name
         * @return the port name
         */
        public String getName() {
            return mName;
        }
    }

    private final int type;    // USB or virtual
    private final int id;      // unique ID generated by MidiService
    private final int inputPortCount;
    private final int outputPortCount;
    private final String[] inputPortNames;
    private final String[] outputPortNames;
    private final Bundle properties;
    private final boolean isPrivate;

    /**
     * MidiDeviceInfo should only be instantiated by MidiService implementation
     * @hide
     */
    public MidiDeviceInfo(final int type, final int id, final int numInputPorts, final int numOutputPorts,
                          final String[] inputPortNames, final String[] outputPortNames, final Bundle properties,
                          final boolean isPrivate) {
        this.type = type;
        this.id = id;
        inputPortCount = numInputPorts;
        outputPortCount = numOutputPorts;
        if (inputPortNames == null) {
            this.inputPortNames = new String[numInputPorts];
        } else {
            this.inputPortNames = inputPortNames;
        }
        if (outputPortNames == null) {
            this.outputPortNames = new String[numOutputPorts];
        } else {
            this.outputPortNames = outputPortNames;
        }
        this.properties = properties;
        this.isPrivate = isPrivate;
    }

    /**
     * Returns the type of the device.
     *
     * @return the device's type
     */
    public int getType() {
        return type;
    }

    /**
     * Returns the ID of the device.
     * This ID is generated by the MIDI service and is not persistent across device unplugs.
     *
     * @return the device's ID
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the device's number of input ports.
     *
     * @return the number of input ports
     */
    public int getInputPortCount() {
        return inputPortCount;
    }

    /**
     * Returns the device's number of output ports.
     *
     * @return the number of output ports
     */
    public int getOutputPortCount() {
        return outputPortCount;
    }

    /**
     * Returns information about the device's ports.
     * The ports are in unspecified order.
     *
     * @return array of {@link PortInfo}
     */
    public PortInfo[] getPorts() {
        final PortInfo[] ports = new PortInfo[inputPortCount + outputPortCount];

        int index = 0;
        for (int i = 0; i < inputPortCount; i++) {
            ports[index++] = new PortInfo(PortInfo.TYPE_INPUT, i, inputPortNames[i]);
        }
        for (int i = 0; i < outputPortCount; i++) {
            ports[index++] = new PortInfo(PortInfo.TYPE_OUTPUT, i, outputPortNames[i]);
        }

        return ports;
    }

    /**
     * Returns the {@link android.os.Bundle} containing the device's properties.
     *
     * @return the device's properties
     */
    public Bundle getProperties() {
        return properties;
    }

    /**
     * Returns true if the device is private.  Private devices are only visible and accessible
     * to clients with the same UID as the application that is hosting the device.
     *
     * @return true if the device is private
     */
    public boolean isPrivate() {
        return isPrivate;
    }

    @Override
    public boolean equals(final Object o) {
        if (o instanceof MidiDeviceInfo) {
            return (((MidiDeviceInfo)o).id == id);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public String toString() {
        // This is a hack to force the properties Bundle to unparcel so we can
        // print all the names and values.
        properties.getString(PROPERTY_NAME);
        return ("MidiDeviceInfo[type=" + type +
                ",inputPortCount=" + inputPortCount +
                ",outputPortCount=" + outputPortCount +
                ",properties=" + properties +
                ",isPrivate=" + isPrivate);
    }

    public static final Creator<MidiDeviceInfo> CREATOR =
            new Creator<MidiDeviceInfo>() {
                public MidiDeviceInfo createFromParcel(final Parcel in) {
                    final int type = in.readInt();
                    final int id = in.readInt();
                    final int inputPorts = in.readInt();
                    final int outputPorts = in.readInt();
                    final String[] inputPortNames = in.createStringArray();
                    final String[] outputPortNames = in.createStringArray();
                    final Bundle properties = in.readBundle();
                    final boolean isPrivate = in.readInt() == 1;
                    return new MidiDeviceInfo(type, id, inputPorts, outputPorts,
                            inputPortNames, outputPortNames, properties, isPrivate);
                }

                public MidiDeviceInfo[] newArray(final int size) {
                    return new MidiDeviceInfo[size];
                }
            };

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(final Parcel parcel, final int flags) {
        parcel.writeInt(type);
        parcel.writeInt(id);
        parcel.writeInt(inputPortCount);
        parcel.writeInt(outputPortCount);
        parcel.writeStringArray(inputPortNames);
        parcel.writeStringArray(outputPortNames);
        parcel.writeBundle(properties);
        parcel.writeInt(isPrivate ? 1 : 0);
    }
}
