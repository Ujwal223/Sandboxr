package android.window;

import android.os.IBinder;
import android.os.RemoteException;
import android.view.SurfaceControl;

public abstract class RemoteTransitionStub extends IRemoteTransition.Stub {
    @Override
    public void startAnimation(
        IBinder token,
        TransitionInfo info,
        SurfaceControl.Transaction t,
        IRemoteTransitionFinishedCallback finishCallback
    ) throws RemoteException {}

    @Override
    public void mergeAnimation(
        IBinder token,
        TransitionInfo info,
        SurfaceControl.Transaction t,
        IBinder mergeTarget,
        IRemoteTransitionFinishedCallback finishCallback
    ) throws RemoteException {}

    @Override
    public void onTransitionConsumed(IBinder token, boolean aborted) throws RemoteException {}

    @Override
    public void takeOverAnimation(
        IBinder token,
        TransitionInfo info,
        SurfaceControl.Transaction t,
        IRemoteTransitionFinishedCallback finishCallback,
        WindowAnimationState[] states
    ) throws RemoteException {}
}
