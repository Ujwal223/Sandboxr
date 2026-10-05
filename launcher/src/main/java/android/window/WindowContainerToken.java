package android.window;

import android.os.Parcel;
import android.os.Parcelable;

public class WindowContainerToken implements Parcelable {
    public WindowContainerToken() {}

    protected WindowContainerToken(Parcel in) {}

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {}

    public static final Creator<WindowContainerToken> CREATOR = new Creator<>() {
        @Override
        public WindowContainerToken createFromParcel(Parcel in) {
            return new WindowContainerToken(in);
        }

        @Override
        public WindowContainerToken[] newArray(int size) {
            return new WindowContainerToken[size];
        }
    };
}
