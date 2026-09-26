package com.sandboxr.virtual.client.stub

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment

/**
 * Host Fragment container that embeds guest virtual fragments in-process.
 * Supports Android 16 (API 36) edge-to-edge insets and predictive back gestures.
 */
open class StubFragment : Fragment() {

    companion object {
        private const val TAG = "StubFragment"
        const val ARG_GUEST_FRAGMENT_CLASS = "arg_guest_fragment_class"
        const val ARG_ENV_ID = "arg_env_id"
        const val ARG_PACKAGE_NAME = "arg_package_name"
    }

    private var guestFragment: Fragment? = null
    private var backPressedCallback: OnBackPressedCallback? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = FrameLayout(requireContext()).apply {
            id = View.generateViewId()
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // Apply edge-to-edge insets padding to avoid status bar/nav bar overlaps
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Wire predictive back callback tied to viewLifecycleOwner
        setupPredictiveBack()

        // Instantiate guest fragment if specified in arguments
        val guestClass = arguments?.getString(ARG_GUEST_FRAGMENT_CLASS)
        if (guestClass != null) {
            try {
                val clazz = requireContext().classLoader.loadClass(guestClass)
                val fragmentInstance = clazz.getDeclaredConstructor().newInstance() as Fragment
                guestFragment = fragmentInstance
                childFragmentManager.beginTransaction()
                    .replace(view.id, fragmentInstance)
                    .commit()
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to instantiate guest fragment $guestClass", t)
            }
        }
    }

    private fun setupPredictiveBack() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackStarted(backEvent: BackEventCompat) {
                Log.d(TAG, "Fragment predictive back started (progress: ${backEvent.progress})")
            }

            override fun handleOnBackProgressed(backEvent: BackEventCompat) {
                Log.d(TAG, "Fragment predictive back progressed: ${backEvent.progress}")
            }

            override fun handleOnBackPressed() {
                val childFm = childFragmentManager
                if (childFm.backStackEntryCount > 0) {
                    childFm.popBackStack()
                } else {
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }

            override fun handleOnBackCancelled() {
                Log.d(TAG, "Fragment predictive back cancelled")
            }
        }
        backPressedCallback = callback
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    fun getGuestFragment(): Fragment? = guestFragment
}
