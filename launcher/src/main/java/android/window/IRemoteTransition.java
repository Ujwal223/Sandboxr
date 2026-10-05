package android.window;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;
import android.view.SurfaceControl;

public interface IRemoteTransition extends IInterface {
    void startAnimation(
        IBinder token,
        TransitionInfo info,
        SurfaceControl.Transaction t,
        IRemoteTransitionFinishedCallback finishCallback
    ) throws RemoteException;

    void mergeAnimation(
        IBinder token,
        TransitionInfo info,
        SurfaceControl.Transaction t,
        IBinder mergeTarget,
        IRemoteTransitionFinishedCallback finishCallback
    ) throws RemoteException;

    void onTransitionConsumed(IBinder token, boolean aborted) throws RemoteException;

    void takeOverAnimation(
        IBinder token,
        TransitionInfo info,
        SurfaceControl.Transaction t,
        IRemoteTransitionFinishedCallback finishCallback,
        WindowAnimationState[] states
    ) throws RemoteException;

    abstract class Stub extends Binder implements IRemoteTransition {
        public static final String DESCRIPTOR = "android.window.IRemoteTransition";

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IRemoteTransition asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin instanceof IRemoteTransition) {
                return (IRemoteTransition) iin;
            }
            return null;
        }

        @Override
        public IBinder asBinder() {
            return this;
        }
    }
}
