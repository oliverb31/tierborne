package com.ollie.tierborne.client.screen;

enum RpgTab {
    PLAYER("Player", 68),
    CLASS_SKILLTREE("Class Skilltree", 104),
    GENERAL_SKILLTREE("General Skilltree", 110);

    final String label;
    final int width;

    RpgTab(String label, int width) {
        this.label = label;
        this.width = width;
    }
}
