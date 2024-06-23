package com.example.homely;

import lombok.Getter;

@Getter
public class SettingsFragmentListItem {
    String name;
    int iconResourceId;

    public SettingsFragmentListItem(String name, int iconResourceId) {
        this.name = name;
        this.iconResourceId = iconResourceId;
    }
}
