/*
 * Copyright (C) 2008 The Android Open Source Project
 * Copyright (C) 2026 Sandboxr Platform
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sandboxr.launcher.folder;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import com.android.launcher3.AbstractFloatingView;
import com.sandboxr.launcher.BubbleTextView;
import com.sandboxr.launcher.DragSource;
import com.sandboxr.launcher.DropTarget;
import com.sandboxr.launcher.Launcher;
import com.sandboxr.launcher.dragndrop.DragController;
import com.sandboxr.launcher.dragndrop.DragLayer;
import com.sandboxr.launcher.dragndrop.DragOptions;
import com.sandboxr.launcher.model.data.AppInfo;
import com.sandboxr.launcher.model.data.FolderInfo;
import com.sandboxr.launcher.model.data.ItemInfo;
import com.sandboxr.launcher.model.data.WorkspaceItemInfo;
import com.sandboxr.launcher.util.LauncherBindableItemsContainer.ItemOperator;
import com.sandboxr.launcher.views.ActivityContext;

import java.util.List;

/**
 * Main floating Folder container displayed over the workspace upon FolderIcon tap.
 * Houses the editable folder title (FolderNameEditText), paginated items grid (FolderPagedView),
 * and implements physics-based spring animations with Liquid Glass visual aesthetics.
 */
public class Folder extends AbstractFloatingView
        implements FolderInfo.FolderListener, DropTarget, DragSource,
        View.OnClickListener, View.OnLongClickListener, DragController.DragListener {

    public static final int STATE_NONE = -1;
    public static final int STATE_SMALL = 0;
    public static final int STATE_ANIMATING = 1;
    public static final int STATE_OPEN = 2;


    public boolean isDestroyed() {
        return false;
    }

    private Launcher mLauncher;
    private DragController mDragController;
    private FolderInfo mInfo;
    private FolderIcon mFolderIcon;

    private FolderNameEditText mFolderName;
    private FolderPagedView mContent;
    private FolderAnimationManager mOpenAnimationManager;
    private FolderAnimationManager mCloseAnimationManager;

    private int mState = STATE_NONE;
    private final Rect mTempRect = new Rect();

    public Folder(Context context) {
        this(context, null);
    }

    public Folder(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Folder(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setOrientation(VERTICAL);
        setFocusable(true);
        setFocusableInTouchMode(true);

        // Liquid Glass surface dialog styling
        GradientDrawable background = new GradientDrawable();
        background.setColor(0xE612121A); // Dark glass base
        background.setCornerRadius(32f);
        background.setStroke(2, 0x40FFFFFF); // Specular glint
        setBackground(background);

        int padding = (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 16f, getResources().getDisplayMetrics()
        );
        setPadding(padding, padding, padding, padding);

        // Header: FolderNameEditText
        mFolderName = new FolderNameEditText(getContext());
        mFolderName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f);
        mFolderName.setGravity(Gravity.CENTER_HORIZONTAL);
        mFolderName.setBackground(null);
        mFolderName.setOnTitleChangeListener(newTitle -> {
            if (mInfo != null) {
                mInfo.setTitle(newTitle, mLauncher != null ? mLauncher.getModelWriter() : null);
            }
        });

        LinearLayout.LayoutParams headerLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        headerLp.bottomMargin = (int) (8 * getResources().getDisplayMetrics().density);
        addView(mFolderName, headerLp);

        // Content: FolderPagedView
        mContent = new FolderPagedView(getContext());
        mContent.setFolder(this);
        LinearLayout.LayoutParams contentLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        addView(mContent, contentLp);
    }

    public static Folder fromXml(Launcher launcher) {
        Folder folder = new Folder(launcher);
        folder.setLauncher(launcher);
        return folder;
    }

    public void setLauncher(Launcher launcher) {
        mLauncher = launcher;
        mDragController = launcher.getDragController();
    }

    public Launcher getLauncher() {
        return mLauncher;
    }

    public void setFolderIcon(FolderIcon icon) {
        mFolderIcon = icon;
    }

    public FolderIcon getFolderIcon() {
        return mFolderIcon;
    }

    public FolderInfo getInfo() {
        return mInfo;
    }

    public void removeFolderContent(boolean animate, ItemInfo item) {
        if (mInfo != null) {
            mInfo.remove(item, animate);
        }
    }

    public FolderPagedView getContent() {
        return mContent;
    }

    public FolderNameEditText getFolderNameEditText() {
        return mFolderName;
    }

    public void bind(FolderInfo info) {
        mInfo = info;
        mInfo.addListener(this);
        mFolderName.setText(info.title != null ? info.title : "");
        mContent.bindItems(info.getContents());
    }

    public void open() {
        if (mIsOpen || mLauncher == null) return;
        mIsOpen = true;
        mState = STATE_ANIMATING;

        DragLayer dragLayer = mLauncher.getDragLayer();
        if (dragLayer == null) return;

        if (getParent() == null) {
            dragLayer.addView(this);
        }

        // Center folder dialog in DragLayer
        int screenWidth = dragLayer.getWidth() > 0 ? dragLayer.getWidth() : 1080;
        int screenHeight = dragLayer.getHeight() > 0 ? dragLayer.getHeight() : 1920;

        int folderWidth = (int) (screenWidth * 0.88f);
        int folderHeight = (int) (screenHeight * 0.50f);

        DragLayer.LayoutParams lp = new DragLayer.LayoutParams(folderWidth, folderHeight);
        lp.x = (screenWidth - folderWidth) / 2;
        lp.y = (screenHeight - folderHeight) / 2;
        lp.customPosition = true;
        setLayoutParams(lp);

        measure(
            MeasureSpec.makeMeasureSpec(folderWidth, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(folderHeight, MeasureSpec.EXACTLY)
        );
        layout(lp.x, lp.y, lp.x + folderWidth, lp.y + folderHeight);

        mOpenAnimationManager = new FolderAnimationManager(this, true);
        mOpenAnimationManager.animate(mFolderIcon, () -> {
            mState = STATE_OPEN;
        });
    }

    public void animateOpen() {
        open();
    }

    public void animateClose() {
        close(true);
    }

    @Override
    protected void handleClose(boolean animate) {
        if (!mIsOpen) return;
        mIsOpen = false;
        mState = STATE_ANIMATING;

        if (animate && mFolderIcon != null) {
            mCloseAnimationManager = new FolderAnimationManager(this, false);
            mCloseAnimationManager.animate(mFolderIcon, () -> {
                mState = STATE_SMALL;
                removeFromDragLayer();
            });
        } else {
            mState = STATE_SMALL;
            setVisibility(View.GONE);
            removeFromDragLayer();
        }
    }

    private void removeFromDragLayer() {
        if (getParent() instanceof ViewGroup) {
            ((ViewGroup) getParent()).removeView(this);
        }
    }

    @Override
    public boolean isOfType(int type) {
        return (type & TYPE_FOLDER) != 0;
    }

    @Override
    public void onClick(View v) {
        Object tag = v.getTag();
        if (tag instanceof WorkspaceItemInfo && mLauncher != null) {
            WorkspaceItemInfo wii = (WorkspaceItemInfo) tag;
            mLauncher.startActivitySafely(v, wii.getIntent(), wii);
            close(true);
        } else if (tag instanceof AppInfo && mLauncher != null) {
            AppInfo ai = (AppInfo) tag;
            mLauncher.startActivitySafely(v, ai.getIntent(), ai);
            close(true);
        }
    }

    @Override
    public boolean onLongClick(View v) {
        // Begin dragging item from folder
        Object tag = v.getTag();
        if (tag instanceof ItemInfo && mDragController != null) {
            close(true);
            mDragController.startDrag(
                v, this, (ItemInfo) tag, new DragOptions()
            );
            return true;
        }
        return false;
    }

    // --- FolderInfo.FolderListener Callbacks ---

    @Override
    public void onAdd(ItemInfo item, int rank) {
        mContent.bindItems(mInfo != null ? mInfo.getContents() : null);
    }

    @Override
    public void onRemove(List<ItemInfo> items) {
        mContent.bindItems(mInfo != null ? mInfo.getContents() : null);
        if (mInfo != null && mInfo.getContents().size() <= 1) {
            // Auto unroll folder if only 1 or 0 items remain
        }
    }

    @Override
    public void onTitleChanged(CharSequence title) {
        if (mFolderName != null && !mFolderName.hasFocus()) {
            mFolderName.setText(title);
        }
    }

    @Override
    public void onItemsChanged(boolean animate) {
        mContent.bindItems(mInfo != null ? mInfo.getContents() : null);
    }

    // --- DropTarget Implementation ---

    @Override
    public boolean isDropEnabled() {
        return mState == STATE_OPEN;
    }

    @Override
    public boolean acceptDrop(DropTarget.DragObject dragObject) {
        if (dragObject == null || dragObject.dragInfo == null) return false;
        return FolderInfo.willAcceptItemType(dragObject.dragInfo.itemType);
    }

    @Override
    public void onDragEnter(DropTarget.DragObject dragObject) {}

    @Override
    public void onDragOver(DropTarget.DragObject dragObject) {}

    @Override
    public void onDragExit(DropTarget.DragObject dragObject) {}

    @Override
    public void onDrop(DropTarget.DragObject dragObject, Object options) {
        if (dragObject != null && dragObject.dragInfo != null && mInfo != null) {
            mInfo.add(dragObject.dragInfo, true);
        }
    }

    @Override
    public void getHitRectRelativeToDragLayer(Rect outRect) {
        getHitRect(outRect);
    }

    // --- DragSource Implementation ---

    @Override
    public void onDropCompleted(View target, DropTarget.DragObject d, boolean success) {
        if (success && d != null && d.dragInfo != null && mInfo != null) {
            if (target != this && target != mFolderIcon) {
                mInfo.remove(d.dragInfo, true);
            }
        }
    }

    // --- DragController.DragListener Callbacks ---

    @Override
    public void onDragStart(DropTarget.DragObject dragObject, DragOptions options) {}

    @Override
    public void onDragEnd() {}

    public static Folder getOpen(ActivityContext activityContext) {
        return AbstractFloatingView.getOpenView(activityContext, AbstractFloatingView.TYPE_FOLDER);
    }

    public View mapOverItems(ItemOperator op) {
        return null;
    }
}
