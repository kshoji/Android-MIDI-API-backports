package android.media.midi;

import android.content.Context;
import android.os.Build.VERSION;

/**
 * @author K.Shoji
 */
public class MidiManagerUtils {
    private static volatile MidiManager instance;
    private static final Object SINGLETON_LOCK = new Object();
    public static MidiManager getMidiManager(final Context context) {
        MidiManager result = instance;
        if (result == null) {
            synchronized (SINGLETON_LOCK) {
                result = instance;
                if (result == null) {
                    if (VERSION.SDK_INT >= 23) {
                        //noinspection ResourceType
                        result = (MidiManager)context.getSystemService("midi");
                    } else {
                        result = new MidiManager(context);
                    }
                    instance = result;
                }
            }
        }

        return result;
    }
}
