package android.window;

import android.os.Parcel;
import android.os.Parcelable;
import android.view.SurfaceControl;
import java.util.ArrayList;
import java.util.List;

public final class TransitionInfo implements Parcelable {
    public static final int FLAG_NONE = 0;
    public static final int FLAG_FIRST_CUSTOM = 1;

    private int mType;
    private int mFlags;
    private final List<Change> mChanges = new ArrayList<>();

    public TransitionInfo() {}

    public TransitionInfo(int type, int flags) {
        mType = type;
        mFlags = flags;
    }

    protected TransitionInfo(Parcel in) {
        mType = in.readInt();
        mFlags = in.readInt();
    }

    public int getType() {
        return mType;
    }

    public int getFlags() {
        return mFlags;
    }

    public List<Change> getChanges() {
        return mChanges;
    }

    public void addChange(Change change) {
        mChanges.add(change);
    }

    public static class Change implements Parcelable {
        private int mMode;
        private int mFlags;
        private SurfaceControl mLeash;
        private WindowContainerToken mContainer;

        public Change() {}

        public Change(int mode, int flags) {
            mMode = mode;
            mFlags = flags;
        }

        protected Change(Parcel in) {
            mMode = in.readInt();
            mFlags = in.readInt();
        }

        public int getMode() {
            return mMode;
        }

        public int getFlags() {
            return mFlags;
        }

        public SurfaceControl getLeash() {
            return mLeash;
        }

        public void setLeash(SurfaceControl leash) {
            mLeash = leash;
        }

        public WindowContainerToken getContainer() {
            return mContainer;
        }

        public void setContainer(WindowContainerToken container) {
            mContainer = container;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(mMode);
            dest.writeInt(mFlags);
        }

        public static final Creator<Change> CREATOR = new Creator<>() {
            @Override
            public Change createFromParcel(Parcel in) {
                return new Change(in);
            }

            @Override
            public Change[] newArray(int size) {
                return new Change[size];
            }
        };
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mType);
        dest.writeInt(mFlags);
    }

    public static final Creator<TransitionInfo> CREATOR = new Creator<>() {
        @Override
        public TransitionInfo createFromParcel(Parcel in) {
            return new TransitionInfo(in);
        }

        @Override
        public TransitionInfo[] newArray(int size) {
            return new TransitionInfo[size];
        }
    };
}
