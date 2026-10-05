/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.sandboxr.launcher.shortcuts;

import android.content.Context;
import android.content.pm.ShortcutInfo;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.sandboxr.launcher.BubbleTextView;
import com.sandboxr.launcher.R;
import com.sandboxr.launcher.model.data.WorkspaceItemInfo;
import com.sandboxr.launcher.popup.PopupContainerWithArrow;
import com.sandboxr.launcher.views.ActivityContext;
import com.sandboxr.launcher.views.BubbleTextHolder;

/**
 * A {@link FrameLayout} that contains an icon and a {@link BubbleTextView} for text.
 */
public class DeepShortcutView extends FrameLayout implements BubbleTextHolder {

    private final Drawable mTransparentDrawable = new ColorDrawable(Color.TRANSPARENT);

    private BubbleTextView mBubbleText;
    private View mIconView;
    private ImageView mAddButton;

    private WorkspaceItemInfo mInfo;
    private ShortcutInfo mDetail;

    public DeepShortcutView(Context context) {
        this(context, null, 0);
    }

    public DeepShortcutView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DeepShortcutView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mBubbleText = findViewById(R.id.bubble_text);
        if (mBubbleText != null) {
            mBubbleText.setHideBadge(true);
        }
        mIconView = findViewById(R.id.icon);
        mAddButton = findViewById(R.id.deep_shortcut_add_button);
        tryUpdateTextBackground();
    }

    @Override
    public void setBackground(Drawable background) {
        super.setBackground(background);
        tryUpdateTextBackground();
    }

    private void tryUpdateTextBackground() {
        if (mBubbleText != null) {
            mBubbleText.setBackground(mTransparentDrawable);
        }
    }

    @Override
    public BubbleTextView getBubbleText() {
        return mBubbleText;
    }

    public void setBubbleText(BubbleTextView bubbleText) {
        mBubbleText = bubbleText;
    }

    public View getIconView() {
        return mIconView;
    }

    public ImageView getAddButton() {
        return mAddButton;
    }

    public void setWillDrawIcon(boolean willDraw) {
        if (mIconView != null) {
            mIconView.setVisibility(willDraw ? VISIBLE : INVISIBLE);
        }
    }

    public Point getIconCenter() {
        Point point = new Point();
        if (mIconView != null) {
            point.x = mIconView.getLeft() + mIconView.getWidth() / 2;
            point.y = mIconView.getTop() + mIconView.getHeight() / 2;
        } else {
            point.x = getWidth() / 2;
            point.y = getHeight() / 2;
        }
        return point;
    }

    public WorkspaceItemInfo getFinalInfo() {
        return mInfo;
    }

    public void applyShortcutInfo(WorkspaceItemInfo info, ShortcutInfo detail) {
        applyShortcutInfo(info, detail, null, null);
    }

    public void applyShortcutInfo(WorkspaceItemInfo info, ShortcutInfo detail,
            PopupContainerWithArrow<?> container, ActivityContext activityContext) {
        mInfo = info;
        mDetail = detail;
        if (mBubbleText != null) {
            mBubbleText.applyFromWorkspaceItem(info, false);
            mBubbleText.setIconVisible(false);
        }
        if (mIconView != null && info.bitmap != null && info.bitmap.icon != null) {
            mIconView.setBackground(new android.graphics.drawable.BitmapDrawable(getResources(), info.bitmap.icon));
        }
        setTag(info);
        if (container != null) {
            setOnClickListener(container.getItemClickListener());
        }
        if (mAddButton != null) {
            if (detail != null) {
                mAddButton.setVisibility(VISIBLE);
                mAddButton.setOnClickListener(v -> {
                    com.sandboxr.launcher.model.ItemInstallQueue.INSTANCE.get(getContext()).queueItem(detail);
                    if (container != null) {
                        container.close(true);
                    }
                });
            } else {
                mAddButton.setVisibility(GONE);
            }
        }
    }

    public ShortcutInfo getDetail() {
        return mDetail;
    }
}
