package xyz.zedler.patrick.grocy.model;

public abstract class GroupedListItem {

    public static final int TYPE_HEADER = 0;
    public static final int TYPE_ENTRY = 1;

    abstract public int getType();
}
