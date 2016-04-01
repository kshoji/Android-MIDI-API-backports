/*
 * Copyright (C) 2015 The Android Open Source Project
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

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * This is an immutable class that describes the current status of a MIDI device's ports.
 */
public final class MidiDeviceStatus implements Parcelable {

    private static final String TAG = "MidiDeviceStatus";

    private final MidiDeviceInfo deviceInfo;
    // true if input ports are open
    private final boolean inputPortOpen[];
    // open counts for output ports
    private final int outputPortOpenCount[];

    /**
     * @hide
     */
    public MidiDeviceStatus(final MidiDeviceInfo deviceInfo, final boolean[] inputPortOpen, final int[] outputPortOpenCount) {
        // MidiDeviceInfo is immutable so we can share references
        this.deviceInfo = deviceInfo;

        // make copies of the arrays
        this.inputPortOpen = new boolean[inputPortOpen.length];
        System.arraycopy(inputPortOpen, 0, this.inputPortOpen, 0, inputPortOpen.length);
        this.outputPortOpenCount = new int[outputPortOpenCount.length];
        System.arraycopy(outputPortOpenCount, 0, this.outputPortOpenCount, 0,
                outputPortOpenCount.length);
    }

    /**
     * Creates a MidiDeviceStatus with zero for all port open counts
     * @hide
     */
    @SuppressLint("NewApi")
    public MidiDeviceStatus(final MidiDeviceInfo deviceInfo) {
        this.deviceInfo = deviceInfo;
        inputPortOpen = new boolean[deviceInfo.getInputPortCount()];
        outputPortOpenCount = new int[deviceInfo.getOutputPortCount()];
    }

    /**
     * Returns the {@link MidiDeviceInfo} of the device.
     *
     * @return the device info
     */
    public MidiDeviceInfo getDeviceInfo() {
        return deviceInfo;
    }

    /**
     * Returns true if an input port is open.
     * An input port can only be opened by one client at a time.
     *
     * @param portNumber the input port's port number
     * @return input port open status
     */
    public boolean isInputPortOpen(final int portNumber) {
        return inputPortOpen[portNumber];
    }

    /**
     * Returns the number of clients currently connected to the specified output port.
     * Unlike input ports, an output port can be opened by multiple clients at the same time.
     *
     * @param portNumber the output port's port number
     * @return output port open count
     */
    public int getOutputPortOpenCount(final int portNumber) {
        return outputPortOpenCount[portNumber];
    }

    @SuppressLint("NewApi")
    @Override
    public String toString() {
        int inputPortCount = deviceInfo.getInputPortCount();
        int outputPortCount = deviceInfo.getOutputPortCount();
        StringBuilder builder = new StringBuilder("inputPortOpen=[");
        for (int i = 0; i < inputPortCount; i++) {
            builder.append(inputPortOpen[i]);
            if (i < inputPortCount -1) {
                builder.append(",");
            }
        }
        builder.append("] outputPortOpenCount=[");
        for (int i = 0; i < outputPortCount; i++) {
            builder.append(outputPortOpenCount[i]);
            if (i < outputPortCount -1) {
                builder.append(",");
            }
        }
        builder.append("]");
        return builder.toString();
    }

    public static final Creator<MidiDeviceStatus> CREATOR =
            new Creator<MidiDeviceStatus>() {
                public MidiDeviceStatus createFromParcel(Parcel in) {
                    ClassLoader classLoader = MidiDeviceInfo.class.getClassLoader();
                    MidiDeviceInfo deviceInfo = in.readParcelable(classLoader);
                    boolean[] inputPortOpen = in.createBooleanArray();
                    int[] outputPortOpenCount = in.createIntArray();
                    return new MidiDeviceStatus(deviceInfo, inputPortOpen, outputPortOpenCount);
                }

                public MidiDeviceStatus[] newArray(int size) {
                    return new MidiDeviceStatus[size];
                }
            };

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(final Parcel parcel, final int flags) {
        parcel.writeParcelable(deviceInfo, flags);
        parcel.writeBooleanArray(inputPortOpen);
        parcel.writeIntArray(outputPortOpenCount);
    }
}
