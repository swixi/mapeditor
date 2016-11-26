package datastructures;

import java.awt.geom.Rectangle2D;

public abstract class MapEntity
{
    public enum EntityType
    {
        BACKGROUND,
        COLLIDABLE,
        SPAWN;
    }
    public abstract int getX();
    public abstract int getY();
    public abstract void setX(int x);
    public abstract void setY(int y);
    public abstract Rectangle2D.Double getHitBox();
    public abstract String getImageName();
    public abstract void setHitBox();
}
