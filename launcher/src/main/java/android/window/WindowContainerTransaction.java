package android.window;

import android.os.Parcel;
import android.os.Parcelable;

public class WindowContainerTransaction implements Parcelable {
    public WindowContainerTransaction() {}

    protected WindowContainerTransaction(Parcel in) {}

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {}

    public static final Creator<WindowContainerTransaction> CREATOR = new Creator<>() {
        @Override
        public WindowContainerTransaction createFromParcel(Parcel in) {
            return new WindowContainerTransaction(in);
        }

        @Override
        public WindowContainerTransaction[] newArray(int size) {
            return new WindowContainerTransaction[size];
        }
    };
}
