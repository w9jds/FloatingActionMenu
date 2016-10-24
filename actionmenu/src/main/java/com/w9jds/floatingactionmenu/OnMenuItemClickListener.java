package com.w9jds.floatingactionmenu;

import android.support.design.widget.FloatingActionButton;

import com.w9jds.FloatingActionMenu;

public interface OnMenuItemClickListener {
    void onMenuItemClick(FloatingActionMenu floatingActionMenu, int index, FloatingActionButton item);
}