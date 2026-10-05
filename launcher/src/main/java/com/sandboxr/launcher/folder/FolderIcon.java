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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.sandboxr.launcher.DropTarget;
import com.sandboxr.launcher.Launcher;
import com.sandboxr.launcher.dot.FolderDotInfo;
import com.sandboxr.launcher.model.data.FolderInfo;
import com.sandboxr.launcher.model.data.ItemInfo;
import com.sandboxr.launcher.model.data.WorkspaceItemInfo;

import java.util.List;

/**
 * Represents the closed folder icon on the workspace/hotseat.
 * Renders the Liquid Glass preview background, up to 4 mini-app icons, title label,
 * and handles dragging items onto the folder to append them.
 */
public class FolderIcon extends FrameLayout implements FolderInfo.FolderListener, DropTarget, View.OnClickListener {

    private Launcher mLauncher;
    private FolderInfo mInfo;
    private Folder mFolder;

    private final PreviewBackground mBackground = new PreviewBackground();
    private final PreviewItemManager mItemManager = new PreviewItemManager(this, mBackground);
    private TextView mLabel;

    public FolderIcon(Context context) {
        this(context, null);
    }

    public FolderIcon(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FolderIcon(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setWillNotDraw(false);
        setOnClickListener(this);

        int iconSize = (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 56f, getResources().getDisplayMetrics()
        );
        mBackground.setup(getContext(), this, iconSize);

        mLabel = new TextView(getContext());
        mLabel.setTextColor(Color.WHITE);
        mLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        mLabel.setGravity(Gravity.CENTER_HORIZONTAL);
        mLabel.setSingleLine(true);
        mLabel.setEllipsize(android.text.TextUtils.TruncateAt.END);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        addView(mLabel, lp);
    }

    public static FolderIcon inflateFolderIcon(Launcher launcher, ViewGroup parent, FolderInfo folderInfo) {
        FolderIcon icon = new FolderIcon(launcher);
        icon.bind(launcher, folderInfo);
        return icon;
    }

    public void bind(Launcher launcher, FolderInfo info) {
        mLauncher = launcher;
        mInfo = info;
        setTag(info);
        info.addListener(this);
        setText(info.title);
        invalidate();
    }

    public void setFolder(Folder folder) {
        mFolder = folder;
    }

    public Folder getFolder() {
        return mFolder;
    }

    public FolderInfo getFolderInfo() {
        return mInfo;
    }

    public PreviewBackground getPreviewBackground() {
        return mBackground;
    }

    public PreviewItemManager getPreviewItemManager() {
        return mItemManager;
    }

    public void setText(CharSequence text) {
        if (mLabel != null) {
            mLabel.setText(text != null ? text : "");
        }
    }

    public CharSequence getText() {
        return mLabel != null ? mLabel.getText() : "";
    }

    @Override
    public void onClick(View v) {
        if (mFolder != null) {
            mFolder.open();
        } else if (mLauncher != null && mInfo != null) {
            Folder folder = Folder.fromXml(mLauncher);
            folder.setFolderIcon(this);
            folder.bind(mInfo);
            setFolder(folder);
            folder.open();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = getMeasuredWidth();
        int iconSize = mBackground.previewSize;
        mBackground.basePreviewOffsetX = (width - iconSize) / 2;
        mBackground.basePreviewOffsetY = (int) (4 * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mBackground.drawBackground(canvas);
        mItemManager.draw(canvas, mInfo);
        mBackground.drawBackgroundStroke(canvas);
    }

    // --- FolderInfo.FolderListener Callbacks ---

    @Override
    public void onAdd(ItemInfo item, int rank) {
        invalidate();
    }

    @Override
    public void onRemove(List<ItemInfo> items) {
        invalidate();
    }

    @Override
    public void onTitleChanged(CharSequence title) {
        setText(title);
    }

    @Override
    public void onItemsChanged(boolean animate) {
        invalidate();
    }

    // --- DropTarget Implementation ---

    @Override
    public boolean isDropEnabled() {
        return true;
    }

    @Override
    public boolean acceptDrop(DropTarget.DragObject dragObject) {
        if (dragObject == null || dragObject.dragInfo == null) return false;
        return FolderInfo.willAcceptItemType(dragObject.dragInfo.itemType);
    }

    @Override
    public void onDragEnter(DropTarget.DragObject dragObject) {
        mBackground.animateToAccept();
    }

    @Override
    public void onDragOver(DropTarget.DragObject dragObject) {}

    @Override
    public void onDragExit(DropTarget.DragObject dragObject) {
        mBackground.animateToRest();
    }

    @Override
    public void onDrop(DropTarget.DragObject dragObject, Object options) {
        mBackground.animateToRest();
        if (dragObject != null && dragObject.dragInfo != null && mInfo != null) {
            ItemInfo item = dragObject.dragInfo;
            mInfo.add(item, true);
        }
    }

    @Override
    public void getHitRectRelativeToDragLayer(Rect outRect) {
        getHitRect(outRect);
    }

    public void updateDotInfo() {
        invalidate();
    }
}
