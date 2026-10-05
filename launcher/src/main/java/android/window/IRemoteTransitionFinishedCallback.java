package android.window;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;
import android.view.SurfaceControl;

public interface IRemoteTransitionFinishedCallback extends IInterface {
    void onTransitionFinished(
        WindowContainerTransaction wct,
        SurfaceControl.Transaction sct
    ) throws RemoteException;

    abstract class Stub extends Binder implements IRemoteTransitionFinishedCallback {
        public static final String DESCRIPTOR = "android.window.IRemoteTransitionFinishedCallback";

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IRemoteTransitionFinishedCallback asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin instanceof IRemoteTransitionFinishedCallback) {
                return (IRemoteTransitionFinishedCallback) iin;
            }
            return null;
        }

        @Override
        public IBinder asBinder() {
            return this;
        }
    }
}
