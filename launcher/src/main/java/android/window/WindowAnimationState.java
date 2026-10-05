package android.window;

import android.os.Parcel;
import android.os.Parcelable;

public class WindowAnimationState implements Parcelable {
    public float timestamp;

    public WindowAnimationState() {}

    protected WindowAnimationState(Parcel in) {
        timestamp = in.readFloat();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeFloat(timestamp);
    }

    public static final Creator<WindowAnimationState> CREATOR = new Creator<>() {
        @Override
        public WindowAnimationState createFromParcel(Parcel in) {
            return new WindowAnimationState(in);
        }

        @Override
        public WindowAnimationState[] newArray(int size) {
            return new WindowAnimationState[size];
        }
    };
}
