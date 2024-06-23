package com.example.homely;

import androidx.fragment.app.Fragment;

public class DeviceCategoryItem {
    private final String title;
    private final String subtitle;
    private final int backgroundColor;
    private final int iconResId;

    public DeviceCategoryItem(String title, String subtitle, int backgroundColor, int iconResId) {
        this.title = title;
        this.subtitle = subtitle;
        this.backgroundColor = backgroundColor;
        this.iconResId = iconResId;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public int getIconResId() {
        return iconResId;
    }
}
