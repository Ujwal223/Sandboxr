package android.window;

import android.os.Parcel;
import android.os.Parcelable;

public class RemoteTransition implements Parcelable {
    private IRemoteTransition mRemoteTransition;

    public RemoteTransition() {}

    public RemoteTransition(IRemoteTransition remoteTransition) {
        this.mRemoteTransition = remoteTransition;
    }

    protected RemoteTransition(Parcel in) {
        mRemoteTransition = IRemoteTransition.Stub.asInterface(in.readStrongBinder());
    }

    public IRemoteTransition getRemoteTransition() {
        return mRemoteTransition;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStrongBinder(mRemoteTransition != null ? mRemoteTransition.asBinder() : null);
    }

    public static final Creator<RemoteTransition> CREATOR = new Creator<>() {
        @Override
        public RemoteTransition createFromParcel(Parcel in) {
            return new RemoteTransition(in);
        }

        @Override
        public RemoteTransition[] newArray(int size) {
            return new RemoteTransition[size];
        }
    };
}
